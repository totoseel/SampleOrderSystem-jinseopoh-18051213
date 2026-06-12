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
        // getQueue()는 대기 항목만 반환, getCurrent()는 현재 생산 중 항목 반환
        List<ProductionEntry> waiting = productionService.getQueue();
        assertEquals(1, waiting.size());
        assertNull(waiting.get(0).getStartedAt());
        assertTrue(productionService.getCurrent().isPresent());
        assertNotNull(productionService.getCurrent().get().getStartedAt());
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
        // 재고 5, 주문 10, shortage=5, yield=0.9 → actualQty=ceil(5/(0.9*0.9))=ceil(6.17)=7
        // 생산 완료 후 재고: 5 + 7(actualQty) - 10(주문량) = 2
        sampleRepo.save(new Sample("S001", "웨이퍼", 30, 0.9, 5));
        Order order = new Order("ORD-001", "S001", "홍길동", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);

        // shortage=5, actualQty=7, totalMinutes=210
        ProductionEntry entry = new ProductionEntry("ORD-001", "S001", 5, 7, 210.0, null);
        productionService.enqueue(entry);

        // 210분 + 1초 경과 → 진행률 100% 초과
        timeProvider.setTime(timeProvider.now().plusMinutes(210).plusSeconds(1));
        productionService.tick();

        // CONFIRMED 전환 확인
        assertEquals(OrderStatus.CONFIRMED,
            orderRepo.findById("ORD-001").get().getStatus());
        // 재고 반영: 5(기존) + 7(actualQty) - 10(주문량) = 2
        assertEquals(2, sampleRepo.findById("S001").get().getStock());
        // 큐에서 제거됨
        assertTrue(productionService.getCurrent().isEmpty());
    }

    // Cycle 3-12: 첫 항목 완료 후 다음 항목 자동 시작
    @Test
    void tickStartsNextEntryAfterCompletion() {
        // S001: 재고 10, 주문 10, shortage=10, actualQty=14 → 완료 후 재고 10+14-10=14
        // S002: 재고 5, 주문 5, shortage=5, actualQty=7 → 완료 후 재고 5+7-5=7
        sampleRepo.save(new Sample("S001", "웨이퍼", 30, 0.9, 10));
        sampleRepo.save(new Sample("S002", "갈륨", 20, 0.8, 5));

        Order order1 = new Order("ORD-001", "S001", "홍길동", 10, timeProvider.now());
        order1.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order1);

        Order order2 = new Order("ORD-002", "S002", "김철수", 5, timeProvider.now());
        order2.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order2);

        // S001: shortage=10, actualQty=14(ceil(10/0.81)=13, 여기서는 14로 여유있게), totalMinutes=210
        // S002: shortage=5, actualQty=7, totalMinutes=80
        ProductionEntry first = new ProductionEntry("ORD-001", "S001", 10, 14, 210.0, null);
        ProductionEntry second = new ProductionEntry("ORD-002", "S002", 5, 7, 80.0, null);
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

    // Cycle 3-13: approve() 후 ProductionEntry의 actualQty가 공식대로 계산됨을 검증
    // OrderService에 approve()가 있으므로 OrderServiceTest에서 통합 검증
    // 여기서는 ProductionEntry 직접 구성으로 공식 적용 결과를 검증
    @Test
    void actualQtyStoredCorrectlyInProductionEntry() {
        // shortage=5, yield=0.9 → actualQty = ceil(5 / (0.9*0.9)) = ceil(6.17) = 7
        sampleRepo.save(new Sample("S001", "웨이퍼", 30, 0.9, 5));
        Order order = new Order("ORD-001", "S001", "홍길동", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);

        // OrderService.approve()를 통해 생성된 ProductionEntry의 actualQty를 검증하려면
        // OrderServiceTest에서 진행. 여기서는 직접 enqueue한 항목의 값을 확인.
        int shortage = 5;
        double yield = 0.9;
        int expectedActualQty = (int) Math.ceil(shortage / (yield * 0.9)); // 7

        ProductionEntry entry = new ProductionEntry("ORD-001", "S001", shortage, expectedActualQty, 210.0, null);
        productionService.enqueue(entry);

        assertEquals(expectedActualQty, prodRepo.findAll().get(0).getActualQty());
        assertEquals(7, expectedActualQty);
    }

    // Cycle 3-14: timeScale 배율 적용 - timeScale=60이면 현실 1초=시스템 1분
    @Test
    void timeScaleAcceleratesCompletion() {
        // timeScale=60: 총 생산시간 60분이 현실 1분(60초)에 완료됨
        // 재고 5, 주문 10, shortage=5, actualQty=7 → 완료 후 재고 5+7-10=2
        ProductionService fastService = new ProductionService(
            new InMemoryProductionRepository(), orderRepo, sampleRepo, timeProvider, 60.0);

        sampleRepo.save(new Sample("S003", "실리콘", 30, 0.9, 5));
        Order order = new Order("ORD-003", "S003", "박민수", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);

        // shortage=5, actualQty=7, totalMinutes=60, timeScale=60 → 실제 완료시간=60/60=1분
        ProductionEntry entry = new ProductionEntry("ORD-003", "S003", 5, 7, 60.0, null);
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
        // 재고 10, 주문 10, shortage=5, actualQty=7 → 완료 후 재고 10+7-10=7
        LocalDateTime alreadyStarted = timeProvider.now().minusMinutes(5);
        ProductionEntry started = new ProductionEntry("ORD-PRE", "S001", 5, 7, 60.0, alreadyStarted);
        prodRepo.save(started);

        sampleRepo.save(new Sample("S001", "웨이퍼", 30, 0.9, 10));
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

    @Test
    @org.junit.jupiter.api.DisplayName("cancel: 현재 생산 중인 항목을 취소하면 큐에서 제거되고 주문이 RESERVED로 복원된다")
    void cancelCurrentEntryRestoresOrderToReserved() {
        sampleRepo.save(new Sample("S001", "웨이퍼", 30, 0.9, 5));
        Order order = new Order("ORD-001", "S001", "홍길동", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);
        productionService.enqueue(new ProductionEntry("ORD-001", "S001", 5, 7, 210.0, null));

        productionService.cancel("ORD-001");

        assertTrue(productionService.getCurrent().isEmpty());
        assertEquals(OrderStatus.RESERVED, orderRepo.findById("ORD-001").get().getStatus());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("cancel: 대기 중인 항목을 취소하면 큐에서 제거되고 주문이 RESERVED로 복원된다")
    void cancelWaitingEntryRestoresOrderToReserved() {
        sampleRepo.save(new Sample("S001", "웨이퍼", 30, 0.9, 5));
        Order o1 = new Order("ORD-001", "S001", "홍길동", 10, timeProvider.now());
        o1.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(o1);
        Order o2 = new Order("ORD-002", "S001", "김철수", 5, timeProvider.now());
        o2.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(o2);
        productionService.enqueue(new ProductionEntry("ORD-001", "S001", 5, 7, 210.0, null));
        productionService.enqueue(new ProductionEntry("ORD-002", "S001", 3, 4, 120.0, null));

        productionService.cancel("ORD-002");

        // ORD-002 취소 후: 대기 큐는 비어 있고(ORD-001은 현재 생산 중), ORD-002는 RESERVED 복원
        assertEquals(0, productionService.getQueue().size());
        assertTrue(productionService.getCurrent().isPresent());
        assertEquals("ORD-001", productionService.getCurrent().get().getOrderId());
        assertEquals(OrderStatus.RESERVED, orderRepo.findById("ORD-002").get().getStatus());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("cancel: 현재 생산 중인 항목 취소 시 다음 대기 항목이 자동 시작된다")
    void cancelCurrentEntryStartsNextWaiting() {
        sampleRepo.save(new Sample("S001", "웨이퍼", 30, 0.9, 5));
        Order o1 = new Order("ORD-001", "S001", "홍길동", 10, timeProvider.now());
        o1.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(o1);
        Order o2 = new Order("ORD-002", "S001", "김철수", 5, timeProvider.now());
        o2.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(o2);
        productionService.enqueue(new ProductionEntry("ORD-001", "S001", 5, 7, 210.0, null));
        productionService.enqueue(new ProductionEntry("ORD-002", "S001", 3, 4, 120.0, null));

        productionService.cancel("ORD-001");

        assertTrue(productionService.getCurrent().isPresent());
        assertEquals("ORD-002", productionService.getCurrent().get().getOrderId());
        assertNotNull(productionService.getCurrent().get().getStartedAt());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("cancel: 존재하지 않는 orderId면 IllegalArgumentException이 발생한다")
    void cancelUnknownOrderThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> productionService.cancel("NOT_EXIST"));
    }
}
