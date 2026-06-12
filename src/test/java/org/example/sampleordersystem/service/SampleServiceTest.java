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
}
