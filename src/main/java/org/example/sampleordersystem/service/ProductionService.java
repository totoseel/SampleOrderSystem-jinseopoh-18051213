package org.example.sampleordersystem.service;

import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.Sample;
import org.example.sampleordersystem.repository.OrderRepository;
import org.example.sampleordersystem.repository.ProductionRepository;
import org.example.sampleordersystem.repository.SampleRepository;
import org.example.sampleordersystem.util.TimeProvider;

import java.time.Duration;
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
        List<ProductionEntry> queue = productionRepository.findAll();
        // 큐가 비어있으면 즉시 startedAt 기록
        if (queue.isEmpty()) {
            ProductionEntry withStart = new ProductionEntry(
                entry.getOrderId(), entry.getSampleId(), entry.getShortage(),
                entry.getActualQty(), entry.getTotalMinutes(), timeProvider.now()
            );
            productionRepository.save(withStart);
        } else {
            productionRepository.save(entry);
        }
    }

    public void tick() {
        Optional<ProductionEntry> currentOpt = getCurrent();
        if (currentOpt.isEmpty()) {
            return;
        }
        ProductionEntry current = currentOpt.get();
        double progress = calculateProgress(current);
        if (progress >= 100.0) {
            completeProduction(current);
            startNextEntry();
        }
    }

    private double calculateProgress(ProductionEntry entry) {
        double elapsedSeconds = Duration.between(entry.getStartedAt(), timeProvider.now()).toSeconds();
        double totalSeconds = (entry.getTotalMinutes() / timeScale) * 60.0;
        return elapsedSeconds / totalSeconds * 100.0;
    }

    private void completeProduction(ProductionEntry entry) {
        // 재고 반영: actualQty만큼 보충 후 주문 수량만큼 출고 차감
        orderRepository.findById(entry.getOrderId()).ifPresent(order -> {
            sampleRepository.findById(entry.getSampleId()).ifPresent(sample -> {
                sample.increaseStock(entry.getActualQty());
                sample.decreaseStock(order.getQuantity());
                sampleRepository.save(sample);
            });
            order.transitionTo(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        });
        // 큐에서 제거
        productionRepository.delete(entry.getOrderId());
    }

    private void startNextEntry() {
        List<ProductionEntry> queue = productionRepository.findAll();
        if (queue.isEmpty()) {
            return;
        }
        // 대기 항목은 항상 startedAt=null이므로 즉시 시작 기록
        ProductionEntry next = queue.get(0);
        ProductionEntry withStart = new ProductionEntry(
            next.getOrderId(), next.getSampleId(), next.getShortage(),
            next.getActualQty(), next.getTotalMinutes(), timeProvider.now()
        );
        productionRepository.save(withStart);
    }

    public double getProgress() {
        return getCurrent().map(this::calculateProgress).orElse(0.0);
    }

    public Optional<ProductionEntry> getCurrent() {
        return productionRepository.findAll().stream()
            .filter(e -> e.getStartedAt() != null)
            .findFirst();
    }

    public List<ProductionEntry> getQueue() {
        return productionRepository.findAll();
    }

    public Optional<LocalDateTime> getEstimatedFinishAt(ProductionEntry entry) {
        if (entry.getStartedAt() == null) {
            return Optional.empty();
        }
        double totalSeconds = (entry.getTotalMinutes() / timeScale) * 60.0;
        return Optional.of(entry.getStartedAt().plusSeconds((long) totalSeconds));
    }
}
