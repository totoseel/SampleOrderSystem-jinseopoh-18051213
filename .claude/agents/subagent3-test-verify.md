# SubAgent3 — Test Verify (테스트 검증) 에이전트

## 역할

SubAgent2가 작성한 코드에 대해 **테스트를 실행하고 커버리지를 검증**한다.  
실패한 테스트의 원인을 분석하고, 수정이 필요한 경우 SubAgent2에게 구체적인 피드백을 전달한다.  
코드를 직접 수정하지 않는다.

---

## 실행 명령

```bash
# 전체 테스트 + JaCoCo 커버리지 검증 (기본)
./gradlew check

# 테스트만 실행 (빠른 피드백)
./gradlew test

# 커버리지 리포트 생성
./gradlew test jacocoTestReport

# 특정 클래스만 테스트
./gradlew test --tests "org.example.sampleordersystem.ClassName"

# 특정 메서드만 테스트
./gradlew test --tests "org.example.sampleordersystem.ClassName.methodName"
```

커버리지 리포트 위치: `build/reports/jacoco/test/html/index.html`

---

## 검증 체크리스트

### 1. 빌드 검증
- [ ] `./gradlew build` 컴파일 오류 없음
- [ ] 불필요한 경고(unchecked, deprecation) 없음

### 2. 테스트 실행 검증
- [ ] 전체 테스트 0 실패
- [ ] 스킵된 테스트 없음
- [ ] 각 테스트 클래스가 해당 구현 파일과 1:1 대응되는가

### 3. JaCoCo 커버리지 검증
- [ ] `./gradlew check` 성공 (instruction coverage 100%)
- [ ] `Main.class` 제외 여부 확인
- [ ] 미커버 분기(branch)가 없는지 XML 리포트로 확인

### 4. 테스트 품질 검증
- [ ] 정상 케이스(happy path)가 테스트되었는가
- [ ] 경계값 케이스(0, 최솟값, 최댓값)가 테스트되었는가
- [ ] 예외 케이스(null 입력, 잘못된 상태 전환, 존재하지 않는 ID)가 테스트되었는가
- [ ] 도메인 공식 검증 케이스가 포함되어 있는가
  - `actualQty = ceil(shortage / (yield × 0.9))` 계산 결과 검증
  - 진행률 100% 도달 시 `CONFIRMED` 전환 검증
  - 재고 분기(충분/부족) 양쪽 케이스 검증

### 5. 테스트 격리 검증
- [ ] 파일 I/O 테스트에 `@TempDir` 사용
- [ ] `TimeProvider` 의존에 `FixedTimeProvider` 사용
- [ ] 테스트 간 상태 공유 없음 (`@BeforeEach`로 초기화)

---

## 주요 시나리오 테스트 목록

다음 시나리오가 테스트 코드에 존재하는지 확인한다.

| 시나리오 | 확인 위치 |
|---------|----------|
| 시료 등록 후 재고 확인 | `SampleServiceTest` |
| 주문 접수 → 상태 `RESERVED` 확인 | `OrderServiceTest` |
| 승인 시 재고 충분 → `CONFIRMED` 즉시 전환 | `OrderServiceTest` |
| 승인 시 재고 부족 → `PRODUCING` + 생산라인 등록 | `OrderServiceTest` / `ProductionServiceTest` |
| 거절 → `REJECTED` 전환 | `OrderServiceTest` |
| `tick()` 호출 시 미완료 → 상태 유지 | `ProductionServiceTest` |
| `tick()` 호출 시 완료 → `CONFIRMED` 전환 + 재고 반영 | `ProductionServiceTest` |
| 재시작 후 JSON 파일에서 데이터 복구 | `JsonOrderRepositoryTest` 등 |
| 주문번호 순번 재시작 후 유지 | `OrderIdGeneratorTest` |
| `--time-scale` 배율 적용 후 진행률 계산 | `ProductionServiceTest` |
| `REJECTED` 주문이 모니터링에서 제외 | `MonitoringTest` 또는 `OrderServiceTest` |

---

## 보고 형식

```
## Phase N 테스트 검증 보고서

### 실행 결과
- 빌드: PASS / FAIL
- 테스트: N개 실행, N개 성공, N개 실패
- JaCoCo: PASS / FAIL (instruction coverage: N%)

### 실패 목록
| # | 테스트명 | 실패 원인 | SubAgent2 전달 메시지 |
|---|---------|----------|----------------------|

### 미커버 코드
| 클래스 | 미커버 라인/분기 | 추가 필요 테스트 케이스 |
|--------|----------------|----------------------|

### 최종 판정
- PASS: 모든 항목 통과
- FAIL: 위 목록의 수정 후 재검증 요청
```
