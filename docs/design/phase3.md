# Phase 3 — Service 레이어

## 목표

핵심 비즈니스 로직을 Service에 집중시킨다.  
재고 판단, 생산라인 스케줄링, 진행률 계산이 이 레이어의 책임이다.  
Controller는 Service를 호출하고, Model·Repository는 Service에 의존하지 않는다.

---

## 산출물 목록

| 파일 | 종류 |
|------|------|
| `service/SampleService.java` | 서비스 |
| `service/OrderService.java` | 서비스 |
| `service/ProductionService.java` | 서비스 |
| `util/FixedTimeProvider.java` | 테스트 전용 구현체 |
| `service/SampleServiceTest.java` | 테스트 |
| `service/OrderServiceTest.java` | 테스트 |
| `service/ProductionServiceTest.java` | 테스트 |

---

## FixedTimeProvider (테스트 전용)

```java
package org.example.sampleordersystem.util;

import java.time.LocalDateTime;

public class FixedTimeProvider implements TimeProvider {
    private LocalDateTime fixed;

    public FixedTimeProvider(LocalDateTime fixed) {
        this.fixed = fixed;
    }

    public void setTime(LocalDateTime time) {
        this.fixed = time;
    }

    @Override
    public LocalDateTime now() {
        return fixed;
    }
}
```

- `setTime()`으로 시각을 진행시켜 tick() 완료 시나리오를 테스트한다.

---

## SampleService 명세

```java
public class SampleService {
    private final SampleRepository sampleRepository;

    public SampleService(SampleRepository sampleRepository) { ... }

    // 중복 ID → IllegalArgumentException("이미 등록된 시료 ID입니다")
    public void register(String id, String name, int avgProductionMinutes,
                         double yield, int stock)

    public List<Sample> findAll()
    public List<Sample> findByNameContaining(String keyword)
    public Optional<Sample> findById(String id)
}
```

---

## OrderService 명세

```java
public class OrderService {
    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;
    private final ProductionService productionService;
    private final OrderIdGenerator orderIdGenerator;

    public OrderService(OrderRepository orderRepository,
                        SampleRepository sampleRepository,
                        ProductionService productionService,
                        OrderIdGenerator orderIdGenerator) { ... }

    // 미등록 시료 → IllegalArgumentException("등록되지 않은 시료입니다")
    public Order placeOrder(String sampleId, String customerName, int quantity)

    // 미존재 주문 → IllegalArgumentException
    // 재고 충분: stock 차감 → CONFIRMED
    // 재고 부족: PRODUCING → enqueue()
    public void approve(String orderId)

    // 미존재 주문 → IllegalArgumentException
    public void reject(String orderId)

    public List<Order> findByStatus(OrderStatus status)
    public List<Order> findAll()
}
```

### approve() 핵심 로직

```java
public void approve(String orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));
    Sample sample = sampleRepository.findById(order.getSampleId())
        .orElseThrow(() -> new IllegalArgumentException("시료를 찾을 수 없습니다"));

    int shortage = Math.max(0, order.getQuantity() - sample.getStock());

    if (shortage == 0) {
        sample.decreaseStock(order.getQuantity());
        sampleRepository.save(sample);
        order.transitionTo(OrderStatus.CONFIRMED);
    } else {
        order.transitionTo(OrderStatus.PRODUCING);
        int actualQty = (int) Math.ceil(shortage / (sample.getYield() * 0.9));
        double totalMinutes = (double) sample.getAvgProductionMinutes() * actualQty;
        ProductionEntry entry = new ProductionEntry(
            orderId, sample.getId(), shortage, actualQty, totalMinutes, null);
        productionService.enqueue(entry);
    }
    orderRepository.save(order);
}
```

---

## ProductionService 명세

```java
public class ProductionService {
    private final ProductionRepository productionRepository;
    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;
    private final TimeProvider timeProvider;
    private final double timeScale;

    public ProductionService(ProductionRepository productionRepository,
                             OrderRepository orderRepository,
                             SampleRepository sampleRepository,
                             TimeProvider timeProvider,
                             double timeScale) { ... }

    public void enqueue(ProductionEntry entry)
    public void tick()
    public Optional<ProductionEntry> getCurrent()
    public List<ProductionEntry> getQueue()
    public double getProgress()
    public LocalDateTime getEstimatedFinishAt(ProductionEntry entry)
}
```

