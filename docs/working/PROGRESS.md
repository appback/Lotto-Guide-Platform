# Lotto Guide Platform - 진행 상태

> **최종 업데이트**: 2026-01-11 (별자리 처리 로직 추가, Lazy Refresh 전략 구현, 프론트엔드 구현)

## 전체 진행 상황

### ✅ 완료된 작업

#### 1. 프로젝트 초기 설정 (01-project-setup.md)
- [x] `pom.xml` 생성 완료
  - Java 17
  - Spring Boot 3.3.5
  - Spring Data JPA
  - PostgreSQL Driver
  - Jackson
  - Spring Security
- [x] 프로젝트 디렉토리 구조 생성
  - `src/main/java/io/appback/lottoguide/`
  - `src/main/resources/`
- [x] `application.yml` 기본 설정 완료
  - 개발/운영 프로파일 설정
  - Hibernate `ddl-auto: update` 설정 (개발 환경)
  - PostgreSQL 연결 설정
- [x] 메인 애플리케이션 클래스 생성 (`LottoGuideApplication.java`)

#### 2. 인프라 및 배포 설정
- [x] Docker 설정 완료
  - `Dockerfile` 생성
  - `docker-compose.local.yml` 생성 (로컬 개발용)
  - `docker-compose.aws.yml` 생성 (AWS 배포용)
  - 포트 충돌 방지 설정 (PostgreSQL: 5434, API: 8083)
- [x] 자동화 스크립트 완료
  - `scripts/run-automation.py` 생성
  - 로컬 배포: `python scripts/run-automation.py --service lotto-api --local`
  - AWS 배포: `python scripts/run-automation.py --service lotto-api --stage aws-deploy`
- [x] 문서화 완료
  - `.ai-config.json` 설정 완료
  - `docs/` 가이드 문서 생성
  - `docs/infrastructure/` 인프라 문서 생성

### 🚧 진행 중 / 대기 중인 작업

#### 2. 데이터베이스 모델 및 엔티티 (02-database-model.md) ✅
- [x] `DrawEntity` (lotto_draw 테이블)
- [x] `LottoNumberMetricsEntity` (lotto_number_metrics 테이블)
- [x] `GeneratedSetEntity` (generated_set 테이블)
- [x] `GeneratedNumbersEntity` (generated_numbers 테이블)
- [x] `MissionLogEntity` (mission_log 테이블)
- [x] `LottoDataStateEntity` (lotto_data_state 테이블)
  - 데이터 갱신 상태 관리 (1행 유지, id=1)
  - 갱신 진행 여부, 쿨다운, 타임아웃 관리
- [x] Spring Data JPA Repository 인터페이스 생성
  - `DrawRepository`
  - `LottoNumberMetricsRepository`
  - `GeneratedSetRepository`
  - `GeneratedNumbersRepository`
  - `MissionLogRepository`
  - `LottoDataStateRepository` (동시성 제어 지원)

#### 3. Domain 모델 생성 (03-domain-model.md) ✅
- [x] Generator Domain
  - `Strategy` enum (FREQUENT_TOP, OVERDUE_TOP, BALANCED)
  - `Constraints` 클래스 (includeNumbers, excludeNumbers, oddEvenRatioRange, sumRange, similarityThreshold)
  - `GeneratedSet` 클래스 (생성된 번호 세트 및 메타데이터)
  - `ExplainTag` enum (WINDOW_50, ODD_3_EVEN_3, SUM_126, FREQ_BIAS, OVERDUE_BIAS, NO_LONG_CONSEC)
- [x] Mission Domain
  - `Tone` enum (LIGHT)
  - `Mission` 클래스 (미션 텍스트 및 메타데이터, 별자리 정보 포함)
  - `ZodiacCalculator` (생년월일 → 별자리 계산)

#### 4. Number Generation 엔진 구현 (04-number-generation.md) ✅
- [x] Preset 구현
  - `Preset` 인터페이스
  - `FrequentTopPreset` (고빈도 번호 우선)
  - `OverdueTopPreset` (과거 데이터 번호 우선)
  - `BalancedPreset` (제약 조건 기반 균형 잡힌 랜덤)
- [x] Engine 구현
  - `GeneratorEngine` (메인 엔진, Preset 선택 및 실행)
  - `CandidateSelector` (후보 번호 선택 로직)
  - `DiversityFilter` (유사도 기반 필터링, 중복 제거)
- [x] Explain Tags
  - `ExplainTagBuilder` (세트 분석 및 태그 생성)

#### 5. Mission LLM 통합 (05-mission-llm.md) ✅ (기본 구조 완료, LLM 통합은 후반 작업)
- [x] Prompt Builder (기본 구조)
  - `PromptBuilder`: Explain Tags + Tone + 별자리 기반 프롬프트 생성 구조
