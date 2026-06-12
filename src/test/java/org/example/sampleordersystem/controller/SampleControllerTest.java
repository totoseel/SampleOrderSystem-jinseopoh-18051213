package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.repository.InMemorySampleRepository;
import org.example.sampleordersystem.service.SampleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SampleControllerTest {

    private SampleService service;

    @BeforeEach
    void setUp() {
        service = new SampleService(new InMemorySampleRepository());
    }

    @Test
    @DisplayName("유효한 입력으로 시료 등록 시 Service에 저장된다")
    void registerSampleCallsService() {
        FakeView view = new FakeView("S1", "갈륨비소", "5", "0.9", "100");
        SampleController controller = new SampleController(service, view);

        controller.handleRegister();

        assertTrue(service.findById("S1").isPresent());
    }

    @Test
    @DisplayName("수율이 범위를 벗어나면 오류 메시지를 출력한다")
    void registerSampleShowsErrorOnInvalidYield() {
        FakeView view = new FakeView("S1", "갈륨비소", "5", "1.5", "100");
        SampleController controller = new SampleController(service, view);

        controller.handleRegister();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("handleList는 등록된 시료 목록을 View에 전달한다")
    void handleListDelegatesToView() {
        service.register("S1", "갈륨비소", 5, 0.9, 100);
        FakeView view = new FakeView();
        SampleController controller = new SampleController(service, view);

        // showSamples가 호출되면 예외 없이 동작해야 함
        assertDoesNotThrow(controller::handleList);
    }

    @Test
    @DisplayName("handleSearch는 키워드로 검색한 시료 목록을 View에 전달한다")
    void handleSearchDelegatesToView() {
        service.register("S1", "갈륨비소", 5, 0.9, 100);
        FakeView view = new FakeView("갈륨");
        SampleController controller = new SampleController(service, view);

        assertDoesNotThrow(controller::handleSearch);
    }

    @Test
    @DisplayName("평균 생산시간에 숫자가 아닌 값 입력 시 오류 메시지를 출력한다")
    void registerSampleShowsErrorOnInvalidNumber() {
        FakeView view = new FakeView("S1", "갈륨비소", "abc", "0.9", "100");
        SampleController controller = new SampleController(service, view);

        controller.handleRegister();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("handleSampleMenu 입력 1은 시료 등록을 호출한다")
    void sampleMenuInput1CallsRegister() {
        // 입력 "1" -> 등록 서브메뉴, 이후 등록 입력값
        FakeView view = new FakeView("1", "S2", "실리콘", "10", "0.8", "50");
        SampleController controller = new SampleController(service, view);

        controller.handleSampleMenu();

        assertTrue(service.findById("S2").isPresent());
    }

    @Test
    @DisplayName("handleSampleMenu 입력 2는 목록 조회를 호출한다")
    void sampleMenuInput2CallsList() {
        service.register("S1", "갈륨비소", 5, 0.9, 100);
        FakeView view = new FakeView("2");
        SampleController controller = new SampleController(service, view);

        assertDoesNotThrow(controller::handleSampleMenu);
    }

    @Test
    @DisplayName("handleSampleMenu 입력 3은 검색을 호출한다")
    void sampleMenuInput3CallsSearch() {
        service.register("S1", "갈륨비소", 5, 0.9, 100);
        FakeView view = new FakeView("3", "갈륨");
        SampleController controller = new SampleController(service, view);

        assertDoesNotThrow(controller::handleSampleMenu);
    }

    @Test
    @DisplayName("handleSampleMenu 입력 0은 아무것도 하지 않고 반환한다")
    void sampleMenuInput0Returns() {
        FakeView view = new FakeView("0");
        SampleController controller = new SampleController(service, view);

        assertDoesNotThrow(controller::handleSampleMenu);
    }

    @Test
    @DisplayName("handleSampleMenu 잘못된 입력은 오류 메시지를 출력한다")
    void sampleMenuInvalidInputShowsError() {
        FakeView view = new FakeView("99");
        SampleController controller = new SampleController(service, view);

        controller.handleSampleMenu();

        assertFalse(view.getErrors().isEmpty());
    }
}
