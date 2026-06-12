# Phase 4 — Controller + View 레이어

## 목표

사용자 입력 검증 및 Service 위임(Controller)과 출력 전담(View)을 분리한다.  
Controller는 비즈니스 로직을 보유하지 않는다.  
View는 출력 포맷팅만 담당하며 도메인 객체를 직접 받는다.

---

## 산출물 목록

| 파일 | 종류 |
|------|------|
| `view/View.java` | 인터페이스 |
| `view/ConsoleView.java` | 구현체 |
| `controller/SampleController.java` | 컨트롤러 |
| `controller/OrderController.java` | 컨트롤러 |
| `controller/ProductionController.java` | 컨트롤러 |
| `controller/MonitoringController.java` | 컨트롤러 |
| `view/ConsoleViewTest.java` | 테스트 |
| `controller/SampleControllerTest.java` | 테스트 |
| `controller/OrderControllerTest.java` | 테스트 |
| `controller/MonitoringControllerTest.java` | 테스트 |

---

## View 인터페이스 명세

```java
package org.example.sampleordersystem.view;

public interface View {

    // 메인 화면 현황 요약
    void showMainSummary(int sampleCount, int totalStock, int orderCount,
                         int queueSize, Optional<ProductionEntry> current,
                         double progress, int confirmedCount);

    // 번호 메뉴 출력
    void showMenu(List<String> options);

    // 시료 목록 출력
    void showSamples(List<Sample> samples);

    // 주문 목록 출력
    void showOrders(List<Order> orders);

    // 생산라인 현황 출력
    void showProductionStatus(Optional<ProductionEntry> current,
                              double progress,
                              LocalDateTime estimatedFinish,
                              List<ProductionEntry> queue);

    // 모니터링 요약 출력
    // statusCounts: REJECTED 제외한 상태별 건수
    void showMonitoringSummary(Map<OrderStatus, Long> statusCounts,
                               List<Sample> samples);

    // 일반 메시지 출력
    void showMessage(String message);

    // 오류 메시지 출력
    void showError(String message);

    // 사용자 입력 한 줄 읽기
    String readLine();
}
```

---

## ConsoleView 명세

```java
public class ConsoleView implements View {
    private final Scanner scanner;
    private final PrintStream out;

    // 테스트: new ConsoleView(new Scanner(new ByteArrayInputStream(...)), System.out)
    // 프로덕션: new ConsoleView(new Scanner(System.in), System.out)
    public ConsoleView(Scanner scanner, PrintStream out) { ... }
}
```

### 출력 형식 예시

**showMainSummary**
```
=== S-Semi 반도체 시료 생산주문관리 시스템 ===
[현황 요약]
  등록 시료 수    : 3
  총 재고 수량    : 250
  전체 주문 수    : 12
  생산라인 대기   : 2
  현재 생산 중   : 갈륨비소 (진행률: 45.3%)
  출고 대기(CONFIRMED): 1
```

**showProductionStatus**
```
=== 생산라인 현황 ===
[현재 생산 중]
  주문번호 : ORD-20240101-0003
  시료명   : 갈륨비소
  진행률   : 45.3%
  완료 예정: 2024-01-01 11:30

[대기 큐]
  1. ORD-20240101-0005 | 실리콘 | 주문량:20 | 부족분:15 | 실생산량:19 | 예상완료: 2024-01-01 13:00
```

**showMonitoringSummary — 재고 상태 라벨**

| 라벨 | 조건 |
|------|------|
| 여유 | PRODUCING 주문 없음 or 재고 > 0 |
| 부족 | 해당 시료 PRODUCING 주문 존재 |
| 고갈 | stock == 0 |

```
=== 모니터링 ===
[주문량 현황]
  RESERVED  : 2건
  PRODUCING : 1건
  CONFIRMED : 3건
  RELEASE   : 5건

[재고량 현황]
  갈륨비소        | 재고: 50  | 상태: 부족
  실리콘          | 재고: 0   | 상태: 고갈
  갈륨나이트라이드 | 재고: 200 | 상태: 여유
```

