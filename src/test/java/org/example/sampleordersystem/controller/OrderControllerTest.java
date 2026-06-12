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

        sampleService.register("S1", "갈륨비소", 5, 0.9, 100);
    }

    @Test
    @DisplayName("주문 접수 시 Service에 저장된다")
    void placeOrderCallsService() {
        FakeView view = new FakeView("S1", "홍길동", "10");
        OrderController controller = new OrderController(orderService, view);

        controller.handlePlace();

        assertEquals(1, orderService.findByStatus(OrderStatus.RESERVED).size());
    }

    @Test
    @DisplayName("존재하지 않는 시료로 주문 접수 시 오류가 출력된다")
    void placeOrderShowsErrorOnUnknownSample() {
        FakeView view = new FakeView("UNKNOWN", "홍길동", "10");
        OrderController controller = new OrderController(orderService, view);

        controller.handlePlace();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("RESERVED 주문 승인 시 해당 주문 상태가 변경된다")
    void approveCallsService() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();

        FakeView approveView = new FakeView("1");
        new OrderController(orderService, approveView).handleApprove();

        assertEquals(OrderStatus.CONFIRMED, orderService.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("RESERVED 주문이 없을 때 승인 시 안내 메시지가 출력된다")
    void approveShowsMessageWhenNoReserved() {
        FakeView view = new FakeView();
        OrderController controller = new OrderController(orderService, view);

        controller.handleApprove();

        assertTrue(view.getMessages().stream().anyMatch(m -> m.contains("없습니다")));
    }

    @Test
    @DisplayName("승인 번호가 범위를 벗어나면 오류가 출력된다")
    void approveShowsErrorOnOutOfRangeIndex() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();

        FakeView approveView = new FakeView("99");
        new OrderController(orderService, approveView).handleApprove();

        assertFalse(approveView.getErrors().isEmpty());
    }

    @Test
    @DisplayName("RESERVED 주문 거절 시 해당 주문 상태가 REJECTED로 변경된다")
    void rejectCallsService() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();

        FakeView rejectView = new FakeView("1");
        new OrderController(orderService, rejectView).handleReject();

        assertEquals(OrderStatus.REJECTED, orderService.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("RESERVED 주문이 없을 때 거절 시 안내 메시지가 출력된다")
    void rejectShowsMessageWhenNoReserved() {
        FakeView view = new FakeView();
        OrderController controller = new OrderController(orderService, view);

        controller.handleReject();

        assertTrue(view.getMessages().stream().anyMatch(m -> m.contains("없습니다")));
    }

    @Test
    @DisplayName("거절 번호가 범위를 벗어나면 오류가 출력된다")
    void rejectShowsErrorOnOutOfRangeIndex() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();

        FakeView rejectView = new FakeView("0");
        new OrderController(orderService, rejectView).handleReject();

        assertFalse(rejectView.getErrors().isEmpty());
    }

    @Test
    @DisplayName("수량에 숫자가 아닌 값 입력 시 오류 메시지를 출력한다")
    void placeOrderShowsErrorOnInvalidNumber() {
        FakeView view = new FakeView("S1", "홍길동", "abc");
        OrderController controller = new OrderController(orderService, view);

        controller.handlePlace();

        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("승인 번호에 숫자가 아닌 값 입력 시 오류 메시지를 출력한다")
    void approveShowsErrorOnInvalidNumber() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();

        FakeView approveView = new FakeView("abc");
        new OrderController(orderService, approveView).handleApprove();

        assertFalse(approveView.getErrors().isEmpty());
    }

    @Test
    @DisplayName("거절 번호에 숫자가 아닌 값 입력 시 오류 메시지를 출력한다")
    void rejectShowsErrorOnInvalidNumber() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();

        FakeView rejectView = new FakeView("abc");
        new OrderController(orderService, rejectView).handleReject();

        assertFalse(rejectView.getErrors().isEmpty());
    }

    @Test
    @DisplayName("handleApproveOrReject 입력 1은 handleApprove를 호출한다")
    void approveOrRejectInput1CallsApprove() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();

        // 입력 "1" -> 승인 서브메뉴, "1" -> 첫 번째 주문 승인
        FakeView view = new FakeView("1", "1");
        new OrderController(orderService, view).handleApproveOrReject();

        assertEquals(org.example.sampleordersystem.model.OrderStatus.CONFIRMED,
            orderService.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("handleApproveOrReject 입력 2는 handleReject를 호출한다")
    void approveOrRejectInput2CallsReject() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();

        // 입력 "2" -> 거절 서브메뉴, "1" -> 첫 번째 주문 거절
        FakeView view = new FakeView("2", "1");
        new OrderController(orderService, view).handleApproveOrReject();

        assertEquals(org.example.sampleordersystem.model.OrderStatus.REJECTED,
            orderService.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("handleApproveOrReject 입력 0은 아무것도 하지 않는다")
    void approveOrRejectInput0Returns() {
        FakeView view = new FakeView("0");
        assertDoesNotThrow(() -> new OrderController(orderService, view).handleApproveOrReject());
    }

    @Test
    @DisplayName("handleApproveOrReject 잘못된 입력은 오류 메시지를 출력한다")
    void approveOrRejectInvalidInputShowsError() {
        FakeView view = new FakeView("99");
        new OrderController(orderService, view).handleApproveOrReject();
        assertFalse(view.getErrors().isEmpty());
    }

    @Test
    @DisplayName("handleRelease CONFIRMED 주문을 RELEASE로 전환한다")
    void handleReleaseChangesStatusToRelease() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();
        String orderId = orderService.findAll().get(0).getOrderId();
        orderService.approve(orderId);

        FakeView releaseView = new FakeView("1");
        new OrderController(orderService, releaseView).handleRelease();

        assertEquals(org.example.sampleordersystem.model.OrderStatus.RELEASE,
            orderService.findAll().get(0).getStatus());
        assertTrue(releaseView.getMessages().stream().anyMatch(m -> m.contains("완료")));
    }

    @Test
    @DisplayName("handleRelease CONFIRMED 주문이 없을 때 안내 메시지를 출력한다")
    void handleReleaseShowsMessageWhenNoConfirmed() {
        FakeView view = new FakeView();
        new OrderController(orderService, view).handleRelease();
        assertTrue(view.getMessages().stream().anyMatch(m -> m.contains("없습니다")));
    }

    @Test
    @DisplayName("handleRelease 범위를 벗어난 번호 입력 시 오류를 출력한다")
    void handleReleaseShowsErrorOnOutOfRangeIndex() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();
        String orderId = orderService.findAll().get(0).getOrderId();
        orderService.approve(orderId);

        FakeView releaseView = new FakeView("99");
        new OrderController(orderService, releaseView).handleRelease();

        assertFalse(releaseView.getErrors().isEmpty());
    }

    @Test
    @DisplayName("handleRelease 숫자가 아닌 입력 시 오류를 출력한다")
    void handleReleaseShowsErrorOnInvalidNumber() {
        FakeView placeView = new FakeView("S1", "홍길동", "10");
        new OrderController(orderService, placeView).handlePlace();
        String orderId = orderService.findAll().get(0).getOrderId();
        orderService.approve(orderId);

        FakeView releaseView = new FakeView("abc");
        new OrderController(orderService, releaseView).handleRelease();

        assertFalse(releaseView.getErrors().isEmpty());
    }
}