### enqueue() 핵심 로직

```java
public void enqueue(ProductionEntry entry) {
    if (getCurrent().isEmpty()) {
        // 즉시 시작: startedAt 기록
        ProductionEntry started = new ProductionEntry(
            entry.getOrderId(), entry.getSampleId(),
            entry.getShortage(), entry.getActualQty(),
            entry.getTotalMinutes(), timeProvider.now());
        productionRepository.save(started);
    } else {
        productionRepository.save(entry); // 대기 (startedAt = null)
    }
}
```

### tick() 핵심 로직

```java
public void tick() {
    getCurrent().ifPresent(current -> {
        double progress = getProgress();
        if (progress >= 100.0) {
            // 1. 주문 CONFIRMED 전환
            Order order = orderRepository.findById(current.getOrderId()).orElseThrow();
            order.transitionTo(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            // 2. 재고 반영 (actualQty만큼 증가 후 주문 수량 차감)
            Sample sample = sampleRepository.findById(current.getSampleId()).orElseThrow();
            sample.increaseStock(current.getActualQty());
            sample.decreaseStock(order.getQuantity());
            sampleRepository.save(sample);

            // 3. 현재 항목 제거
            productionRepository.delete(current.getOrderId());

            // 4. 다음 항목 시작
            getQueue().stream()
                .filter(e -> e.getStartedAt() == null)
                .findFirst()
                .ifPresent(next -> {
                    ProductionEntry started = new ProductionEntry(
                        next.getOrderId(), next.getSampleId(),
                        next.getShortage(), next.getActualQty(),
                        next.getTotalMinutes(), timeProvider.now());
                    productionRepository.save(started);
                });
        }
    });
}
```

### getProgress() 계산

```java
public double getProgress() {
    return getCurrent().map(current -> {
        long elapsedSeconds = Duration.between(
            current.getStartedAt(), timeProvider.now()).toSeconds();
        double totalSeconds = current.getTotalMinutes() / timeScale * 60.0;
        return Math.min(100.0, elapsedSeconds / totalSeconds * 100.0);
    }).orElse(0.0);
}
```

### getEstimatedFinishAt() 계산

```java
public LocalDateTime getEstimatedFinishAt(ProductionEntry entry) {
    // 대기 중인 항목은 앞선 모든 항목 완료 후 시작
    // 현재 생산 중인 항목: startedAt + (totalMinutes / timeScale)분
    ...
}
```

---

## TDD 사이클 상세

### Cycle 3-1: `registerSampleSavesToRepository`

| 항목 | 내용 |
|------|------|
| Given | `SampleService(InMemorySampleRepository)` |
| When | `register("S1", "갈륨비소", 5, 0.9, 100)` |
| Then | `findById("S1")` → 존재 |

```java
@Test
@DisplayName("시료를 등록하면 Repository에 저장된다")
void registerSampleSavesToRepository() {
    SampleRepository repo = new InMemorySampleRepository();
    SampleService service = new SampleService(repo);
    service.register("S1", "갈륨비소", 5, 0.9, 100);

    assertTrue(service.findById("S1").isPresent());
}
```

---

### Cycle 3-2: `registerRejectsDuplicateId`

| 항목 | 내용 |
|------|------|
| Given | "S1" 등록 후 |
| When | 동일 ID "S1"로 재등록 |
| Then | `IllegalArgumentException` |

```java
@Test
@DisplayName("중복 ID로 시료 등록 시 예외를 던진다")
void registerRejectsDuplicateId() {
    SampleService service = new SampleService(new InMemorySampleRepository());
    service.register("S1", "갈륨비소", 5, 0.9, 100);

    assertThrows(IllegalArgumentException.class,
        () -> service.register("S1", "다른이름", 5, 0.9, 50));
}
```

---

### Cycle 3-3: `placeOrderCreatesReservedOrder`

| 항목 | 내용 |
|------|------|
| Given | 시료 등록 후 |
| When | `placeOrder("S1", "홍길동", 10)` |
| Then | 반환된 Order의 status = RESERVED |

