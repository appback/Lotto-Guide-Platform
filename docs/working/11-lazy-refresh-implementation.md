# 11. Lazy Refresh 전략 구현

## 목표
- 요청 기반 데이터 갱신 (Lazy Refresh)
- 동시성 제어로 중복 갱신 방지
- Non-blocking 갱신으로 응답 속도 보장

## 작업 항목

### 11.1 상태 관리 엔티티 ✅
- [x] `LottoDataStateEntity` 생성
  - 항상 1행만 유지 (id=1)
  - `asOfDrawNo`: 마지막 반영 회차
  - `refreshedAt`: 마지막 갱신 완료 시각
  - `refreshing`: 갱신 진행 여부
  - `refreshStartedAt`: 갱신 시작 시각
  - `refreshLockUntil`: 쿨다운 종료 시각
  - `lastError`: 마지막 에러 메시지

### 11.2 Repository 및 동시성 제어 ✅
- [x] `LottoDataStateRepository` 생성
  - `findByIdWithLock()`: `@Lock(PESSIMISTIC_WRITE)` 사용
  - `findByIdWithoutLock()`: 일반 조회

### 11.3 Lazy Refresh 서비스 ✅
- [x] `DrawRefreshService` 생성
  - `ensureRefreshStartedIfNeeded()`: 갱신 필요 여부 판단 및 시작
  - `refreshDrawsAsync()`: 백그라운드 갱신 (`@Async`)
  - 갱신 판단 기준:
    - `now - refreshedAt >= 7일`
    - `now >= refreshLockUntil`
    - `refreshing == false`
  - 쿨다운 정책: 실패 시 30분
  - 타임아웃 처리: 10분 초과 시 복구

### 11.4 UseCase 통합 ✅
- [x] `GenerateUseCase`에 Lazy Refresh 통합
  - 번호 생성 요청 시 자동으로 갱신 필요 여부 확인
  - Non-blocking 처리로 응답 속도 보장

### 11.5 Async 지원 ✅
- [x] `@EnableAsync` 추가
  - `LottoGuideApplication`에 추가
  - Non-blocking 백그라운드 갱신 지원

## 참고
- 패키지: `io.appback.lottoguide.infra.persistence.entity`
- 패키지: `io.appback.lottoguide.infra.persistence.repository`
- 패키지: `io.appback.lottoguide.infra.refresh`
- 스케줄러는 보조 수단으로 유지 (장기간 요청 없을 때 대비)

## 다음 단계
- ✅ Phase 2: 외부 API 연동 (동행복권 JSON API) 완료
- ✅ Phase 3: 통계 재계산 통합 완료

## Phase 3 완료 사항

### 3.1 MetricsRecomputeService 생성 ✅
- [x] 메트릭 재계산 로직 구현
  - windowSize별 재계산 (20, 50, 100)
  - 빈도, 과거 데이터, 마지막 출현 회차 계산
  - 캐시 테이블 저장

### 3.2 DrawRefreshService 통합 ✅
- [x] 데이터 갱신 성공 시 메트릭 재계산 트리거
  - 저장된 데이터가 있을 때만 트리거
  - `@Async`로 Non-blocking 처리

### 3.3 엔티티 및 Repository 업데이트 ✅
- [x] `LottoNumberMetricsEntity`에 `@Setter` 추가
- [x] `DrawRepository`에 `findByDrawNoRange()` 추가

### 3.4 RecomputeMetricsJob 업데이트 ✅
- [x] `MetricsRecomputeService` 사용
  - 보조 수단으로 유지

## Phase 2 완료 사항

### 2.1 동행복권 API Client ✅
- [x] `DonghaengLottoApiClient` 생성
  - `fetchDraw(int drawNo)`: 특정 회차 조회
  - `fetchDraws(int startDrawNo, int count)`: 여러 회차 조회
  - `findLatestDrawNo(int estimatedLatestDrawNo)`: 최신 회차 번호 탐색
  - 재시도 로직 (최대 3회)
  - API 부하 방지 (200ms 대기)

### 2.2 API 응답 DTO ✅
- [x] `DrawApiResponse` 생성
  - JSON 필드 매핑
  - 성공 여부 확인 (`isSuccess()`)
  - 추첨일 변환 (`getDrawDate()`)
  - 번호 배열 반환 (`getNumbers()`)

### 2.3 DrawRefreshService 통합 ✅
- [x] 실제 API 호출 로직 구현
  - 최신 회차 번호 탐색
  - 새로 추가된 회차만 조회
  - DB 저장 (중복 방지)
  - 상태 업데이트 (`asOfDrawNo`)

### 2.4 에러 처리 ✅
- [x] 재시도 로직 (최대 3회, 지수 백오프)
- [x] 쿨다운 정책 (실패 시 30분)
- [x] 타임아웃 처리 (10분 초과 시 복구)
- [x] 상세 로깅

### 2.5 RestTemplate Bean 등록 ✅
- [x] `AppConfig`에 `RestTemplate` Bean 추가
