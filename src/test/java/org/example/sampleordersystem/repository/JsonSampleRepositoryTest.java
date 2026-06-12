package org.example.sampleordersystem.repository;

import org.example.sampleordersystem.model.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonSampleRepositoryTest {

    @Test
    @DisplayName("JSON 파일에 저장 후 새 인스턴스에서 재로드할 수 있다")
    void jsonSaveAndReload(@TempDir Path dir) {
        Path file = dir.resolve("samples.json");
        JsonSampleRepository repo1 = new JsonSampleRepository(file);
        repo1.save(new Sample("S1", "갈륨비소", 5, 0.9, 100));

        JsonSampleRepository repo2 = new JsonSampleRepository(file);
        Optional<Sample> found = repo2.findById("S1");
        assertTrue(found.isPresent());
        assertEquals("갈륨비소", found.get().getName());
    }

    @Test
    @DisplayName("저장 후 임시 파일이 남지 않는다")
    void jsonAtomicWrite(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("samples.json");
        JsonSampleRepository repo = new JsonSampleRepository(file);
        repo.save(new Sample("S1", "A", 5, 0.9, 10));

        long fileCount = Files.list(dir).count();
        assertEquals(1, fileCount);
    }

    @Test
    @DisplayName("파일이 없을 때 findAll은 빈 리스트를 반환한다")
    void findAllEmptyWhenNoFile(@TempDir Path dir) {
        Path file = dir.resolve("samples.json");
        JsonSampleRepository repo = new JsonSampleRepository(file);
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    @DisplayName("이름 키워드로 시료를 검색한다")
    void findByNameContaining(@TempDir Path dir) {
        Path file = dir.resolve("samples.json");
        JsonSampleRepository repo = new JsonSampleRepository(file);
        repo.save(new Sample("S1", "갈륨비소", 5, 0.9, 10));
        repo.save(new Sample("S2", "실리콘", 5, 0.9, 10));
        assertEquals(1, repo.findByNameContaining("갈륨").size());
    }

    @Test
    @DisplayName("존재하지 않는 ID 조회 시 빈 Optional을 반환한다")
    void findByIdReturnsEmpty(@TempDir Path dir) {
        Path file = dir.resolve("samples.json");
        JsonSampleRepository repo = new JsonSampleRepository(file);
        assertTrue(repo.findById("NONE").isEmpty());
    }

    @Test
    @DisplayName("같은 ID로 저장하면 기존 항목이 업데이트된다")
    void saveUpdatesExisting(@TempDir Path dir) {
        Path file = dir.resolve("samples.json");
        JsonSampleRepository repo = new JsonSampleRepository(file);
        repo.save(new Sample("S1", "이름A", 5, 0.9, 10));
        repo.save(new Sample("S1", "이름B", 5, 0.9, 20));
        assertEquals(1, repo.findAll().size());
        assertEquals("이름B", repo.findById("S1").get().getName());
    }

    @Test
    @DisplayName("잘못된 JSON 파일이면 readJson에서 RuntimeException이 발생한다")
    void readJsonThrowsOnInvalidFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("samples.json");
        Files.writeString(file, "NOT_VALID_JSON");
        JsonSampleRepository repo = new JsonSampleRepository(file);
        assertThrows(RuntimeException.class, () -> repo.findAll());
    }

    @Test
    @DisplayName("존재하지 않는 부모 디렉토리에 쓰면 writeJson에서 RuntimeException이 발생한다")
    void writeJsonThrowsOnInvalidPath(@TempDir Path dir) {
        Path file = dir.resolve("nonexistent").resolve("samples.json");
        JsonSampleRepository repo = new JsonSampleRepository(file);
        assertThrows(RuntimeException.class, () -> repo.save(new Sample("S1", "A", 5, 0.9, 10)));
    }

}
