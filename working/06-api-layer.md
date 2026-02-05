# 06. API 레이어 구현

## 목표
- REST API 엔드포인트 구현
- DTO 정의
- 예외 처리

## 작업 항목

### 6.1 Controllers
- [x] `GenerateController`
  - `POST /api/v1/generate`
  - Guest/Member 구분 (X-User-Id 헤더로 구분)
  - Member인 경우 DB 저장 (setId 반환)
  - 기본값 처리 (strategy: BALANCED, count: 1, windowSize: 50)
- [x] `MissionController`
  - `POST /api/v1/mission`
  - ExplainTags 문자열을 enum으로 변환
  - 생년월일 → 별자리 계산 (저장하지 않음)
  - 프롬프트 생성 (별자리 정보 포함) → LLM 호출 → 정책 검사 → Disclaimer 추가
- [x] `HistoryController`
  - `GET /api/v1/history` (Member only)
  - X-User-Id 헤더 필수
  - 페이징 지원 (page, size 파라미터)

### 6.2 DTOs
- [x] `GenerateRequest`
  - strategy, constraints, count, windowSize
- [x] `GenerateResponse`
  - generatedSets (GeneratedSetDto 리스트), setId (nullable)
  - GeneratedSetDto: index, numbers, tags
- [x] `MissionRequest`
  - explainTags (String 리스트), tone
  - birthDate (선택적, LocalDate) - 별자리 계산에만 사용, 저장되지 않음
- [x] `MissionResponse`
  - missionText, tokenUsage, costEstimate
  - zodiacSign (별자리 정보, nullable) - 응답에만 포함, DB 저장 안 함
- [x] `HistoryResponse`
  - content (HistoryItemDto 리스트), page, size, totalElements, totalPages
  - HistoryItemDto: setId, strategyCode, generatedSets, createdAt
- [x] `ErrorResponse`
  - message, errorCode, timestamp, details

### 6.3 Exception Handling
- [x] `ApiExceptionHandler`
  - 전역 예외 처리 (@RestControllerAdvice)
  - MethodArgumentNotValidException 처리 (400)
  - IllegalArgumentException 처리 (400)
  - 일반 Exception 처리 (500)
  - 표준 에러 응답 형식 (ErrorResponse)

## 참고
- 패키지: `io.appback.lottoguide.api.controller`
- 패키지: `io.appback.lottoguide.api.dto`
- 패키지: `io.appback.lottoguide.api.advice`
- API 버전: `/api/v1`