---

## Controller 명세

### SampleController

```java
public class SampleController {
    private final SampleService sampleService;
    private final View view;

    public void handleRegister() {
        view.showMessage("시료 ID 입력:");  String id = view.readLine();
        view.showMessage("시료 이름 입력:"); String name = view.readLine();
        view.showMessage("평균 생산시간(분) 입력:"); int avgMin = parseInt(view.readLine());
        view.showMessage("수율(0 초과 1 이하) 입력:"); double yield = parseDouble(view.readLine());
        view.showMessage("초기 재고 수량 입력:"); int stock = parseInt(view.readLine());

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
        view.showMessage("검색 키워드 입력:"); String kw = view.readLine();
        view.showSamples(sampleService.findByNameContaining(kw));
    }
}
```

### OrderController

```java
public class OrderController {
    private final OrderService orderService;
    private final View view;

    public void handlePlace()   // 시료ID, 고객명, 수량 입력 → placeOrder()
    public void handleApprove() // RESERVED 목록 표시 → 선택 → approve()
    public void handleReject()  // RESERVED 목록 표시 → 선택 → reject()
}
```

**handleApprove/handleReject 흐름**
```
RESERVED 주문 목록 조회
  → 없으면: "처리할 주문이 없습니다" 출력 후 반환
  → 있으면: 번호 목록 표시, 번호 입력 받기
  → 유효한 번호 → approve()/reject() 호출
  → 범위 외 번호 → "올바른 번호를 입력하세요" 오류 출력
```

### ProductionController

```java
public class ProductionController {
    private final ProductionService productionService;
    private final View view;

    public void handleView() {
        view.showProductionStatus(
            productionService.getCurrent(),
            productionService.getProgress(),
            productionService.getCurrent()
                .map(productionService::getEstimatedFinishAt)
                .orElse(null),
            productionService.getQueue()
        );
    }
}
```

### MonitoringController

```java
public class MonitoringController {
    private final OrderService orderService;
    private final SampleService sampleService;
    private final View view;

    public void handleView() {
        // REJECTED 제외한 상태별 건수
        Map<OrderStatus, Long> counts = orderService.findAll().stream()
            .filter(o -> o.getStatus() != OrderStatus.REJECTED)
            .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        view.showMonitoringSummary(counts, sampleService.findAll());
    }
}
```

---

## TDD 사이클 상세

> Controller 테스트는 `View`의 테스트용 구현체(`FakeView`)를 사용한다.  
> `FakeView`는 `readLine()` 응답 목록을 생성자로 주입받고, 출력 내용을 캡처한다.

### FakeView 패턴

```java
class FakeView implements View {
    private final Queue<String> inputs;
    private final List<String> messages = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    FakeView(String... inputs) {
        this.inputs = new LinkedList<>(Arrays.asList(inputs));
    }

    @Override public String readLine() { return inputs.poll(); }
    @Override public void showMessage(String msg) { messages.add(msg); }
    @Override public void showError(String msg) { errors.add(msg); }
    // 나머지 메서드는 빈 구현
    ...
}
```

---

### Cycle 4-1: `registerSampleCallsService`

| 항목 | 내용 |
|------|------|
| Given | FakeView 입력: "S1", "갈륨비소", "5", "0.9", "100" |
| When | `handleRegister()` |
| Then | `sampleService.findById("S1")` 존재 |

```java
@Test
@DisplayName("유효한 입력으로 시료 등록 시 Service에 저장된다")
void registerSampleCallsService() {
    SampleService service = new SampleService(new InMemorySampleRepository());
    FakeView view = new FakeView("S1", "갈륨비소", "5", "0.9", "100");
    SampleController controller = new SampleController(service, view);

    controller.handleRegister();

    assertTrue(service.findById("S1").isPresent());
}
```

