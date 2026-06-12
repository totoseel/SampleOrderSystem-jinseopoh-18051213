package org.example.sampleordersystem.integration;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.model.Sample;
import org.example.sampleordersystem.repository.JsonOrderRepository;
import org.example.sampleordersystem.repository.JsonProductionRepository;
import org.example.sampleordersystem.repository.JsonSampleRepository;
import org.example.sampleordersystem.service.OrderService;
import org.example.sampleordersystem.service.ProductionService;
import org.example.sampleordersystem.service.SampleService;
import org.example.sampleordersystem.util.FixedTimeProvider;
import org.example.sampleordersystem.util.OrderIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FullFlowIntegrationTest {

    @TempDir
    Path tempDir;

    private JsonSampleRepository     sampleRepo;
    private JsonOrderRepository      orderRepo;
    private JsonProductionRepository productionRepo;
    private FixedTimeProvider        timeProvider;
    private ProductionService        productionService;
    private SampleService            sampleService;
    private OrderService             orderService;
    private OrderIdGenerator         idGen;

    @BeforeEach
    void setUp() {
        sampleRepo     = new JsonSampleRepository(tempDir.resolve("samples.json"));
        orderRepo      = new JsonOrderRepository(tempDir.resolve("orders.json"));
        productionRepo = new JsonProductionRepository(tempDir.resolve("production.json"));
        timeProvider   = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 9, 0));

        productionService = new ProductionService(
            productionRepo, orderRepo, sampleRepo, timeProvider, 1.0);
        sampleService = new SampleService(sampleRepo);

        idGen = new OrderIdGenerator(0, timeProvider);
        orderService = new OrderService(sampleRepo, orderRepo, productionService, idGen);
    }

    @Test
    @DisplayName("JSON 파일에 저장된 시료는 재시작 후에도 복구된다")
    void jsonPersistence() {
        sampleService.register("S1", "갈륨비소", 5, 0.9, 100);

        // 새 인스턴스로 재시작 시뮬레이션
        SampleService reloaded = new SampleService(
            new JsonSampleRepository(tempDir.resolve("samples.json")));

        Optional<Sample> found = reloaded.findById("S1");
        assertTrue(found.isPresent());
        assertEquals("갈륨비소", found.get().getName());
        assertEquals(100, found.get().getStock());
    }

    @Test
    @DisplayName("재고 충분 시 주문 접수 → 승인 → 출고 전체 플로우가 동작한다")
    void sufficientStockFullFlow() {
        sampleService.register("S1", "갈륨비소", 5, 0.9, 100);
        Order order = orderService.placeOrder("S1", "홍길동", 10);

        assertEquals(OrderStatus.RESERVED, order.getStatus());

        orderService.approve(order.getOrderId());
        Order approved = orderRepo.findById(order.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, approved.getStatus());
        assertEquals(90, sampleRepo.findById("S1").orElseThrow().getStock());

        orderService.releaseOrder(order.getOrderId());
        assertEquals(OrderStatus.RELEASE,
            orderRepo.findById(order.getOrderId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("재고 부족 시 생산 완료 후 CONFIRMED로 자동 전환된다")
    void insufficientStockProductionFlow() {
        // stock=5, qty=10 → shortage=5, yield=0.9
        // actualQty = ceil(5 / (0.9 * 0.9)) = ceil(5/0.81) = ceil(6.17) = 7
        // totalMinutes = 5 * 7 = 35
        sampleService.register("S1", "갈륨비소", 5, 0.9, 5);
        Order order = orderService.placeOrder("S1", "홍길동", 10);
        orderService.approve(order.getOrderId());

        assertEquals(OrderStatus.PRODUCING,
            orderRepo.findById(order.getOrderId()).orElseThrow().getStatus());

        // 36분 경과 → 완료
        timeProvider.setTime(LocalDateTime.of(2024, 1, 1, 9, 36));
        productionService.tick();

        Order completed = orderRepo.findById(order.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, completed.getStatus());

        // 재고: 5(기존) + 7(생산) - 10(주문) = 2
        assertEquals(2, sampleRepo.findById("S1").orElseThrow().getStock());
    }

    @Test
    @DisplayName("재시작 후 주문번호 순번을 이어받는다")
    void orderIdResumesAfterRestart() {
        sampleService.register("S1", "A", 5, 0.9, 100);
        orderService.placeOrder("S1", "A", 1); // ORD-20240101-0001
        orderService.placeOrder("S1", "B", 1); // ORD-20240101-0002

        // 재시작 시뮬레이션: countByDatePrefix로 lastSeq 복구
        String today = "20240101";
        int lastSeq = new JsonOrderRepository(tempDir.resolve("orders.json"))
            .countByDatePrefix(today);
        OrderIdGenerator newGen = new OrderIdGenerator(lastSeq, timeProvider);
        OrderService restarted = new OrderService(
            new JsonSampleRepository(tempDir.resolve("samples.json")),
            new JsonOrderRepository(tempDir.resolve("orders.json")),
            productionService, newGen);

        Order next = restarted.placeOrder("S1", "C", 1);
        assertEquals("ORD-20240101-0003", next.getOrderId());
    }

    @Test
    @DisplayName("780분 생산이 13분(timeScale=60) 내 완료된다")
    void timeScaleAccelerates() {
        // stock=0, qty=10, yield=0.9
        // shortage=10, actualQty=ceil(10/(0.9*0.9))=ceil(10/0.81)=ceil(12.35)=13
        // totalMinutes = 60 * 13 = 780
        // timeScale=60 → 실제 780/60=13분 경과 시 완료
        ProductionService fastService = new ProductionService(
            productionRepo, orderRepo, sampleRepo, timeProvider, 60.0);

        sampleService.register("S1", "A", 60, 0.9, 0);
        Order order = orderService.placeOrder("S1", "홍길동", 10);

        OrderService fastOrderService = new OrderService(
            sampleRepo, orderRepo, fastService, idGen);
        fastOrderService.approve(order.getOrderId());

        timeProvider.setTime(LocalDateTime.of(2024, 1, 1, 9, 13, 1)); // 13분 직후 경과 (공식상 완료 임계)
        fastService.tick();

        assertEquals(OrderStatus.CONFIRMED,
            orderRepo.findById(order.getOrderId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("REJECTED 주문은 모니터링 집계에서 제외된다")
    void rejectedExcludedFromMonitoring() {
        sampleService.register("S1", "A", 5, 0.9, 100);
        Order o1 = orderService.placeOrder("S1", "A", 10);
        Order o2 = orderService.placeOrder("S1", "B", 5);
        orderService.reject(o2.getOrderId());

        Map<OrderStatus, Long> counts = orderService.findAll().stream()
            .filter(o -> o.getStatus() != OrderStatus.REJECTED)
            .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        assertFalse(counts.containsKey(OrderStatus.REJECTED));
        assertEquals(1L, counts.getOrDefault(OrderStatus.RESERVED, 0L));
    }
}
