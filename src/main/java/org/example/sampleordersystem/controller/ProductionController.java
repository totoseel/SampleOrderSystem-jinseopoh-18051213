package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.service.ProductionService;
import org.example.sampleordersystem.view.View;

import java.util.List;

public class ProductionController {
    private final ProductionService productionService;
    private final View view;

    public ProductionController(ProductionService productionService, View view) {
        this.productionService = productionService;
        this.view = view;
    }

    public void handleView() {
        productionService.tick();
        view.showProductionStatus(
            productionService.getCurrent(),
            productionService.getProgress(),
            productionService.getCurrent()
                .flatMap(productionService::getEstimatedFinishAt),
            productionService.getQueue()
        );
        view.showMenu(List.of("1. 새로고침", "2. 취소", "0. 돌아가기"));
        String input = view.readLine();
        switch (input) {
            case "1" -> handleView();
            case "2" -> handleCancel();
            case "0" -> {}
            default -> view.showError("올바른 메뉴 번호를 입력하세요");
        }
    }

    private void handleCancel() {
        List<ProductionEntry> queue = productionService.getAllEntries();
        if (queue.isEmpty()) {
            view.showMessage("취소할 생산 항목이 없습니다.");
            return;
        }
        view.showMessage("취소할 항목 번호 입력 (현재 생산 중 포함):");
        try {
            int index = Integer.parseInt(view.readLine()) - 1;
            if (index < 0 || index >= queue.size()) {
                view.showError("올바른 번호를 입력하세요.");
                return;
            }
            String orderId = queue.get(index).getOrderId();
            productionService.cancel(orderId);
            view.showMessage("생산이 취소되었습니다. 주문이 대기 상태로 복원되었습니다.");
        } catch (NumberFormatException e) {
            view.showError("숫자 형식이 올바르지 않습니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            view.showError(e.getMessage());
        }
    }
}
