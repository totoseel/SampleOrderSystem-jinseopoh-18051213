package org.example.sampleordersystem.util;

import java.time.LocalDateTime;

public class FixedTimeProvider implements TimeProvider {
    private LocalDateTime fixed;

    public FixedTimeProvider(LocalDateTime fixed) {
        this.fixed = fixed;
    }

    public void setTime(LocalDateTime time) {
        this.fixed = time;
    }

    @Override
    public LocalDateTime now() {
        return fixed;
    }
}
