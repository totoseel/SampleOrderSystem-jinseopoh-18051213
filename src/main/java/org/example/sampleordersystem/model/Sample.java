package org.example.sampleordersystem.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Sample {

    private final String id;
    private final String name;
    private final int avgProductionMinutes;
    private final double yield;
    private int stock;

    @JsonCreator
    public Sample(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("avgProductionMinutes") int avgProductionMinutes,
        @JsonProperty("yield") double yield,
        @JsonProperty("stock") int stock
    ) {
        if (yield <= 0 || yield > 1) {
            throw new IllegalArgumentException("수율은 0 초과 1 이하여야 합니다");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("초기 재고는 0 이상이어야 합니다");
        }
        this.id = id;
        this.name = name;
        this.avgProductionMinutes = avgProductionMinutes;
        this.yield = yield;
        this.stock = stock;
    }

    public void decreaseStock(int qty) {
        if (qty > stock) {
            throw new IllegalArgumentException("재고가 부족합니다");
        }
        stock -= qty;
    }

    public void increaseStock(int qty) {
        stock += qty;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getAvgProductionMinutes() { return avgProductionMinutes; }
    public double getYield() { return yield; }
    public int getStock() { return stock; }
}