```java
@Test
@DisplayName("주문 접수 시 RESERVED 상태 주문이 생성된다")
void placeOrderCreatesReservedOrder() {
    // setup
    SampleRepository sampleRepo = new InMemorySampleRepository();
    sampleRepo.save(new Sample("S1", "A", 5, 0.9, 100));
    OrderRepository orderRepo = new InMemoryOrderRepository();
    ProductionService prodService = buildProductionService();
    OrderIdGenerator gen = new OrderIdGenerator(0,
        () -> LocalDateTime.of(2024, 1, 1, 0, 0));
    OrderService service = new OrderService(orderRepo, sampleRepo, prodService, gen);

    Order order = service.placeOrder("S1", "홍길동", 10);

    assertEquals(OrderStatus.RESERVED, order.getStatus());
    assertTrue(order.getOrderId().startsWith("ORD-"));
}
```

---

### Cycle 3-4: `placeOrderRejectsUnknownSample`

| 항목 | 내용 |
|------|------|
| Given | 비어있는 SampleRepository |
| When | `placeOrder("UNKNOWN", "홍길동", 10)` |
| Then | `IllegalArgumentException` |

```java
@Test
@DisplayName("미등록 시료로 주문 시 예외를 던진다")
void placeOrderRejectsUnknownSample() {
    OrderService service = buildOrderService(new InMemorySampleRepository());

    assertThrows(IllegalArgumentException.class,
        () -> service.placeOrder("UNKNOWN", "홍길동", 10));
}
```

---

### Cycle 3-5: `approveWithSufficientStock`

| 항목 | 내용 |
|------|------|
| Given | stock=100인 시료, 주문수량=10 |
| When | `approve(orderId)` |
| Then | 주문 status = CONFIRMED, stock = 90 |

```java
@Test
@DisplayName("재고 충분 시 승인하면 CONFIRMED 전환 및 재고 차감된다")
void approveWithSufficientStock() {
    SampleRepository sampleRepo = new InMemorySampleRepository();
    sampleRepo.save(new Sample("S1", "A", 5, 0.9, 100));
    // ... setup
    Order order = orderService.placeOrder("S1", "홍길동", 10);
    orderService.approve(order.getOrderId());

    Order approved = orderRepo.findById(order.getOrderId()).orElseThrow();
    assertEquals(OrderStatus.CONFIRMED, approved.getStatus());
    assertEquals(90, sampleRepo.findById("S1").orElseThrow().getStock());
}
```

---

### Cycle 3-6: `approveWithInsufficientStock`

| 항목 | 내용 |
|------|------|
| Given | stock=5인 시료, 주문수량=10 |
| When | `approve(orderId)` |
| Then | 주문 status = PRODUCING, ProductionRepository에 항목 추가 |

```java
@Test
@DisplayName("재고 부족 시 승인하면 PRODUCING 전환 및 생산 등록된다")
void approveWithInsufficientStock() {
    // stock=5, quantity=10 → shortage=5
    // actualQty = ceil(5 / (0.9 * 0.9)) = ceil(6.17) = 7
    // ...
    orderService.approve(order.getOrderId());

    assertEquals(OrderStatus.PRODUCING,
        orderRepo.findById(order.getOrderId()).orElseThrow().getStatus());
    assertFalse(productionRepo.findAll().isEmpty());
}
```

---

### Cycle 3-7: `rejectOrderTransitionsToRejected`

```java
@Test
@DisplayName("주문 거절 시 REJECTED 상태로 전환된다")
void rejectOrderTransitionsToRejected() {
    // ...
    orderService.reject(order.getOrderId());
    assertEquals(OrderStatus.REJECTED,
        orderRepo.findById(order.getOrderId()).orElseThrow().getStatus());
}
```

---

### Cycle 3-8: `enqueueStartsImmediatelyWhenIdle`

| 항목 | 내용 |
|------|------|
| Given | 빈 생산 큐 |
| When | `enqueue(entry)` |
| Then | `getCurrent()` 존재, `startedAt` != null |

```java
@Test
@DisplayName("생산 큐가 비어있으면 enqueue 즉시 생산을 시작한다")
void enqueueStartsImmediatelyWhenIdle() {
    FixedTimeProvider time = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 9, 0));
    ProductionService service = new ProductionService(
        new InMemoryProductionRepository(), orderRepo, sampleRepo, time, 1.0);

    service.enqueue(new ProductionEntry("ORD-1", "S1", 5, 7, 35.0, null));

    assertTrue(service.getCurrent().isPresent());
    assertNotNull(service.getCurrent().get().getStartedAt());
}
```

---

### Cycle 3-9: `enqueueWaitsWhenBusy`

