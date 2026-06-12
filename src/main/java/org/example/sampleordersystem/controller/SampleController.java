package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.service.SampleService;
import org.example.sampleordersystem.view.View;

public class SampleController {
    private final SampleService sampleService;
    private final View view;

    public SampleController(SampleService sampleService, View view) {
        this.sampleService = sampleService;
        this.view = view;
    }

    public void handleSampleMenu() {
        view.showMenu(java.util.List.of("1. 시료 등록", "2. 시료 목록", "3. 시료 검색", "0. 돌아가기"));
        String input = view.readLine();
        switch (input) {
            case "1" -> handleRegister();
            case "2" -> handleList();
            case "3" -> handleSearch();
            case "0" -> {}
            default -> view.showError("올바른 메뉴 번호를 입력하세요");
        }
    }

    public void handleRegister() {
        view.showMessage("시료 ID 입력:");
        String id = view.readLine();
        view.showMessage("시료 이름 입력:");
        String name = view.readLine();
        view.showMessage("평균 생산시간(분) 입력:");
        String avgMinStr = view.readLine();
        view.showMessage("수율(0 초과 1 이하) 입력:");
        String yieldStr = view.readLine();
        view.showMessage("초기 재고 수량 입력:");
        String stockStr = view.readLine();

        try {
            int avgMin = Integer.parseInt(avgMinStr);
            double yield = Double.parseDouble(yieldStr);
            int stock = Integer.parseInt(stockStr);
            sampleService.register(id, name, avgMin, yield, stock);
            view.showMessage("시료가 등록되었습니다.");
        } catch (NumberFormatException e) {
            view.showError("숫자 형식이 올바르지 않습니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            view.showError(e.getMessage());
        }
    }

    public void handleList() {
        view.showSamples(sampleService.findAll());
    }

    public void handleSearch() {
        view.showMessage("검색 키워드 입력:");
        String kw = view.readLine();
        view.showSamples(sampleService.findByNameContaining(kw));
    }
}
