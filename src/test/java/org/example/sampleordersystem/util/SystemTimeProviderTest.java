package org.example.sampleordersystem.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SystemTimeProviderTest {

    @Test
    @DisplayName("SystemTimeProvider는 null이 아닌 현재 시각을 반환한다")
    void systemTimeProviderReturnsNow() {
        SystemTimeProvider provider = new SystemTimeProvider();
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime result = provider.now();
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(result);
        assertFalse(result.isBefore(before));
        assertFalse(result.isAfter(after));
    }
}
