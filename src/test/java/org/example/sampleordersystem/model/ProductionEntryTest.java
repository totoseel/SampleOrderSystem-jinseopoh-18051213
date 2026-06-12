package org.example.sampleordersystem.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductionEntryTest {

    @Test
    @DisplayName("ProductionEntry는 모든 필드를 저장한다")
    void productionEntryStoresFields() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 9, 0);
        ProductionEntry entry = new ProductionEntry(
            "ORD-20240101-0001", "S1", 50, 62, 310.0, start);

        assertEquals("ORD-20240101-0001", entry.getOrderId());
        assertEquals("S1", entry.getSampleId());
        assertEquals(50, entry.getShortage());
        assertEquals(62, entry.getActualQty());
        assertEquals(310.0, entry.getTotalMinutes());
        assertEquals(start, entry.getStartedAt());
    }

    @Test
    @DisplayName("startedAt이 null인 ProductionEntry는 대기 중 상태이다")
    void productionEntryAllowsNullStartedAt() {
        ProductionEntry entry = new ProductionEntry(
            "ORD-20240101-0001", "S1", 50, 62, 310.0, null);

        assertNull(entry.getStartedAt());
    }
}
