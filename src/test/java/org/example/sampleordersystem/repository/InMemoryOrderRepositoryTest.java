package org.example.sampleordersystem.repository;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryOrderRepositoryTest {

    @Test
    @DisplayName("상태로 주문을 필터링한다")
    void findByStatusFilters() {
        OrderRepository repo = new InMemoryOrderRepository();
        Order o1 = new Order("ORD-20240101-0001", "S1", "A", 10, LocalDateTime.now());
        Order o2 = new Order("ORD-20240101-0002", "S1", "B", 5, LocalDateTime.now());
        Order o3 = new Order("ORD-20240101-0003", "S1", "C", 3, LocalDateTime.now());
        o3.transitionTo(OrderStatus.CONFIRMED);
        repo.save(o1); repo.save(o2); repo.save(o3);
        assertEquals(2, repo.findByStatus(OrderStatus.RESERVED).size());
    }

    @Test
    @DisplayName("특정 날짜 주문 수를 카운트한다")
    void countByDatePrefix() {
        OrderRepository repo = new InMemoryOrderRepository();
        repo.save(new Order("ORD-20240101-0001", "S1", "A", 1, LocalDateTime.now()));
        repo.save(new Order("ORD-20240101-0002", "S1", "B", 1, LocalDateTime.now()));
        repo.save(new Order("ORD-20240101-0003", "S1", "C", 1, LocalDateTime.now()));
        repo.save(new Order("ORD-20240102-0001", "S1", "D", 1, LocalDateTime.now()));
        assertEquals(3, repo.countByDatePrefix("20240101"));
    }

    @Test
    @DisplayName("ID로 저장된 주문을 조회한다")
    void findById() {
        OrderRepository repo = new InMemoryOrderRepository();
        Order order = new Order("ORD-20240101-0001", "S1", "A", 10, LocalDateTime.now());
        repo.save(order);
        assertTrue(repo.findById("ORD-20240101-0001").isPresent());
        assertTrue(repo.findById("NONE").isEmpty());
    }

    @Test
    @DisplayName("저장된 모든 주문을 반환한다")
    void findAll() {
        OrderRepository repo = new InMemoryOrderRepository();
        repo.save(new Order("ORD-20240101-0001", "S1", "A", 1, LocalDateTime.now()));
        repo.save(new Order("ORD-20240101-0002", "S1", "B", 1, LocalDateTime.now()));
        assertEquals(2, repo.findAll().size());
    }
}
