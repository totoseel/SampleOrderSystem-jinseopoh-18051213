# Phase 0 — 프로젝트 기반 설정

## 목표

이후 모든 페이즈의 컴파일·테스트·커버리지 실행 기반을 마련한다.  
`build.gradle`은 설정 파일이므로 TDD 사이클 없이 구성하고,  
`TimeProvider` / `OrderIdGenerator`는 TDD 사이클로 구현한다.

---

## 산출물 목록

| 파일 | 종류 | TDD 여부 |
|------|------|---------|
| `build.gradle` | 빌드 설정 | 제외 (설정 파일) |
| `util/TimeProvider.java` | 인터페이스 | 제외 (구현 없음) |
| `util/SystemTimeProvider.java` | 구현체 | Cycle 0-5 |
| `util/OrderIdGenerator.java` | 유틸리티 | Cycle 0-1 ~ 0-4 |
| `util/OrderIdGeneratorTest.java` | 테스트 | — |
| `util/SystemTimeProviderTest.java` | 테스트 | — |

---

## build.gradle 명세

```groovy
plugins {
    id 'java'
    id 'application'
    id 'jacoco'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = 'org.example.sampleordersystem.Main'
}

dependencies {
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.1'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1'

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    dependsOn test
    reports { xml.required = true; html.required = true }
}

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

check { dependsOn jacocoTestCoverageVerification }
```

---

## TimeProvider 인터페이스

```java
package org.example.sampleordersystem.util;

import java.time.LocalDateTime;

public interface TimeProvider {
    LocalDateTime now();
}
```

- 단일 메서드 인터페이스.
- 프로덕션: `SystemTimeProvider`, 테스트: `FixedTimeProvider` (Phase 3에서 작성).

---

## SystemTimeProvider 명세

```java
package org.example.sampleordersystem.util;

import java.time.LocalDateTime;

public class SystemTimeProvider implements TimeProvider {
    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
```

### Cycle 0-5: `systemTimeProviderReturnsNow`

| 항목 | 내용 |
|------|------|
| Given | `SystemTimeProvider` 인스턴스 |
| When | `now()` 호출 |
| Then | 반환값이 `null`이 아니고, 현재 시각과 1초 이내 차이 |

```java
@Test
@DisplayName("SystemTimeProvider는 null이 아닌 현재 시각을 반환한다")
void systemTimeProviderReturnsNow() {
    SystemTimeProvider provider = new SystemTimeProvider();
    LocalDateTime before = LocalDateTime.now();
    LocalDateTime result = provider.now();
    LocalDateTime after = LocalDateTime.now();

    assertNotNull(result);
    assertFalse(result.isBefore(before));
    assertFalse(result.isAfter(after));
}
```

---

## OrderIdGenerator 명세

### 역할

`ORD-YYYYMMDD-NNNN` 형식의 주문번호를 생성한다.  
날짜가 바뀌면 순번을 1로 리셋하고, 재시작 후에도 순번을 이어받는다.

### 생성자

```java
// lastSeq: 재시작 전 마지막 순번 (최초 실행 시 0)
// timeProvider: 날짜 계산용 (테스트 격리)
public OrderIdGenerator(int lastSeq, TimeProvider timeProvider)
```

### 메서드

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `next()` | `String` | 다음 주문번호 반환, 내부 순번 증가 |
| `currentSeq()` | `int` | 현재 순번 (JSON 저장용) |

### 내부 동작

```
next() 호출 시:
  today = timeProvider.now().toLocalDate()
  if (lastDate != today):
      seq = 1
      lastDate = today
  else:
      seq += 1
  return "ORD-" + today.format("yyyyMMdd") + "-" + String.format("%04d", seq)
```

---

## TDD 사이클 상세

### Cycle 0-1: `generatesCorrectFormat`

