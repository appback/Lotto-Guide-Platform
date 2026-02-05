# 08. Infrastructure 레이어 구현

## 목표
- 외부 시스템 연동 구현
- 스케줄러 구현
- 보안 설정

## 작업 항목

### 8.1 Persistence
- [x] `EntityMapper`
  - Entity ↔ Domain 모델 변환
  - DrawEntity → DrawInfo 변환
  - LottoNumberMetricsEntity → NumberMetrics 변환
  - GeneratedSetEntity/GeneratedNumbersEntity ↔ GeneratedSet 변환
  - ExplainTags JSON 파싱/생성
- [x] Repository Adapter 구현체
  - `DrawRepositoryAdapter`: DrawRepositoryPort 구현
  - `MetricsRepositoryAdapter`: MetricsRepositoryPort 구현
  - `GeneratedSetRepositoryAdapter`: GeneratedSetRepositoryPort 구현
  - `LlmClientAdapter`: LlmClientPort 구현
- [x] Data Refresh State
  - `LottoDataStateEntity`: 데이터 갱신 상태 관리 (1행 유지)
  - `LottoDataStateRepository`: 동시성 제어 지원

### 8.2 LLM
- [x] `LlmClient` 구현체
  - `SimpleLlmClient`: 프로토타입용 임시 구현 (고정 텍스트 반환)
  - `LlmClientAdapter`: Application Port 어댑터
- [x] `LlmResponseSanitizer`
  - 응답 정제 (기본 구조 완료)

### 8.3 Scheduler & Data Refresh
- [x] `RefreshDrawsJob`
  - 매일 새벽 2시 실행 (보조 수단)
  - 프로토타입에서는 기본 구조만 구현 (TODO: 외부 API 연동)
- [x] `RecomputeMetricsJob`
  - 매일 새벽 3시 실행 (보조 수단)
  - `MetricsRecomputeService` 사용
- [x] `MetricsRecomputeService` (메트릭 재계산)
  - windowSize별 메트릭 재계산 (20, 50, 100)
  - 빈도, 과거 데이터, 마지막 출현 회차 계산
  - 캐시 테이블 저장 (`LottoNumberMetricsEntity`)
  - 데이터 갱신 성공 시 즉시 재계산 트리거
- [x] `DrawRefreshService` (Lazy Refresh 전략)
  - 요청 기반 데이터 갱신
  - `ensureRefreshStartedIfNeeded()`: 갱신 필요 여부 판단 및 시작
  - 동시성 제어: `@Lock(PESSIMISTIC_WRITE)`
  - Non-blocking 갱신: `@Async`
  - 갱신 판단 기준: 7일 경과, 쿨다운 종료, 진행 중 아님
  - 외부 API 연동: 동행복권 API를 통한 실제 데이터 수집
- [x] External API
  - `DonghaengLottoApiClient`: 동행복권 API 클라이언트
    - 특정 회차 조회, 여러 회차 조회, 최신 회차 탐색
    - 재시도 로직 (최대 3회)
    - API 부하 방지 (200ms 대기)
  - `DrawApiResponse`: API 응답 DTO
  - 쿨다운 정책: 실패 시 30분
  - 타임아웃 처리: 10분 초과 시 복구

### 8.4 Security
- [x] `AuthConfig`
  - 프로토타입용 간단한 보안 설정
  - CSRF 비활성화
  - Stateless 세션
  - 모든 API 허용 (X-User-Id 헤더로 Guest/Member 구분)
  - SPA 라우팅 경로 허용 (/lotto/**)
- [x] `RateLimitFilter`
  - 기본 구조 완료 (프로토타입에서는 모든 요청 허용)
  - TODO: 실제 Rate Limiting 로직 구현

### 8.5 Config
- [x] `AppConfig`
  - ObjectMapper Bean 등록 (JavaTimeModule 포함)
  - LlmClient Bean 등록 (SimpleLlmClient)

## 참고
- 패키지: `io.appback.lottoguide.infra`
- 프로토타입에서는 최소한의 보안 설정
