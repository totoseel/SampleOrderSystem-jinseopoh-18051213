package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.service.OrderService;
import org.example.sampleordersystem.view.View;

import java.util.List;

public class OrderController {
    private final OrderService orderService;
    private final View view;

    public OrderController(OrderService orderService, View view) {
        this.orderService = orderService;
        this.view = view;
    }

    public void handlePlace() {
        view.showMessage("시료 ID 입력:");
        String sampleId = view.readLine();
        view.showMessage("고객명 입력:");
        String customerName = view.readLine();
        view.showMessage("수량 입력:");
        String quantityStr = view.readLine();

        try {
            int quantity = Integer.parseInt(quantityStr);
            orderService.placeOrder(sampleId, customerName, quantity);
            view.showMessage("주문이 접수되었습니다.");
        } catch (NumberFormatException e) {
            view.showError("숫자 형식이 올바르지 않습니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            view.showError(e.getMessage());
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
