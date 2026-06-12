# PLAN — S-Semi 반도체 시료 생산주문관리 시스템

## 패키지 구조

```
src/
├── main/java/org/example/sampleordersystem/
│   ├── model/
│   │   ├── Sample.java
│   │   ├── Order.java
│   │   ├── OrderStatus.java
│   │   └── ProductionEntry.java
│   ├── repository/
│   │   ├── SampleRepository.java               (인터페이스)
│   │   ├── OrderRepository.java                (인터페이스)
│   │   ├── ProductionRepository.java           (인터페이스)
│   │   ├── InMemorySampleRepository.java
│   │   ├── InMemoryOrderRepository.java
│   │   ├── InMemoryProductionRepository.java
│   │   ├── JsonSampleRepository.java
│   │   ├── JsonOrderRepository.java
│   │   └── JsonProductionRepository.java
│   ├── service/
│   │   ├── SampleService.java
│   │   ├── OrderService.java
│   │   └── ProductionService.java
│   ├── controller/
│   │   ├── SampleController.java
│   │   ├── OrderController.java
│   │   ├── ProductionController.java
│   │   └── MonitoringController.java
│   ├── view/
│   │   ├── View.java                           (인터페이스)
│   │   └── ConsoleView.java
│   ├── util/
│   │   ├── TimeProvider.java                   (인터페이스)
│   │   ├── SystemTimeProvider.java
│   │   └── OrderIdGenerator.java
│   ├── app/
│   │   └── App.java
│   └── Main.java
└── test/java/org/example/sampleordersystem/
    ├── model/
    ├── repository/
    ├── service/
    ├── controller/
    ├── view/
    └── util/
```

---

## 페이즈 구성

| Phase | 범위 | 핵심 산출물 |
|-------|------|------------|
| 0 | 프로젝트 기반 설정 | `build.gradle`, `TimeProvider`, `OrderIdGenerator` |
| 1 | 도메인 모델 | `Sample`, `Order`, `OrderStatus`, `ProductionEntry` |
| 2 | Repository 레이어 | 인터페이스 3종 + InMemory 구현체 3종 + JsonFile 구현체 3종 |
| 3 | Service 레이어 | `SampleService`, `OrderService`, `ProductionService` |
| 4 | Controller + View 레이어 | Controller 4종, `View` 인터페이스, `ConsoleView` |
| 5 | App + Main | `App`, `Main` |
| 6 | 통합 검증 | 영속성·시간배율·전체 플로우 E2E |
| 7 | 마무리 | JaCoCo 100% 확인, `.gitignore`, 문서 동기화 |

---

## Phase 0 — 프로젝트 기반 설정

### 목표
빌드 환경과 공통 유틸리티를 갖춰 이후 페이즈의 컴파일·테스트 실행 기반을 마련한다.

### 구현 대상

#### `build.gradle`
- Java 21 toolchain
- `application` 플러그인, `mainClass = 'org.example.sampleordersystem.Main'`
- JaCoCo 0.8.11: `Main.class` 제외, instruction coverage 100% 강제
- 의존성: `jackson-databind:2.17.1`, `jackson-datatype-jsr310:2.17.1`, `junit-bom:5.10.2`

#### `TimeProvider` (인터페이스)
```
패키지: util
메서드: LocalDateTime now()
```

#### `SystemTimeProvider` (구현체)
```
패키지: util
now() → LocalDateTime.now()
```

#### `OrderIdGenerator`
```
패키지: util
역할: ORD-YYYYMMDD-NNNN 형식 생성
생성자: OrderIdGenerator(int lastSeq)   ← 재시작 후 순번 복구용
메서드: String next()                   ← 날짜 바뀌면 순번 1 리셋
        int currentSeq()               ← 현재 순번 (JSON 저장용)
```

### TDD 사이클 목록 (SubAgent2 실행 순서)

