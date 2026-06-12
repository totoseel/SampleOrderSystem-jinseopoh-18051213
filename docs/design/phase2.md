# Phase 2 — Repository 레이어

## 목표

도메인 객체의 저장·조회 계약(인터페이스)을 정의하고,  
InMemory 구현체(테스트·개발용)와 JsonFile 구현체(프로덕션)를 제공한다.

---

## 산출물 목록

| 파일 | 종류 |
|------|------|
| `repository/SampleRepository.java` | 인터페이스 |
| `repository/OrderRepository.java` | 인터페이스 |
| `repository/ProductionRepository.java` | 인터페이스 |
| `repository/InMemorySampleRepository.java` | 구현체 |
| `repository/InMemoryOrderRepository.java` | 구현체 |
| `repository/InMemoryProductionRepository.java` | 구현체 |
| `repository/JsonSampleRepository.java` | 구현체 |
| `repository/JsonOrderRepository.java` | 구현체 |
| `repository/JsonProductionRepository.java` | 구현체 |
| `repository/InMemorySampleRepositoryTest.java` | 테스트 |
| `repository/InMemoryOrderRepositoryTest.java` | 테스트 |
| `repository/InMemoryProductionRepositoryTest.java` | 테스트 |
| `repository/JsonSampleRepositoryTest.java` | 테스트 |
| `repository/JsonOrderRepositoryTest.java` | 테스트 |
| `repository/JsonProductionRepositoryTest.java` | 테스트 |

---

## 인터페이스 명세

### SampleRepository

```java
package org.example.sampleordersystem.repository;

public interface SampleRepository {
    void save(Sample sample);
    Optional<Sample> findById(String id);
    List<Sample> findAll();
    List<Sample> findByNameContaining(String keyword);
}
```

### OrderRepository

```java
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String id);
    List<Order> findAll();
    List<Order> findByStatus(OrderStatus status);
    int countByDatePrefix(String yyyymmdd);  // 당일 순번 계산용
}
```

### ProductionRepository

```java
public interface ProductionRepository {
    void save(ProductionEntry entry);
    List<ProductionEntry> findAll();
    Optional<ProductionEntry> findByOrderId(String orderId);
    void delete(String orderId);
}
```

---

## InMemory 구현체 명세

### 공통 규칙
- `HashMap<String, T>` 기반 저장 (key = 도메인 ID)
- `ProductionRepository`는 삽입 순서를 유지하는 `LinkedHashMap` 사용 (FIFO)
- 반환 컬렉션은 방어 복사 (`new ArrayList<>(map.values())`)

### InMemorySampleRepository

```java
// 내부: Map<String, Sample> store = new HashMap<>()
save(sample)               → store.put(sample.getId(), sample)
findById(id)               → Optional.ofNullable(store.get(id))
findAll()                  → new ArrayList<>(store.values())
findByNameContaining(kw)   → store.values().stream()
                                  .filter(s -> s.getName().contains(kw))
                                  .toList()
```

### InMemoryOrderRepository

```java
// 내부: Map<String, Order> store = new LinkedHashMap<>()
save(order)                → store.put(order.getOrderId(), order)
findById(id)               → Optional.ofNullable(store.get(id))
findAll()                  → new ArrayList<>(store.values())
findByStatus(status)       → store.values().stream()
                                  .filter(o -> o.getStatus() == status)
                                  .toList()
countByDatePrefix(prefix)  → (int) store.values().stream()
                                  .filter(o -> o.getOrderId().contains(prefix))
                                  .count()
```

### InMemoryProductionRepository

```java
// 내부: Map<String, ProductionEntry> store = new LinkedHashMap<>()
save(entry)                → store.put(entry.getOrderId(), entry)
findAll()                  → new ArrayList<>(store.values())
findByOrderId(orderId)     → Optional.ofNullable(store.get(orderId))
delete(orderId)            → store.remove(orderId)
```

---

## JsonFile 구현체 명세

### 공통 규칙

