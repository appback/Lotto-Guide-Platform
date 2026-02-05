# 13. 통계 재계산 통합 구현

## 목표
- 데이터 갱신 성공 시 메트릭 즉시 재계산
- 캐시 테이블에 저장 (실시간 계산 금지)
- windowSize별 메트릭 계산 (20, 50, 100)

## 작업 항목

### 13.1 MetricsRecomputeService 생성 ✅
- [x] `MetricsRecomputeService` 생성
  - 위치: `infra/refresh/MetricsRecomputeService.java`
  - 메서드:
    - `recomputeAllMetrics()`: 모든 windowSize에 대해 재계산 (`@Async`)
    - `recomputeMetricsForWindowSize(Integer windowSize)`: 특정 windowSize 재계산
  - 계산 로직:
    - 최근 N회 추첨 데이터 조회
    - 각 번호(1-45)의 빈도 계산
    - 각 번호의 과거 데이터 계산 (마지막 출현 회차로부터 현재까지)
    - 마지막 출현 회차 추적
    - DB에 저장/업데이트

### 13.2 DrawRefreshService 통합 ✅
- [x] 데이터 갱신 성공 시 메트릭 재계산 트리거
  - `refreshDrawsAsync()` 메서드에서 저장된 데이터가 있을 때만 트리거
  - `@Async`로 Non-blocking 처리

### 13.3 LottoNumberMetricsEntity 업데이트 ✅
- [x] `@Setter` 추가
  - 메트릭 업데이트를 위해 필요

### 13.4 DrawRepository 확장 ✅
- [x] `findByDrawNoRange()` 메서드 추가
  - 특정 회차 범위의 추첨 결과 조회 (최신 순)
  - 메트릭 재계산 성능 향상

### 13.5 RecomputeMetricsJob 업데이트 ✅
- [x] `MetricsRecomputeService` 사용
  - 보조 수단: 장기간 요청이 없을 때를 대비
  - 매일 새벽 3시 실행

## 구현 세부사항

### 메트릭 계산 로직

#### 1. 빈도 (Frequency)
- 최근 N회 추첨에서 해당 번호가 나온 횟수
- 예: 최근 50회에서 번호 7이 5번 나왔다면 `freq = 5`

#### 2. 과거 데이터 (Overdue)
- 마지막으로 나온 추첨 번호로부터 현재 최신 회차까지의 회차 차이
- 예: 마지막 출현이 회차 1200이고 현재 최신이 회차 1250이면 `overdue = 50`
- 한 번도 나오지 않은 경우: `overdue = 최신 회차 번호`

#### 3. 마지막 출현 회차 (LastSeenDrawNo)
- 최근 N회 추첨에서 해당 번호가 마지막으로 나온 회차 번호
- 한 번도 나오지 않은 경우: `0`

### 데이터 저장 흐름
1. 데이터 갱신 성공 (`DrawRefreshService`)
2. 저장된 데이터가 있으면 메트릭 재계산 트리거
3. 각 windowSize(20, 50, 100)별로:
   - 최근 N회 추첨 데이터 조회
   - 각 번호(1-45)의 메트릭 계산
   - DB에 저장/업데이트 (`LottoNumberMetricsEntity`)

### 캐시 전략
- **실시간 계산 금지**: 메트릭은 항상 캐시 테이블에서만 조회
- **갱신 시점**: 데이터 갱신 성공 시 즉시 재계산
- **보조 수단**: 스케줄러를 통한 주기적 재계산 (매일 새벽 3시)

## 성능 최적화
- `findByDrawNoRange()` 사용으로 회차 범위 조회 최적화
- `@Async`로 Non-blocking 처리
- windowSize별 병렬 처리 가능 (향후 개선)

## 다음 단계
- 모든 Phase 완료 ✅
- Lazy Refresh 전략 전체 구현 완료
