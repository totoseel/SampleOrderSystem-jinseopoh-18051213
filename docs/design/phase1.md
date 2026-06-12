# Phase 1 — 도메인 모델

## 목표

비즈니스 규칙을 담은 순수 Java 도메인 객체를 정의한다.  
외부 레이어(Repository, Service)에 의존하지 않으며, 검증 로직과 상태 전환 규칙만 보유한다.

---

## 산출물 목록

| 파일 | 종류 |
|------|------|
| `model/OrderStatus.java` | enum |
| `model/Sample.java` | 도메인 객체 |
| `model/Order.java` | 도메인 객체 |
| `model/ProductionEntry.java` | 도메인 객체 |
| `model/SampleTest.java` | 테스트 |
| `model/OrderTest.java` | 테스트 |
| `model/ProductionEntryTest.java` | 테스트 |

---

## OrderStatus 명세

```java
package org.example.sampleordersystem.model;

public enum OrderStatus {
    RESERVED,   // 주문 접수, 승인 대기
    REJECTED,   // 거절 (모니터링 제외)
    PRODUCING,  // 재고 부족으로 생산 중
    CONFIRMED,  // 출고 대기
    RELEASE     // 출고 완료
}
```

- 별도 테스트 없음. `Order.transitionTo()` 테스트에서 간접 검증.

---

## Sample 명세

### 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `String` | 시료 고유 식별자 |
| `name` | `String` | 시료 이름 |
| `avgProductionMinutes` | `int` | 개당 평균 생산시간(분) |
| `yield` | `double` | 수율 (0 초과 1 이하) |
| `stock` | `int` | 현재 재고 수량 |

### 생성자 검증 규칙

| 조건 | 예외 |
|------|------|
| `yield <= 0` | `IllegalArgumentException("yield must be > 0")` |
| `yield > 1` | `IllegalArgumentException("yield must be <= 1")` |
| `stock < 0` | `IllegalArgumentException("stock must be >= 0")` |

### 메서드

```java
// stock -= qty. qty가 stock보다 크면 IllegalArgumentException
void decreaseStock(int qty)

// stock += qty
void increaseStock(int qty)

// getter 전체 (id, name, avgProductionMinutes, yield, stock)
```

### Jackson 직렬화

Phase 2 JsonRepository에서 사용하므로 `@JsonCreator` / `@JsonProperty`를 생성자에 적용한다.

```java
@JsonCreator
public Sample(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("avgProductionMinutes") int avgProductionMinutes,
    @JsonProperty("yield") double yield,
    @JsonProperty("stock") int stock
) { ... }
```

---

## Order 명세

### 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `orderId` | `String` | 주문번호 (`ORD-YYYYMMDD-NNNN`) |
| `sampleId` | `String` | 시료 ID |
| `customerName` | `String` | 고객명 |
| `quantity` | `int` | 주문 수량 |
| `status` | `OrderStatus` | 현재 상태 |
| `orderedAt` | `LocalDateTime` | 주문 접수 시각 |

### 생성자

```java
// 생성 시 status는 항상 RESERVED로 고정
public Order(String orderId, String sampleId, String customerName,
             int quantity, LocalDateTime orderedAt)
```

### 상태 전환 규칙

```java
void transitionTo(OrderStatus next)
// 허용되지 않은 전환 시 IllegalStateException
```

| 현재 상태 | 허용 전환 |
|-----------|---------|
| `RESERVED` | `CONFIRMED`, `PRODUCING`, `REJECTED` |
| `PRODUCING` | `CONFIRMED` |
| `CONFIRMED` | `RELEASE` |
| `REJECTED` | 없음 |
| `RELEASE` | 없음 |

### Jackson 직렬화

```java
@JsonCreator
public Order(
    @JsonProperty("orderId") String orderId,
    @JsonProperty("sampleId") String sampleId,
    @JsonProperty("customerName") String customerName,
    @JsonProperty("quantity") int quantity,
    @JsonProperty("status") OrderStatus status,
    @JsonProperty("orderedAt") LocalDateTime orderedAt
) { ... }
```

---

## ProductionEntry 명세

### 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `orderId` | `String` | 연결된 주문번호 |
| `sampleId` | `String` | 생산 시료 ID |
| `shortage` | `int` | 부족 수량 |
| `actualQty` | `int` | 실 생산량 `ceil(shortage / (yield × 0.9))` |
| `totalMinutes` | `double` | 총 생산시간(분) `avgProductionMinutes × actualQty` |
| `startedAt` | `LocalDateTime` | 생산 시작 시각 (null = 대기 중) |

### 생성자

```java
public ProductionEntry(String orderId, String sampleId,
                       int shortage, int actualQty,
                       double totalMinutes, LocalDateTime startedAt)
```

- `startedAt`은 큐 대기 중일 때 `null`을 허용한다.

### Jackson 직렬화

```java
@JsonCreator
public ProductionEntry(
    @JsonProperty("orderId") String orderId,
    @JsonProperty("sampleId") String sampleId,
    @JsonProperty("shortage") int shortage,
    @JsonProperty("actualQty") int actualQty,
    @JsonProperty("totalMinutes") double totalMinutes,
    @JsonProperty("startedAt") LocalDateTime startedAt
) { ... }
```

---

## TDD 사이클 상세

### Cycle 1-1: `sampleRejectsNonPositiveYield`

| 항목 | 내용 |
|------|------|
| Given | yield = 0 |
| When | `new Sample("S1", "샘플", 10, 0, 100)` |
| Then | `IllegalArgumentException` 발생 |
| 예상 실패 | 검증 없이 객체 생성 성공 |

