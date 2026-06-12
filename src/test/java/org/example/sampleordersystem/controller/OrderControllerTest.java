package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.repository.InMemoryOrderRepository;
import org.example.sampleordersystem.repository.InMemoryProductionRepository;
import org.example.sampleordersystem.repository.InMemorySampleRepository;
import org.example.sampleordersystem.service.OrderService;
import org.example.sampleordersystem.service.ProductionService;
import org.example.sampleordersystem.service.SampleService;
import org.example.sampleordersystem.util.FixedTimeProvider;
import org.example.sampleordersystem.util.OrderIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrderControllerTest {

    private SampleService sampleService;
    private OrderService orderService;
    private InMemoryOrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        InMemorySampleRepository sampleRepo = new InMemorySampleRepository();
        orderRepository = new InMemoryOrderRepository();
        InMemoryProductionRepository productionRepo = new InMemoryProductionRepository();
        FixedTimeProvider timeProvider = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 10, 0));
        ProductionService productionService = new ProductionService(
            productionRepo, orderRepository, sampleRepo, timeProvider, 1.0);
        OrderIdGenerator idGenerator = new OrderIdGenerator(0, timeProvider);
        sampleService = new SampleService(sampleRepo);
        orderService = new OrderService(sampleRepo, orderRepository, productionService, idGenerator);

        sampleService.register("S1", "갈륨비소", 5, 0.9, 100);
    }

    @Test
    @DisplayName("주문 접수 시 Service에 저장된다")
    void placeOrderCallsService() {
        FakeView view = new FakeView("S1", "홍길동", "10");
        OrderController controller = new OrderController(orderService, view);

        controller.handlePlace();

        assertEquals(1, orderService.findByStatus(OrderStatus.RESERVED).size());
    }

    @Test
    @DisplayName("존재하지 않는 시료로 주문 접수 시 오류가 출력된다")
    void placeOrderShowsErrorOnUnknownSample() {
        FakeView view = new FakeView("UNKNOWN", "홍길동", "10");
        OrderController controller = new OrderController(orderService, view);

        controller.handlePlace();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("RESERVED 주문 승인 시 해당 주문 상태가 변경된다")
    void approveCallsService() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();

        FakeView approveView = new FakeView("1");
        new OrderController(orderService, approveView).handleApprove();

        assertEquals(OrderStatus.CONFIRMED, orderService.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("RESERVED 주문이 없을 때 승인 시 안내 메시지가 출력된다")
    void approveShowsMessageWhenNoReserved() {
        FakeView view = new FakeView();
        OrderController controller = new OrderController(orderService, view);

        controller.handleApprove();

        assertTrue(view.getMessages().stream().anyMatch(m -> m.contains("없습니다")));
    }

    @Test
    @DisplayName("승인 번호가 범위를 벗어나면 오류가 출력된다")
    void approveShowsErrorOnOutOfRangeIndex() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();

        FakeView approveView = new FakeView("99");
        new OrderController(orderService, approveView).handleApprove();

        assertFalse(approveView.getErrors().isEmpty());
    }

    @Test
    @DisplayName("RESERVED 주문 거절 시 해당 주문 상태가 REJECTED로 변경된다")
    void rejectCallsService() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();

        FakeView rejectView = new FakeView("1");
        new OrderController(orderService, rejectView).handleReject();

        assertEquals(OrderStatus.REJECTED, orderService.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("RESERVED 주문이 없을 때 거절 시 안내 메시지가 출력된다")
    void rejectShowsMessageWhenNoReserved() {
        FakeView view = new FakeView();
        OrderController controller = new OrderController(orderService, view);

        controller.handleReject();

        assertTrue(view.getMessages().stream().anyMatch(m -> m.contains("없습니다")));
    }

    @Test
    @DisplayName("거절 번호가 범위를 벗어나면 오류가 출력된다")
    void rejectShowsErrorOnOutOfRangeIndex() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();

        FakeView rejectView = new FakeView("0");
        new OrderController(orderService, rejectView).handleReject();

        assertFalse(rejectView.getErrors().isEmpty());
    }
}
