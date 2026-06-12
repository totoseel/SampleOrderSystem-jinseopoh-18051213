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

class MonitoringControllerTest {

    private OrderService orderService;
    private SampleService sampleService;

    @BeforeEach
    void setUp() {
        InMemorySampleRepository sampleRepo = new InMemorySampleRepository();
        InMemoryOrderRepository orderRepo = new InMemoryOrderRepository();
        InMemoryProductionRepository productionRepo = new InMemoryProductionRepository();
        FixedTimeProvider timeProvider = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 10, 0));
        ProductionService productionService = new ProductionService(
            productionRepo, orderRepo, sampleRepo, timeProvider, 1.0);
        OrderIdGenerator idGen = new OrderIdGenerator(0, timeProvider);
        sampleService = new SampleService(sampleRepo);
        orderService = new OrderService(sampleRepo, orderRepo, productionService, idGen);

        sampleService.register("S1", "갈륨비소", 5, 0.9, 100);
        // RESERVED 1건
        orderService.placeOrder("S1", "홍길동", 10);
        // REJECTED 대상 2건 접수
        orderService.placeOrder("S1", "김철수", 5);
        orderService.placeOrder("S1", "이영희", 5);
        // 홍길동 외 나머지 주문 거절
        orderService.findByStatus(OrderStatus.RESERVED).stream()
            .filter(o -> !o.getCustomerName().equals("홍길동"))
            .forEach(o -> orderService.reject(o.getOrderId()));
    }

    @Test
    @DisplayName("모니터링에서 REJECTED 주문은 제외된다")
    void monitoringExcludesRejected() {
        FakeView view = new FakeView();
        MonitoringController controller = new MonitoringController(orderService, sampleService, view);

        controller.handleView();

        assertFalse(view.getCapturedCounts().containsKey(OrderStatus.REJECTED));
    }
}
