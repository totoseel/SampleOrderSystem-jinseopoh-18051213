package org.example.sampleordersystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import org.example.sampleordersystem.app.App;
import org.example.sampleordersystem.controller.MonitoringController;
import org.example.sampleordersystem.controller.OrderController;
import org.example.sampleordersystem.controller.ProductionController;
import org.example.sampleordersystem.controller.SampleController;
import org.example.sampleordersystem.repository.JsonOrderRepository;
import org.example.sampleordersystem.repository.JsonProductionRepository;
import org.example.sampleordersystem.repository.JsonSampleRepository;
import org.example.sampleordersystem.service.OrderService;
import org.example.sampleordersystem.service.ProductionService;
import org.example.sampleordersystem.service.SampleService;
import org.example.sampleordersystem.util.OrderIdGenerator;
import org.example.sampleordersystem.util.SystemTimeProvider;
import org.example.sampleordersystem.util.TimeProvider;
import org.example.sampleordersystem.view.ConsoleView;
import org.example.sampleordersystem.view.View;

public class Main {
    public static void main(String[] args) throws IOException {
        double timeScale = parseTimeScale(args);

        Path dataDir = Path.of("data");
        Files.createDirectories(dataDir);

        JsonSampleRepository sampleRepo = new JsonSampleRepository(dataDir.resolve("samples.json"));
        JsonOrderRepository orderRepo = new JsonOrderRepository(dataDir.resolve("orders.json"));
        JsonProductionRepository productionRepo = new JsonProductionRepository(dataDir.resolve("production.json"));

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int lastSeq = orderRepo.countByDatePrefix(today);
        TimeProvider timeProvider = new SystemTimeProvider();
        OrderIdGenerator idGenerator = new OrderIdGenerator(lastSeq, timeProvider);

        ProductionService productionService = new ProductionService(
            productionRepo, orderRepo, sampleRepo, timeProvider, timeScale);
        SampleService sampleService = new SampleService(sampleRepo);
        OrderService orderService = new OrderService(sampleRepo, orderRepo, productionService, idGenerator);

        View view = new ConsoleView(new Scanner(System.in), System.out);
        SampleController sampleCtrl = new SampleController(sampleService, view);
        OrderController orderCtrl = new OrderController(orderService, sampleService, view);
        ProductionController productionCtrl = new ProductionController(productionService, view);
        MonitoringController monitoringCtrl = new MonitoringController(orderService, sampleService, view);

        new App(sampleCtrl, orderCtrl, productionCtrl, monitoringCtrl,
            productionService, sampleService, orderService, view).run();
    }

    private static double parseTimeScale(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--time-scale".equals(args[i])) {
                try {
                    double scale = Double.parseDouble(args[i + 1]);
                    if (scale > 0) return scale;
                } catch (NumberFormatException ignored) {}
            }
        }
        return 1.0;
    }
}
