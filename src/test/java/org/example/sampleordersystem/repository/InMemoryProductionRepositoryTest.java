package org.example.sampleordersystem.repository;

import org.example.sampleordersystem.model.ProductionEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryProductionRepositoryTest {

    @Test
    @DisplayName("생산 항목을 삭제하면 조회되지 않는다")
    void productionDeleteRemovesEntry() {
        ProductionRepository repo = new InMemoryProductionRepository();
        ProductionEntry entry = new ProductionEntry("ORD-20240101-0001", "S1", 50, 62, 310.0, null);
        repo.save(entry);
        repo.delete("ORD-20240101-0001");
        assertTrue(repo.findByOrderId("ORD-20240101-0001").isEmpty());
    }

    @Test
    @DisplayName("저장된 모든 생산 항목을 삽입 순서대로 반환한다")
    void findAllReturnsFifoOrder() {
        ProductionRepository repo = new InMemoryProductionRepository();
        repo.save(new ProductionEntry("ORD-20240101-0001", "S1", 50, 62, 310.0, null));
        repo.save(new ProductionEntry("ORD-20240101-0002", "S2", 30, 34, 170.0, null));
        List<ProductionEntry> all = repo.findAll();
        assertEquals(2, all.size());
        assertEquals("ORD-20240101-0001", all.get(0).getOrderId());
        assertEquals("ORD-20240101-0002", all.get(1).getOrderId());
    }

    @Test
    @DisplayName("orderId로 생산 항목을 조회한다")
    void findByOrderIdReturnsEntry() {
        ProductionRepository repo = new InMemoryProductionRepository();
        repo.save(new ProductionEntry("ORD-20240101-0001", "S1", 50, 62, 310.0, null));
        Optional<ProductionEntry> found = repo.findByOrderId("ORD-20240101-0001");
        assertTrue(found.isPresent());
        assertEquals("S1", found.get().getSampleId());
    }
}
