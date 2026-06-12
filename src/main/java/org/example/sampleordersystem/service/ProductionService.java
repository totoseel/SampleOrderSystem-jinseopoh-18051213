package org.example.sampleordersystem.service;

import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.repository.OrderRepository;
import org.example.sampleordersystem.repository.ProductionRepository;
import org.example.sampleordersystem.repository.SampleRepository;
import org.example.sampleordersystem.util.TimeProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ProductionService {

    private final ProductionRepository productionRepository;
    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;
    private final TimeProvider timeProvider;
    private final double timeScale;

    public ProductionService(ProductionRepository productionRepository,
                             OrderRepository orderRepository,
                             SampleRepository sampleRepository,
                             TimeProvider timeProvider,
                             double timeScale) {
        this.productionRepository = productionRepository;
        this.orderRepository = orderRepository;
        this.sampleRepository = sampleRepository;
        this.timeProvider = timeProvider;
        this.timeScale = timeScale;
    }

    public void enqueue(ProductionEntry entry) {
        productionRepository.save(entry);
    }

    public void tick() {
        // 그룹 B에서 구현
    }

    public double getProgress() {
        return 0.0;
    }

    public Optional<ProductionEntry> getCurrent() {
        return Optional.empty();
    }

    public List<ProductionEntry> getQueue() {
        return List.of();
    }

    public Optional<LocalDateTime> getEstimatedFinishAt(ProductionEntry entry) {
        return Optional.empty();
    }
}
