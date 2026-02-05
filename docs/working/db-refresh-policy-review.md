# DB Schema & Lazy Refresh Policy 검토 결과

> **검토 일자**: 2026-01-11  
> **참조 문서**: `db_schema_and_lazy_refresh_policy_appback_v1.0.md`

## 현재 구현 상태

### ✅ 이미 구현된 부분

1. **Hibernate 스키마 관리 정책**
   - ✅ 개발 환경: `ddl-auto: update`
   - ✅ 운영 환경: `ddl-auto: validate`
   - ✅ 엔티티 기반 스키마 관리

2. **기본 엔티티 구조**
   - ✅ `DrawEntity`: 추첨 결과 저장
   - ✅ `LottoNumberMetricsEntity`: 번호 메트릭 저장
   - ✅ Repository 인터페이스 및 Adapter 구현

3. **스케줄러 기본 구조**
   - ✅ `RefreshDrawsJob`: 매일 새벽 2시 실행 (기본 구조)
   - ✅ `RecomputeMetricsJob`: 매일 새벽 3시 실행 (기본 구조)

---

## 문서 제안 사항 vs 현재 구현

### 1. 데이터 갱신 전략

| 항목 | 문서 제안 | 현재 구현 | 적용 필요 |
|------|----------|----------|----------|
| 갱신 방식 | **Lazy Refresh** (요청 기반) | 스케줄러 기반 (매일 새벽 2시) | ✅ **적용 필요** |
| 갱신 트리거 | API 요청 시 갱신 필요 여부 판단 | 시간 기반 자동 실행 | ✅ **적용 필요** |
| 동시성 제어 | `@Lock(PESSIMISTIC_WRITE)` | 없음 | ✅ **적용 필요** |
| Non-blocking | `@Async` 백그라운드 갱신 | 동기 실행 | ✅ **적용 필요** |

**권장**: Lazy Refresh 전략으로 전환 (스케줄러는 보조 수단으로 유지)

---

### 2. 데이터 갱신 상태 관리

| 항목 | 문서 제안 | 현재 구현 | 적용 필요 |
|------|----------|----------|----------|
| 상태 엔티티 | `LottoDataStateEntity` (1행 유지) | 없음 | ✅ **필수 적용** |
| 상태 필드 | `asOfDrawNo`, `refreshedAt`, `refreshing`, `refreshLockUntil` 등 | 없음 | ✅ **필수 적용** |

**필수 구현 항목**:
- `LottoDataStateEntity` 생성
- 갱신 상태 추적 로직
- 쿨다운/타임아웃 처리

---

### 3. 외부 API 연동

| 항목 | 문서 제안 | 현재 구현 | 적용 필요 |
|------|----------|----------|----------|
| 데이터 소스 | 동행복권 JSON API | TODO 주석만 존재 | ✅ **적용 필요** |
| API 호출 | 요청 기반 (Lazy Refresh) | 없음 | ✅ **적용 필요** |
| 에러 처리 | 쿨다운 정책 (30분) | 없음 | ✅ **적용 필요** |
| 타임아웃 | 10분 초과 시 복구 | 없음 | ✅ **적용 필요** |

---

### 4. 갱신 판단 기준

**문서 제안 조건** (모두 만족 시 갱신):
- `now - refreshedAt >= 7일`
- `now >= refreshLockUntil`
- `refreshing == false`

**현재 구현**: 조건 없음 (시간 기반)

**적용 필요**: ✅ 갱신 판단 로직 구현

---

### 5. 통계 재계산 전략

| 항목 | 문서 제안 | 현재 구현 | 적용 필요 |
|------|----------|----------|----------|
| 재계산 시점 | 데이터 갱신 성공 시 즉시 | 스케줄러 기반 (매일 새벽 3시) | ✅ **적용 필요** |
| 캐시 저장 | 통계 결과를 캐시 테이블에 저장 | 없음 | ✅ **적용 필요** |
| 실시간 계산 | 금지 (캐시만 조회) | 없음 | ✅ **적용 필요** |

---

## 적용 우선순위

### 🔴 높은 우선순위 (필수)

1. **LottoDataStateEntity 생성**
   - 데이터 갱신 상태 관리
   - 동시성 제어 기반

2. **Lazy Refresh 전략 구현**
   - 요청 기반 갱신 로직
   - 동시성 제어 (`@Lock(PESSIMISTIC_WRITE)`)
   - Non-blocking 갱신 (`@Async`)

3. **갱신 판단 로직**
   - 7일 경과 체크
   - 쿨다운 체크
   - 진행 중 상태 체크

### 🟡 중간 우선순위 (권장)

4. **외부 API 연동**
   - 동행복권 JSON API 호출
   - 데이터 파싱 및 저장

5. **에러 처리 및 복구**
   - 쿨다운 정책 (30분)
   - 타임아웃 처리 (10분)

6. **통계 재계산 전략**
   - 데이터 갱신 성공 시 즉시 재계산
   - 캐시 테이블 저장

### 🟢 낮은 우선순위 (선택)

7. **스케줄러 보조 역할**
   - Lazy Refresh와 병행
   - 장기간 요청 없을 때 대비

---

## 구현 제안

### Phase 1: 상태 관리 엔티티 및 기본 구조 ✅

1. ✅ `LottoDataStateEntity` 생성
2. ✅ Repository 및 동시성 제어 구현 (`@Lock(PESSIMISTIC_WRITE)`)
3. ✅ `DrawRefreshService` 생성 (Lazy Refresh 전략)
4. ✅ `GenerateUseCase`에 Lazy Refresh 통합
5. ✅ `@EnableAsync` 추가 (Non-blocking 갱신 지원)

### Phase 2: Lazy Refresh 로직

1. `DrawRefreshService` 생성
2. `ensureRefreshStartedIfNeeded()` 메서드 구현
3. 동시성 제어 로직 (`@Lock(PESSIMISTIC_WRITE)`)
4. Non-blocking 갱신 (`@Async`)

### Phase 3: 외부 API 연동 ✅

1. ✅ 동행복권 API Client 구현 (`DonghaengLottoApiClient`)
2. ✅ 데이터 파싱 및 저장 로직 (`DrawApiResponse`, `DrawRefreshService`)
3. ✅ 에러 처리 및 쿨다운 정책 (재시도 로직, 쿨다운 30분)

### Phase 4: 통계 재계산 통합 ✅

1. ✅ `MetricsRecomputeService` 생성 (메트릭 재계산 로직)
2. ✅ 데이터 갱신 성공 시 메트릭 재계산 트리거 (`DrawRefreshService`)
3. ✅ 캐시 저장 로직 (`LottoNumberMetricsEntity` 업데이트)
4. ✅ `RecomputeMetricsJob` 업데이트 (보조 수단)

---

## 현재 스케줄러와의 관계

**제안**: 스케줄러는 보조 수단으로 유지
- Lazy Refresh가 주 전략
- 스케줄러는 장기간 요청이 없을 때를 대비한 백업
- 둘 다 동일한 `LottoDataStateEntity`를 사용하여 중복 갱신 방지

---

## 참고 사항

- 문서의 "최근 1주 누락 허용" 정책은 통계 품질에 큰 영향 없음
- Stale-While-Revalidate(SWR) 패턴 적용
- Single-flight 보장 (동시 요청 시 1회만 갱신)
