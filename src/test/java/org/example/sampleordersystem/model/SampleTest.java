package org.example.sampleordersystem.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SampleTest {

    @Test
    @DisplayName("수율이 0 이하이면 예외를 던진다")
    void sampleRejectsNonPositiveYield() {
        assertThrows(IllegalArgumentException.class,
            () -> new Sample("S1", "샘플", 10, 0.0, 100));
        assertThrows(IllegalArgumentException.class,
            () -> new Sample("S1", "샘플", 10, -0.1, 100));
    }

    @Test
    @DisplayName("수율이 1 초과이면 예외를 던진다")
    void sampleRejectsYieldAboveOne() {
        assertThrows(IllegalArgumentException.class,
            () -> new Sample("S1", "샘플", 10, 1.1, 100));
    }

    @Test
    @DisplayName("초기 재고가 음수이면 예외를 던진다")
    void sampleRejectsNegativeStock() {
        assertThrows(IllegalArgumentException.class,
            () -> new Sample("S1", "샘플", 10, 0.9, -1));
    }

    @Test
    @DisplayName("재고를 정상 차감한다")
    void decreaseStockReducesStock() {
        Sample sample = new Sample("S1", "샘플", 10, 0.9, 100);
        sample.decreaseStock(30);
        assertEquals(70, sample.getStock());
    }

    @Test
    @DisplayName("재고보다 많은 수량을 차감하면 예외를 던진다")
    void decreaseStockRejectsOverdraft() {
        Sample sample = new Sample("S1", "샘플", 10, 0.9, 10);
        assertThrows(IllegalArgumentException.class, () -> sample.decreaseStock(20));
        assertEquals(10, sample.getStock());
    }

    @Test
    @DisplayName("재고를 정상 증가시킨다")
    void increaseStockAddsToStock() {
        Sample sample = new Sample("S1", "샘플", 10, 0.9, 50);
        sample.increaseStock(30);
        assertEquals(80, sample.getStock());
    }
}
