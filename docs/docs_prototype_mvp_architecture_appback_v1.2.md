# Prototype & MVP Architecture / Design Document

**Project**: Lotto Guide Platform  
**Document Version**: 1.3  
**Date**: 2026-01-11 (Asia/Seoul)  
**Last Updated**: 2026-01-11 (별자리 처리 로직 추가)  
**Scope**: Deployable Prototype → (config-only) MVP, Spring Boot backend + Web frontend, LLM-assisted “mission” content.  
**Company/Brand**: appback

---

## 1. Purpose

This document defines:

- A **deployable prototype** architecture that is intentionally **production-shaped** (no throwaway code paths).
- A **config-only promotion path** from Prototype → MVP (no structural refactor required).
- A Spring Boot-centric package/module structure suitable for long-term maintenance under the **appback** namespace.

---

## 2. Terminology

### 2.1 Deployable Prototype
A prototype that:
- Implements **real** functionality (no mock-only flows).
- Uses the **same architecture, package structure, and DB schema direction** as MVP.
- Minimizes scope (fewer presets, fewer constraints, limited UX polish).
- Can be promoted to MVP by **turning on production settings** (profiles/flags), not by refactoring.

### 2.2 MVP
Deployable Prototype +:
- Production profile enabled
- Rate limiting / abuse controls hardened
- Advertising enabled (if applicable)
- Guardrails/disclaimers locked
- Observability baseline (logs/metrics/cost tracking) active

---

## 3. Design Principles (Non-negotiables)

1. **No throwaway code**: prototype code is the first production commit.
2. **Server owns the logic**: strategy, constraints, guardrails, and storage decisions reside on the backend.
3. **Risky language is blocked server-side**: any “probability increase/guarantee” semantics are prevented at the API layer.
4. **Minimal but complete vertical slice**: generate numbers → explain → generate mission → store history per user type.
5. **Naming is production-grade from day one**: avoid `com.example` to eliminate later mass refactors.

---

## 4. System Overview

### 4.1 Components (MVP-shaped)
```
[Browser/Web]
     |
     v
[Web Frontend]  --HTTPS-->  [Spring Boot API]  --JDBC-->  [PostgreSQL/MySQL]
                                   |
                                   +--HTTPS--> [LLM Provider]
```

### 4.2 Responsibilities
- **Web Frontend**
  - Form inputs (strategy/constraints)
  - Rendering results and “explain tags”
  - Local history storage (guest and member cache)
- **Spring Boot API**
  - Number generation engine (strategy + constraints + diversity)
  - Mission generation proxy (prompt + guardrails + caching + rate limit)
  - Persistence (members only)
- **DB**
  - Draw data + computed metrics
  - Member history (generated sets, saved missions if needed)
- **LLM Provider**
  - Produces mission text; backend enforces policy and safety filters

---

## 5. User Types & History Storage Policy (Final)

### 5.1 User Types
- **Guest**
  - No account
  - Identified locally by `anonId` stored in browser LocalStorage
- **Member**
  - Authenticated
  - Identified by `userId` (DB primary key)

### 5.2 Storage Rules
| User Type | Server DB | Local Storage |
|---|---:|---:|
| Guest | No | Yes |
| Member | Yes | Yes |

Notes:
- Server does **not** persist guest history.
- Local storage exists for responsiveness/offline and as a cache for members.

---

## 6. Prototype Scope (Must-have)

### 6.1 Number Generation
**Presets (3)**
1. `FREQUENT_TOP`: favor high-frequency numbers in recent N draws
2. `OVERDUE_TOP`: favor numbers not seen recently (overdue)
3. `BALANCED`: constraint-driven balanced random

**Constraints (core)**
- includeNumbers
- excludeNumbers
- odd/even ratio range
- sum range
- simple diversity filter (similarity threshold)

**Explain Tags**
- Each generated set returns tags like:
  - `WINDOW_50`, `ODD_3_EVEN_3`, `SUM_126`, `FREQ_BIAS`, `OVERDUE_BIAS`, `NO_LONG_CONSEC`

### 6.2 Mission (LLM + Zodiac)
- Tone: 1 (`LIGHT`)
- Prompt version: 1
- Zodiac support:
  - Birth date (optional) → zodiac sign calculation
  - **Privacy principle**: Birth date is never stored, only used for zodiac calculation
  - Zodiac sign is included in prompt and response, but not stored
- Guardrails:
  - Block probability/guarantee claims
  - Add fixed disclaimer appended by server
- Fallback: if LLM fails or violates policy, return a template mission

### 6.3 History
- Guest: LocalStorage only
- Member: DB + LocalStorage cache
- API:
  - `GET /api/v1/history` (member only)

---

## 7. Production-Grade Naming (Using appback namespace)

### 7.1 Recommended Root Package
Because the brand/company name is **appback**, we should standardize the Java root package accordingly.

**Recommended:**
- Root package: `io.appback.lottoguide`

This keeps the namespace stable without requiring a domain name.

### 7.2 Maven Coordinates (recommended)
- `groupId`: `io.appback`
- `artifactId`:
  - backend: `lotto-api`
  - web: `lotto-web` (if separate)
- `name`: `appback Lotto Guide API`

---

## 8. Spring Boot Package Structure (Final)

> **Root Package**: `io.appback.lottoguide`

