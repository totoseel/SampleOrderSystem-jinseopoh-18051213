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

    public void handleRegister() {
        view.showMessage("시료 ID 입력:");
        String id = view.readLine();
        view.showMessage("시료 이름 입력:");
        String name = view.readLine();
        view.showMessage("평균 생산시간(분) 입력:");
        int avgMin = Integer.parseInt(view.readLine());
        view.showMessage("수율(0 초과 1 이하) 입력:");
        double yield = Double.parseDouble(view.readLine());
        view.showMessage("초기 재고 수량 입력:");
        int stock = Integer.parseInt(view.readLine());

        try {
            sampleService.register(id, name, avgMin, yield, stock);
            view.showMessage("시료가 등록되었습니다.");
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
