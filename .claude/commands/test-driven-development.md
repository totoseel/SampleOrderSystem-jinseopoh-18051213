---
name: test-driven-development
description: 모든 기능 개발 또는 버그 수정 시 구현 코드를 작성하기 전에 사용 (Java + Gradle + JUnit 5)
---

# 테스트 주도 개발 (TDD) — Java / Gradle / JUnit 5

## 개요

테스트를 먼저 작성한다. 실패하는 것을 확인한다. 통과시킬 최소한의 코드를 작성한다.

**핵심 원칙:** 테스트가 실패하는 것을 직접 보지 않았다면, 그 테스트가 올바른 것을 검증하는지 알 수 없다.

**규칙의 문구를 어기는 것은 규칙의 정신을 어기는 것이다.**

## 언제 사용하는가

**항상:**
- 새로운 기능
- 버그 수정
- 리팩터링
- 동작 변경

**예외 (사용자에게 확인 필요):**
- 일회성 프로토타입
- 자동 생성된 코드
- 설정 파일

"이번 한 번만 TDD를 건너뛰자"는 생각이 든다면? 멈춰라. 그것은 합리화다.

## 절대 법칙

```
실패하는 테스트 없이 프로덕션 코드를 작성하지 말 것
RED, REVIEW 단계는 사람의 검토 없이 넘어가지 말 것
```

테스트보다 코드를 먼저 작성했는가? 삭제하라. 처음부터 다시 시작하라.

**예외 없음:**
- "참고용"으로 보관하지 마라
- 테스트를 작성하면서 그 코드를 "각색"하지 마라
- 그 코드를 보지 마라
- 삭제는 삭제다

테스트로부터 새롭게 구현하라. 끝.

## Red-Green-Review 사이클

```
RED (계획 → 사람 검토 → 테스트 작성 → 실패 확인)
  ↓  사람이 승인해야 GREEN으로 진입
GREEN (최소 구현 → 통과 확인)
  ↓
REVIEW (코드 검토 → 사람 검토)
  ↓  사람이 승인해야 다음 사이클 RED로 진입
다음 사이클
```

> **⛔ RED 단계 종료 시, REVIEW 단계 종료 시 반드시 멈추고 사람에게 검토를 요청한다.**
> 사람의 명시적 승인 없이 다음 단계로 넘어가지 않는다.

---

### RED — 계획 작성 → 사람 검토 → 테스트 작성 → 실패 확인

#### 1단계: Plan.md 작성

테스트를 작성하기 전에 **Plan.md** 파일을 만들어 다음을 기록한다:

```markdown
## Cycle N — [테스트 이름]

### 검증할 동작
- (무엇을 테스트하는가)

### 테스트 시나리오
- Given: (초기 상태)
- When: (실행하는 동작)
- Then: (기대하는 결과)

### 예상 실패 이유
- (어떤 기능이 없어서 실패하는가)

### 구현 범위
- (이 테스트를 통과시키기 위해 최소한 무엇이 필요한가)
```

#### 2단계: 사람 검토 요청 (필수)

Plan.md 작성 후 **반드시 멈추고** 다음과 같이 검토를 요청한다:

```
🔴 RED 계획 검토 요청 — Cycle N: [테스트 이름]

Plan.md를 작성했습니다. 검토해주세요:
- 검증할 동작이 맞는가?
- 테스트 시나리오가 요구사항을 올바르게 반영하는가?
- 이 사이클의 범위가 적절한가?

승인하시면 테스트 코드를 작성하겠습니다.
```

사람이 승인하기 전까지 테스트 코드를 작성하지 않는다.

#### 3단계: 테스트 작성

승인 후, 계획에 따라 최소한의 테스트 하나를 작성한다.

<Good>
```java
@Test
@DisplayName("실패한 작업을 3번 재시도한다")
void retriesFailedOperationsThreeTimes() {
    AtomicInteger attempts = new AtomicInteger(0);
    Supplier<String> operation = () -> {
        int current = attempts.incrementAndGet();
        if (current < 3) throw new RuntimeException("fail");
        return "success";
    };

    String result = RetryHelper.retryOperation(operation);

    assertEquals("success", result);
    assertEquals(3, attempts.get());
}
```
명확한 이름, 실제 동작 검증, 한 가지만 테스트
</Good>