| 순서 | 테스트명 | 검증 동작 |
|------|---------|----------|
| 0-1 | `generatesCorrectFormat` | `ORD-20240101-0001` 형식 생성 |
| 0-2 | `incrementsSequencePerCall` | 호출마다 순번 증가 |
| 0-3 | `resumesSequenceFromLastSeq` | 재시작 후 순번 이어받기 |
| 0-4 | `resetsSequenceOnNewDay` | 날짜 변경 시 순번 1로 리셋 |
| 0-5 | `systemTimeProviderReturnsNow` | `SystemTimeProvider.now()` 검증 |

### 완료 기준
- `./gradlew build` 성공
- `./gradlew check` 성공 (커버리지 포함)

---

## Phase 1 — 도메인 모델

### 목표
비즈니스 규칙을 담은 도메인 객체를 정의한다. 레이어 의존성 없이 순수 Java 객체로만 구성한다.

### 구현 대상

#### `OrderStatus` (enum)
```
값: RESERVED, REJECTED, PRODUCING, CONFIRMED, RELEASE
```

#### `Sample`
```
필드: String id, String name, int avgProductionMinutes, double yield, int stock
생성자 검증:
  - yield: 0 초과 1 이하 → 위반 시 IllegalArgumentException
  - stock: 0 이상 → 위반 시 IllegalArgumentException
메서드:
  - decreaseStock(int qty)   → stock -= qty, 음수 불가
  - increaseStock(int qty)   → stock += qty
```

#### `Order`
```
필드: String orderId, String sampleId, String customerName,
      int quantity, OrderStatus status, LocalDateTime orderedAt
생성 시 status = RESERVED
메서드:
  - transitionTo(OrderStatus next) → 허용된 전환만 허용, 위반 시 IllegalStateException
허용 전환표:
  RESERVED  → CONFIRMED, PRODUCING, REJECTED
  PRODUCING → CONFIRMED
  CONFIRMED → RELEASE
  REJECTED, RELEASE → 전환 불가
```

#### `ProductionEntry`
```
필드: String orderId, String sampleId, int shortage,
      int actualQty, double totalMinutes, LocalDateTime startedAt
생성자: 모든 필드 주입
totalMinutes = avgProductionMinutes × actualQty  (timeScale은 Service에서 적용)
```

### TDD 사이클 목록

| 순서 | 테스트명 | 검증 동작 |
|------|---------|----------|
| 1-1 | `sampleRejectsNonPositiveYield` | yield ≤ 0 → 예외 |
| 1-2 | `sampleRejectsYieldAboveOne` | yield > 1 → 예외 |
| 1-3 | `sampleRejectsNegativeStock` | stock < 0 → 예외 |
| 1-4 | `decreaseStockReducesStock` | stock 정상 차감 |
| 1-5 | `decreaseStockRejectsOverdraft` | 재고 초과 차감 → 예외 |
| 1-6 | `increaseStockAddsToStock` | stock 정상 증가 |
| 1-7 | `orderInitialStatusIsReserved` | 생성 시 RESERVED |
| 1-8 | `orderAllowsValidTransition` | 허용된 상태 전환 성공 |
| 1-9 | `orderRejectsInvalidTransition` | 금지된 전환 → 예외 |
| 1-10 | `productionEntryStoresFields` | ProductionEntry 필드 저장 확인 |

### 완료 기준
- `./gradlew check` 성공

---

## Phase 2 — Repository 레이어

### 목표
도메인 객체의 저장·조회 계약(인터페이스)을 정의하고, InMemory 구현체와 JsonFile 구현체를 제공한다.

### 구현 대상

#### 인터페이스 3종

**`SampleRepository`**
```
save(Sample)
findById(String id) → Optional<Sample>
findAll() → List<Sample>
findByNameContaining(String keyword) → List<Sample>
```

**`OrderRepository`**
```
save(Order)
findById(String id) → Optional<Order>
findAll() → List<Order>
findByStatus(OrderStatus) → List<Order>
countByDatePrefix(String yyyymmdd) → int   ← 당일 주문번호 순번 계산용
```