**ObjectMapper 설정**
```java
ObjectMapper mapper = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

**원자적 쓰기**
```java
private void writeJson(List<T> data) throws IOException {
    Path parent = filePath.toAbsolutePath().getParent();
    Path tmp = Files.createTempFile(parent, "tmp-", ".json");
    try {
        mapper.writeValue(tmp.toFile(), data);
        Files.move(tmp, filePath, StandardCopyOption.ATOMIC_MOVE,
                                  StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
        Files.deleteIfExists(tmp);
        throw e;
    }
}
```

**읽기 — 파일 없으면 빈 리스트**
```java
private List<T> readJson() {
    if (!Files.exists(filePath)) return new ArrayList<>();
    return mapper.readValue(filePath.toFile(), listTypeRef);
}
```

### JsonSampleRepository

```java
public class JsonSampleRepository implements SampleRepository {
    private final Path filePath;
    private final ObjectMapper mapper;

    public JsonSampleRepository(Path filePath) { ... }
    // save, findById, findAll, findByNameContaining — readJson/writeJson 기반
}
```

### JsonOrderRepository

- `countByDatePrefix`: 전체 리스트 로드 후 `orderId.startsWith("ORD-" + yyyymmdd)` 필터 카운트

### JsonProductionRepository

- `findAll()`: 삽입 순서 유지 (JSON 배열 순서 보존)

---

## TDD 사이클 상세

### Cycle 2-1: `saveAndFindById` (InMemory)

| 항목 | 내용 |
|------|------|
| Given | `InMemorySampleRepository` |
| When | `save(sample)` 후 `findById(id)` |
| Then | `Optional` 내 객체가 저장한 sample과 동일 |
| 예상 실패 | 클래스 미존재 → 컴파일 오류 |

```java
@Test
@DisplayName("저장한 시료를 ID로 조회할 수 있다")
void saveAndFindById() {
    SampleRepository repo = new InMemorySampleRepository();
    Sample sample = new Sample("S1", "실리콘", 5, 0.9, 100);
    repo.save(sample);

    Optional<Sample> found = repo.findById("S1");
    assertTrue(found.isPresent());
    assertEquals("실리콘", found.get().getName());
}
```

---

### Cycle 2-2: `findAllReturnsAll` (InMemory)

| 항목 | 내용 |
|------|------|
| Given | 시료 3개 저장 |
| When | `findAll()` |
| Then | 리스트 크기 = 3 |

```java
@Test
@DisplayName("저장된 모든 시료를 반환한다")
void findAllReturnsAll() {
    SampleRepository repo = new InMemorySampleRepository();
    repo.save(new Sample("S1", "A", 5, 0.9, 10));
    repo.save(new Sample("S2", "B", 5, 0.9, 10));
    repo.save(new Sample("S3", "C", 5, 0.9, 10));

    assertEquals(3, repo.findAll().size());
}
```

---

### Cycle 2-3: `findByStatusFilters` (InMemory)

| 항목 | 내용 |
|------|------|
| Given | RESERVED 2건, CONFIRMED 1건 저장 |
| When | `findByStatus(RESERVED)` |
| Then | 크기 = 2 |

```java
@Test
@DisplayName("상태로 주문을 필터링한다")
void findByStatusFilters() {
    OrderRepository repo = new InMemoryOrderRepository();
    Order o1 = new Order("ORD-20240101-0001", "S1", "A", 10, LocalDateTime.now());
    Order o2 = new Order("ORD-20240101-0002", "S1", "B", 5, LocalDateTime.now());
    Order o3 = new Order("ORD-20240101-0003", "S1", "C", 3, LocalDateTime.now());
    o3.transitionTo(OrderStatus.CONFIRMED);
    repo.save(o1); repo.save(o2); repo.save(o3);

    assertEquals(2, repo.findByStatus(OrderStatus.RESERVED).size());
}
```

---

### Cycle 2-4: `findByNameContaining` (InMemory)

| 항목 | 내용 |
|------|------|
| Given | "갈륨비소", "갈륨나이트라이드", "실리콘" 저장 |
| When | `findByNameContaining("갈륨")` |
| Then | 크기 = 2 |

```java
@Test
@DisplayName("이름에 키워드가 포함된 시료를 반환한다")
void findByNameContaining() {
    SampleRepository repo = new InMemorySampleRepository();
    repo.save(new Sample("S1", "갈륨비소", 5, 0.9, 10));
    repo.save(new Sample("S2", "갈륨나이트라이드", 5, 0.9, 10));
    repo.save(new Sample("S3", "실리콘", 5, 0.9, 10));

    assertEquals(2, repo.findByNameContaining("갈륨").size());
}
```

---

### Cycle 2-5: `countByDatePrefix` (InMemory)

| 항목 | 내용 |
|------|------|
| Given | 20240101 주문 3건, 20240102 주문 1건 |
| When | `countByDatePrefix("20240101")` |
| Then | 3 |

```java
@Test
@DisplayName("특정 날짜 주문 수를 카운트한다")
void countByDatePrefix() {
    OrderRepository repo = new InMemoryOrderRepository();
    repo.save(new Order("ORD-20240101-0001", "S1", "A", 1, LocalDateTime.now()));
    repo.save(new Order("ORD-20240101-0002", "S1", "B", 1, LocalDateTime.now()));
    repo.save(new Order("ORD-20240101-0003", "S1", "C", 1, LocalDateTime.now()));
    repo.save(new Order("ORD-20240102-0001", "S1", "D", 1, LocalDateTime.now()));

    assertEquals(3, repo.countByDatePrefix("20240101"));
}
```

---

### Cycle 2-6: `productionDeleteRemovesEntry` (InMemory)

| 항목 | 내용 |
|------|------|
| Given | ProductionEntry 저장 후 |
| When | `delete(orderId)` |
| Then | `findByOrderId(orderId)` → `Optional.empty()` |

```java
@Test
@DisplayName("생산 항목을 삭제하면 조회되지 않는다")
void productionDeleteRemovesEntry() {
    ProductionRepository repo = new InMemoryProductionRepository();
    ProductionEntry entry = new ProductionEntry("ORD-20240101-0001", "S1",
                                               50, 62, 310.0, null);
    repo.save(entry);
    repo.delete("ORD-20240101-0001");

    assertTrue(repo.findByOrderId("ORD-20240101-0001").isEmpty());
}
```

---

### Cycle 2-7: `jsonSaveAndReload` (JsonFile)

| 항목 | 내용 |
|------|------|
| Given | `@TempDir`로 격리된 경로의 `JsonSampleRepository` |
| When | `save(sample)` 후 **새 인스턴스** 생성, `findById()` |
| Then | 동일 데이터 반환 |
| 예상 실패 | 파일 저장 로직 미구현 |

```java
@Test
@DisplayName("JSON 파일에 저장 후 새 인스턴스에서 재로드할 수 있다")
void jsonSaveAndReload(@TempDir Path dir) {
    Path file = dir.resolve("samples.json");
    JsonSampleRepository repo1 = new JsonSampleRepository(file);
    repo1.save(new Sample("S1", "갈륨비소", 5, 0.9, 100));

    JsonSampleRepository repo2 = new JsonSampleRepository(file);
    Optional<Sample> found = repo2.findById("S1");
    assertTrue(found.isPresent());
    assertEquals("갈륨비소", found.get().getName());
}
```

---

### Cycle 2-8: `jsonAtomicWrite` (JsonFile)

| 항목 | 내용 |
|------|------|
| Given | `@TempDir` 경로 |
| When | `save()` 완료 후 |
| Then | 최종 파일만 존재 (임시 파일 없음) |

```java
@Test
@DisplayName("저장 후 임시 파일이 남지 않는다")
void jsonAtomicWrite(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("samples.json");
    JsonSampleRepository repo = new JsonSampleRepository(file);
    repo.save(new Sample("S1", "A", 5, 0.9, 10));

    long fileCount = Files.list(dir).count();
    assertEquals(1, fileCount); // samples.json 하나만
}
```

---

### Cycle 2-9: `jsonSurvivesRestart` (JsonFile)

| 항목 | 내용 |
|------|------|
| Given | 주문 저장 후 OrderRepository 인스턴스 재생성 |
| When | `findAll()` |
| Then | 저장한 주문 포함 |

```java
@Test
@DisplayName("재시작 후에도 JSON 파일에서 주문 데이터를 복구한다")
void jsonSurvivesRestart(@TempDir Path dir) {
    Path file = dir.resolve("orders.json");
    Order order = new Order("ORD-20240101-0001", "S1", "홍길동", 10,
                            LocalDateTime.of(2024, 1, 1, 9, 0));

    new JsonOrderRepository(file).save(order);

    List<Order> orders = new JsonOrderRepository(file).findAll();
    assertEquals(1, orders.size());
    assertEquals("ORD-20240101-0001", orders.get(0).getOrderId());
}
```

---

### Cycle 2-10: `jsonHandlesLocalDateTime` (JsonFile)

| 항목 | 내용 |
|------|------|
| Given | `startedAt`이 포함된 `ProductionEntry` 저장 |
| When | 재로드 후 `getStartedAt()` |
| Then | 원본 `LocalDateTime`과 동일 (나노초 단위까지) |

```java
@Test
@DisplayName("LocalDateTime이 JSON 직렬화·역직렬화 후에도 동일하다")
void jsonHandlesLocalDateTime(@TempDir Path dir) {
    Path file = dir.resolve("production.json");
    LocalDateTime started = LocalDateTime.of(2024, 1, 1, 9, 30, 0);
    ProductionEntry entry = new ProductionEntry(
        "ORD-20240101-0001", "S1", 50, 62, 310.0, started);

    new JsonProductionRepository(file).save(entry);

    Optional<ProductionEntry> found =
        new JsonProductionRepository(file).findByOrderId("ORD-20240101-0001");
    assertTrue(found.isPresent());
    assertEquals(started, found.get().getStartedAt());
}
```

---

## 완료 기준

- [ ] `./gradlew check` 성공
- [ ] 커밋 이력: Cycle별 `test(phase2): [테스트명] RED → GREEN` 커밋 10개
