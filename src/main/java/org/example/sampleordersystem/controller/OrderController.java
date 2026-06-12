package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.model.Sample;
import org.example.sampleordersystem.service.OrderService;
import org.example.sampleordersystem.service.SampleService;
import org.example.sampleordersystem.view.View;

import java.util.List;

public class OrderController {
    private final OrderService orderService;
    private final SampleService sampleService;
    private final View view;

    public OrderController(OrderService orderService, SampleService sampleService, View view) {
        this.orderService = orderService;
        this.sampleService = sampleService;
        this.view = view;
    }

    public void handlePlace() {
        List<Sample> samples = sampleService.findAll();
        if (samples.isEmpty()) {
            view.showMessage("등록된 시료가 없습니다.");
            return;
        }
        view.showSamples(samples);
        view.showMessage("시료 번호 입력:");
        int sampleIndex;
        try {
            sampleIndex = Integer.parseInt(view.readLine()) - 1;
        } catch (NumberFormatException e) {
            view.showError("숫자 형식이 올바르지 않습니다: " + e.getMessage());
            return;
        }
        if (sampleIndex < 0 || sampleIndex >= samples.size()) {
            view.showError("올바른 번호를 입력하세요.");
            return;
        }
        String sampleId = samples.get(sampleIndex).getId();

        view.showMessage("고객명 입력:");
        String customerName = view.readLine();
        view.showMessage("수량 입력:");
        try {
            int quantity = Integer.parseInt(view.readLine());
            orderService.placeOrder(sampleId, customerName, quantity);
            view.showMessage("주문이 접수되었습니다.");
        } catch (NumberFormatException e) {
            view.showError("숫자 형식이 올바르지 않습니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            view.showError(e.getMessage());
        }
    }

    public void handleApproveOrReject() {
        view.showMenu(java.util.List.of("1. 승인", "2. 거절", "0. 돌아가기"));
        String input = view.readLine();
        switch (input) {
            case "1" -> handleApprove();
            case "2" -> handleReject();
            case "0" -> {}
            default -> view.showError("올바른 메뉴 번호를 입력하세요");
        }
    }

    public void handleRelease() {
        java.util.List<Order> confirmed = orderService.findByStatus(OrderStatus.CONFIRMED);
        if (confirmed.isEmpty()) {
            view.showMessage("출고할 주문이 없습니다.");
            return;
        }
        view.showOrders(confirmed);
        view.showMessage("출고할 주문 번호 입력:");
        try {
            int index = Integer.parseInt(view.readLine()) - 1;
            if (index < 0 || index >= confirmed.size()) {
                view.showError("올바른 번호를 입력하세요.");
                return;
            }
            orderService.releaseOrder(confirmed.get(index).getOrderId());
            view.showMessage("출고 처리가 완료되었습니다.");
        } catch (NumberFormatException e) {
            view.showError("숫자 형식이 올바르지 않습니다: " + e.getMessage());
        }
    }

    public void handleApprove() {
        List<Order> reserved = orderService.findByStatus(OrderStatus.RESERVED);
        if (reserved.isEmpty()) {
            view.showMessage("처리할 주문이 없습니다.");
            return;
        }
        view.showOrders(reserved);
        view.showMessage("승인할 주문 번호 입력:");
        try {
            int index = Integer.parseInt(view.readLine()) - 1;
            if (index < 0 || index >= reserved.size()) {
                view.showError("올바른 번호를 입력하세요.");
                return;
            }
            orderService.approve(reserved.get(index).getOrderId());
            view.showMessage("주문이 승인되었습니다.");
        } catch (NumberFormatException e) {
            view.showError("숫자 형식이 올바르지 않습니다: " + e.getMessage());
        }
    }

    public void handleReject() {
        List<Order> reserved = orderService.findByStatus(OrderStatus.RESERVED);
        if (reserved.isEmpty()) {
            view.showMessage("처리할 주문이 없습니다.");
            return;
        }
        view.showOrders(reserved);
        view.showMessage("거절할 주문 번호 입력:");
        try {
            int index = Integer.parseInt(view.readLine()) - 1;
            if (index < 0 || index >= reserved.size()) {
                view.showError("올바른 번호를 입력하세요.");
                return;
            }
            orderService.reject(reserved.get(index).getOrderId());
            view.showMessage("주문이 거절되었습니다.");
        } catch (NumberFormatException e) {
            view.showError("숫자 형식이 올바르지 않습니다: " + e.getMessage());
        }
    }
}