**`ProductionRepository`**
```
save(ProductionEntry)
findAll() → List<ProductionEntry>
findByOrderId(String orderId) → Optional<ProductionEntry>
delete(String orderId)
```

#### InMemory 구현체 3종
- `HashMap` / `ArrayList` 기반
- 테스트·개발 환경용

#### JsonFile 구현체 3종
- 파일 경로는 생성자로 주입
- Jackson `@JsonCreator` / `@JsonProperty`, `JavaTimeModule`, `WRITE_DATES_AS_TIMESTAMPS = false`
- 쓰기: `Files.createTempFile` → `Files.move(ATOMIC_MOVE)`
- `getParent()` null 방어: `toAbsolutePath().getParent()` fallback

### TDD 사이클 목록

| 순서 | 테스트명 | 검증 동작 |
|------|---------|----------|
| 2-1 | `saveAndFindById` | InMemory: 저장 후 ID 조회 |
| 2-2 | `findAllReturnsAll` | InMemory: 전체 조회 |
| 2-3 | `findByStatusFilters` | InMemory: 상태 필터 |
| 2-4 | `findByNameContaining` | InMemory: 이름 부분 검색 |
| 2-5 | `countByDatePrefix` | InMemory: 당일 주문 수 카운트 |
| 2-6 | `productionDeleteRemovesEntry` | InMemory: 생산 항목 삭제 |
| 2-7 | `jsonSaveAndReload` | JsonFile: 저장 후 재로드 동일성 |
| 2-8 | `jsonAtomicWrite` | JsonFile: 임시파일→ATOMIC_MOVE 확인 |
| 2-9 | `jsonSurvivesRestart` | JsonFile: 재시작 후 데이터 유지 |
| 2-10 | `jsonHandlesLocalDateTime` | JsonFile: LocalDateTime 직렬화·역직렬화 |

### 완료 기준
- `./gradlew check` 성공

---

## Phase 3 — Service 레이어

### 목표
핵심 비즈니스 로직(재고 판단, 생산라인 스케줄링, 진행률 계산)을 Service에 집중시킨다.

### 구현 대상

#### `SampleService`
```
register(String id, String name, int avgMin, double yield, int stock)
  → 중복 ID 검증 후 Sample 저장
findAll() → List<Sample>
findByNameContaining(String keyword) → List<Sample>
findById(String id) → Optional<Sample>
```

#### `OrderService`
```
생성자: OrderService(OrderRepository, SampleRepository, ProductionService, OrderIdGenerator)

placeOrder(String sampleId, String customerName, int quantity) → Order
  → 시료 존재 검증, RESERVED 상태로 저장

approve(String orderId)
  → shortage = max(0, quantity - stock)
  → shortage == 0: stock 차감, CONFIRMED 전환
  → shortage > 0: PRODUCING 전환, ProductionService.enqueue()

reject(String orderId)
  → REJECTED 전환

findByStatus(OrderStatus) → List<Order>
findAll() → List<Order>
```

#### `ProductionService`
```
생성자: ProductionService(ProductionRepository, OrderRepository, SampleRepository, TimeProvider, double timeScale)

enqueue(ProductionEntry entry)
  → 큐 말단 추가
  → 현재 생산 중인 항목 없으면 즉시 startedAt = now() 기록

tick()
  → 현재 생산 중인 항목의 진행률 계산
  → 진행률 = (now - startedAt).toSeconds() / (totalMinutes / timeScale * 60) × 100
  → 진행률 ≥ 100: CONFIRMED 전환, 재고 반영, 항목 제거, 다음 항목 시작

getQueue() → List<ProductionEntry>       (대기 포함 전체, FIFO 순)
getCurrent() → Optional<ProductionEntry>
getProgress() → double                   (현재 항목 진행률 %)
getEstimatedFinishAt(ProductionEntry) → LocalDateTime
```

### TDD 사이클 목록

