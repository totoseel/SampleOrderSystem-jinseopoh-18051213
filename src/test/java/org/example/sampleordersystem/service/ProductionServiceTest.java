package org.example.sampleordersystem.service;

import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.repository.InMemoryOrderRepository;
import org.example.sampleordersystem.repository.InMemoryProductionRepository;
import org.example.sampleordersystem.repository.InMemorySampleRepository;
import org.example.sampleordersystem.util.FixedTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductionServiceTest {

    private InMemoryProductionRepository prodRepo;
    private InMemoryOrderRepository orderRepo;
    private InMemorySampleRepository sampleRepo;
    private FixedTimeProvider timeProvider;
    private ProductionService productionService;

    @BeforeEach
    void setUp() {
        prodRepo = new InMemoryProductionRepository();
        orderRepo = new InMemoryOrderRepository();
        sampleRepo = new InMemorySampleRepository();
        timeProvider = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 0, 0));
        productionService = new ProductionService(prodRepo, orderRepo, sampleRepo, timeProvider, 1.0);
    }

    @Test
    void enqueue_저장후_조회() {
        ProductionEntry entry = new ProductionEntry("ORD-001", "S001", 5, 7, 210.0, null);
        productionService.enqueue(entry);
        assertEquals(1, prodRepo.findAll().size());
    }

    @Test
    void tick_초기_상태_빈_큐() {
        // tick은 그룹 B에서 구현 예정이므로 예외 없이 실행되는지만 확인
        assertDoesNotThrow(() -> productionService.tick());
    }

    @Test
    void getProgress_초기값_0() {
        assertEquals(0.0, productionService.getProgress(), 1e-9);
    }

    @Test
    void getCurrent_초기_비어있음() {
        assertTrue(productionService.getCurrent().isEmpty());
    }

    @Test
    void getQueue_초기_빈_리스트() {
        assertTrue(productionService.getQueue().isEmpty());
    }

    @Test
    void getEstimatedFinishAt_초기_비어있음() {
        ProductionEntry entry = new ProductionEntry("ORD-001", "S001", 5, 7, 210.0, null);
        assertTrue(productionService.getEstimatedFinishAt(entry).isEmpty());
    }
}
