package org.example.sampleordersystem.repository;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> store = new LinkedHashMap<>();

    @Override
    public void save(Order order) {
        store.put(order.getOrderId(), order);
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return store.values().stream()
            .filter(o -> o.getStatus() == status)
            .toList();
    }

    @Override
    public int countByDatePrefix(String yyyymmdd) {
        return (int) store.keySet().stream()
            .filter(id -> id.contains(yyyymmdd))
            .count();
    }
}