| 순서 | 테스트명 | 검증 동작 |
|------|---------|----------|
| 3-1 | `registerSampleSavesToRepository` | 시료 등록 저장 확인 |
| 3-2 | `registerRejectsDuplicateId` | 중복 ID 거절 |
| 3-3 | `placeOrderCreatesReservedOrder` | 주문 접수 → RESERVED |
| 3-4 | `placeOrderRejectsUnknownSample` | 미등록 시료 → 예외 |
| 3-5 | `approveWithSufficientStock` | 재고 충분 → CONFIRMED + 재고 차감 |
| 3-6 | `approveWithInsufficientStock` | 재고 부족 → PRODUCING + 생산 등록 |
| 3-7 | `rejectOrderTransitionsToRejected` | 거절 → REJECTED |
| 3-8 | `enqueueStartsImmediatelyWhenIdle` | 큐 비어있으면 즉시 시작 |
| 3-9 | `enqueueWaitsWhenBusy` | 생산 중이면 대기 |
| 3-10 | `tickDoesNotCompleteBeforeTime` | 미완료 시 상태 유지 |
| 3-11 | `tickCompletesAndTransitionsToConfirmed` | 완료 시 CONFIRMED + 재고 반영 |
| 3-12 | `tickStartsNextEntryAfterCompletion` | 완료 후 다음 항목 자동 시작 |
| 3-13 | `actualQtyFormula` | `ceil(shortage / (yield × 0.9))` 계산 검증 |
| 3-14 | `timeScaleAcceleratesCompletion` | timeScale 배율 적용 검증 |

### 완료 기준
- `./gradlew check` 성공

---

## Phase 4 — Controller + View 레이어

### 목표
사용자 입력을 검증하고 Service에 위임한다. View는 출력만 담당한다.

### 구현 대상

#### `View` (인터페이스)
```
showMainSummary(int sampleCount, int totalStock, int orderCount,
                int queueSize, Optional<ProductionEntry> current,
                double progress, int confirmedCount)
showMenu(List<String> options)
showSamples(List<Sample> samples)
showOrders(List<Order> orders)
showProductionStatus(Optional<ProductionEntry> current, double progress,
                     LocalDateTime estimatedFinish, List<ProductionEntry> queue)
showMonitoringSummary(Map<OrderStatus, Long> statusCounts, List<Sample> samples)
showMessage(String message)
showError(String message)
readLine() → String
```

#### `ConsoleView`
- `Scanner(System.in)` 기반
- 생성자로 `Scanner` 주입 (테스트 격리용)
- 모든 출력 포맷팅은 ConsoleView 내부 처리

#### `SampleController`
```
handleRegister()   → View로 입력 수집 → SampleService.register()
handleList()       → SampleService.findAll() → View 출력
handleSearch()     → View로 키워드 수집 → SampleService.findByNameContaining() → View 출력
```

#### `OrderController`
```
handlePlace()      → 시료 ID, 고객명, 수량 입력 → OrderService.placeOrder()
handleApprove()    → RESERVED 목록 표시 → 선택 → OrderService.approve()
handleReject()     → RESERVED 목록 표시 → 선택 → OrderService.reject()
```

#### `ProductionController`
```
handleView()       → ProductionService 조회 → View.showProductionStatus()
```

#### `MonitoringController`
```
handleView()       → 상태별 주문 수 + 시료별 재고 → View.showMonitoringSummary()
                    (REJECTED 제외)
```

### TDD 사이클 목록

| 순서 | 테스트명 | 검증 동작 |
|------|---------|----------|
| 4-1 | `registerSampleCallsService` | 유효 입력 → Service.register() 호출 |
| 4-2 | `registerSampleShowsErrorOnInvalidYield` | 잘못된 수율 → 오류 출력 |
| 4-3 | `placeOrderCallsService` | 유효 입력 → Service.placeOrder() 호출 |
| 4-4 | `approveCallsService` | 주문 선택 → Service.approve() 호출 |
| 4-5 | `rejectCallsService` | 주문 선택 → Service.reject() 호출 |
| 4-6 | `monitoringExcludesRejected` | REJECTED 주문 미포함 확인 |
| 4-7 | `consoleViewRendersMainSummary` | 현황 요약 출력 형식 확인 |
| 4-8 | `consoleViewRendersProductionStatus` | 생산라인 출력 형식 확인 |

