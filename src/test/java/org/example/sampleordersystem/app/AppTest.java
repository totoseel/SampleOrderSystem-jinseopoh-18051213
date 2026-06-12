package org.example.sampleordersystem.app;

import org.example.sampleordersystem.controller.*;
import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.model.ProductionEntry;
import org.example.sampleordersystem.model.Sample;
import org.example.sampleordersystem.repository.*;
import org.example.sampleordersystem.service.*;
import org.example.sampleordersystem.util.FixedTimeProvider;
import org.example.sampleordersystem.util.OrderIdGenerator;
import org.example.sampleordersystem.view.View;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    private static class FakeView implements View {
        private final Queue<String> inputs;
        FakeView(String... inputs) { this.inputs = new LinkedList<>(Arrays.asList(inputs)); }

        @Override public String readLine() { return inputs.poll(); }
        @Override public void showMessage(String msg) {}
        @Override public void showError(String msg) {}
        @Override public void showMainSummary(int a, int b, int c, int d,
                                              Optional<ProductionEntry> e, double f, int g) {}
        @Override public void showMenu(List<String> options) {}
        @Override public void showSamples(List<Sample> samples) {}
        @Override public void showOrders(List<Order> orders) {}
        @Override public void showProductionStatus(Optional<ProductionEntry> current,
                                                   double progress,
                                                   java.time.LocalDateTime estimatedFinish,
                                                   List<ProductionEntry> queue) {}
        @Override public void showMonitoringSummary(Map<OrderStatus, Long> counts,
                                                    List<Sample> samples,
                                                    Set<String> producingSampleIds) {}
    }

    private InMemorySampleRepository sampleRepo;
    private InMemoryOrderRepository orderRepo;
    private InMemoryProductionRepository productionRepo;
    private FixedTimeProvider timeProvider;
    private ProductionService productionService;
    private SampleService sampleService;
    private OrderService orderService;
    private SampleController sampleController;
    private OrderController orderController;
    private ProductionController productionController;
    private MonitoringController monitoringController;

    @BeforeEach
    void setUp() {
        sampleRepo = new InMemorySampleRepository();
        orderRepo = new InMemoryOrderRepository();
        productionRepo = new InMemoryProductionRepository();
        timeProvider = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 10, 0));
        productionService = new ProductionService(productionRepo, orderRepo, sampleRepo, timeProvider, 1.0);
        sampleService = new SampleService(sampleRepo);
        OrderIdGenerator idGenerator = new OrderIdGenerator(0, timeProvider);
        orderService = new OrderService(sampleRepo, orderRepo, productionService, idGenerator);
        FakeView view = new FakeView();
        sampleController = new SampleController(sampleService, view);
        orderController = new OrderController(orderService, view);
        productionController = new ProductionController(productionService, view);
        monitoringController = new MonitoringController(orderService, sampleService, view);
    }

    private App buildApp(View view) {
        SampleController sc = new SampleController(sampleService, view);
        OrderController oc = new OrderController(orderService, view);
        ProductionController pc = new ProductionController(productionService, view);
        MonitoringController mc = new MonitoringController(orderService, sampleService, view);
        return new App(sc, oc, pc, mc, productionService, sampleService, orderService, view);
    }

    @Test
    @DisplayName("앱은 루프 1회마다 tick()을 호출한다")
    void appCallsTickOnEachLoop() {
        int[] tickCount = {0};
        ProductionService countingService = new ProductionService(
            productionRepo, orderRepo, sampleRepo, timeProvider, 1.0) {
            @Override
            public void tick() { tickCount[0]++; }
        };

        FakeView view = new FakeView("0");
        SampleController sc = new SampleController(sampleService, view);
        OrderController oc = new OrderController(orderService, view);
        ProductionController pc = new ProductionController(countingService, view);
        MonitoringController mc = new MonitoringController(orderService, sampleService, view);
        App app = new App(sc, oc, pc, mc, countingService, sampleService, orderService, view);

        app.run();

        assertEquals(1, tickCount[0]);
    }

    @Test
    @DisplayName("입력 0은 앱을 종료한다 - 무한루프 없음")
    void appExitsOnZeroInput() {
        FakeView view = new FakeView("0");
        App app = buildApp(view);

        assertTimeoutPreemptively(Duration.ofSeconds(5), app::run);
    }

    @Test
    @DisplayName("입력 2는 OrderController.handlePlace()로 라우팅된다")
    void appRoutesToOrderPlaceOnInput2() {
        boolean[] called = {false};
        OrderController countingOrder = new OrderController(orderService, new FakeView()) {
            @Override
            public void handlePlace() { called[0] = true; }
        };

        FakeView view = new FakeView("2", "0");
        SampleController sc = new SampleController(sampleService, view);
        ProductionController pc = new ProductionController(productionService, view);
        MonitoringController mc = new MonitoringController(orderService, sampleService, view);
        App app = new App(sc, countingOrder, pc, mc, productionService, sampleService, orderService, view);

        assertTimeoutPreemptively(Duration.ofSeconds(5), app::run);
        assertTrue(called[0]);
    }

    @Test
    @DisplayName("입력 1은 SampleController.handleSampleMenu()로 라우팅된다")
    void appRoutesToSampleMenuOnInput1() {
        boolean[] called = {false};
        SampleController countingSample = new SampleController(sampleService, new FakeView()) {
            @Override
            public void handleSampleMenu() { called[0] = true; }
        };

        FakeView view = new FakeView("1", "0");
        OrderController oc = new OrderController(orderService, view);
        ProductionController pc = new ProductionController(productionService, view);
        MonitoringController mc = new MonitoringController(orderService, sampleService, view);
        App app = new App(countingSample, oc, pc, mc, productionService, sampleService, orderService, view);

        assertTimeoutPreemptively(Duration.ofSeconds(5), app::run);
        assertTrue(called[0]);
    }

    @Test
    @DisplayName("입력 3은 OrderController.handleApproveOrReject()로 라우팅된다")
    void appRoutesToApproveOrRejectOnInput3() {
        boolean[] called = {false};
        OrderController countingOrder = new OrderController(orderService, new FakeView()) {
            @Override
            public void handleApproveOrReject() { called[0] = true; }
        };

        FakeView view = new FakeView("3", "0");
        SampleController sc = new SampleController(sampleService, view);
        ProductionController pc = new ProductionController(productionService, view);
        MonitoringController mc = new MonitoringController(orderService, sampleService, view);
        App app = new App(sc, countingOrder, pc, mc, productionService, sampleService, orderService, view);

        assertTimeoutPreemptively(Duration.ofSeconds(5), app::run);
        assertTrue(called[0]);
    }

    @Test
    @DisplayName("입력 4는 MonitoringController.handleView()로 라우팅된다")
    void appRoutesToMonitoringOnInput4() {
        boolean[] called = {false};
        MonitoringController countingMonitoring = new MonitoringController(orderService, sampleService, new FakeView()) {
            @Override
            public void handleView() { called[0] = true; }
        };

        FakeView view = new FakeView("4", "0");
        SampleController sc = new SampleController(sampleService, view);
        OrderController oc = new OrderController(orderService, view);
        ProductionController pc = new ProductionController(productionService, view);
        App app = new App(sc, oc, pc, countingMonitoring, productionService, sampleService, orderService, view);

        assertTimeoutPreemptively(Duration.ofSeconds(5), app::run);
        assertTrue(called[0]);
    }

    @Test
    @DisplayName("입력 5는 ProductionController.handleView()로 라우팅된다")
    void appRoutesToProductionOnInput5() {
        boolean[] called = {false};
        ProductionController countingProduction = new ProductionController(productionService, new FakeView()) {
            @Override
            public void handleView() { called[0] = true; }
        };

        FakeView view = new FakeView("5", "0");
        SampleController sc = new SampleController(sampleService, view);
        OrderController oc = new OrderController(orderService, view);
        MonitoringController mc = new MonitoringController(orderService, sampleService, view);
        App app = new App(sc, oc, countingProduction, mc, productionService, sampleService, orderService, view);

        assertTimeoutPreemptively(Duration.ofSeconds(5), app::run);
        assertTrue(called[0]);
    }

    @Test
    @DisplayName("입력 6은 OrderController.handleRelease()로 라우팅된다")
    void appRoutesToReleaseOnInput6() {
        boolean[] called = {false};
        OrderController countingOrder = new OrderController(orderService, new FakeView()) {
            @Override
            public void handleRelease() { called[0] = true; }
        };

        FakeView view = new FakeView("6", "0");
        SampleController sc = new SampleController(sampleService, view);
        ProductionController pc = new ProductionController(productionService, view);
        MonitoringController mc = new MonitoringController(orderService, sampleService, view);
        App app = new App(sc, countingOrder, pc, mc, productionService, sampleService, orderService, view);

        assertTimeoutPreemptively(Duration.ofSeconds(5), app::run);
        assertTrue(called[0]);
    }

    @Test
    @DisplayName("잘못된 입력(99)은 showError를 호출한다")
    void appShowsErrorOnInvalidInput() {
        List<String> errors = new ArrayList<>();
        View errorCapture = new FakeView("99", "0") {
            @Override
            public void showError(String msg) { errors.add(msg); }
        };
        App app = buildApp(errorCapture);

        assertTimeoutPreemptively(Duration.ofSeconds(5), app::run);
        assertFalse(errors.isEmpty());
    }
}