<Bad>
```java
@Test
void retryWorks() {
    @SuppressWarnings("unchecked")
    Supplier<String> mock = mock(Supplier.class);
    when(mock.get())
        .thenThrow(new RuntimeException())
        .thenThrow(new RuntimeException())
        .thenReturn("success");

    RetryHelper.retryOperation(mock);

    verify(mock, times(3)).get();
}
```
모호한 이름, 실제 코드가 아닌 mock을 검증
</Bad>

**요구사항:**
- 하나의 동작
- 명확한 이름
- 실제 코드 사용 (불가피하지 않다면 mock 사용 금지)

#### 4단계: 실패 확인 (필수. 절대 건너뛰지 말 것)

```bash
./gradlew test --tests "com.example.RetryHelperTest.retriesFailedOperationsThreeTimes"
```

확인할 것:
- 테스트가 실패하는가 (오류가 아닌)
- 실패 메시지가 예상한 그대로인가
- 기능이 없어서 실패하는가 (오타 때문이 아닌)

**테스트가 통과한다고?** 이미 존재하는 동작을 테스트하고 있는 것이다. 테스트를 고쳐라.

**테스트가 컴파일/실행 오류를 낸다고?** 오류를 고치고, 올바르게 실패할 때까지 다시 실행하라.

---

### GREEN — 최소 구현 코드 → 통과 확인

#### 1단계: 최소 구현

테스트를 통과시킬 가장 단순한 코드를 작성한다.

<Good>
```java
public final class RetryHelper {
    private RetryHelper() {}

    public static <T> T retryOperation(Supplier<T> fn) {
        for (int i = 0; i < 3; i++) {
            try {
                return fn.get();
            } catch (RuntimeException e) {
                if (i == 2) throw e;
            }
        }
        throw new IllegalStateException("unreachable");
    }
}
```
통과시킬 만큼만
</Good>

<Bad>
```java
public final class RetryHelper {
    public static <T> T retryOperation(
        Supplier<T> fn,
        int maxRetries,
        BackoffStrategy backoff,
        Consumer<Integer> onRetry,
        Predicate<Throwable> retryOn
    ) {
        // YAGNI — 지금 필요 없음
    }
}
```
과도한 설계
</Bad>

기능을 추가하지 마라, 다른 코드를 리팩터링하지 마라, 테스트가 요구하는 것 이상으로 "개선"하지 마라.

#### 2단계: 통과 확인 (필수)

```bash
./gradlew test
```

확인할 것:
- 새 테스트가 통과하는가
- 기존 테스트도 모두 여전히 통과하는가
- 출력이 깨끗한가 (오류, 경고 없음)

**테스트가 실패한다고?** 코드를 고쳐라. 테스트가 아니다.

**다른 테스트가 깨졌다고?** 지금 고쳐라.

---

### REVIEW — 코드 검토 → 사람 검토 → 다음 사이클 결정

#### 1단계: 코드 자가 검토

GREEN 상태에서 다음을 점검한다:

- 중복이 있는가? → 제거
- 이름이 의도를 표현하는가? → 개선
- 추출할 헬퍼가 있는가? → 추출

리팩터링 시 테스트를 계속 그린 상태로 유지한다. 동작을 추가하지 않는다.

리팩터링 후 다시 실행:
```bash
./gradlew test
```

#### 2단계: 사람 검토 요청 (필수)

**반드시 멈추고** 다음과 같이 검토를 요청한다:

```
✅ REVIEW 검토 요청 — Cycle N: [테스트 이름]

GREEN 단계가 완료되었습니다. 검토해주세요:

[구현한 코드 요약]
- 변경된 프로덕션 코드: (파일명:줄번호)
- 테스트: (테스트 메서드명)

점검 항목:
- 구현이 테스트의 의도를 올바르게 반영하는가?
- 불필요한 코드가 없는가?
- 다음 사이클에서 다룰 케이스가 있는가?

승인하시면 다음 사이클 RED로 진입하겠습니다.
```

