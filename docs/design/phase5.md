# Phase 5 — App + Main

## 목표

메뉴 루프를 구성(`App`)하고 모든 의존성을 조립(`Main`)한다.  
`App`은 메뉴 라우팅과 `tick()` 호출만 담당하며 비즈니스 로직이 없다.  
`Main`은 JaCoCo 커버리지 측정 대상에서 제외된다.

---

## 산출물 목록

| 파일 | 종류 | JaCoCo |
|------|------|--------|
| `app/App.java` | 애플리케이션 루프 | 포함 |
| `Main.java` | 의존성 조립 진입점 | **제외** |
| `app/AppTest.java` | 테스트 | — |

---

## App 명세

```java
package org.example.sampleordersystem.app;

public class App {
    private final SampleController sampleController;
    private final OrderController orderController;
    private final ProductionController productionController;
    private final MonitoringController monitoringController;
    private final ProductionService productionService;
    private final View view;

    public App(SampleController sampleController,
               OrderController orderController,
               ProductionController productionController,
               MonitoringController monitoringController,
               ProductionService productionService,
               View view) { ... }

    public void run() { ... }
}
```

### run() 동작 흐름

```
while (true):
    1. productionService.tick()
    2. 현황 요약 데이터 수집
    3. view.showMainSummary(...)
    4. view.showMenu(메뉴 목록)
    5. input = view.readLine()
    6. switch(input):
         "1" → sampleController.handleSampleMenu()
         "2" → orderController.handlePlace()
         "3" → orderController.handleApproveOrReject()
         "4" → monitoringController.handleView()
         "5" → productionController.handleView()
         "6" → orderController.handleRelease()
         "0" → return (루프 종료)
         else → view.showError("올바른 메뉴 번호를 입력하세요")
```

### 메뉴 항목

```
0. 종료
1. 시료 관리
2. 시료 주문
3. 주문 승인/거절
4. 모니터링
5. 생산라인
6. 출고 처리
```

### 현황 요약 수집 로직

```java
int sampleCount   = sampleService.findAll().size();
int totalStock    = sampleService.findAll().stream()
                       .mapToInt(Sample::getStock).sum();
int orderCount    = orderService.findAll().size();
int queueSize     = productionService.getQueue().size();
Optional<ProductionEntry> current = productionService.getCurrent();
double progress   = productionService.getProgress();
int confirmedCount = orderService.findByStatus(OrderStatus.CONFIRMED).size();
```

> `sampleService`를 App 생성자에 추가하거나,  
> 필요한 데이터만 별도 DTO로 묶어 전달할 수 있다.  
> 선택은 구현 시 SubAgent2가 결정하되 View 인터페이스 시그니처를 변경하지 않는다.

---

## Main 명세

```java
package org.example.sampleordersystem;

public class Main {
    public static void main(String[] args) {
        double timeScale = parseTimeScale(args); // 기본값 1.0

        // 데이터 디렉토리 준비
        Path dataDir = Path.of("data");
        Files.createDirectories(dataDir);

        // Repository 생성
        JsonSampleRepository     sampleRepo     = new JsonSampleRepository(dataDir.resolve("samples.json"));
        JsonOrderRepository      orderRepo      = new JsonOrderRepository(dataDir.resolve("orders.json"));
        JsonProductionRepository productionRepo = new JsonProductionRepository(dataDir.resolve("production.json"));

        // OrderIdGenerator 초기화 (당일 최대 순번 복구)
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int lastSeq  = orderRepo.countByDatePrefix(today);
        TimeProvider timeProvider = new SystemTimeProvider();
        OrderIdGenerator idGenerator = new OrderIdGenerator(lastSeq, timeProvider);

        // Service 생성
        ProductionService productionService = new ProductionService(
            productionRepo, orderRepo, sampleRepo, timeProvider, timeScale);
        SampleService sampleService   = new SampleService(sampleRepo);
        OrderService  orderService    = new OrderService(
            orderRepo, sampleRepo, productionService, idGenerator);

        // View / Controller 생성
        View view = new ConsoleView(new Scanner(System.in), System.out);
        SampleController     sampleCtrl     = new SampleController(sampleService, view);
        OrderController      orderCtrl      = new OrderController(orderService, view);
        ProductionController productionCtrl = new ProductionController(productionService, view);
        MonitoringController monitoringCtrl = new MonitoringController(orderService, sampleService, view);

        // 실행
        new App(sampleCtrl, orderCtrl, productionCtrl, monitoringCtrl,
                productionService, view).run();
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
```

