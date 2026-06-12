# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

**S-Semi 반도체 시료 생산주문관리 시스템** — 콘솔(CLI) 기반 Java 애플리케이션.  
시료(Sample) 등록·주문 접수·승인/거절·생산라인 관리·출고 처리를 하나의 인터랙티브 메뉴로 제공한다.

## 빌드 및 실행 명령

```bash
# 빌드
./gradlew build

# 전체 테스트
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "org.example.ClassName"

# 특정 테스트 메서드 실행
./gradlew test --tests "org.example.ClassName.methodName"

# 커버리지 리포트 (build/reports/jacoco/test/html/index.html)
./gradlew test jacocoTestReport

# 커버리지 검증 (100% 강제)
./gradlew check

# 애플리케이션 실행
./gradlew run
```

## 기술 스택

- **Java 21**, **Gradle 9.x** (single-project)
- **JUnit 5** (`junit-bom:5.10.2`), **JaCoCo 0.8.11**
- **Jackson** (`jackson-databind:2.17.1`, `jackson-datatype-jsr310:2.17.1`) — JSON 영속성
- 베이스 패키지: `org.example.sampleordersystem`

## 아키텍처

MVC 레이어 분리 원칙을 따른다.

```
org.example.sampleordersystem
├── model/          # 도메인 객체 (Sample, Order, ProductionQueue 등)
├── repository/     # 인터페이스 + InMemory / JsonFile 구현체
├── service/        # 비즈니스 로직 (재고 판단, 생산라인 스케줄링 등)
├── controller/     # 입력 검증 → Service 호출 → View 통지
├── view/           # 인터페이스 + ConsoleView 구현체 (출력 전담)
├── util/           # TimeProvider(인터페이스), SystemTimeProvider, OrderIdGenerator
├── app/            # App.java — 메뉴 루프 진입점
└── Main.java       # main() — 의존성 조립 후 App.run() 호출
```

App 생성자:
```
App(SampleController, OrderController, ProductionController,
    MonitoringController, ProductionService, SampleService, OrderService, View)
```

`Main`은 JaCoCo 커버리지 측정에서 제외한다.

## 도메인 핵심 규칙

### 주문 상태 흐름
```
RESERVED → 승인 → 재고 충분 → CONFIRMED → RELEASE
                → 재고 부족 → PRODUCING → CONFIRMED → RELEASE
         → 거절 → REJECTED
```
- `REJECTED`는 모니터링에서 제외한다.

### 생산라인
- 단일 FIFO 라인. 재고 부족분만 생산.
- **실 생산량** = `ceil(부족분 / (수율 × 0.9))`
- **총 생산시간** = `평균 생산시간 × 실 생산량`
- 생산 완료 시 `PRODUCING → CONFIRMED` 자동 전환.

### 재고 상태 라벨
| 라벨 | 조건 |
|------|------|
| 여유 | 주문 대비 재고 충분 |
| 부족 | 주문 대비 재고 수량 부족 |
| 고갈 | 재고 수량 = 0 |

### 시료 등록 입력값
시료 ID, 이름, 평균 생산시간(분), 수율(`0 < yield ≤ 1`), **초기 재고 수량**(0 이상 정수)

### 주문번호 형식
`ORD-YYYYMMDD-NNNN` — 당일 순번 4자리, 재시작 후에도 순번 유지

### 생산 진행률
- 실시간 시각 기반: `진행률 = (현재시각 - 생산시작시각) / 총 생산시간`
- `TimeProvider` 인터페이스로 시각을 주입 → 테스트 시 실시간 의존성 제거
- CLI 인수 `--time-scale <배율>` (기본값 `1.0`)으로 시간 가속 가능 (예: `60` → 현실 1초 = 시스템 1분)

### 메인 화면 현황 요약 항목
등록 시료 수 / 총 재고 수량 / 전체 주문 수 / 생산라인 대기 수 / 현재 생산 중인 시료 / CONFIRMED 주문 수

## 문서

- `docs/project-summary.md` — 시스템 배경·기능 개요
- `docs/PRD.md` — 기능·비기능 요구사항 상세
- `docs/PLAN.md` — 개발 페이즈, 패키지 구조, 테스트 전략

## 코드 규칙

- `View`는 출력만, `Controller`는 입력 검증 후 Service/Repository 위임.
- `Service` 레이어가 재고 판단·생산라인 등록 등 핵심 비즈니스 로직을 담당.
- JaCoCo 커버리지: `Main.class` 제외, 나머지 **100% instruction coverage** 강제.
- 테스트에서 `Scanner` 의존은 `ByteArrayInputStream`으로 주입, 파일 의존은 `@TempDir`로 격리.
