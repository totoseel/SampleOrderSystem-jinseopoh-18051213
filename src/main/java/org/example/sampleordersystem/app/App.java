package org.example.sampleordersystem.app;

import java.util.List;
import java.util.Optional;
import org.example.sampleordersystem.controller.MonitoringController;
import org.example.sampleordersystem.controller.OrderController;
import org.example.sampleordersystem.controller.ProductionController;
import org.example.sampleordersystem.controller.SampleController;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.model.Sample;
import org.example.sampleordersystem.service.OrderService;
import org.example.sampleordersystem.service.ProductionService;
import org.example.sampleordersystem.service.SampleService;
import org.example.sampleordersystem.view.View;

public class App {
    private final SampleController sampleController;
    private final OrderController orderController;
    private final ProductionController productionController;
    private final MonitoringController monitoringController;
    private final ProductionService productionService;
    private final SampleService sampleService;
    private final OrderService orderService;
    private final View view;

    public App(SampleController sampleController,
               OrderController orderController,
               ProductionController productionController,
               MonitoringController monitoringController,
               ProductionService productionService,
               SampleService sampleService,
               OrderService orderService,
               View view) {
        this.sampleController = sampleController;
        this.orderController = orderController;
        this.productionController = productionController;
        this.monitoringController = monitoringController;
        this.productionService = productionService;
        this.sampleService = sampleService;
        this.orderService = orderService;
        this.view = view;
    }

    public void run() {
        List<String> menu = List.of(
            "0. 종료", "1. 시료 관리", "2. 시료 주문",
            "3. 주문 승인/거절", "4. 모니터링",
            "5. 생산라인", "6. 출고 처리"
        );
        while (true) {
            productionService.tick();

            int sampleCount = sampleService.findAll().size();
            int totalStock = sampleService.findAll().stream()
                .mapToInt(Sample::getStock).sum();
            int orderCount = orderService.findAll().size();
            int queueSize = productionService.getQueue().size();
            Optional<ProductionEntry> current = productionService.getCurrent();
            double progress = productionService.getProgress();
            int confirmedCount = orderService.findByStatus(OrderStatus.CONFIRMED).size();

            view.showMainSummary(sampleCount, totalStock, orderCount, queueSize,
                current, progress, confirmedCount);
            view.showMenu(menu);
            String input = view.readLine();

            switch (input) {
                case "1" -> sampleController.handleSampleMenu();
                case "2" -> orderController.handlePlace();
                case "3" -> orderController.handleApproveOrReject();
                case "4" -> monitoringController.handleView();
                case "5" -> productionController.handleView();
                case "6" -> orderController.handleRelease();
                case "0" -> { return; }
                default -> view.showError("올바른 메뉴 번호를 입력하세요");
            }
        }
    }
}
