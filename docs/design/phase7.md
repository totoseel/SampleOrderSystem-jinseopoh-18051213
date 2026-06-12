# Phase 7 — 마무리

## 목표

전체 코드베이스의 품질을 최종 점검하고 배포 가능한 상태로 마무리한다.  
SubAgent1(문서 정합성), SubAgent3(커버리지), SubAgent4(규칙 준수)가 모두 PASS여야 완료된다.

---

## 체크리스트

### 1. JaCoCo 커버리지 최종 확인

```bash
./gradlew clean check
```

- [ ] `Main.class` 제외 적용 확인
- [ ] instruction coverage 100% 달성
- [ ] 리포트 위치: `build/reports/jacoco/test/html/index.html`

**미커버 코드 발견 시 처리 원칙**
- 미커버 라인이 있으면 TDD 사이클로 테스트를 추가한다
- 커버가 불가능한 방어 코드(예: `throw new IllegalStateException("unreachable")`)는 설계를 재검토한다
- 커버리지 임계값을 낮추는 것은 금지한다

---

### 2. .gitignore 정비

```gitignore
# 빌드 산출물
build/
.gradle/

# IntelliJ IDEA
.idea/
*.iml

# 애플리케이션 데이터 파일 (프로덕션 데이터는 버전 관리 제외)
data/*.json

# OS 파생 파일
.DS_Store
Thumbs.db
```

- [ ] `data/` 디렉토리 자체는 `.gitkeep`으로 유지 (빈 디렉토리 추적)
- [ ] `build/` 디렉토리 미추적 확인
- [ ] `.idea/` 미추적 확인

---

### 3. 문서 최종 동기화 (SubAgent1 담당)

#### CLAUDE.md 업데이트 확인 항목

- [ ] 패키지 구조가 실제 구현과 일치하는가
- [ ] 기술 스택 버전이 `build.gradle`과 일치하는가
- [ ] 도메인 핵심 규칙(공식, 상태 흐름)이 코드와 일치하는가
- [ ] 빌드 및 실행 명령이 현재 `build.gradle`과 일치하는가

#### PRD.md ↔ PLAN.md ↔ CLAUDE.md ↔ 코드 최종 정합성

| 검증 항목 | 위치 |
|----------|------|
| `ORD-YYYYMMDD-NNNN` 형식 | `OrderIdGenerator`, `PRD 4-3` |
| 실 생산량 공식 `ceil(부족분 / (수율 × 0.9))` | `OrderService.approve()`, `PRD 4-6` |
| 진행률 공식 `(현재-시작) / 총시간 × 100` | `ProductionService.getProgress()`, `PRD 7-1` |
| `REJECTED` 모니터링 제외 | `MonitoringController`, `PRD 4-5` |
| 재고 상태 라벨(여유/부족/고갈) | `ConsoleView`, `PRD 4-5` |
| `TimeProvider` 인터페이스 | `util/TimeProvider.java`, `CLAUDE.md` |

---

### 4. 코드 품질 최종 점검 (SubAgent4 담당)

#### 레이어 방향성

```
허용:  Controller → Service → Repository → Model
금지:  View → Service 직접 참조
금지:  Controller → Repository 직접 참조
금지:  Model → 다른 레이어 참조
```

#### 금지 패턴 grep 확인

```bash
# View에서 Service 참조 여부
grep -r "Service" src/main/java/org/example/sampleordersystem/view/

# Controller에서 Repository 직접 참조 여부
grep -r "Repository" src/main/java/org/example/sampleordersystem/controller/

# null 직접 반환 여부
grep -rn "return null" src/main/java/org/example/sampleordersystem/

# System.exit() 사용 여부
grep -rn "System.exit" src/main/java/org/example/sampleordersystem/
```

#### CleanCode 최종 점검

- [ ] 모든 public 메서드: 인수 3개 이하
- [ ] `null` 반환 없음 (`Optional` 사용)
- [ ] 불필요한 주석 없음
- [ ] 테스트 메서드명이 동작을 명확히 설명

---

### 5. 커밋 이력 확인

```bash
git log --oneline
```

기대 커밋 패턴:
```
test(phase0): generatesCorrectFormat RED → GREEN
test(phase0): incrementsSequencePerCall RED → GREEN
...
feat(phase0): Phase 0 완료 — TimeProvider, OrderIdGenerator
test(phase1): sampleRejectsNonPositiveYield RED → GREEN
...
feat(phase7): 마무리 — .gitignore, 문서 동기화
```

- [ ] 각 페이즈 GREEN 커밋이 존재하는가
- [ ] 의미 없는 커밋("fix", "wip", "asdf") 없음
- [ ] 커밋 메시지 규칙 준수 (`feat/test/fix/docs/chore`)

---

### 6. 최종 실행 확인

```bash
# 전체 빌드 및 테스트
./gradlew clean check

# 기본 실행
./gradlew run

# 시간 배율 실행
./gradlew run --args="--time-scale 60"

# 잘못된 시간 배율 (기본값 1.0 적용 확인)
./gradlew run --args="--time-scale abc"
```

---

## SubAgent 역할 분담

| SubAgent | 담당 작업 |
|---------|----------|
| SubAgent1 | CLAUDE.md·PRD.md·PLAN.md·코드 정합성 검증, 보고서 작성 |
| SubAgent3 | `./gradlew clean check` 실행, 미커버 코드 보고 |
| SubAgent4 | 레이어 방향성, CleanCode, 도메인 규칙 위반 검증, 보고서 작성 |
| SubAgent2 | SubAgent3/4 FAIL 시 수정 후 재검증 요청 |

---

## 완료 기준 (최종)

모든 항목이 체크되어야 프로젝트 완료로 간주한다.

- [ ] `./gradlew clean check` 성공 (instruction coverage 100%)
- [ ] `.gitignore` 정비 완료
- [ ] `CLAUDE.md` 최종 동기화 완료
- [ ] SubAgent1 정합성 보고: PASS
- [ ] SubAgent4 규칙 준수 보고: PASS (BLOCKER·MAJOR 0건)
- [ ] 최종 실행 확인 4개 항목 통과
- [ ] 커밋 이력 규칙 준수 확인