사람이 승인하기 전까지 다음 사이클 RED를 시작하지 않는다.

---

## 좋은 테스트

| 품질 | 좋음 | 나쁨 |
|------|------|------|
| **최소** | 한 가지만. 이름에 "and"가 있나? 분리하라. | `validatesEmailAndDomainAndWhitespace` |
| **명확** | 이름이 동작을 설명한다 | `test1`, `testHelper` |
| **의도 표현** | 원하는 API를 보여준다 | 코드가 어떻게 동작해야 하는지 흐려놓는다 |

JUnit 5에서는 `@DisplayName`을 활용해 한글로 의도를 명확하게 표현할 수 있다.

## 순서가 중요한 이유

**"코드 작성 후 테스트로 검증하면 되지 않나"**

코드 작성 후 작성한 테스트는 즉시 통과한다. 즉시 통과하는 것은 아무것도 증명하지 않는다:
- 잘못된 것을 테스트했을 수 있다
- 동작이 아닌 구현을 테스트했을 수 있다
- 잊어버린 엣지 케이스를 놓쳤을 수 있다
- 그 테스트가 실제로 버그를 잡는 것을 본 적이 없다

테스트 우선은 테스트가 실패하는 것을 직접 보게 만들어, 실제로 무언가를 검증한다는 사실을 증명한다.

**"엣지 케이스는 이미 수동으로 다 테스트했다"**

수동 테스트는 즉흥적이다. 모두 테스트했다고 생각하지만:
- 무엇을 테스트했는지 기록이 없다
- 코드가 변경되면 다시 실행할 수 없다
- 압박 상황에서는 케이스를 잊기 쉽다
- "내가 시도했을 때는 됐다" ≠ 포괄적

자동 테스트는 체계적이다. 매번 동일하게 실행된다.

**"X시간의 작업을 지우는 것은 낭비다"**

매몰비용 오류다. 그 시간은 이미 지나갔다. 지금의 선택지:
- 삭제하고 TDD로 재작성 (X시간 추가, 높은 신뢰도)
- 그대로 두고 사후 테스트 추가 (30분, 낮은 신뢰도, 버그 가능성 높음)

"낭비"는 신뢰할 수 없는 코드를 그대로 두는 것이다. 진짜 테스트가 없는 동작 코드는 기술 부채다.

**"TDD는 교조적이다, 실용주의는 적응하는 것이다"**

TDD가 곧 실용적이다:
- 커밋 전에 버그를 찾는다 (사후 디버깅보다 빠르다)
- 회귀를 방지한다 (테스트가 즉시 깨짐을 잡아낸다)
- 동작을 문서화한다 (테스트가 사용법을 보여준다)
- 리팩터링을 가능하게 한다 (자유롭게 변경, 테스트가 깨짐을 잡는다)

"실용적인" 지름길 = 운영 환경 디버깅 = 더 느려진다.

**"사후 테스트도 같은 목표를 달성한다 — 형식이 아닌 정신이다"**

아니다. 사후 테스트는 "이 코드가 무엇을 하는가?"에 답한다. 우선 테스트는 "이 코드가 무엇을 해야 하는가?"에 답한다.

사후 테스트는 당신의 구현에 편향된다. 요구사항이 아닌 만든 것을 테스트한다. 발견한 엣지 케이스가 아닌 기억나는 엣지 케이스를 검증한다.

우선 테스트는 구현 전에 엣지 케이스 발견을 강제한다. 사후 테스트는 모든 것을 기억했는지 검증할 뿐이다 (기억하지 못한다).

사후 30분의 테스트 ≠ TDD. 커버리지는 얻지만 테스트가 작동한다는 증명은 잃는다.

## 흔한 합리화

