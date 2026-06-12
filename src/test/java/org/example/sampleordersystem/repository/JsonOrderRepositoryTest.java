package org.example.sampleordersystem.repository;

import org.example.sampleordersystem.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonOrderRepositoryTest {

    @Test
    @DisplayName("재시작 후에도 JSON 파일에서 주문 데이터를 복구한다")
    void jsonSurvivesRestart(@TempDir Path dir) {
        Path file = dir.resolve("orders.json");
        Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
                                LocalDateTime.of(2024, 1, 1, 9, 0));

        new JsonOrderRepository(file).save(order);

        List<Order> orders = new JsonOrderRepository(file).findAll();
        assertEquals(1, orders.size());
        assertEquals("ORD-20240101-0001", orders.get(0).getOrderId());
    }

    @Test
    @DisplayName("파일이 없을 때 findAll은 빈 리스트를 반환한다")
    void findAllEmptyWhenNoFile(@TempDir Path dir) {
        Path file = dir.resolve("orders.json");
        assertTrue(new JsonOrderRepository(file).findAll().isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 ID 조회 시 빈 Optional을 반환한다")
    void findByIdReturnsEmpty(@TempDir Path dir) {
        Path file = dir.resolve("orders.json");
        assertTrue(new JsonOrderRepository(file).findById("NONE").isEmpty());
    }

    @Test
    @DisplayName("상태로 주문을 필터링한다")
    void findByStatus(@TempDir Path dir) {
        Path file = dir.resolve("orders.json");
        JsonOrderRepository repo = new JsonOrderRepository(file);
        Order o1 = new Order("ORD-20240101-0001", "S1", "A", 10, LocalDateTime.now());
        Order o2 = new Order("ORD-20240101-0002", "S1", "B", 5, LocalDateTime.now());
        o2.transitionTo(org.example.sampleordersystem.model.OrderStatus.CONFIRMED);
        repo.save(o1);
        repo.save(o2);
        // RESERVED 1개, CONFIRMED 1개 → 두 브랜치 모두 커버
        assertEquals(1, repo.findByStatus(org.example.sampleordersystem.model.OrderStatus.RESERVED).size());
        assertEquals(1, repo.findByStatus(org.example.sampleordersystem.model.OrderStatus.CONFIRMED).size());
    }

    @Test
    @DisplayName("날짜 prefix로 주문 수를 카운트한다")
    void countByDatePrefix(@TempDir Path dir) {
        Path file = dir.resolve("orders.json");
        JsonOrderRepository repo = new JsonOrderRepository(file);
        repo.save(new Order("ORD-20240101-0001", "S1", "A", 1, LocalDateTime.now()));
        repo.save(new Order("ORD-20240102-0001", "S1", "B", 1, LocalDateTime.now()));
        assertEquals(1, repo.countByDatePrefix("20240101"));
    }

    @Test
    @DisplayName("같은 ID로 저장하면 기존 주문이 업데이트된다")
    void saveUpdatesExisting(@TempDir Path dir) {
        Path file = dir.resolve("orders.json");
        JsonOrderRepository repo = new JsonOrderRepository(file);
        Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10, LocalDateTime.now());
        repo.save(order);
        order.transitionTo(org.example.sampleordersystem.model.OrderStatus.CONFIRMED);
        repo.save(order);
        assertEquals(1, repo.findAll().size());
        assertEquals(org.example.sampleordersystem.model.OrderStatus.CONFIRMED,
                     repo.findById("ORD-20240101-0001").get().getStatus());
    }

    @Test
    @DisplayName("잘못된 JSON 파일이면 readJson에서 RuntimeException이 발생한다")
    void readJsonThrowsOnInvalidFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("orders.json");
        Files.writeString(file, "NOT_VALID_JSON");
        JsonOrderRepository repo = new JsonOrderRepository(file);
        assertThrows(RuntimeException.class, () -> repo.findAll());
    }

    @Test
    @DisplayName("존재하지 않는 부모 디렉토리에 쓰면 writeJson에서 RuntimeException이 발생한다")
    void writeJsonThrowsOnInvalidPath(@TempDir Path dir) {
        Path file = dir.resolve("nonexistent").resolve("orders.json");
        JsonOrderRepository repo = new JsonOrderRepository(file);
        assertThrows(RuntimeException.class,
            () -> repo.save(new Order("ORD-20240101-0001", "S1", "A", 1, LocalDateTime.now())));
    }

}