```
io.appback.lottoguide
 ├─ api
 │   ├─ controller
 │   │   ├─ GenerateController
 │   │   ├─ MissionController
 │   │   └─ HistoryController
 │   ├─ dto
 │   │   ├─ GenerateRequest / GenerateResponse
 │   │   ├─ MissionRequest / MissionResponse
 │   │   └─ HistoryItemResponse
 │   └─ advice
 │       └─ ApiExceptionHandler
 ├─ application
 │   ├─ usecase
 │   │   ├─ GenerateUseCase
 │   │   ├─ MissionUseCase
 │   │   └─ HistoryUseCase
 │   └─ port
 │       ├─ out (repositories, clients)
 │       └─ in (interfaces)
 ├─ domain
 │   ├─ generator
 │   │   ├─ model (Strategy, Constraints, GeneratedSet, ExplainTags)
 │   │   ├─ preset (FrequentTopPreset, OverdueTopPreset, BalancedPreset)
 │   │   ├─ engine (GeneratorEngine, CandidateSelector, DiversityFilter)
 │   │   └─ explain (ExplainTagBuilder)
 │   └─ mission
 │       ├─ model (Tone, Mission)
 │       ├─ policy (MissionPolicy, ForbiddenPhraseDetector)
 │       ├─ zodiac (ZodiacCalculator)
 │       └─ prompt (PromptBuilder)
 ├─ infra
 │   ├─ persistence
 │   │   ├─ entity (GeneratedSetEntity, GeneratedNumbersEntity, DrawEntity, MetricsEntity, LottoDataStateEntity)
 │   │   ├─ repository (SpringData repos)
 │   │   └─ mapper (EntityMapper)
 │   ├─ refresh
 │   │   └─ DrawRefreshService (Lazy Refresh 전략)
 │   ├─ external
 │   │   ├─ DonghaengLottoApiClient (동행복권 API 클라이언트)
 │   │   └─ dto (DrawApiResponse)
 │   ├─ security
 │   │   ├─ AuthConfig (Spring Security 설정, SPA 라우팅 지원)
 │   │   └─ RateLimitFilter (기본 구조)
 │   ├─ llm
 │   │   ├─ LlmClient
 │   │   └─ LlmResponseSanitizer
 │   ├─ scheduler
 │   │   ├─ RefreshDrawsJob
 │   │   └─ RecomputeMetricsJob
 │   └─ security
 │       ├─ AuthConfig (prototype simple)
 │       └─ RateLimitFilter
 └─ config
     ├─ WebConfig
     ├─ JacksonConfig
     └─ FeatureFlags
```

---

## 9. Data Model (Prototype/MVP-Shaped)

### 9.1 Core Tables

**lotto_draw**
- `draw_no` (PK)
- `draw_date`
- `n1..n6`, `bonus`
- `created_at`

**lotto_number_metrics**
- `id` (PK)
- `window_size` (20/50/100)
- `number` (1..45)
- `freq`
- `overdue`
- `last_seen_draw_no`
- `updated_at`
- UNIQUE(`window_size`, `number`)

### 9.2 Member History Tables (Member-only persistence)

**generated_set**
- `id` (PK)
- `user_id` (NOT NULL, FK)
- `strategy_code`
- `strategy_params_json`
- `constraints_json`
- `generated_count`
- `created_at`

**generated_numbers**
- `id` (PK)
- `generated_set_id` (FK)
- `idx`
- `n1..n6`
- `tags_json`

### 9.3 Observability / Cost (optional but recommended)
**mission_log**
- `id` (PK)
- `user_id` (nullable)
- `anon_id` (nullable)
- `tone`
- `input_tags_json`
- `mission_text`
- `token_usage` (nullable)
- `cost_estimate` (nullable)
- `created_at`

### 9.4 Data Refresh State
**lotto_data_state** (1행만 유지, id=1)
- `id` (PK, 항상 1)
- `as_of_draw_no` (마지막 반영 회차)
- `refreshed_at` (마지막 갱신 완료 시각)
- `refreshing` (갱신 진행 여부)
- `refresh_started_at` (갱신 시작 시각)
- `refresh_lock_until` (쿨다운 종료 시각)
- `last_error` (마지막 에러 메시지)
- `created_at`, `updated_at`

---

## 10. API Summary

### 10.1 Generate
`POST /api/v1/generate`
- Always returns generated sets
- If member authenticated:
  - persists `generated_set` + `generated_numbers`
  - returns `setId`
- If guest:
  - does not persist
  - `setId = null`

### 10.2 Mission
`POST /api/v1/mission`
- Server builds prompt from explain tags + tone + zodiac sign (optional)
- Birth date (if provided) is converted to zodiac sign but **never stored**
- Server blocks forbidden semantics
- Server appends disclaimer
- Response includes zodiac sign (calculated from birth date, not stored)

### 10.3 History (Member only)
`GET /api/v1/history`
- Returns member history (paged)

---

## 11. Prototype → MVP Promotion Checklist (Config-only)

- [ ] `spring.profiles.active=prod`
- [ ] rate limit enabled and tuned
- [ ] LLM budget cap enabled (daily/monthly)
- [ ] mission cache enabled (if Redis introduced) or DB-backed cache
- [ ] fixed disclaimer enforced in API responses
- [ ] ad flags enabled (frontend placement ready)
- [ ] basic monitoring dashboards or log queries prepared

---

## 12. Open Decisions (ADRs to write)

- ADR-001: Auth method for prototype (simple email OTP vs social login)  
- ADR-002: Cache introduction timing (Redis now vs later)  
- ADR-003: Mission caching key (by date, by tags, by user)  
- ADR-004: DB choice final (PostgreSQL vs MySQL) and migration tool (Flyway recommended)

---

## 13. Conclusion

This document uses the **appback** namespace from day one so that the deployable prototype remains **MVP-shaped**, and promotion to MVP is a **deployment/config switch**, not a refactor project.