| 변명 | 현실 |
|------|------|
| "테스트하기엔 너무 단순하다" | 단순한 코드도 깨진다. 테스트는 30초면 된다. |
| "나중에 테스트하겠다" | 즉시 통과하는 테스트는 아무것도 증명하지 않는다. |
| "사후 테스트도 같은 목표를 달성한다" | 사후 = "이 코드가 무엇을 하는가?", 우선 = "무엇을 해야 하는가?" |
| "이미 수동으로 테스트했다" | 즉흥적 ≠ 체계적. 기록이 없고, 다시 실행할 수 없다. |
| "X시간을 지우는 것은 낭비" | 매몰비용 오류. 검증되지 않은 코드를 두는 것이 기술 부채다. |
| "참고용으로 두고 테스트 먼저 작성한다" | 그것을 각색하게 된다. 그건 사후 테스트다. 삭제는 삭제다. |
| "먼저 탐색해야 한다" | 좋다. 탐색 코드는 버리고, TDD로 시작하라. |
| "테스트하기 어렵다 = 설계가 불명확하다" | 테스트의 말을 들어라. 테스트하기 어려우면 사용하기도 어렵다. |
| "TDD는 나를 느리게 한다" | TDD는 디버깅보다 빠르다. 실용적 = 테스트 우선. |
| "수동 테스트가 더 빠르다" | 수동은 엣지 케이스를 증명하지 않는다. 변경할 때마다 재테스트해야 한다. |
| "기존 코드에 테스트가 없다" | 당신이 그것을 개선하는 중이다. 기존 코드에도 테스트를 추가하라. |
| "검토 없이 넘어가도 된다" | RED와 REVIEW는 사람의 승인 없이 절대 넘어가지 않는다. |

## 위험 신호 — 멈추고 처음부터 다시 시작

- 테스트보다 먼저 작성된 코드
- 구현 후 작성된 테스트
- 테스트가 즉시 통과
- 테스트가 왜 실패했는지 설명할 수 없음
- 테스트를 "나중에" 추가
- "이번 한 번만"이라는 합리화
- "이미 수동으로 테스트했다"
- "사후 테스트도 같은 목적을 달성한다"
- "형식이 아니라 정신이다"
- "참고용으로 두자" 또는 "기존 코드를 각색하자"
- "이미 X시간 썼는데 지우는 건 낭비다"
- "TDD는 교조적이다, 나는 실용적이다"
- "이건 다르다, 왜냐하면…"
- **Plan.md 검토 없이 테스트를 작성했다**
- **REVIEW 검토 없이 다음 사이클을 시작했다**

**이 모든 것의 의미: 코드를 삭제하라. TDD로 처음부터 다시 시작하라.**

## 예시: 버그 수정

**버그:** 빈 이메일이 허용됨

---

**🔴 RED — Plan.md 작성 후 검토 요청**

```markdown
## Cycle 1 — 빈 이메일 거부

### 검증할 동작
빈 문자열 이메일로 폼 제출 시 오류 메시지를 반환한다.

### 테스트 시나리오
- Given: 이메일이 빈 문자열인 FormData
- When: FormService.submitForm() 호출
- Then: "Email required" 오류 반환

### 예상 실패 이유
현재 빈 이메일 검증 로직 없음

### 구현 범위
submitForm()에 빈 이메일 검증 추가
```

```
🔴 RED 계획 검토 요청 — Cycle 1: 빈 이메일 거부

Plan.md를 작성했습니다. 검토해주세요.
승인하시면 테스트 코드를 작성하겠습니다.
```

*(사람 승인 후 테스트 작성)*

```java
@Test
@DisplayName("빈 이메일을 거부한다")
void rejectsEmptyEmail() {
    FormData data = new FormData("");
    Result result = FormService.submitForm(data);
    assertEquals("Email required", result.getError());
}
```

**RED 검증**
```bash
$ ./gradlew test --tests "com.example.FormServiceTest.rejectsEmptyEmail"
FAILED — expected: "Email required" but was: null
```

---

**🟢 GREEN — 최소 구현**

```java
public final class FormService {
    public static Result submitForm(FormData data) {
        if (data.getEmail() == null || data.getEmail().trim().isEmpty()) {
            return Result.error("Email required");
        }
        return Result.ok();
    }
}
```

```bash
$ ./gradlew test
BUILD SUCCESSFUL
```

---

**✅ REVIEW — 코드 검토 후 사람 검토 요청**

자가 검토: 중복 없음, 이름 명확, 추출할 헬퍼 없음.

