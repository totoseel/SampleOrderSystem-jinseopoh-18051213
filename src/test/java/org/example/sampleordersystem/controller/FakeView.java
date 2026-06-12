package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.model.Sample;
import org.example.sampleordersystem.view.View;

import java.time.LocalDateTime;
import java.util.*;

class FakeView implements View {
    private final Queue<String> inputs;
    private final List<String> messages = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private Map<OrderStatus, Long> capturedCounts;
    private List<Sample> capturedSamples;

    FakeView(String... inputs) {
        this.inputs = new LinkedList<>(Arrays.asList(inputs));
    }

    @Override public String readLine() { return inputs.poll(); }
    @Override public void showMessage(String msg) { messages.add(msg); }
    @Override public void showError(String msg) { errors.add(msg); }
    @Override public void showMainSummary(int sampleCount, int totalStock, int orderCount,
                                          int queueSize, Optional<ProductionEntry> current,
                                          double progress, int confirmedCount) {}
    @Override public void showMenu(List<String> options) {}
    @Override public void showSamples(List<Sample> samples) {}
    @Override public void showOrders(List<Order> orders) {}
    @Override public void showProductionStatus(Optional<ProductionEntry> current,
                                               double progress,
                                               LocalDateTime estimatedFinish,
                                               List<ProductionEntry> queue) {}
    @Override public void showMonitoringSummary(Map<OrderStatus, Long> counts, List<Sample> samples) {
        this.capturedCounts = counts;
        this.capturedSamples = samples;
    }

    public List<String> getMessages() { return messages; }
    public List<String> getErrors() { return errors; }
    public Map<OrderStatus, Long> getCapturedCounts() { return capturedCounts; }
    public List<Sample> getCapturedSamples() { return capturedSamples; }
}
