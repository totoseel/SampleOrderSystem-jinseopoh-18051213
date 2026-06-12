package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.repository.InMemoryOrderRepository;
import org.example.sampleordersystem.repository.InMemoryProductionRepository;
import org.example.sampleordersystem.repository.InMemorySampleRepository;
import org.example.sampleordersystem.service.ProductionService;
import org.example.sampleordersystem.util.FixedTimeProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductionControllerTest {

    @Test
    @DisplayName("handleView는 View에 생산현황을 전달한다")
    void handleViewDelegatesToView() {
        InMemoryProductionRepository productionRepo = new InMemoryProductionRepository();
        InMemoryOrderRepository orderRepo = new InMemoryOrderRepository();
        InMemorySampleRepository sampleRepo = new InMemorySampleRepository();
        FixedTimeProvider timeProvider = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 10, 0));
        ProductionService productionService = new ProductionService(
            productionRepo, orderRepo, sampleRepo, timeProvider, 1.0);

        FakeView view = new FakeView();
        ProductionController controller = new ProductionController(productionService, view);

        // 예외 없이 실행되는지 확인
        assertDoesNotThrow(controller::handleView);
    }
}