### 완료 기준
- `./gradlew check` 성공

---

## Phase 5 — App + Main

### 목표
메뉴 루프를 구성하고 의존성을 조립한다.

### 구현 대상

#### `App`
```
생성자: App(SampleController, OrderController, ProductionController,
            MonitoringController, ProductionService, View)

run()
  → 루프:
      1. ProductionService.tick()
      2. View.showMainSummary(...)
      3. View.showMenu(6개 항목)
      4. 입력에 따라 각 Controller 위임
      5. "0" 입력 시 루프 종료
```

#### `Main`
```
main(String[] args)
  → --time-scale 파싱 (잘못된 값 → 1.0)
  → JsonRepository 생성 (파일 경로: data/*.json)
  → OrderIdGenerator 초기화 (OrderRepository에서 당일 최대 순번 복구)
  → 나머지 의존성 조립
  → App.run()
```

### TDD 사이클 목록

| 순서 | 테스트명 | 검증 동작 |
|------|---------|----------|
| 5-1 | `appCallsTickOnEachLoop` | 루프마다 tick() 호출 확인 |
| 5-2 | `appExitsOnZeroInput` | "0" 입력 시 루프 종료 |
| 5-3 | `appRoutesToCorrectController` | 메뉴 번호 → 올바른 Controller 위임 |

> `Main`은 JaCoCo 제외 대상이므로 별도 테스트를 작성하지 않는다.

### 완료 기준
- `./gradlew check` 성공
- `./gradlew run` 실행 후 메인 메뉴 출력 확인

---

## Phase 6 — 통합 검증

### 목표
실제 JsonFile Repository를 사용해 전체 플로우가 정상 동작하는지 검증한다.

### 검증 시나리오

| 시나리오 | 검증 항목 |
|---------|----------|
| 시료 등록 → 재시작 → 시료 목록 조회 | JSON 영속성 |
| 주문 접수 → 승인(재고 충분) → 출고 | 재고 충분 플로우 |
| 주문 접수 → 승인(재고 부족) → tick() 반복 → CONFIRMED | 생산 완료 플로우 |
| 재시작 후 주문번호 순번 이어받기 | OrderIdGenerator 복구 |
| `--time-scale 60` 적용 후 빠른 생산 완료 | 시간 배율 |
| REJECTED 주문 모니터링 미포함 | 모니터링 필터 |

### 완료 기준
- `./gradlew check` 성공
- 위 시나리오 수동 확인

---

## Phase 7 — 마무리

### 체크리스트

- [ ] `./gradlew check` 최종 통과 (instruction coverage 100%)
- [ ] `.gitignore` 정비 (`build/`, `*.json` 데이터 파일, `.idea/`)
- [ ] `CLAUDE.md` 아키텍처 섹션 최종 동기화
- [ ] `PRD.md` ↔ `PLAN.md` ↔ `CLAUDE.md` 정합성 최종 확인 (SubAgent1)
- [ ] 페이즈별 커밋 이력 정리

---

## 커밋 메시지 규칙

| 상황 | 형식 |
|------|------|
| TDD GREEN 완료 | `test(phaseN): [테스트명] RED → GREEN` |
| 페이즈 구현 완료 | `feat(phaseN): <내용 요약>` |
| 버그 수정 | `fix(phaseN): <수정 내용>` |
| 문서 변경 | `docs: <내용>` |
| 설정 변경 | `chore: <내용>` |

---

## 페이즈 진행 기준

다음 조건을 **모두** 충족해야 다음 페이즈로 진행한다.

- [ ] SubAgent1: 문서 정합성 이상 없음
- [ ] SubAgent3: `./gradlew check` 성공 (빌드 + 테스트 + JaCoCo 100%)
- [ ] SubAgent4: 아키텍처·코드 규칙 위반 없음

하나라도 실패하면 SubAgent2에게 수정 지시 후 재검증한다.
