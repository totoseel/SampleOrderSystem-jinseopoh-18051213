# Phase 6 — 통합 검증

## 목표

실제 `JsonFile` Repository를 사용해 전체 비즈니스 플로우가  
영속성·시간 배율·상태 전환 모든 측면에서 정상 동작함을 검증한다.

---

## 산출물 목록

| 파일 | 종류 |
|------|------|
| `integration/FullFlowIntegrationTest.java` | 통합 테스트 |

> 통합 테스트도 `./gradlew check` 대상에 포함되므로 JaCoCo 커버리지에 기여한다.

---

## 테스트 환경 구성

```java
@TempDir
Path tempDir;

private JsonSampleRepository     sampleRepo;
private JsonOrderRepository      orderRepo;
private JsonProductionRepository productionRepo;
private FixedTimeProvider        timeProvider;
private ProductionService        productionService;
private SampleService            sampleService;
private OrderService             orderService;

@BeforeEach
void setUp() {
    sampleRepo     = new JsonSampleRepository(tempDir.resolve("samples.json"));
    orderRepo      = new JsonOrderRepository(tempDir.resolve("orders.json"));
    productionRepo = new JsonProductionRepository(tempDir.resolve("production.json"));
    timeProvider   = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 9, 0));

    productionService = new ProductionService(
        productionRepo, orderRepo, sampleRepo, timeProvider, 1.0);
    sampleService = new SampleService(sampleRepo);

    OrderIdGenerator idGen = new OrderIdGenerator(0, timeProvider);
    orderService = new OrderService(orderRepo, sampleRepo, productionService, idGen);
}
```

---

## 시나리오 상세

### 시나리오 6-1: JSON 영속성

| 단계 | 행동 |
|------|------|
| 1 | `sampleService.register("S1", "갈륨비소", 5, 0.9, 100)` |
| 2 | **새 인스턴스** 생성 (동일 tempDir 파일 사용) |
| 3 | `sampleService.findById("S1")` |
| 기대 | 시료 존재, 이름 = "갈륨비소", stock = 100 |

```java
@Test
@DisplayName("JSON 파일에 저장된 시료는 재시작 후에도 복구된다")
void jsonPersistence() {
    sampleService.register("S1", "갈륨비소", 5, 0.9, 100);

    // 새 인스턴스 (재시작 시뮬레이션)
    SampleService reloaded = new SampleService(
        new JsonSampleRepository(tempDir.resolve("samples.json")));

    Optional<Sample> found = reloaded.findById("S1");
    assertTrue(found.isPresent());
    assertEquals("갈륨비소", found.get().getName());
    assertEquals(100, found.get().getStock());
}
```

---

### 시나리오 6-2: 재고 충분 전체 플로우

```
시료 등록(stock=100) → 주문 접수(qty=10) → 승인 → CONFIRMED → 출고 → RELEASE
```

```java
@Test
@DisplayName("재고 충분 시 주문 접수 → 승인 → 출고 전체 플로우가 동작한다")
void sufficientStockFullFlow() {
    sampleService.register("S1", "갈륨비소", 5, 0.9, 100);
    Order order = orderService.placeOrder("S1", "홍길동", 10);

    assertEquals(OrderStatus.RESERVED, order.getStatus());

    orderService.approve(order.getOrderId());
    Order approved = orderRepo.findById(order.getOrderId()).orElseThrow();
    assertEquals(OrderStatus.CONFIRMED, approved.getStatus());
    assertEquals(90, sampleRepo.findById("S1").orElseThrow().getStock());

    approved.transitionTo(OrderStatus.RELEASE);
    orderRepo.save(approved);
    assertEquals(OrderStatus.RELEASE,
        orderRepo.findById(order.getOrderId()).orElseThrow().getStatus());
}
```

---

### 시나리오 6-3: 재고 부족 → 생산 완료 플로우

```
시료 등록(stock=5) → 주문 접수(qty=10) → 승인 → PRODUCING
→ tick() (시간 경과) → CONFIRMED → 재고 반영
```

