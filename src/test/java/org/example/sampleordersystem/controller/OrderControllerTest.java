package org.example.sampleordersystem.controller;

import org.example.sampleordersystem.model.OrderStatus;
import org.example.sampleordersystem.repository.InMemoryOrderRepository;
import org.example.sampleordersystem.repository.InMemoryProductionRepository;
import org.example.sampleordersystem.repository.InMemorySampleRepository;
import org.example.sampleordersystem.service.OrderService;
import org.example.sampleordersystem.service.ProductionService;
import org.example.sampleordersystem.service.SampleService;
import org.example.sampleordersystem.util.FixedTimeProvider;
import org.example.sampleordersystem.util.OrderIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrderControllerTest {

    private SampleService sampleService;
    private OrderService orderService;
    private InMemoryOrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        InMemorySampleRepository sampleRepo = new InMemorySampleRepository();
        orderRepository = new InMemoryOrderRepository();
        InMemoryProductionRepository productionRepo = new InMemoryProductionRepository();
        FixedTimeProvider timeProvider = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 10, 0));
        ProductionService productionService = new ProductionService(
            productionRepo, orderRepository, sampleRepo, timeProvider, 1.0);
        OrderIdGenerator idGenerator = new OrderIdGenerator(0, timeProvider);
        sampleService = new SampleService(sampleRepo);
        orderService = new OrderService(sampleRepo, orderRepository, productionService, idGenerator);

        // 시료 목록 번호 선택 테스트를 위해 2개 등록: 1=갈륨비소, 2=실리콘
        sampleService.register("S1", "갈륨비소", 5, 0.9, 100);
        sampleService.register("S2", "실리콘", 3, 0.8, 50);
    }

    @Test
    @DisplayName("번호로 시료를 선택하여 주문 접수 시 Service에 저장된다")
    void placeOrderCallsService() {
        FakeView view = new FakeView("1", "홍길동", "10");
        OrderController controller = new OrderController(orderService, sampleService, view);

        controller.handlePlace();

        assertEquals(1, orderService.findByStatus(OrderStatus.RESERVED).size());
    }

    @Test
    @DisplayName("두 번째 시료를 번호로 선택하여 주문 시 해당 시료 ID로 주문된다")
    void placeOrderSelectsCorrectSampleByIndex() {
        FakeView view = new FakeView("2", "홍길동", "5");
        OrderController controller = new OrderController(orderService, sampleService, view);

        controller.handlePlace();

        assertEquals("S2", orderService.findByStatus(OrderStatus.RESERVED).get(0).getSampleId());
    }

    @Test
    @DisplayName("범위를 벗어난 시료 번호(초과) 입력 시 오류가 출력된다")
    void placeOrderShowsErrorOnOutOfRangeSampleIndex() {
        FakeView view = new FakeView("99");
        OrderController controller = new OrderController(orderService, sampleService, view);

        controller.handlePlace();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("0 이하 시료 번호 입력 시 오류가 출력된다")
    void placeOrderShowsErrorOnZeroSampleIndex() {
        FakeView view = new FakeView("0");
        OrderController controller = new OrderController(orderService, sampleService, view);

        controller.handlePlace();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("시료 번호에 숫자가 아닌 값 입력 시 오류가 출력된다")
    void placeOrderShowsErrorOnNonNumericSampleIndex() {
        FakeView view = new FakeView("abc");
        OrderController controller = new OrderController(orderService, sampleService, view);

        controller.handlePlace();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("시료 선택 후 OrderService가 IllegalArgumentException을 던지면 오류가 출력된다")
    void placeOrderShowsErrorOnServiceException() {
        OrderService throwingService = new OrderService(
                new InMemorySampleRepository(), orderRepository,
                new ProductionService(new InMemoryProductionRepository(), orderRepository,
                        new InMemorySampleRepository(),
                        new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 10, 0)), 1.0),
                new OrderIdGenerator(0, new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 10, 0)))) {
            @Override
            public org.example.sampleordersystem.model.Order placeOrder(
                    String sampleId, String customerName, int quantity) {
                throw new IllegalArgumentException("서비스 오류");
            }
        };
        FakeView view = new FakeView("1", "홍길동", "10");
        OrderController controller = new OrderController(throwingService, sampleService, view);

        controller.handlePlace();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("등록된 시료가 없을 때 주문 접수 시 안내 메시지가 출력된다")
    void placeOrderShowsMessageWhenNoSamples() {
        SampleService emptySampleService = new SampleService(new InMemorySampleRepository());
        FakeView view = new FakeView();
        OrderController controller = new OrderController(orderService, emptySampleService, view);

        controller.handlePlace();

        assertTrue(view.getMessages().stream().anyMatch(m -> m.contains("없습니다")));
    }

    @Test
    @DisplayName("RESERVED 주문 승인 시 해당 주문 상태가 변경된다")
    void approveCallsService() {
        FakeView placeView = new FakeView("1", "홍길동", "10");
        new OrderController(orderService, sampleService, placeView).handlePlace();

        FakeView approveView = new FakeView("1");
        new OrderController(orderService, sampleService, approveView).handleApprove();

        assertEquals(OrderStatus.CONFIRMED, orderService.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("RESERVED 주문이 없을 때 승인 시 안내 메시지가 출력된다")
    void approveShowsMessageWhenNoReserved() {
        FakeView view = new FakeView();
        OrderController controller = new OrderController(orderService, sampleService, view);

        controller.handleApprove();

        assertTrue(view.getMessages().stream().anyMatch(m -> m.contains("없습니다")));
    }

    @Test
    @DisplayName("승인 번호가 범위를 벗어나면 오류가 출력된다")
    void approveShowsErrorOnOutOfRangeIndex() {
        FakeView placeView = new FakeView("1", "홍길동", "10");
        new OrderController(orderService, sampleService, placeView).handlePlace();

        FakeView approveView = new FakeView("99");
        new OrderController(orderService, sampleService, approveView).handleApprove();

        assertFalse(approveView.getErrors().isEmpty());
    }

    @Test
    @DisplayName("RESERVED 주문 거절 시 해당 주문 상태가 REJECTED로 변경된다")
    void rejectCallsService() {
        FakeView placeView = new FakeView("1", "홍길동", "10");
        new OrderController(orderService, sampleService, placeView).handlePlace();

        FakeView rejectView = new FakeView("1");
        new OrderController(orderService, sampleService, rejectView).handleReject();

        assertEquals(OrderStatus.REJECTED, orderService.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("RESERVED 주문이 없을 때 거절 시 안내 메시지가 출력된다")
    void rejectShowsMessageWhenNoReserved() {
        FakeView view = new FakeView();
        OrderController controller = new OrderController(orderService, sampleService, view);

        controller.handleReject();

        assertTrue(view.getMessages().stream().anyMatch(m -> m.contains("없습니다")));
    }

    @Test
    @DisplayName("거절 번호가 범위를 벗어나면 오류가 출력된다")
    void rejectShowsErrorOnOutOfRangeIndex() {
        FakeView placeView = new FakeView("1", "홍길동", "10");
        new OrderController(orderService, sampleService, placeView).handlePlace();

        FakeView rejectView = new FakeView("0");
        new OrderController(orderService, sampleService, rejectView).handleReject();

        assertFalse(rejectView.getErrors().isEmpty());
    }

    @Test
    @DisplayName("수량에 숫자가 아닌 값 입력 시 오류 메시지를 출력한다")
    void placeOrderShowsErrorOnInvalidNumber() {
        FakeView view = new FakeView("1", "홍길동", "abc");
        OrderController controller = new OrderController(orderService, sampleService, view);

        controller.handlePlace();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("승인 번호에 숫자가 아닌 값 입력 시 오류 메시지를 출력한다")
    void approveShowsErrorOnInvalidNumber() {
        FakeView placeView = new FakeView("1", "홍길동", "10");
        new OrderController(orderService, sampleService, placeView).handlePlace();

        FakeView approveView = new FakeView("abc");
        new OrderController(orderService, sampleService, approveView).handleApprove();

        assertFalse(approveView.getErrors().isEmpty());
    }

    @Test
    @DisplayName("거절 번호에 숫자가 아닌 값 입력 시 오류 메시지를 출력한다")
    void rejectShowsErrorOnInvalidNumber() {
        FakeView placeView = new FakeView("1", "홍길동", "10");
        new OrderController(orderService, sampleService, placeView).handlePlace();

        FakeView rejectView = new FakeView("abc");
        new OrderController(orderService, sampleService, rejectView).handleReject();

        assertFalse(rejectView.getErrors().isEmpty());
    }

    @Test
    @DisplayName("handleApproveOrReject 입력 1은 handleApprove를 호출한다")
    void approveOrRejectInput1CallsApprove() {
        FakeView placeView = new FakeView("1", "홍길동", "10");
        new OrderController(orderService, sampleService, placeView).handlePlace();

        // 입력 "1" -> 승인 서브메뉴, "1" -> 첫 번째 주문 승인
        FakeView view = new FakeView("1", "1");
        new OrderController(orderService, sampleService, view).handleApproveOrReject();

        assertEquals(org.example.sampleordersystem.model.OrderStatus.CONFIRMED,
            orderService.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("handleApproveOrReject 입력 2는 handleReject를 호출한다")
    void approveOrRejectInput2CallsReject() {
        FakeView placeView = new FakeView("1", "홍길동", "10");
        new OrderController(orderService, sampleService, placeView).handlePlace();

        // 입력 "2" -> 거절 서브메뉴, "1" -> 첫 번째 주문 거절
        FakeView view = new FakeView("2", "1");
        new OrderController(orderService, sampleService, view).handleApproveOrReject();

        assertEquals(org.example.sampleordersystem.model.OrderStatus.REJECTED,
            orderService.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("handleApproveOrReject 입력 0은 아무것도 하지 않는다")
    void approveOrRejectInput0Returns() {
        FakeView view = new FakeView("0");
        assertDoesNotThrow(() -> new OrderController(orderService, sampleService, view).handleApproveOrReject());
    }

    @Test
    @DisplayName("handleApproveOrReject 잘못된 입력은 오류 메시지를 출력한다")
    void approveOrRejectInvalidInputShowsError() {
        FakeView view = new FakeView("99");
        new OrderController(orderService, sampleService, view).handleApproveOrReject();
        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("handleRelease CONFIRMED 주문을 RELEASE로 전환한다")
    void handleReleaseChangesStatusToRelease() {
        FakeView placeView = new FakeView("1", "홍길동", "10");
        new OrderController(orderService, sampleService, placeView).handlePlace();
        String orderId = orderService.findAll().get(0).getOrderId();
        orderService.approve(orderId);

        FakeView releaseView = new FakeView("1");
        new OrderController(orderService, sampleService, releaseView).handleRelease();

        assertEquals(org.example.sampleordersystem.model.OrderStatus.RELEASE,
            orderService.findAll().get(0).getStatus());
        assertTrue(releaseView.getMessages().stream().anyMatch(m -> m.contains("완료")));
    }

    @Test
    @DisplayName("handleRelease CONFIRMED 주문이 없을 때 안내 메시지를 출력한다")
    void handleReleaseShowsMessageWhenNoConfirmed() {
        FakeView view = new FakeView();
        new OrderController(orderService, sampleService, view).handleRelease();
        assertTrue(view.getMessages().stream().anyMatch(m -> m.contains("없습니다")));
    }

    @Test
    @DisplayName("handleRelease 범위를 벗어난 번호 입력 시 오류를 출력한다")
    void handleReleaseShowsErrorOnOutOfRangeIndex() {
        FakeView placeView = new FakeView("1", "홍길동", "10");
        new OrderController(orderService, sampleService, placeView).handlePlace();
        String orderId = orderService.findAll().get(0).getOrderId();
        orderService.approve(orderId);

        FakeView releaseView = new FakeView("99");
        new OrderController(orderService, sampleService, releaseView).handleRelease();

        assertFalse(releaseView.getErrors().isEmpty());
    }

    @Test
    @DisplayName("handleRelease 숫자가 아닌 입력 시 오류를 출력한다")
    void handleReleaseShowsErrorOnInvalidNumber() {
        FakeView placeView = new FakeView("1", "홍길동", "10");
        new OrderController(orderService, sampleService, placeView).handlePlace();
        String orderId = orderService.findAll().get(0).getOrderId();
        orderService.approve(orderId);

        FakeView releaseView = new FakeView("abc");
        new OrderController(orderService, sampleService, releaseView).handleRelease();

        assertFalse(releaseView.getErrors().isEmpty());
    }
}