```
✅ REVIEW 검토 요청 — Cycle 1: 빈 이메일 거부

GREEN 완료. 검토해주세요:
- FormService.java: 빈 이메일 검증 추가 (submitForm 메서드)
- 테스트: rejectsEmptyEmail — 통과

다음 사이클 후보: null 이메일 거부, 형식 검증
승인하시면 다음 사이클로 진입하겠습니다.
```

---

## Gradle / JUnit 5 실용 명령어

```bash
# 단일 테스트 메서드 실행
./gradlew test --tests "com.example.RetryHelperTest.retriesFailedOperationsThreeTimes"

# 단일 클래스 실행
./gradlew test --tests "com.example.RetryHelperTest"

# 패키지 단위 실행
./gradlew test --tests "com.example.*"

# 테스트만 다시 실행 (캐시 무시)
./gradlew test --rerun-tasks

# 실패한 테스트만 자세히 보기
./gradlew test --info

# 테스트 리포트 위치
# build/reports/tests/test/index.html
```

`build.gradle` 최소 설정 예시:

```groovy
plugins {
    id 'java'
}

dependencies {
    testImplementation platform('org.junit:junit-bom:6.0.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
}
```

## 검증 체크리스트

사이클을 완료로 표시하기 전에:

- [ ] Plan.md를 작성하고 사람의 검토를 받았다 (RED 진입 전)
- [ ] 테스트가 구현 전에 실패하는 것을 직접 보았다
- [ ] 테스트가 예상한 이유로 실패했다 (오타가 아닌 기능 부재)
- [ ] 테스트를 통과시킬 최소 코드를 작성했다
- [ ] 모든 테스트가 통과한다 (`./gradlew test`)
- [ ] 출력이 깨끗하다 (오류, 경고 없음)
- [ ] 테스트가 실제 코드를 사용한다 (mock은 불가피한 경우만)
- [ ] REVIEW 단계에서 사람의 검토를 받았다 (다음 사이클 진입 전)

모두 체크할 수 없다면? TDD를 건너뛴 것이다. 처음부터 다시 시작하라.

## 막힐 때

| 문제 | 해결 |
|------|------|
| 어떻게 테스트할지 모르겠다 | Plan.md에 시나리오를 먼저 적어보라. assertion부터 작성하라. 사용자에게 물어보라. |
| 테스트가 너무 복잡하다 | 설계가 너무 복잡하다. 인터페이스를 단순화하라. |
| 모든 것을 mock해야 한다 | 코드가 너무 결합되어 있다. 의존성 주입을 사용하라. |
| 테스트 셋업이 너무 크다 | 헬퍼를 추출하라. 그래도 복잡하면 설계를 단순화하라. |
| Plan.md 범위가 너무 크다 | 사이클을 더 작게 쪼개라. 한 사이클 = 한 동작. |

## 디버깅과의 통합

버그를 발견했나? Plan.md를 작성하고 사람의 검토를 받아라. 이를 재현하는 실패 테스트를 작성하라. TDD 사이클을 따른다. 테스트가 수정을 증명하고 회귀를 방지한다.

테스트 없이 버그를 고치지 마라.

## 테스트 안티패턴

mock이나 테스트 유틸리티를 추가할 때, 흔한 함정을 피하기 위해 점검하라:
- 실제 동작이 아닌 mock의 동작을 테스트하기
- 프로덕션 클래스에 테스트 전용 메서드 추가하기
- 의존성을 이해하지 않고 mock하기

JUnit 5에서 추가로 유용한 것들:
- `@ParameterizedTest` — 같은 동작을 여러 입력으로 검증할 때 사용
- `@Nested` — 관련 테스트를 그룹화하여 의도를 명확히
- `assertAll(...)` — 한 번에 여러 assertion을 실행해 모든 실패를 보고
- `assertThrows(...)` — 예외 동작을 명시적으로 검증

## 최종 규칙

```
프로덕션 코드 → 먼저 실패한 테스트가 존재한다
RED 종료 → 사람의 검토와 승인이 존재한다
REVIEW 종료 → 사람의 검토와 승인이 존재한다
그렇지 않으면 → TDD가 아니다
```

사용자의 명시적인 허락 없이는 예외 없음.
