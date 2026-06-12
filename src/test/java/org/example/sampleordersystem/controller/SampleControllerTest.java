package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.repository.InMemorySampleRepository;
import org.example.sampleordersystem.service.SampleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SampleControllerTest {

    @Test
    @DisplayName("유효한 입력으로 시료 등록 시 Service에 저장된다")
    void registerSampleCallsService() {
        SampleService service = new SampleService(new InMemorySampleRepository());
        FakeView view = new FakeView("S1", "갈륨비소", "5", "0.9", "100");
        SampleController controller = new SampleController(service, view);

        controller.handleRegister();

        assertTrue(service.findById("S1").isPresent());
    }

    @Test
    @DisplayName("수율이 범위를 벗어나면 오류 메시지를 출력한다")
    void registerSampleShowsErrorOnInvalidYield() {
        SampleService service = new SampleService(new InMemorySampleRepository());
        FakeView view = new FakeView("S1", "갈륨비소", "5", "1.5", "100");
        SampleController controller = new SampleController(service, view);

        controller.handleRegister();

        assertFalse(view.getErrors().isEmpty());
    }
}