| 항목 | 내용 |
|------|------|
| Given | 이미 생산 중인 항목 있음 |
| When | 두 번째 `enqueue(entry)` |
| Then | `getQueue()` 크기 = 2, 두 번째 항목 `startedAt` = null |

```java
@Test
@DisplayName("생산 중인 항목이 있으면 새 항목은 대기한다")
void enqueueWaitsWhenBusy() {
    // first enqueue → 즉시 시작
    service.enqueue(entry1);
    // second enqueue → 대기
    service.enqueue(entry2);

    assertEquals(2, service.getQueue().size());
    assertNull(service.getQueue().get(1).getStartedAt());
}
```

---

### Cycle 3-10: `tickDoesNotCompleteBeforeTime`

| 항목 | 내용 |
|------|------|
| Given | totalMinutes=60, timeScale=1, 생산 시작 1분 후 시각 |
| When | `tick()` |
| Then | 주문 여전히 PRODUCING |

```java
@Test
@DisplayName("생산 시간이 지나지 않으면 tick 후에도 PRODUCING 상태를 유지한다")
void tickDoesNotCompleteBeforeTime() {
    FixedTimeProvider time = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 9, 0));
    // totalMinutes=60, 1분 경과
    // ...
    time.setTime(LocalDateTime.of(2024, 1, 1, 9, 1));
    service.tick();

    assertEquals(OrderStatus.PRODUCING,
        orderRepo.findById("ORD-1").orElseThrow().getStatus());
}
```

---

### Cycle 3-11: `tickCompletesAndTransitionsToConfirmed`

| 항목 | 내용 |
|------|------|
| Given | totalMinutes=60, timeScale=1, 60분 이상 경과 |
| When | `tick()` |
| Then | 주문 CONFIRMED, 재고 반영 |

```java
@Test
@DisplayName("생산 완료 시 tick이 CONFIRMED 전환 및 재고를 반영한다")
void tickCompletesAndTransitionsToConfirmed() {
    FixedTimeProvider time = new FixedTimeProvider(LocalDateTime.of(2024, 1, 1, 9, 0));
    // totalMinutes=60, 61분 경과
    time.setTime(LocalDateTime.of(2024, 1, 1, 10, 1));
    service.tick();

    assertEquals(OrderStatus.CONFIRMED,
        orderRepo.findById("ORD-1").orElseThrow().getStatus());
}
```

---

### Cycle 3-12: `tickStartsNextEntryAfterCompletion`

| 항목 | 내용 |
|------|------|
| Given | 항목 2개 큐, 첫 항목 완료 시간 경과 |
| When | `tick()` |
| Then | 두 번째 항목 `startedAt` != null |

---

### Cycle 3-13: `actualQtyFormula`

| 항목 | 내용 |
|------|------|
| Given | shortage=50, yield=0.9 |
| When | approve() 실행 |
| Then | `actualQty = ceil(50 / (0.9 × 0.9)) = ceil(61.73) = 62` |

```java
@Test
@DisplayName("실 생산량은 ceil(shortage / (yield × 0.9)) 공식으로 계산된다")
void actualQtyFormula() {
    // stock=0, quantity=50, yield=0.9
    // shortage=50 → actualQty = ceil(50/0.81) = ceil(61.73) = 62
    // ...
    ProductionEntry entry = productionRepo.findAll().get(0);
    assertEquals(62, entry.getActualQty());
}
```

---

### Cycle 3-14: `timeScaleAcceleratesCompletion`

| 항목 | 내용 |
|------|------|
| Given | totalMinutes=60, timeScale=60, 1분 경과 |
| When | `tick()` |
| Then | 진행률 = 100% → CONFIRMED 전환 |

```java
@Test
@DisplayName("timeScale 배율을 적용하면 실제 경과 시간이 짧아도 생산이 완료된다")
void timeScaleAcceleratesCompletion() {
    // totalMinutes=60, timeScale=60
    // 실제 1분 경과 = 시스템 60분 경과 → 완료
    time.setTime(start.plusMinutes(1));
    service.tick();

    assertEquals(OrderStatus.CONFIRMED,
        orderRepo.findById("ORD-1").orElseThrow().getStatus());
}
```

---

## 완료 기준

- [ ] `./gradlew check` 성공
- [ ] 커밋 이력: Cycle별 `test(phase3): [테스트명] RED → GREEN` 커밋 14개
