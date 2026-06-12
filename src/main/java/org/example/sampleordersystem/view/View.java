package org.example.sampleordersystem.view;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.model.Sample;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface View {
    void showMainSummary(int sampleCount, int totalStock, int orderCount,
                         int queueSize, Optional<ProductionEntry> current,
                         double progress, int confirmedCount);
    void showMenu(List<String> options);
    void showSamples(List<Sample> samples);
    void showOrders(List<Order> orders);
    void showProductionStatus(Optional<ProductionEntry> current,
                              double progress,
                              LocalDateTime estimatedFinish,
                              List<ProductionEntry> queue);
    void showMonitoringSummary(Map<OrderStatus, Long> statusCounts,
                               List<Sample> samples,
                               Set<String> producingSampleIds);
    void showMessage(String message);
    void showError(String message);
    String readLine();
}
