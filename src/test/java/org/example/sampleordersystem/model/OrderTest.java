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

    @Test
    @DisplayName("모든 필드를 올바르게 저장하고 반환한다")
    void orderStoresAllFields() {
        LocalDateTime orderedAt = LocalDateTime.of(2024, 1, 1, 9, 0);
        Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10, orderedAt);

        assertEquals("ORD-20240101-0001", order.getOrderId());
        assertEquals("S1", order.getSampleId());
        assertEquals("홍길동", order.getCustomerName());
        assertEquals(10, order.getQuantity());
        assertEquals(orderedAt, order.getOrderedAt());
    }

    @Test
    @DisplayName("Jackson 역직렬화용 생성자로 임의 상태를 주입할 수 있다")
    void orderJsonCreatorConstructorRestoresStatus() {
        LocalDateTime orderedAt = LocalDateTime.of(2024, 1, 1, 9, 0);
        Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
            OrderStatus.CONFIRMED, orderedAt);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    @DisplayName("PRODUCING → CONFIRMED 전환은 허용된다")
    void orderAllowsProducingToConfirmed() {
        Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
            LocalDateTime.now());
        order.transitionTo(OrderStatus.PRODUCING);
        order.transitionTo(OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    @DisplayName("CONFIRMED → RELEASE 전환은 허용된다")
    void orderAllowsConfirmedToRelease() {
        Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
            LocalDateTime.now());
        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.RELEASE);
        assertEquals(OrderStatus.RELEASE, order.getStatus());
    }

    @Test
    @DisplayName("RELEASE 상태에서 전환 시도 시 예외를 던진다")
    void orderRejectsTransitionFromRelease() {
        Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
            LocalDateTime.now());
        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.RELEASE);
        assertThrows(IllegalStateException.class,
            () -> order.transitionTo(OrderStatus.CONFIRMED));
    }

    @Test
    @DisplayName("PRODUCING → RESERVED 전환은 허용된다 (생산 취소 복원)")
    void orderAllowsProducingToReserved() {
        Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
            LocalDateTime.now());
        order.transitionTo(OrderStatus.PRODUCING);
        order.transitionTo(OrderStatus.RESERVED);
        assertEquals(OrderStatus.RESERVED, order.getStatus());
    }
}