---

### Cycle 4-2: `registerSampleShowsErrorOnInvalidYield`

| 항목 | 내용 |
|------|------|
| Given | FakeView 입력: "S1", "갈륨비소", "5", "1.5", "100" (수율 범위 초과) |
| When | `handleRegister()` |
| Then | `view.errors`에 오류 메시지 포함 |

```java
@Test
@DisplayName("수율이 범위를 벗어나면 오류 메시지를 출력한다")
void registerSampleShowsErrorOnInvalidYield() {
    SampleService service = new SampleService(new InMemorySampleRepository());
    FakeView view = new FakeView("S1", "갈륨비소", "5", "1.5", "100");
    SampleController controller = new SampleController(service, view);

    controller.handleRegister();

    assertFalse(view.getErrors().isEmpty());
}
```

---

### Cycle 4-3: `placeOrderCallsService`

| 항목 | 내용 |
|------|------|
| Given | 시료 등록, FakeView 입력: "S1", "홍길동", "10" |
| When | `handlePlace()` |
| Then | `orderService.findByStatus(RESERVED)` 크기 = 1 |

---

### Cycle 4-4: `approveCallsService`

| 항목 | 내용 |
|------|------|
| Given | RESERVED 주문 1건, FakeView 입력: "1" (첫 번째 선택) |
| When | `handleApprove()` |
| Then | 해당 주문 status = CONFIRMED (재고 충분 케이스) |

---

### Cycle 4-5: `rejectCallsService`

| 항목 | 내용 |
|------|------|
| Given | RESERVED 주문 1건, FakeView 입력: "1" |
| When | `handleReject()` |
| Then | 해당 주문 status = REJECTED |

---

### Cycle 4-6: `monitoringExcludesRejected`

| 항목 | 내용 |
|------|------|
| Given | RESERVED 1건, REJECTED 2건 저장 |
| When | `MonitoringController.handleView()` |
| Then | `FakeView`에 캡처된 statusCounts에 REJECTED 키 없음 |

```java
@Test
@DisplayName("모니터링에서 REJECTED 주문은 제외된다")
void monitoringExcludesRejected() {
    // REJECTED 2건 포함 설정
    FakeMonitoringView view = new FakeMonitoringView();
    MonitoringController controller = new MonitoringController(orderService, sampleService, view);

    controller.handleView();

    assertFalse(view.getCapturedCounts().containsKey(OrderStatus.REJECTED));
}
```

---

### Cycle 4-7: `consoleViewRendersMainSummary`

| 항목 | 내용 |
|------|------|
| Given | 샘플 데이터로 ConsoleView 구성 |
| When | `showMainSummary(3, 250, 12, 2, Optional.empty(), 0.0, 1)` |
| Then | 출력 문자열에 "등록 시료 수", "3", "250" 포함 |

```java
@Test
@DisplayName("showMainSummary는 현황 요약 항목을 모두 출력한다")
void consoleViewRendersMainSummary() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ConsoleView view = new ConsoleView(
        new Scanner(new ByteArrayInputStream("".getBytes())),
        new PrintStream(out));

    view.showMainSummary(3, 250, 12, 2, Optional.empty(), 0.0, 1);

    String output = out.toString();
    assertTrue(output.contains("3"));
    assertTrue(output.contains("250"));
    assertTrue(output.contains("생산 없음"));
}
```

---

### Cycle 4-8: `consoleViewRendersProductionStatus`

| 항목 | 내용 |
|------|------|
| Given | 생산 중인 ProductionEntry |
| When | `showProductionStatus(...)` |
| Then | 출력에 주문번호, 진행률(%) 포함 |

---

## 완료 기준

- [ ] `./gradlew check` 성공
- [ ] 커밋 이력: Cycle별 `test(phase4): [테스트명] RED → GREEN` 커밋 8개
