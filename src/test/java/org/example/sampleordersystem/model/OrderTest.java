package org.example.sampleordersystem.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    @DisplayName("주문 생성 시 상태는 RESERVED이다")
    void orderInitialStatusIsReserved() {
        Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
            LocalDateTime.now());
        assertEquals(OrderStatus.RESERVED, order.getStatus());
    }

    @Test
    @DisplayName("허용된 상태 전환은 성공한다")
    void orderAllowsValidTransition() {
        Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
            LocalDateTime.now());
        order.transitionTo(OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    @DisplayName("허용되지 않은 상태 전환은 예외를 던진다")
    void orderRejectsInvalidTransition() {
        Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
            LocalDateTime.now());
        order.transitionTo(OrderStatus.REJECTED);
        assertThrows(IllegalStateException.class,
            () -> order.transitionTo(OrderStatus.CONFIRMED));
    }
}
