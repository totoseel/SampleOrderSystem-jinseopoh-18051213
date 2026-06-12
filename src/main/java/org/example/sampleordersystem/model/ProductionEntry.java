package org.example.sampleordersystem.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class ProductionEntry {

    private final String orderId;
    private final String sampleId;
    private final int shortage;
    private final int actualQty;
    private final double totalMinutes;
    private final LocalDateTime startedAt;

    @JsonCreator
    public ProductionEntry(
        @JsonProperty("orderId") String orderId,
        @JsonProperty("sampleId") String sampleId,
        @JsonProperty("shortage") int shortage,
        @JsonProperty("actualQty") int actualQty,
        @JsonProperty("totalMinutes") double totalMinutes,
        @JsonProperty("startedAt") LocalDateTime startedAt
    ) {
        this.orderId = orderId;
        this.sampleId = sampleId;
        this.shortage = shortage;
        this.actualQty = actualQty;
        this.totalMinutes = totalMinutes;
        this.startedAt = startedAt;
    }

    public String getOrderId() { return orderId; }
    public String getSampleId() { return sampleId; }
    public int getShortage() { return shortage; }
    public int getActualQty() { return actualQty; }
    public double getTotalMinutes() { return totalMinutes; }
    public LocalDateTime getStartedAt() { return startedAt; }
}