---

## TDD 사이클 상세

### Cycle 5-1: `appCallsTickOnEachLoop`

| 항목 | 내용 |
|------|------|
| Given | FakeView 입력: "0" (즉시 종료) |
| When | `app.run()` |
| Then | `ProductionService.tick()` 1회 호출됨 |
| 검증 방법 | tick 호출 횟수를 카운트하는 `CountingProductionService` 또는 spy 패턴 |

```java
@Test
@DisplayName("run() 루프마다 ProductionService.tick()을 호출한다")
void appCallsTickOnEachLoop() {
    AtomicInteger tickCount = new AtomicInteger(0);
    ProductionService spyService = new ProductionService(...) {
        @Override public void tick() { tickCount.incrementAndGet(); }
    };
    FakeView view = new FakeView("0"); // 즉시 종료
    App app = new App(..., spyService, view);

    app.run();

    assertEquals(1, tickCount.get());
}
```

> `ProductionService`를 익명 클래스로 오버라이드하거나,  
> 별도 `TestProductionService` 서브클래스를 테스트 패키지에 작성해도 무방하다.

---

### Cycle 5-2: `appExitsOnZeroInput`

| 항목 | 내용 |
|------|------|
| Given | FakeView 입력: "0" |
| When | `app.run()` |
| Then | 메서드가 정상 반환 (무한 루프 아님) |

```java
@Test
@DisplayName("'0' 입력 시 루프가 종료된다")
void appExitsOnZeroInput() {
    FakeView view = new FakeView("0");
    App app = buildApp(view);

    // 5초 내 반환되면 통과 (타임아웃 = 무한 루프 방지)
    assertTimeoutPreemptively(Duration.ofSeconds(5), app::run);
}
```

---

### Cycle 5-3: `appRoutesToCorrectController`

| 항목 | 내용 |
|------|------|
| Given | FakeView 입력: "2", "0" (시료 주문 후 종료) |
| When | `app.run()` |
| Then | `OrderController.handlePlace()` 1회 호출됨 |

```java
@Test
@DisplayName("메뉴 번호 '2' 입력 시 OrderController.handlePlace()가 호출된다")
void appRoutesToCorrectController() {
    AtomicBoolean placeCalled = new AtomicBoolean(false);
    OrderController spyOrderCtrl = new OrderController(...) {
        @Override public void handlePlace() { placeCalled.set(true); }
    };
    // 입력: "2"(시료주문 선택) → handlePlace에서 추가 입력 없이 반환하도록 FakeView 구성
    // → "0"(종료)
    FakeView view = new FakeView("2", "0");
    App app = new App(..., spyOrderCtrl, ..., view);

    app.run();

    assertTrue(placeCalled.get());
}
```

---

## Main 제외 설정 확인

`build.gradle`의 JaCoCo 설정에 `Main` 제외가 적용되어 있는지 확인한다.

```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            excludes = ['org.example.sampleordersystem.Main']
            limit {
                counter = 'INSTRUCTION'
                value   = 'COVEREDRATIO'
                minimum = 1.0
            }
        }
    }
}
```

---

## 완료 기준

- [ ] `./gradlew check` 성공
- [ ] `./gradlew run` 실행 후 메인 메뉴 정상 출력
- [ ] `--time-scale 60` 인수로 실행 시 가속 동작 확인
- [ ] 커밋 이력: Cycle별 `test(phase5): [테스트명] RED → GREEN` 커밋 3개
