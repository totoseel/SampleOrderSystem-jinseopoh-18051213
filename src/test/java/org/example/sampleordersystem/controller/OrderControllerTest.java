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

        // 시료 등록
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
    @DisplayName("RESERVED 주문 승인 시 해당 주문 상태가 변경된다")
    void approveCallsService() {
        // 주문 생성 후 승인 (재고 충분 케이스: stock=100, qty=10)
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        OrderController controller = new OrderController(orderService, placeView);
        controller.handlePlace();

        FakeView approveView = new FakeView("1");
        OrderController approveController = new OrderController(orderService, approveView);
        approveController.handleApprove();

        assertEquals(OrderStatus.CONFIRMED, orderService.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("RESERVED 주문 거절 시 해당 주문 상태가 REJECTED로 변경된다")
    void rejectCallsService() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        OrderController controller = new OrderController(orderService, placeView);
        controller.handlePlace();

        FakeView rejectView = new FakeView("1");
        OrderController rejectController = new OrderController(orderService, rejectView);
        rejectController.handleReject();

        assertEquals(OrderStatus.REJECTED, orderService.findAll().get(0).getStatus());
    }
}
