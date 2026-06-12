package org.example.sampleordersystem.service;

import org.example.sampleordersystem.model.Sample;
import org.example.sampleordersystem.repository.InMemorySampleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SampleServiceTest {

    @Test
    void register_정상등록() {
        SampleService service = new SampleService(new InMemorySampleRepository());
        Sample result = service.register("S001", "실리콘웨이퍼", 30, 0.9, 100);
        assertNotNull(result);
        assertEquals("S001", result.getId());
        assertEquals("실리콘웨이퍼", result.getName());
        assertEquals(30, result.getAvgProductionMinutes());
        assertEquals(0.9, result.getYield(), 1e-9);
        assertEquals(100, result.getStock());
    }

    @Test
    void register_중복ID_예외() {
        SampleService service = new SampleService(new InMemorySampleRepository());
        service.register("S001", "웨이퍼A", 30, 0.9, 100);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.register("S001", "웨이퍼B", 20, 0.8, 50));
        assertEquals("이미 등록된 시료 ID입니다", ex.getMessage());
    }

    @Test
    void findAll_등록된_시료_목록() {
        SampleService service = new SampleService(new InMemorySampleRepository());
        service.register("S001", "웨이퍼A", 30, 0.9, 100);
        service.register("S002", "웨이퍼B", 20, 0.8, 50);
        assertEquals(2, service.findAll().size());
    }

    @Test
    void findByNameContaining_검색() {
        SampleService service = new SampleService(new InMemorySampleRepository());
        service.register("S001", "실리콘웨이퍼", 30, 0.9, 100);
        service.register("S002", "갈륨비소", 20, 0.8, 50);
        List<Sample> result = service.findByNameContaining("웨이퍼");
        assertEquals(1, result.size());
        assertEquals("S001", result.get(0).getId());
    }

    @Test
    void findById_존재하는_시료() {
        SampleService service = new SampleService(new InMemorySampleRepository());
        service.register("S001", "웨이퍼", 30, 0.9, 100);
        Optional<Sample> result = service.findById("S001");
        assertTrue(result.isPresent());
    }

    @Test
    void findById_없는_시료() {
        SampleService service = new SampleService(new InMemorySampleRepository());
        Optional<Sample> result = service.findById("NONE");
        assertTrue(result.isEmpty());
    }
}
