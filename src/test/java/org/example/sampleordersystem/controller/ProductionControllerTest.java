package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.model.Sample;
import org.example.sampleordersystem.repository.InMemoryOrderRepository;
import org.example.sampleordersystem.repository.InMemoryProductionRepository;
import org.example.sampleordersystem.repository.InMemorySampleRepository;
import org.example.sampleordersystem.service.ProductionService;
import org.example.sampleordersystem.util.FixedTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductionControllerTest {

    private InMemoryProductionRepository productionRepo;
    private InMemoryOrderRepository orderRepo;
    private InMemorySampleRepository sampleRepo;
    private FixedTimeProvider timeProvider;
    private ProductionService productionService;

    @BeforeEach
    void setUp() {
        productionRepo = new InMemoryProductionRepository();
        orderRepo = new InMemoryOrderRepository();
        sampleRepo = new InMemorySampleRepository();
        timeProvider = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 10, 0));
        productionService = new ProductionService(productionRepo, orderRepo, sampleRepo, timeProvider, 1.0);
    }

    @Test
    @DisplayName("handleView는 View에 생산현황을 전달하고 '0' 입력으로 돌아간다")
    void handleViewDelegatesToView() {
        FakeView view = new FakeView("0");
        ProductionController controller = new ProductionController(productionService, view);

        assertDoesNotThrow(controller::handleView);
    }

    @Test
    @DisplayName("handleView에서 잘못된 메뉴 입력 시 오류 메시지를 출력한다")
    void handleViewInvalidMenuShowsError() {
        FakeView view = new FakeView("9");
        ProductionController controller = new ProductionController(productionService, view);

        controller.handleView();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("handleView에서 취소 선택 시 큐가 비어있으면 안내 메시지를 출력한다")
    void handleViewCancelWithEmptyQueueShowsMessage() {
        FakeView view = new FakeView("1");
        ProductionController controller = new ProductionController(productionService, view);

        controller.handleView();

        assertTrue(view.getMessages().stream().anyMatch(m -> m.contains("없습니다")));
    }

    @Test
    @DisplayName("handleView에서 취소 선택 후 유효한 번호 입력 시 해당 항목이 취소된다")
    void handleViewCancelValidIndexCancelsEntry() {
        sampleRepo.save(new Sample("S1", "웨이퍼", 30, 0.9, 5));
        Order order = new Order("ORD-001", "S1", "홍길동", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);
        productionService.enqueue(new ProductionEntry("ORD-001", "S1", 5, 7, 210.0, null));

        FakeView view = new FakeView("1", "1");
        ProductionController controller = new ProductionController(productionService, view);

        controller.handleView();

        assertTrue(view.getMessages().stream().anyMatch(m -> m.contains("취소")));
        assertEquals(OrderStatus.RESERVED, orderRepo.findById("ORD-001").get().getStatus());
    }

    @Test
    @DisplayName("handleView에서 취소 선택 후 범위 초과 번호 입력 시 오류를 출력한다")
    void handleViewCancelOutOfRangeIndexShowsError() {
        sampleRepo.save(new Sample("S1", "웨이퍼", 30, 0.9, 5));
        Order order = new Order("ORD-001", "S1", "홍길동", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);
        productionService.enqueue(new ProductionEntry("ORD-001", "S1", 5, 7, 210.0, null));

        FakeView view = new FakeView("1", "99");
        ProductionController controller = new ProductionController(productionService, view);

        controller.handleView();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("handleView에서 취소 선택 후 음수 번호 입력 시 오류를 출력한다")
    void handleViewCancelNegativeIndexShowsError() {
        sampleRepo.save(new Sample("S1", "웨이퍼", 30, 0.9, 5));
        Order order = new Order("ORD-001", "S1", "홍길동", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);
        productionService.enqueue(new ProductionEntry("ORD-001", "S1", 5, 7, 210.0, null));

        FakeView view = new FakeView("1", "0");
        ProductionController controller = new ProductionController(productionService, view);

        controller.handleView();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("handleView에서 취소 선택 후 숫자가 아닌 입력 시 오류를 출력한다")
    void handleViewCancelNonNumericInputShowsError() {
        sampleRepo.save(new Sample("S1", "웨이퍼", 30, 0.9, 5));
        Order order = new Order("ORD-001", "S1", "홍길동", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);
        productionService.enqueue(new ProductionEntry("ORD-001", "S1", 5, 7, 210.0, null));

        FakeView view = new FakeView("1", "abc");
        ProductionController controller = new ProductionController(productionService, view);

        controller.handleView();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("handleView에서 cancel이 IllegalArgumentException을 던지면 오류를 출력한다")
    void handleViewCancelServiceThrowsIllegalArgumentShowsError() {
        sampleRepo.save(new Sample("S1", "웨이퍼", 30, 0.9, 5));
        Order order = new Order("ORD-001", "S1", "홍길동", 10, timeProvider.now());
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);
        productionService.enqueue(new ProductionEntry("ORD-001", "S1", 5, 7, 210.0, null));

        ProductionService throwingService = new ProductionService(
                productionRepo, orderRepo, sampleRepo, timeProvider, 1.0) {
            @Override
            public void cancel(String orderId) {
                throw new IllegalArgumentException("테스트 오류");
            }
        };

        FakeView view = new FakeView("1", "1");
        ProductionController controller = new ProductionController(throwingService, view);

        controller.handleView();

        assertFalse(view.getErrors().isEmpty());
    }
}
