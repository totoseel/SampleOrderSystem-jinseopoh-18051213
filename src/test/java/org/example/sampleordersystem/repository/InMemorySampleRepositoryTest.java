package org.example.sampleordersystem.repository;

import org.example.sampleordersystem.model.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemorySampleRepositoryTest {

    @Test
    @DisplayName("저장한 시료를 ID로 조회할 수 있다")
    void saveAndFindById() {
        SampleRepository repo = new InMemorySampleRepository();
        Sample sample = new Sample("S1", "실리콘", 5, 0.9, 100);
        repo.save(sample);
        Optional<Sample> found = repo.findById("S1");
        assertTrue(found.isPresent());
        assertEquals("실리콘", found.get().getName());
    }

    @Test
    @DisplayName("저장된 모든 시료를 반환한다")
    void findAllReturnsAll() {
        SampleRepository repo = new InMemorySampleRepository();
        repo.save(new Sample("S1", "A", 5, 0.9, 10));
        repo.save(new Sample("S2", "B", 5, 0.9, 10));
        repo.save(new Sample("S3", "C", 5, 0.9, 10));
        assertEquals(3, repo.findAll().size());
    }

    @Test
    @DisplayName("이름에 키워드가 포함된 시료를 반환한다")
    void findByNameContaining() {
        SampleRepository repo = new InMemorySampleRepository();
        repo.save(new Sample("S1", "갈륨비소", 5, 0.9, 10));
        repo.save(new Sample("S2", "갈륨나이트라이드", 5, 0.9, 10));
        repo.save(new Sample("S3", "실리콘", 5, 0.9, 10));
        assertEquals(2, repo.findByNameContaining("갈륨").size());
    }
}
