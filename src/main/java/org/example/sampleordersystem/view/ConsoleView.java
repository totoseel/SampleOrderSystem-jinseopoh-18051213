package org.example.sampleordersystem.view;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.model.Sample;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

public class ConsoleView implements View {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Scanner scanner;
    private final PrintStream out;

    public ConsoleView(Scanner scanner, PrintStream out) {
        this.scanner = scanner;
        this.out = out;
    }

    @Override
    public void showMainSummary(int sampleCount, int totalStock, int orderCount,
                                int queueSize, Optional<ProductionEntry> current,
                                double progress, int confirmedCount) {
        out.println("=== S-Semi 반도체 시료 생산주문관리 시스템 ===");
        out.println("[현황 요약]");
        out.printf("  등록 시료 수    : %d%n", sampleCount);
        out.printf("  총 재고 수량    : %d%n", totalStock);
        out.printf("  전체 주문 수    : %d%n", orderCount);
        out.printf("  생산라인 대기   : %d%n", queueSize);
        if (current.isPresent()) {
            out.printf("  현재 생산 중   : %s (진행률: %.1f%%)%n",
                current.get().getSampleId(), progress);
        } else {
            out.println("  현재 생산 중   : 생산 없음");
        }
        out.printf("  출고 대기(CONFIRMED): %d%n", confirmedCount);
    }

    @Override
    public void showMenu(List<String> options) {
        out.println("[메뉴]");
        for (String option : options) {
            out.printf("  %s%n", option);
        }
    }

    @Override
    public void showSamples(List<Sample> samples) {
        out.println("[시료 목록]");
        for (int i = 0; i < samples.size(); i++) {
            Sample s = samples.get(i);
            out.printf("  %d. %s | %s | 재고: %d%n", i + 1, s.getId(), s.getName(), s.getStock());
        }
    }

    @Override
    public void showOrders(List<Order> orders) {
        out.println("[주문 목록]");
        out.printf("  %-5s %-22s %-8s %-10s %-5s %-10s %s%n",
            "번호", "주문번호", "시료ID", "고객명", "수량", "상태", "주문일시");
        out.println("  " + "-".repeat(80));
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            out.printf("  %-5d %-22s %-8s %-10s %-5d %-10s %s%n",
                i + 1, o.getOrderId(), o.getSampleId(), o.getCustomerName(),
                o.getQuantity(), o.getStatus(),
                o.getOrderedAt().format(FORMATTER));
        }
    }

    @Override
    public void showProductionStatus(Optional<ProductionEntry> current,
                                     double progress,
                                     Optional<LocalDateTime> estimatedFinish,
                                     List<ProductionEntry> queue) {
        out.println("=== 생산라인 현황 ===");
        out.println("[현재 생산 중]");
        if (current.isPresent()) {
            ProductionEntry e = current.get();
            out.printf("  주문번호 : %s%n", e.getOrderId());
            out.printf("  시료명   : %s%n", e.getSampleId());
            out.printf("  진행률   : %.1f%%%n", progress);
            estimatedFinish.ifPresent(dt ->
                out.printf("  완료 예정: %s%n", dt.format(FORMATTER)));
        } else {
            out.println("  생산 없음");
        }
        out.println("[대기 큐]");
        for (int i = 0; i < queue.size(); i++) {
            ProductionEntry e = queue.get(i);
            out.printf("  %d. %s | %s | 부족분:%d | 실생산량:%d%n",
                i + 1, e.getOrderId(), e.getSampleId(), e.getShortage(), e.getActualQty());
        }
    }

    @Override
    public void showMonitoringSummary(Map<OrderStatus, Long> statusCounts,
                                      List<Sample> samples,
                                      Set<String> producingSampleIds) {
        out.println("=== 모니터링 ===");
        out.println("[주문량 현황]");
        OrderStatus[] order = {OrderStatus.RESERVED, OrderStatus.PRODUCING,
                               OrderStatus.CONFIRMED, OrderStatus.RELEASE};
        for (OrderStatus s : order) {
            out.printf("  %-10s: %d건%n", s, statusCounts.getOrDefault(s, 0L));
        }
        out.println("[재고량 현황]");
        for (Sample s : samples) {
            String label;
            if (s.getStock() == 0) {
                label = "고갈";
            } else if (producingSampleIds.contains(s.getId())) {
                label = "부족";
            } else {
                label = "여유";
            }
            out.printf("  %-15s | 재고: %-5d | 상태: %s%n", s.getName(), s.getStock(), label);
        }
    }

    @Override
    public void showMessage(String message) {
        out.println(message);
    }

    @Override
    public void showError(String message) {
        out.println("[오류] " + message);
    }

    @Override
    public String readLine() {
        return scanner.nextLine();
    }
}
