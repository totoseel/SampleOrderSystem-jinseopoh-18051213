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
}
