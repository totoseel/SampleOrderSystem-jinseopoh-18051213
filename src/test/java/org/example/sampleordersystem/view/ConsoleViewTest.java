package org.example.sampleordersystem.view;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.model.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleViewTest {

    private ConsoleView viewWith(String input, ByteArrayOutputStream out) {
        return new ConsoleView(
            new Scanner(new ByteArrayInputStream(input.getBytes())),
            new PrintStream(out));
    }

    @Test
    @DisplayName("showMainSummary는 현황 요약 항목을 모두 출력한다")
    void consoleViewRendersMainSummary() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("", out);

        view.showMainSummary(3, 250, 12, 2, Optional.empty(), 0.0, 1);

        String output = out.toString();
        assertTrue(output.contains("3"));
        assertTrue(output.contains("250"));
        assertTrue(output.contains("생산 없음"));
    }

    @Test
    @DisplayName("showMainSummary는 생산 중 항목이 있으면 시료ID와 진행률을 출력한다")
    void consoleViewRendersMainSummaryWithCurrentProduction() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("", out);
        ProductionEntry entry = new ProductionEntry(
            "ORD-20240101-0001", "S1", 5, 6, 30.0,
            LocalDateTime.of(2024, 1, 1, 10, 0));

        view.showMainSummary(1, 100, 1, 0, Optional.of(entry), 50.0, 0);

        String output = out.toString();
        assertTrue(output.contains("S1"));
        assertTrue(output.contains("50.0"));
    }

    @Test
    @DisplayName("showMenu는 메뉴 항목을 번호와 함께 출력한다")
    void consoleViewRendersMenu() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("", out);

        view.showMenu(List.of("시료 등록", "주문 접수", "종료"));

        String output = out.toString();
        assertTrue(output.contains("1"));
        assertTrue(output.contains("시료 등록"));
        assertTrue(output.contains("종료"));
    }

    @Test
    @DisplayName("showSamples는 시료 목록을 출력한다")
    void consoleViewRendersSamples() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("", out);
        Sample sample = new Sample("S1", "갈륨비소", 5, 0.9, 100);

        view.showSamples(List.of(sample));

        String output = out.toString();
        assertTrue(output.contains("S1"));
        assertTrue(output.contains("갈륨비소"));
        assertTrue(output.contains("100"));
    }

    @Test
    @DisplayName("showOrders는 주문 목록을 번호와 함께 출력한다")
    void consoleViewRendersOrders() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("", out);
        Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
            LocalDateTime.of(2024, 1, 1, 10, 0));

        view.showOrders(List.of(order));

        String output = out.toString();
        assertTrue(output.contains("ORD-20240101-0001"));
        assertTrue(output.contains("S1"));
    }

    @Test
    @DisplayName("showProductionStatus는 주문번호와 진행률을 출력한다")
    void consoleViewRendersProductionStatus() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("", out);
        ProductionEntry entry = new ProductionEntry(
            "ORD-20240101-0003", "S1", 10, 12, 60.0,
            LocalDateTime.of(2024, 1, 1, 10, 0));

        view.showProductionStatus(
            Optional.of(entry),
            45.3,
            LocalDateTime.of(2024, 1, 1, 11, 0),
            List.of());

        String output = out.toString();
        assertTrue(output.contains("ORD-20240101-0003"));
        assertTrue(output.contains("45.3"));
    }

    @Test
    @DisplayName("showProductionStatus는 대기 큐 항목을 출력한다")
    void consoleViewRendersProductionQueue() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("", out);
        ProductionEntry queued = new ProductionEntry(
            "ORD-20240101-0002", "S2", 3, 4, 20.0, null);

        view.showProductionStatus(Optional.empty(), 0.0, null, List.of(queued));

        String output = out.toString();
        assertTrue(output.contains("ORD-20240101-0002"));
        assertTrue(output.contains("S2"));
    }

    @Test
    @DisplayName("showMonitoringSummary는 REJECTED 제외한 주문 상태별 수량과 시료 재고를 출력한다")
    void consoleViewRendersMonitoringSummary() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("", out);
        Sample sample = new Sample("S1", "갈륨비소", 5, 0.9, 0);

        view.showMonitoringSummary(
            Map.of(OrderStatus.RESERVED, 2L),
            List.of(sample),
            Set.of());

        String output = out.toString();
        assertTrue(output.contains("RESERVED"));
        assertTrue(output.contains("갈륨비소"));
        assertTrue(output.contains("고갈"));
    }

    @Test
    @DisplayName("showMonitoringSummary는 재고가 있는 시료를 여유로 표시한다")
    void consoleViewRendersMonitoringSummaryWithSufficientStock() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("", out);
        Sample sample = new Sample("S1", "웨이퍼", 10, 0.8, 50);

        view.showMonitoringSummary(Map.of(), List.of(sample), Set.of());

        String output = out.toString();
        assertTrue(output.contains("여유"));
    }

    @Test
    @DisplayName("showMonitoringSummary는 PRODUCING 중인 시료를 부족으로 표시한다")
    void consoleViewRendersMonitoringSummaryWithProducingStock() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("", out);
        Sample sample = new Sample("S1", "웨이퍼", 10, 0.8, 30);

        view.showMonitoringSummary(Map.of(OrderStatus.PRODUCING, 1L), List.of(sample), Set.of("S1"));

        String output = out.toString();
        assertTrue(output.contains("부족"));
    }

    @Test
    @DisplayName("showError는 오류 메시지를 출력한다")
    void consoleViewRendersError() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("", out);

        view.showError("잘못된 입력입니다");

        assertTrue(out.toString().contains("잘못된 입력입니다"));
    }

    @Test
    @DisplayName("showMessage는 일반 메시지를 출력한다")
    void consoleViewRendersMessage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("", out);

        view.showMessage("작업이 완료되었습니다");

        assertTrue(out.toString().contains("작업이 완료되었습니다"));
    }

    @Test
    @DisplayName("readLine은 입력된 줄을 반환한다")
    void consoleViewReadsLine() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = viewWith("테스트입력\n", out);

        String line = view.readLine();

        assertEquals("테스트입력", line);
    }
}