```java
@Test
@DisplayName("재고 부족 시 생산 완료 후 CONFIRMED로 자동 전환된다")
void insufficientStockProductionFlow() {
    // stock=5, qty=10 → shortage=5, yield=0.9
    // actualQty = ceil(5 / 0.81) = 7, totalMinutes = 5 * 7 = 35
    sampleService.register("S1", "갈륨비소", 5, 0.9, 5);
    Order order = orderService.placeOrder("S1", "홍길동", 10);
    orderService.approve(order.getOrderId());

    assertEquals(OrderStatus.PRODUCING,
        orderRepo.findById(order.getOrderId()).orElseThrow().getStatus());

    // 36분 경과 → 완료
    timeProvider.setTime(LocalDateTime.of(2024, 1, 1, 9, 36));
    productionService.tick();

    Order completed = orderRepo.findById(order.getOrderId()).orElseThrow();
    assertEquals(OrderStatus.CONFIRMED, completed.getStatus());

    // 재고: 5(기존) + 7(생산) - 10(주문) = 2
    assertEquals(2, sampleRepo.findById("S1").orElseThrow().getStock());
}
```

---

### 시나리오 6-4: 재시작 후 주문번호 순번 이어받기

```java
@Test
@DisplayName("재시작 후 주문번호 순번을 이어받는다")
void orderIdResumesAfterRestart() {
    sampleService.register("S1", "A", 5, 0.9, 100);
    orderService.placeOrder("S1", "A", 1); // ORD-20240101-0001
    orderService.placeOrder("S1", "B", 1); // ORD-20240101-0002

    // 재시작 시뮬레이션: countByDatePrefix로 lastSeq 복구
    String today = "20240101";
    int lastSeq = new JsonOrderRepository(tempDir.resolve("orders.json"))
        .countByDatePrefix(today);
    OrderIdGenerator newGen = new OrderIdGenerator(lastSeq, timeProvider);
    OrderService restarted = new OrderService(
        new JsonOrderRepository(tempDir.resolve("orders.json")),
        new JsonSampleRepository(tempDir.resolve("samples.json")),
        productionService, newGen);

    Order next = restarted.placeOrder("S1", "C", 1);
    assertEquals("ORD-20240101-0003", next.getOrderId());
}
```

---

### 시나리오 6-5: 시간 배율 적용

```java
@Test
@DisplayName("timeScale=60 적용 시 실제 1분 경과로 60분 생산이 완료된다")
void timeScaleAccelerates() {
    // totalMinutes=60, timeScale=60 → 실제 1분 경과면 완료
    ProductionService fastService = new ProductionService(
        productionRepo, orderRepo, sampleRepo, timeProvider, 60.0);
    // ...
    sampleService.register("S1", "A", 60, 0.9, 0);
    // shortage=10, actualQty=ceil(10/0.81)=13, totalMinutes=60*13=780
    // timeScale=60 → 실제 780/60=13분 경과 시 완료
    Order order = orderService.placeOrder("S1", "홍길동", 10);
    OrderService fastOrderService = new OrderService(
        orderRepo, sampleRepo, fastService, idGenerator);
    fastOrderService.approve(order.getOrderId());

    timeProvider.setTime(LocalDateTime.of(2024, 1, 1, 9, 14)); // 14분 경과
    fastService.tick();

    assertEquals(OrderStatus.CONFIRMED,
        orderRepo.findById(order.getOrderId()).orElseThrow().getStatus());
}
```

---

### 시나리오 6-6: REJECTED 주문 모니터링 미포함

```java
@Test
@DisplayName("REJECTED 주문은 모니터링 집계에서 제외된다")
void rejectedExcludedFromMonitoring() {
    sampleService.register("S1", "A", 5, 0.9, 100);
    Order o1 = orderService.placeOrder("S1", "A", 10);
    Order o2 = orderService.placeOrder("S1", "B", 5);
    orderService.reject(o2.getOrderId());

    Map<OrderStatus, Long> counts = orderService.findAll().stream()
        .filter(o -> o.getStatus() != OrderStatus.REJECTED)
        .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

    assertFalse(counts.containsKey(OrderStatus.REJECTED));
    assertEquals(2L, counts.getOrDefault(OrderStatus.RESERVED, 0L));
}
```

---

## 수동 확인 항목

통합 테스트 자동화 외에 실제 CLI 실행으로 확인한다.

| 항목 | 명령어 | 기대 결과 |
|------|--------|----------|
| 기본 실행 | `./gradlew run` | 메인 메뉴 출력 |
| 시간 배율 | `./gradlew run --args="--time-scale 60"` | 1분 내 생산 완료 가능 |
| 잘못된 배율 | `./gradlew run --args="--time-scale abc"` | 기본값 1.0 적용 |
| 재시작 데이터 유지 | 종료 후 재실행 | 이전 시료·주문 데이터 유지 |

---

## 완료 기준

- [ ] `./gradlew check` 성공 (통합 테스트 포함)
- [ ] 시나리오 6-1 ~ 6-6 모두 통과
- [ ] 수동 확인 항목 통과