```java
@Test
@DisplayName("수율이 0 이하이면 예외를 던진다")
void sampleRejectsNonPositiveYield() {
    assertThrows(IllegalArgumentException.class,
        () -> new Sample("S1", "샘플", 10, 0, 100));
    assertThrows(IllegalArgumentException.class,
        () -> new Sample("S1", "샘플", 10, -0.1, 100));
}
```

---

### Cycle 1-2: `sampleRejectsYieldAboveOne`

| 항목 | 내용 |
|------|------|
| Given | yield = 1.1 |
| When | `new Sample(...)` |
| Then | `IllegalArgumentException` 발생 |
| 예상 실패 | 검증 없이 객체 생성 성공 |

```java
@Test
@DisplayName("수율이 1 초과이면 예외를 던진다")
void sampleRejectsYieldAboveOne() {
    assertThrows(IllegalArgumentException.class,
        () -> new Sample("S1", "샘플", 10, 1.1, 100));
}
```

---

### Cycle 1-3: `sampleRejectsNegativeStock`

| 항목 | 내용 |
|------|------|
| Given | stock = -1 |
| When | `new Sample(...)` |
| Then | `IllegalArgumentException` 발생 |

```java
@Test
@DisplayName("초기 재고가 음수이면 예외를 던진다")
void sampleRejectsNegativeStock() {
    assertThrows(IllegalArgumentException.class,
        () -> new Sample("S1", "샘플", 10, 0.9, -1));
}
```

---

### Cycle 1-4: `decreaseStockReducesStock`

| 항목 | 내용 |
|------|------|
| Given | stock=100인 Sample |
| When | `decreaseStock(30)` |
| Then | `getStock() == 70` |

```java
@Test
@DisplayName("재고를 정상 차감한다")
void decreaseStockReducesStock() {
    Sample sample = new Sample("S1", "샘플", 10, 0.9, 100);
    sample.decreaseStock(30);
    assertEquals(70, sample.getStock());
}
```

---

### Cycle 1-5: `decreaseStockRejectsOverdraft`

| 항목 | 내용 |
|------|------|
| Given | stock=10인 Sample |
| When | `decreaseStock(20)` |
| Then | `IllegalArgumentException` 발생, stock 변경 없음 |

```java
@Test
@DisplayName("재고보다 많은 수량을 차감하면 예외를 던진다")
void decreaseStockRejectsOverdraft() {
    Sample sample = new Sample("S1", "샘플", 10, 0.9, 10);
    assertThrows(IllegalArgumentException.class, () -> sample.decreaseStock(20));
    assertEquals(10, sample.getStock());
}
```

---

### Cycle 1-6: `increaseStockAddsToStock`

| 항목 | 내용 |
|------|------|
| Given | stock=50인 Sample |
| When | `increaseStock(30)` |
| Then | `getStock() == 80` |

```java
@Test
@DisplayName("재고를 정상 증가시킨다")
void increaseStockAddsToStock() {
    Sample sample = new Sample("S1", "샘플", 10, 0.9, 50);
    sample.increaseStock(30);
    assertEquals(80, sample.getStock());
}
```

---

### Cycle 1-7: `orderInitialStatusIsReserved`

| 항목 | 내용 |
|------|------|
| Given | Order 생성자 호출 |
| When | `getStatus()` |
| Then | `OrderStatus.RESERVED` |

```java
@Test
@DisplayName("주문 생성 시 상태는 RESERVED이다")
void orderInitialStatusIsReserved() {
    Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
                            LocalDateTime.now());
    assertEquals(OrderStatus.RESERVED, order.getStatus());
}
```

---

### Cycle 1-8: `orderAllowsValidTransition`

| 항목 | 내용 |
|------|------|
| Given | RESERVED 상태 Order |
| When | `transitionTo(CONFIRMED)` |
| Then | `getStatus() == CONFIRMED` |

```java
@Test
@DisplayName("허용된 상태 전환은 성공한다")
void orderAllowsValidTransition() {
    Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
                            LocalDateTime.now());
    order.transitionTo(OrderStatus.CONFIRMED);
    assertEquals(OrderStatus.CONFIRMED, order.getStatus());
}
```

---

### Cycle 1-9: `orderRejectsInvalidTransition`

| 항목 | 내용 |
|------|------|
| Given | REJECTED 상태 Order |
| When | `transitionTo(CONFIRMED)` |
| Then | `IllegalStateException` 발생 |

```java
@Test
@DisplayName("허용되지 않은 상태 전환은 예외를 던진다")
void orderRejectsInvalidTransition() {
    Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
                            LocalDateTime.now());
    order.transitionTo(OrderStatus.REJECTED);
    assertThrows(IllegalStateException.class,
        () -> order.transitionTo(OrderStatus.CONFIRMED));
}
```

---

### Cycle 1-10: `productionEntryStoresFields`

| 항목 | 내용 |
|------|------|
| Given | ProductionEntry 생성자에 모든 값 전달 |
| When | 각 getter 호출 |
| Then | 전달한 값과 동일 |

```java
@Test
@DisplayName("ProductionEntry는 모든 필드를 저장한다")
void productionEntryStoresFields() {
    LocalDateTime start = LocalDateTime.of(2024, 1, 1, 9, 0);
    ProductionEntry entry = new ProductionEntry(
        "ORD-20240101-0001", "S1", 50, 62, 310.0, start);

    assertEquals("ORD-20240101-0001", entry.getOrderId());
    assertEquals("S1", entry.getSampleId());
    assertEquals(50, entry.getShortage());
    assertEquals(62, entry.getActualQty());
    assertEquals(310.0, entry.getTotalMinutes());
    assertEquals(start, entry.getStartedAt());
}
```

---

## 완료 기준

- [ ] `./gradlew check` 성공
- [ ] 커밋 이력: Cycle별 `test(phase1): [테스트명] RED → GREEN` 커밋 10개
