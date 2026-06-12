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
}
