package org.example.sampleordersystem.repository;

import org.example.sampleordersystem.model.ProductionEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonProductionRepositoryTest {

    @Test
    @DisplayName("LocalDateTime이 JSON 직렬화·역직렬화 후에도 동일하다")
    void jsonHandlesLocalDateTime(@TempDir Path dir) {
        Path file = dir.resolve("production.json");
        LocalDateTime started = LocalDateTime.of(2024, 1, 1, 9, 30, 0);
        ProductionEntry entry = new ProductionEntry(
            "ORD-20240101-0001", "S1", 50, 62, 310.0, started);

        new JsonProductionRepository(file).save(entry);

        Optional<ProductionEntry> found =
            new JsonProductionRepository(file).findByOrderId("ORD-20240101-0001");
        assertTrue(found.isPresent());
        assertEquals(started, found.get().getStartedAt());
    }

    @Test
    @DisplayName("파일이 없을 때 findAll은 빈 리스트를 반환한다")
    void findAllEmptyWhenNoFile(@TempDir Path dir) {
        Path file = dir.resolve("production.json");
        assertTrue(new JsonProductionRepository(file).findAll().isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 orderId 조회 시 빈 Optional을 반환한다")
    void findByOrderIdReturnsEmpty(@TempDir Path dir) {
        Path file = dir.resolve("production.json");
        assertTrue(new JsonProductionRepository(file).findByOrderId("NONE").isEmpty());
    }

    @Test
    @DisplayName("생산 항목을 삭제하면 파일에서도 제거된다")
    void deleteRemovesFromFile(@TempDir Path dir) {
        Path file = dir.resolve("production.json");
        JsonProductionRepository repo = new JsonProductionRepository(file);
        repo.save(new ProductionEntry("ORD-20240101-0001", "S1", 50, 62, 310.0, null));
        repo.delete("ORD-20240101-0001");
        assertTrue(repo.findByOrderId("ORD-20240101-0001").isEmpty());
    }

    @Test
    @DisplayName("같은 orderId로 저장하면 기존 항목이 업데이트된다")
    void saveUpdatesExisting(@TempDir Path dir) {
        Path file = dir.resolve("production.json");
        JsonProductionRepository repo = new JsonProductionRepository(file);
        repo.save(new ProductionEntry("ORD-20240101-0001", "S1", 50, 62, 310.0, null));
        repo.save(new ProductionEntry("ORD-20240101-0001", "S2", 30, 34, 170.0, null));
        assertEquals(1, repo.findAll().size());
        assertEquals("S2", repo.findByOrderId("ORD-20240101-0001").get().getSampleId());
    }

    @Test
    @DisplayName("잘못된 JSON 파일이면 readJson에서 RuntimeException이 발생한다")
    void readJsonThrowsOnInvalidFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("production.json");
        Files.writeString(file, "NOT_VALID_JSON");
        JsonProductionRepository repo = new JsonProductionRepository(file);
        assertThrows(RuntimeException.class, () -> repo.findAll());
    }

    @Test
    @DisplayName("존재하지 않는 부모 디렉토리에 쓰면 writeJson에서 RuntimeException이 발생한다")
    void writeJsonThrowsOnInvalidPath(@TempDir Path dir) {
        Path file = dir.resolve("nonexistent").resolve("production.json");
        JsonProductionRepository repo = new JsonProductionRepository(file);
        assertThrows(RuntimeException.class,
            () -> repo.save(new ProductionEntry("ORD-20240101-0001", "S1", 50, 62, 310.0, null)));
    }

}
