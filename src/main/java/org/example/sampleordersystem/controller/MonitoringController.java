package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.service.OrderService;
import org.example.sampleordersystem.service.SampleService;
import org.example.sampleordersystem.view.View;

import java.util.Map;
import java.util.stream.Collectors;

public class MonitoringController {
    private final OrderService orderService;
    private final SampleService sampleService;
    private final View view;

    public MonitoringController(OrderService orderService, SampleService sampleService, View view) {
        this.orderService = orderService;
        this.sampleService = sampleService;
        this.view = view;
    }

    public void handleView() {
        Map<OrderStatus, Long> counts = orderService.findAll().stream()
            .filter(o -> o.getStatus() != OrderStatus.REJECTED)
            .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        view.showMonitoringSummary(counts, sampleService.findAll());
    }
}
