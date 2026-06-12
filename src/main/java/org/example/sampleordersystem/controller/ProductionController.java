package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.service.ProductionService;
import org.example.sampleordersystem.view.View;

public class ProductionController {
    private final ProductionService productionService;
    private final View view;

    public ProductionController(ProductionService productionService, View view) {
        this.productionService = productionService;
        this.view = view;
    }

    public void handleView() {
        view.showProductionStatus(
            productionService.getCurrent(),
            productionService.getProgress(),
            productionService.getCurrent()
                .flatMap(productionService::getEstimatedFinishAt),
            productionService.getQueue()
        );
    }
}
