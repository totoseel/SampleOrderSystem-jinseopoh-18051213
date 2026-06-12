package org.example.sampleordersystem.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class OrderIdGenerator {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TimeProvider timeProvider;
    private int seq;
    private LocalDate lastDate;

    public OrderIdGenerator(int lastSeq, TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
        this.seq = lastSeq;
        this.lastDate = timeProvider.now().toLocalDate();
    }

    public String next() {
        LocalDate today = timeProvider.now().toLocalDate();
        if (!today.equals(lastDate)) {
            seq = 0;
            lastDate = today;
        }
        seq++;
        return "ORD-" + today.format(DATE_FORMATTER) + "-" + String.format("%04d", seq);
    }

    public int currentSeq() {
        return seq;
    }
}
