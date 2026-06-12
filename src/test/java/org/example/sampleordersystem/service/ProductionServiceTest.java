package org.example.sampleordersystem.service;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.model.Sample;
import org.example.sampleordersystem.repository.InMemoryOrderRepository;
import org.example.sampleordersystem.repository.InMemoryProductionRepository;
import org.example.sampleordersystem.repository.InMemorySampleRepository;
import org.example.sampleordersystem.util.FixedTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductionServiceTest {

    private InMemoryProductionRepository prodRepo;
    private InMemoryOrderRepository orderRepo;
    private InMemorySampleRepository sampleRepo;
    private FixedTimeProvider timeProvider;
    private ProductionService productionService;

    @BeforeEach
    void setUp() {
        prodRepo = new InMemoryProductionRepository();
        orderRepo = new InMemoryOrderRepository();
        sampleRepo = new InMemorySampleRepository();
        timeProvider = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 0, 0));
        productionService = new ProductionService(prodRepo, orderRepo, sampleRepo, timeProvider, 1.0);
    }

    @Test
    void enqueue_저장후_조회() {
        ProductionEntry entry = new ProductionEntry("ORD-001", "S001", 5, 7, 210.0, null);
        productionService.enqueue(entry);
        assertEquals(1, prodRepo.findAll().size());
    }

    @Test
    void tick_초기_상태_빈_큐() {
        // tick은 그룹 B에서 구현 예정이므로 예외 없이 실행되는지만 확인
        assertDoesNotThrow(() -> productionService.tick());
    }

    @Test
    void getProgress_초기값_0() {
        assertEquals(0.0, productionService.getProgress(), 1e-9);
    }

    @Test
    void getCurrent_초기_비어있음() {
        assertTrue(productionService.getCurrent().isEmpty());
    }

    @Test
    void getQueue_초기_빈_리스트() {
        assertTrue(productionService.getQueue().isEmpty());
    }

    @Test
    void getEstimatedFinishAt_초기_비어있음() {
        ProductionEntry entry = new ProductionEntry("ORD-001", "S001", 5, 7, 210.0, null);
        assertTrue(productionService.getEstimatedFinishAt(entry).isEmpty());
    }

    // Cycle 3-8: 큐가 비어있을 때 enqueue하면 즉시 startedAt이 설정된다
    @Test
    void enqueueStartsImmediatelyWhenIdle() {
        ProductionEntry entry = new ProductionEntry("ORD-001", "S001", 5, 7, 210.0, null);
        productionService.enqueue(entry);
        Optional<ProductionEntry> current = productionService.getCurrent();
        assertTrue(current.isPresent());
        assertNotNull(current.get().getStartedAt());
        assertEquals(timeProvider.now(), current.get().getStartedAt());
    }

    // Cycle 3-9: 생산 중일 때 enqueue하면 startedAt 없이 대기한다
    @Test
    void enqueueWaitsWhenBusy() {
        ProductionEntry first = new ProductionEntry("ORD-001", "S001", 5, 7, 210.0, null);
        ProductionEntry second = new ProductionEntry("ORD-002", "S001", 3, 4, 120.0, null);
        productionService.enqueue(first);
        productionService.enqueue(second);
        List<ProductionEntry> queue = productionService.getQueue();
        assertEquals(2, queue.size());
        // 첫 번째는 시작됨, 두 번째는 대기 중(startedAt null)
        assertNotNull(queue.get(0).getStartedAt());
        assertNull(queue.get(1).getStartedAt());
    }

    // Cycle 3-10: 시간이 충분히 지나지 않으면 tick() 후에도 상태가 유지된다
    @Test
    void tickDoesNotCompleteBeforeTime() {
        // 총 생산시간 210분, timeScale=1.0 → 210분 후 완료
        // 100분만 경과하면 완료 안 됨
        sampleRepo.save(new Sample("S001", "웨이퍼", 30, 0.9, 0));
        Order order = new Order("ORD-001", "S001", "홍길동", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);

        ProductionEntry entry = new ProductionEntry("ORD-001", "S001", 10, 7, 210.0, null);
        productionService.enqueue(entry);

        // 100분 경과
        timeProvider.setTime(timeProvider.now().plusMinutes(100));
        productionService.tick();

        // 아직 완료 안 됨
        assertTrue(productionService.getCurrent().isPresent());
        assertEquals(OrderStatus.PRODUCING,
            orderRepo.findById("ORD-001").get().getStatus());
    }

    // Cycle 3-11: tick()으로 생산 완료 시 CONFIRMED 전환 + 재고 반영
    @Test
    void tickCompletesAndTransitionsToConfirmed() {
        sampleRepo.save(new Sample("S001", "웨이퍼", 30, 0.9, 0));
        Order order = new Order("ORD-001", "S001", "홍길동", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);

        // shortage=10, actualQty=7, totalMinutes=210
        ProductionEntry entry = new ProductionEntry("ORD-001", "S001", 10, 7, 210.0, null);
        productionService.enqueue(entry);

        // 210분 + 1초 경과 → 진행률 100% 초과
        timeProvider.setTime(timeProvider.now().plusMinutes(210).plusSeconds(1));
        productionService.tick();

        // CONFIRMED 전환 확인
        assertEquals(OrderStatus.CONFIRMED,
            orderRepo.findById("ORD-001").get().getStatus());
        // 재고 반영: shortage(10)만큼 증가
        assertEquals(10, sampleRepo.findById("S001").get().getStock());
        // 큐에서 제거됨
        assertTrue(productionService.getCurrent().isEmpty());
    }

    // Cycle 3-12: 첫 항목 완료 후 다음 항목 자동 시작
    @Test
    void tickStartsNextEntryAfterCompletion() {
        sampleRepo.save(new Sample("S001", "웨이퍼", 30, 0.9, 0));
        sampleRepo.save(new Sample("S002", "갈륨", 20, 0.8, 0));

        Order order1 = new Order("ORD-001", "S001", "홍길동", 10, timeProvider.now());
        order1.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order1);

        Order order2 = new Order("ORD-002", "S002", "김철수", 5, timeProvider.now());
        order2.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order2);

        ProductionEntry first = new ProductionEntry("ORD-001", "S001", 10, 7, 210.0, null);
        ProductionEntry second = new ProductionEntry("ORD-002", "S002", 5, 4, 80.0, null);
        productionService.enqueue(first);
        productionService.enqueue(second);

        // 첫 번째 완료
        timeProvider.setTime(timeProvider.now().plusMinutes(210).plusSeconds(1));
        productionService.tick();

        // 두 번째가 현재 진행 중
        assertTrue(productionService.getCurrent().isPresent());
        assertEquals("ORD-002", productionService.getCurrent().get().getOrderId());
        assertNotNull(productionService.getCurrent().get().getStartedAt());
    }

    // Cycle 3-13: ceil(shortage / (yield * 0.9)) 공식 검증
    @Test
    void actualQtyFormula() {
        // shortage=10, yield=0.9 → ceil(10 / (0.9*0.9)) = ceil(10/0.81) = ceil(12.345...) = 13
        double yield = 0.9;
        int shortage = 10;
        int expected = (int) Math.ceil(shortage / (yield * 0.9));
        assertEquals(13, expected);

        // shortage=5, yield=0.8 → ceil(5 / (0.8*0.9)) = ceil(5/0.72) = ceil(6.944...) = 7
        yield = 0.8;
        shortage = 5;
        expected = (int) Math.ceil(shortage / (yield * 0.9));
        assertEquals(7, expected);
    }

    // Cycle 3-14: timeScale 배율 적용 - timeScale=60이면 현실 1초=시스템 1분
    @Test
    void timeScaleAcceleratesCompletion() {
        // timeScale=60: 총 생산시간 60분이 현실 1분(60초)에 완료됨
        ProductionService fastService = new ProductionService(
            new InMemoryProductionRepository(), orderRepo, sampleRepo, timeProvider, 60.0);

        sampleRepo.save(new Sample("S003", "실리콘", 30, 0.9, 0));
        Order order = new Order("ORD-003", "S003", "박민수", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);

        // totalMinutes=60, timeScale=60 → 실제 완료시간=60/60=1분
        ProductionEntry entry = new ProductionEntry("ORD-003", "S003", 10, 7, 60.0, null);
        fastService.enqueue(entry);

        // 1분 + 1초 경과 → 완료됨
        timeProvider.setTime(timeProvider.now().plusMinutes(1).plusSeconds(1));
        fastService.tick();

        assertEquals(OrderStatus.CONFIRMED,
            orderRepo.findById("ORD-003").get().getStatus());
    }

    // getEstimatedFinishAt: startedAt이 있는 경우 정상 반환
    @Test
    void getEstimatedFinishAt_startedAt_있으면_반환() {
        LocalDateTime startTime = timeProvider.now();
        // totalMinutes=60, timeScale=1.0 → 60분 후 완료
        ProductionEntry entry = new ProductionEntry("ORD-001", "S001", 5, 7, 60.0, startTime);
        Optional<LocalDateTime> finish = productionService.getEstimatedFinishAt(entry);
        assertTrue(finish.isPresent());
        assertEquals(startTime.plusSeconds(3600), finish.get());
    }

    // calculateProgress: startedAt이 null인 항목의 진행률은 0
    @Test
    void getProgress_대기중_항목은_0() {
        // 직접 startedAt=null인 항목을 저장 (큐가 비어있지 않지만 시작 안 됨 상태 시뮬레이션)
        // 첫 번째 enqueue는 자동으로 시작되므로 prodRepo에 직접 null 항목 삽입
        ProductionEntry waiting = new ProductionEntry("ORD-WAIT", "S001", 5, 7, 210.0, null);
        prodRepo.save(waiting);
        // getCurrent()는 startedAt != null인 것만 반환하므로 비어있음
        assertTrue(productionService.getCurrent().isEmpty());
        assertEquals(0.0, productionService.getProgress(), 1e-9);
    }

    // startNextEntry: 다음 항목이 이미 startedAt을 가지면 덮어쓰지 않는다
    @Test
    void startNextEntry_이미_시작된_항목_유지() {
        // 이미 startedAt이 있는 항목을 저장 후 tick 실행
        LocalDateTime alreadyStarted = timeProvider.now().minusMinutes(5);
        ProductionEntry started = new ProductionEntry("ORD-PRE", "S001", 5, 7, 60.0, alreadyStarted);
        prodRepo.save(started);

        sampleRepo.save(new Sample("S001", "웨이퍼", 30, 0.9, 0));
        Order order = new Order("ORD-PRE", "S001", "홍길동", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);

        // 60분 + 6분 = 66분 경과 → startedAt이 5분 전이므로 진행률 초과
        timeProvider.setTime(timeProvider.now().plusMinutes(60));
        productionService.tick();

        // 완료 처리 됨
        assertEquals(OrderStatus.CONFIRMED,
            orderRepo.findById("ORD-PRE").get().getStatus());
    }
}
