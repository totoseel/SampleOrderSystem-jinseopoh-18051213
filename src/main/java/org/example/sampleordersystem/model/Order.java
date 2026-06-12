package org.example.sampleordersystem.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class Order {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
        OrderStatus.RESERVED,  EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PRODUCING, OrderStatus.REJECTED),
        OrderStatus.PRODUCING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.RESERVED),
        OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.RELEASE),
        OrderStatus.REJECTED,  EnumSet.noneOf(OrderStatus.class),
        OrderStatus.RELEASE,   EnumSet.noneOf(OrderStatus.class)
    );

    private final String orderId;
    private final String sampleId;
    private final String customerName;
    private final int quantity;
    private OrderStatus status;
    private final LocalDateTime orderedAt;

    public Order(String orderId, String sampleId, String customerName,
                 int quantity, LocalDateTime orderedAt) {
        this.orderId = orderId;
        this.sampleId = sampleId;
        this.customerName = customerName;
        this.quantity = quantity;
        this.status = OrderStatus.RESERVED;
        this.orderedAt = orderedAt;
    }

    @JsonCreator
    public Order(
        @JsonProperty("orderId") String orderId,
        @JsonProperty("sampleId") String sampleId,
        @JsonProperty("customerName") String customerName,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("status") OrderStatus status,
        @JsonProperty("orderedAt") LocalDateTime orderedAt
    ) {
        this.orderId = orderId;
        this.sampleId = sampleId;
        this.customerName = customerName;
        this.quantity = quantity;
        this.status = status;
        this.orderedAt = orderedAt;
    }

    public void transitionTo(OrderStatus next) {
        if (!ALLOWED_TRANSITIONS.get(status).contains(next)) {
            throw new IllegalStateException(
                "상태 전환 불가: " + status + " → " + next);
        }
        this.status = next;
    }

    public String getOrderId() { return orderId; }
    public String getSampleId() { return sampleId; }
    public String getCustomerName() { return customerName; }
    public int getQuantity() { return quantity; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getOrderedAt() { return orderedAt; }
}
