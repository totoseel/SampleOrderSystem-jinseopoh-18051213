package org.example.sampleordersystem.view;

import org.example.sampleordersystem.model.ProductionEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleViewTest {

    @Test
    @DisplayName("showMainSummary는 현황 요약 항목을 모두 출력한다")
    void consoleViewRendersMainSummary() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = new ConsoleView(
            new Scanner(new ByteArrayInputStream("".getBytes())),
            new PrintStream(out));

        view.showMainSummary(3, 250, 12, 2, Optional.empty(), 0.0, 1);

        String output = out.toString();
        assertTrue(output.contains("3"));
        assertTrue(output.contains("250"));
        assertTrue(output.contains("생산 없음"));
    }

    @Test
    @DisplayName("showProductionStatus는 주문번호와 진행률을 출력한다")
    void consoleViewRendersProductionStatus() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleView view = new ConsoleView(
            new Scanner(new ByteArrayInputStream("".getBytes())),
            new PrintStream(out));

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
}