- [x] Policy & Safety (기본 구조)
  - `MissionPolicy`: 정책 검증 로직 구조, Disclaimer 정의
  - `ForbiddenPhraseDetector`: 금지된 표현 목록 정의
- [x] LLM Client (임시 구현)
  - `LlmClient` 인터페이스
  - `SimpleLlmClient`: 현재 "LLM 서비스는 준비 중 입니다." 반환
  - `LlmResponseSanitizer`: 응답 정제 구조
- [x] 별자리 처리
  - `ZodiacCalculator`: 생년월일 → 별자리 계산 (12개 별자리 지원)
  - 생년월일은 계산에만 사용되며 저장되지 않음
  - 별자리 정보는 프롬프트에 포함되고 응답에 반환됨
- [x] Fallback (기본 구조)
  - `SimpleLlmClient`가 고정 텍스트 반환 (Fallback 역할)
- **후반 작업**: 실제 LLM Provider 구현 (OpenAI, Anthropic 등)

#### 6. API 레이어 구현 (06-api-layer.md) ✅
- [x] Controllers
  - `GenerateController`: POST /api/v1/generate, Guest/Member 구분, 기본값 처리
  - `MissionController`: POST /api/v1/mission, ExplainTags 변환, LLM 호출
  - `HistoryController`: GET /api/v1/history, Member 전용, 페이징 지원
  - `SpaRedirectController`: SPA 라우팅 Fallback (Context Path: /lotto)
- [x] DTOs
  - `GenerateRequest`, `GenerateResponse` (GeneratedSetDto 포함)
  - `MissionRequest`, `MissionResponse`
  - `HistoryResponse` (HistoryItemDto 포함)
  - `ErrorResponse`
- [x] Exception Handling
  - `ApiExceptionHandler`: 전역 예외 처리, 표준 에러 응답 형식

#### 7. Application 레이어 구현 (07-application-layer.md) ✅
- [x] UseCases
  - `GenerateUseCase`: 번호 생성 로직 조합, Guest/Member 구분, DB 저장, Lazy Refresh 통합
  - `MissionUseCase`: 프롬프트 생성 → LLM 호출 → 정책 검사 → Disclaimer 추가
  - `HistoryUseCase`: Member 히스토리 조회, 페이징 처리
- [x] Ports (Interfaces)
  - `DrawRepositoryPort`: 추첨 결과 조회
  - `MetricsRepositoryPort`: 번호 메트릭 조회
  - `GeneratedSetRepositoryPort`: 생성된 세트 저장/조회
  - `LlmClientPort`: LLM 미션 생성

#### 8. Infrastructure 레이어 구현 (08-infrastructure-layer.md) ✅
- [x] Persistence
  - `EntityMapper`: Entity ↔ Domain 모델 변환
  - Repository Adapter: DrawRepositoryAdapter, MetricsRepositoryAdapter, GeneratedSetRepositoryAdapter
  - LlmClientAdapter: LlmClientPort 구현
  - `LottoDataStateEntity`: 데이터 갱신 상태 관리
