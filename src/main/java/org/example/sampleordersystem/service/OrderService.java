package org.example.sampleordersystem.service;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.model.Sample;
import org.example.sampleordersystem.repository.OrderRepository;
import org.example.sampleordersystem.repository.SampleRepository;
import org.example.sampleordersystem.util.OrderIdGenerator;

import java.time.LocalDateTime;
import java.util.List;

public class OrderService {

    private final SampleRepository sampleRepository;
    private final OrderRepository orderRepository;
    private final ProductionService productionService;
    private final OrderIdGenerator orderIdGenerator;

    public OrderService(SampleRepository sampleRepository,
                        OrderRepository orderRepository,
                        ProductionService productionService,
                        OrderIdGenerator orderIdGenerator) {
        this.sampleRepository = sampleRepository;
        this.orderRepository = orderRepository;
        this.productionService = productionService;
        this.orderIdGenerator = orderIdGenerator;
    }

    public Order placeOrder(String sampleId, String customerName, int quantity) {
        sampleRepository.findById(sampleId)
            .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 시료입니다"));
        String orderId = orderIdGenerator.next();
        Order order = new Order(orderId, sampleId, customerName, quantity, LocalDateTime.now());
        orderRepository.save(order);
        return order;
    }

    public void approve(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다"));
        Sample sample = sampleRepository.findById(order.getSampleId())
            .orElseThrow(() -> new IllegalStateException("시료를 찾을 수 없습니다"));

        int quantity = order.getQuantity();
        int stock = sample.getStock();
        int shortage = Math.max(0, quantity - stock);

        if (shortage == 0) {
            sample.decreaseStock(quantity);
            sampleRepository.save(sample);
            order.transitionTo(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        } else {
            order.transitionTo(OrderStatus.PRODUCING);
            orderRepository.save(order);
            int actualQty = (int) Math.ceil(shortage / (sample.getYield() * 0.9));
            double totalMinutes = sample.getAvgProductionMinutes() * actualQty;
            ProductionEntry entry = new ProductionEntry(
                order.getOrderId(), sample.getId(), shortage, actualQty, totalMinutes, null
            );
            productionService.enqueue(entry);
        }
    }

    public void reject(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다"));
        order.transitionTo(OrderStatus.REJECTED);
        orderRepository.save(order);
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public void releaseOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다"));
        order.transitionTo(OrderStatus.RELEASE);
        orderRepository.save(order);
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
}
