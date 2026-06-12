package org.example.sampleordersystem.repository;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String id);
    List<Order> findAll();
    List<Order> findByStatus(OrderStatus status);
    int countByDatePrefix(String yyyymmdd);
}
