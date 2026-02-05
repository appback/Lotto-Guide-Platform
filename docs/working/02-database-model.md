# 02. 데이터베이스 모델 및 엔티티

## 목표
- JPA Entity 클래스 생성
- 데이터베이스 스키마 정의
- Flyway 마이그레이션 스크립트 작성 (선택)

## 작업 항목

### 2.1 Core Tables
- [x] `DrawEntity` (lotto_draw 테이블)
  - `drawNo` (PK)
  - `drawDate`
  - `n1..n6`, `bonus`
  - `createdAt`
- [x] `LottoNumberMetricsEntity` (lotto_number_metrics 테이블)
  - `id` (PK)
  - `windowSize` (20/50/100)
  - `number` (1..45)
  - `freq`, `overdue`, `lastSeenDrawNo`
  - `updatedAt`
  - UNIQUE(`windowSize`, `number`)

### 2.2 Member History Tables
- [x] `GeneratedSetEntity` (generated_set 테이블)
  - `id` (PK)
  - `userId` (NOT NULL, FK)
  - `strategyCode`
  - `strategyParamsJson`
  - `constraintsJson`
  - `generatedCount`
  - `createdAt`
- [x] `GeneratedNumbersEntity` (generated_numbers 테이블)
  - `id` (PK)
  - `generatedSetId` (FK)
  - `idx`
  - `n1..n6`
  - `tagsJson`

### 2.3 Observability
- [x] `MissionLogEntity` (mission_log 테이블)
  - `id` (PK)
  - `userId` (nullable)
  - `anonId` (nullable)
  - `tone`
  - `inputTagsJson`
  - `missionText`
  - `tokenUsage` (nullable)
  - `costEstimate` (nullable)
  - `createdAt`

### 2.4 Data Refresh State
- [x] `LottoDataStateEntity` (lotto_data_state 테이블)
  - `id` (PK, 항상 1)
  - `asOfDrawNo` (마지막 반영 회차)
  - `refreshedAt` (마지막 갱신 완료 시각)
  - `refreshing` (갱신 진행 여부)
  - `refreshStartedAt` (갱신 시작 시각)
  - `refreshLockUntil` (쿨다운 종료 시각)
  - `lastError` (마지막 에러 메시지)
  - `createdAt`, `updatedAt`

### 2.5 Repository
- [x] Spring Data JPA Repository 인터페이스 생성
  - `DrawRepository`
  - `LottoNumberMetricsRepository`
  - `GeneratedSetRepository`
  - `GeneratedNumbersRepository`
  - `MissionLogRepository`
  - `LottoDataStateRepository` (동시성 제어 지원: `@Lock(PESSIMISTIC_WRITE)`)

## 참고
- 패키지: `io.appback.lottoguide.infra.persistence.entity`
- Repository 패키지: `io.appback.lottoguide.infra.persistence.repository`
