package org.example.sampleordersystem.service;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.repository.InMemoryOrderRepository;
import org.example.sampleordersystem.repository.InMemoryProductionRepository;
import org.example.sampleordersystem.repository.InMemorySampleRepository;
import org.example.sampleordersystem.repository.OrderRepository;
import org.example.sampleordersystem.repository.ProductionRepository;
import org.example.sampleordersystem.repository.SampleRepository;
import org.example.sampleordersystem.util.FixedTimeProvider;
import org.example.sampleordersystem.util.OrderIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private ProductionRepository prodRepo;
    private FixedTimeProvider timeProvider;
    private OrderIdGenerator gen;
    private SampleService sampleService;
    private ProductionService prodService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        sampleRepo = new InMemorySampleRepository();
        orderRepo = new InMemoryOrderRepository();
        prodRepo = new InMemoryProductionRepository();
        timeProvider = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 0, 0));
        gen = new OrderIdGenerator(0, timeProvider);
        sampleService = new SampleService(sampleRepo);
        prodService = new ProductionService(prodRepo, orderRepo, sampleRepo, timeProvider, 1.0);
        orderService = new OrderService(sampleRepo, orderRepo, prodService, gen);
    }

    @Test
    void placeOrder_정상주문() {
        sampleService.register("S001", "웨이퍼", 30, 0.9, 100);
        Order order = orderService.placeOrder("S001", "홍길동", 10);
        assertNotNull(order);
        assertEquals(OrderStatus.RESERVED, order.getStatus());
        assertEquals("홍길동", order.getCustomerName());
        assertEquals(10, order.getQuantity());
    }

    @Test
    void placeOrder_미등록시료_예외() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> orderService.placeOrder("NONE", "홍길동", 10));
        assertEquals("등록되지 않은 시료입니다", ex.getMessage());
    }

    @Test
    void approve_재고충분_CONFIRMED() {
        sampleService.register("S001", "웨이퍼", 30, 0.9, 100);
        orderService.placeOrder("S001", "홍길동", 10);
        String orderId = orderRepo.findAll().get(0).getOrderId();
        orderService.approve(orderId);
        Order approved = orderRepo.findById(orderId).get();
        assertEquals(OrderStatus.CONFIRMED, approved.getStatus());
        assertEquals(90, sampleRepo.findById("S001").get().getStock());
    }

    @Test
    void approve_재고부족_PRODUCING() {
        sampleService.register("S001", "웨이퍼", 30, 0.9, 5);
        orderService.placeOrder("S001", "홍길동", 10);
        String orderId = orderRepo.findAll().get(0).getOrderId();
        orderService.approve(orderId);
        Order approved = orderRepo.findById(orderId).get();
        assertEquals(OrderStatus.PRODUCING, approved.getStatus());
        assertEquals(1, prodRepo.findAll().size());
    }

    @Test
    void reject_REJECTED전환() {
        sampleService.register("S001", "웨이퍼", 30, 0.9, 100);
        orderService.placeOrder("S001", "홍길동", 10);
        String orderId = orderRepo.findAll().get(0).getOrderId();
        orderService.reject(orderId);
        Order rejected = orderRepo.findById(orderId).get();
        assertEquals(OrderStatus.REJECTED, rejected.getStatus());
    }

    @Test
    void findByStatus_RESERVED주문조회() {
        sampleService.register("S001", "웨이퍼", 30, 0.9, 100);
        orderService.placeOrder("S001", "홍길동", 10);
        orderService.placeOrder("S001", "김철수", 5);
        String firstOrderId = orderRepo.findAll().get(0).getOrderId();
        orderService.approve(firstOrderId);
        assertEquals(1, orderService.findByStatus(OrderStatus.RESERVED).size());
        assertEquals(1, orderService.findByStatus(OrderStatus.CONFIRMED).size());
    }

    @Test
    void findAll_전체주문조회() {
        sampleService.register("S001", "웨이퍼", 30, 0.9, 100);
        orderService.placeOrder("S001", "홍길동", 10);
        orderService.placeOrder("S001", "김철수", 5);
        assertEquals(2, orderService.findAll().size());
    }

    @Test
    void approve_존재하지않는주문_예외() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> orderService.approve("ORD-NONE"));
        assertEquals("존재하지 않는 주문입니다", ex.getMessage());
    }

    @Test
    void approve_시료없는주문_예외() {
        // 주문을 직접 저장 후 시료를 삭제하는 상황 시뮬레이션 불가하므로
        // 대신 InMemoryOrderRepository에 직접 저장하여 테스트
        sampleService.register("S001", "웨이퍼", 30, 0.9, 100);
        orderService.placeOrder("S001", "홍길동", 10);
        String orderId = orderRepo.findAll().get(0).getOrderId();
        // sampleRepo에서 시료를 직접 삭제하는 방법이 없으므로
        // 대신 다른 sampleId로 주문된 상황을 만든다: 직접 Order를 저장
        Order orphanOrder = new Order("ORD-99990101-9999", "DELETED_SAMPLE", "테스트", 5,
            LocalDateTime.of(2024, 1, 1, 0, 0));
        orderRepo.save(orphanOrder);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> orderService.approve("ORD-99990101-9999"));
        assertEquals("시료를 찾을 수 없습니다", ex.getMessage());
    }

    @Test
    void reject_존재하지않는주문_예외() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> orderService.reject("ORD-NONE"));
        assertEquals("존재하지 않는 주문입니다", ex.getMessage());
    }

    @Test
    void approve_재고부족시_actualQty가_공식대로_계산됨() {
        // 재고 5, 주문 10, shortage=5, yield=0.9 → actualQty=ceil(5/(0.9*0.9))=ceil(6.17)=7
        sampleService.register("S001", "웨이퍼", 30, 0.9, 5);
        orderService.placeOrder("S001", "홍길동", 10);
        String orderId = orderRepo.findAll().get(0).getOrderId();
        orderService.approve(orderId);

        assertEquals(1, prodRepo.findAll().size());
        assertEquals(7, prodRepo.findAll().get(0).getActualQty());
        assertEquals(5, prodRepo.findAll().get(0).getShortage());
    }
}