- [x] Security
  - `AuthConfig`: SPA 라우팅 경로 허용 (/lotto/**)
- [x] LLM
  - `SimpleLlmClient`: 프로토타입용 임시 구현
  - `LlmResponseSanitizer`: 기본 구조 완료
- [x] Scheduler & Data Refresh
  - `RefreshDrawsJob`: 매일 새벽 2시 실행 (보조 수단, 기본 구조)
  - `RecomputeMetricsJob`: 매일 새벽 3시 실행 (보조 수단, `MetricsRecomputeService` 사용)
  - `DrawRefreshService`: Lazy Refresh 전략 구현
    - 요청 기반 갱신 (`ensureRefreshStartedIfNeeded()`)
    - 동시성 제어 (`@Lock(PESSIMISTIC_WRITE)`)
    - Non-blocking 갱신 (`@Async`)
    - 갱신 판단 기준 (7일 경과, 쿨다운, 진행 중 체크)
    - 쿨다운 정책 (30분), 타임아웃 처리 (10분)
    - 외부 API 연동 (동행복권 API)
    - 데이터 갱신 성공 시 메트릭 재계산 트리거
  - `MetricsRecomputeService`: 메트릭 재계산
    - windowSize별 메트릭 재계산 (20, 50, 100)
    - 빈도, 과거 데이터, 마지막 출현 회차 계산
    - 캐시 테이블 저장
- [x] External API
  - `DonghaengLottoApiClient`: 동행복권 API 클라이언트
    - 특정 회차 조회, 여러 회차 조회, 최신 회차 탐색
    - 재시도 로직 (최대 3회)
    - API 부하 방지 (200ms 대기)
  - `DrawApiResponse`: API 응답 DTO
- [x] Security
  - `AuthConfig`: 프로토타입용 간단한 보안 설정
  - `RateLimitFilter`: 기본 구조 완료
- [x] Config
  - `AppConfig`: ObjectMapper, LlmClient Bean 등록

#### 9. Config 레이어 구현 (09-config-layer.md) ✅
- [x] Configuration Classes
  - `WebConfig`: CORS 설정, 정적 리소스 핸들러 설정 (SPA 라우팅 지원)
  - `JacksonConfig`: JSON 직렬화/역직렬화 설정
  - `AppConfig`: LlmClient Bean 등록 (llm.provider 기반)
- [x] Application Properties
  - `application.yml`: Context Path 설정 (/lotto)
- [x] Feature Flags
  - `FeatureFlags`: 프로토타입/MVP 전환 플래그
  - Rate limiting, LLM 캐싱, 광고, Budget Cap, Mission Cache, Monitoring
- [x] Application Properties
  - `application.yml`: 프로파일별 설정 (dev/prod)
  - Feature Flags 설정 (dev: 모두 false, prod: 모두 true)
  - LLM Provider 설정

#### 10. 테스트 (10-testing.md)
- [ ] Unit Tests
- [ ] Integration Tests
- [ ] Test Data

#### 11. 프론트엔드 구현 ✅
- [x] 프로젝트 초기 설정
  - Vite + React + TypeScript 프로젝트 생성
  - 필수 패키지 설치 (Ant Design, Zustand, Axios, React Router 등)
- [x] 기본 구조 생성
  - 디렉토리 구조 생성
  - API 클라이언트 설정 (`services/api.ts`)
  - 라우터 설정 (`router.tsx`)
  - 타입 정의 (`types/api.ts`)
  - 상태 관리 (Zustand `stores/authStore.ts`)
- [x] 핵심 페이지 구현
  - `GeneratePage`: 기본 번호 생성 페이지
  - `DeepGeneratePage`: Deep 생성 페이지 (생년월일 → 별자리 → 번호 + 미션)
  - `HistoryPage`: 생성 히스토리 조회 페이지
- [x] Vite 설정
  - base path 설정 (`/lotto/`)
  - API 프록시 설정 (localhost:8083)
  - TypeScript path alias 설정 (`@/*`)

## 다음 단계

1. **테스트** (10-testing.md)
   - Unit Tests
   - Integration Tests
   - Test Data

3. **Infrastructure 레이어 구현** (08-infrastructure-layer.md)
   - Persistence (EntityMapper)
   - Scheduler (RefreshDrawsJob, RecomputeMetricsJob)
   - Security (AuthConfig, RateLimitFilter)

4. **Config 레이어 구현** (09-config-layer.md)
   - Configuration Classes
   - Feature Flags

5. **테스트** (10-testing.md)
   - Unit Tests
   - Integration Tests

## 후반 작업 (LLM 통합)
- 실제 LLM Provider 구현 (OpenAI, Anthropic 등)
- 실제 프롬프트 생성 로직
- 실제 정책 검증 로직
- LLM 실패 시 템플릿 미션 반환

## 현재 프로젝트 상태

### 구현된 파일
```
lotto-api/
├── pom.xml ✅
├── Dockerfile ✅
├── src/
│   ├── main/
│   │   ├── java/io/appback/lottoguide/
│   │   │   └── LottoGuideApplication.java ✅
│   │   └── resources/
│   │       └── application.yml ✅
│   └── test/ (비어있음)
└── README.md ✅
```

### 설정 완료 사항
- ✅ Maven 프로젝트 설정
- ✅ Spring Boot 3.3.5 설정
- ✅ PostgreSQL 연결 설정
- ✅ Hibernate 자동 스키마 업데이트 설정
- ✅ Docker 컨테이너화
- ✅ 자동화 배포 스크립트
- ✅ 로컬 개발 환경 구동 확인

### 아직 구현되지 않은 영역
- ❌ UseCase 및 Port 인터페이스
- ❌ API Controller 및 DTO
- ❌ Infrastructure 구현 (Persistence Mapper, Scheduler, Security)
- ❌ Config 레이어
- ❌ 테스트 코드
- ⏳ LLM 통합 (후반 작업, 현재는 SimpleLlmClient로 고정 텍스트 반환)

## 참고 사항

- Hibernate `ddl-auto: update` 설정으로 엔티티 생성 시 자동으로 스키마가 생성됩니다.
- 로컬 개발 환경은 Docker Compose로 구동 가능합니다.
- 배포는 자동화 스크립트를 통해 진행합니다.