| 항목 | 내용 |
|------|------|
| Given | `OrderIdGenerator(0, fixedTime)` — 2024-01-01 고정 |
| When | `next()` 1회 호출 |
| Then | `"ORD-20240101-0001"` 반환 |
| 예상 실패 | `OrderIdGenerator` 클래스 미존재 → 컴파일 오류 |

```java
@Test
@DisplayName("ORD-YYYYMMDD-NNNN 형식의 주문번호를 생성한다")
void generatesCorrectFormat() {
    TimeProvider fixed = () -> LocalDateTime.of(2024, 1, 1, 0, 0);
    OrderIdGenerator gen = new OrderIdGenerator(0, fixed);

    assertEquals("ORD-20240101-0001", gen.next());
}
```

---

### Cycle 0-2: `incrementsSequencePerCall`

| 항목 | 내용 |
|------|------|
| Given | `OrderIdGenerator(0, fixedTime)` — 2024-01-01 고정 |
| When | `next()` 3회 연속 호출 |
| Then | `"ORD-20240101-0001"`, `"ORD-20240101-0002"`, `"ORD-20240101-0003"` |
| 예상 실패 | 순번 증가 로직 미구현 → 항상 0001 반환 |

```java
@Test
@DisplayName("호출할 때마다 순번이 1씩 증가한다")
void incrementsSequencePerCall() {
    TimeProvider fixed = () -> LocalDateTime.of(2024, 1, 1, 0, 0);
    OrderIdGenerator gen = new OrderIdGenerator(0, fixed);

    assertEquals("ORD-20240101-0001", gen.next());
    assertEquals("ORD-20240101-0002", gen.next());
    assertEquals("ORD-20240101-0003", gen.next());
}
```

---

### Cycle 0-3: `resumesSequenceFromLastSeq`

| 항목 | 내용 |
|------|------|
| Given | `OrderIdGenerator(5, fixedTime)` — lastSeq=5, 2024-01-01 고정 |
| When | `next()` 1회 호출 |
| Then | `"ORD-20240101-0006"` — 5 다음 순번 |
| 예상 실패 | lastSeq 무시하고 1부터 시작 |

```java
@Test
@DisplayName("재시작 후 마지막 순번 이후부터 이어받는다")
void resumesSequenceFromLastSeq() {
    TimeProvider fixed = () -> LocalDateTime.of(2024, 1, 1, 0, 0);
    OrderIdGenerator gen = new OrderIdGenerator(5, fixed);

    assertEquals("ORD-20240101-0006", gen.next());
}
```

---

### Cycle 0-4: `resetsSequenceOnNewDay`

| 항목 | 내용 |
|------|------|
| Given | 1월 1일로 고정된 generator, `next()` 1회 호출 후 날짜를 1월 2일로 변경 |
| When | `next()` 재호출 |
| Then | `"ORD-20240102-0001"` — 새 날짜, 순번 리셋 |
| 예상 실패 | 날짜 변경 감지 없이 순번 계속 증가 |

```java
@Test
@DisplayName("날짜가 바뀌면 순번을 1로 리셋한다")
void resetsSequenceOnNewDay() {
    AtomicReference<LocalDateTime> time =
        new AtomicReference<>(LocalDateTime.of(2024, 1, 1, 0, 0));
    OrderIdGenerator gen = new OrderIdGenerator(0, time::get);

    gen.next(); // 2024-01-01, seq=1

    time.set(LocalDateTime.of(2024, 1, 2, 0, 0));
    assertEquals("ORD-20240102-0001", gen.next());
}
```

---

## 테스트 파일 위치

```
src/test/java/org/example/sampleordersystem/util/
├── OrderIdGeneratorTest.java
└── SystemTimeProviderTest.java
```

---

## 완료 기준

- [ ] `./gradlew build` 성공 (컴파일 오류 없음)
- [ ] `./gradlew check` 성공 (테스트 전체 통과 + JaCoCo 100%)
- [ ] 커밋 이력: Cycle별 `test(phase0): [테스트명] RED → GREEN` 커밋 5개
