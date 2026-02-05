# 05. Mission LLM 통합

## 목표
- LLM Provider 연동
- Mission 생성 프롬프트 빌더
- 정책 및 안전 필터 구현
- Fallback 처리

## 작업 항목

### 5.1 Prompt Builder
- [x] `PromptBuilder` (기본 구조)
  - Explain Tags + Tone 기반 프롬프트 생성 구조
  - 별자리 정보 포함 지원 (선택적)
  - Prompt version 1 정의
  - **참고**: 실제 프롬프트 생성 로직은 LLM 통합 시 구현 예정

### 5.2 Policy & Safety
- [x] `MissionPolicy` (기본 구조)
  - 정책 검증 로직 구조
  - 고정 Disclaimer 텍스트 정의
  - **참고**: 실제 정책 검증 로직은 LLM 통합 시 구현 예정
- [x] `ForbiddenPhraseDetector` (기본 구조)
  - 확률/보장 관련 표현 목록 정의
  - 금지된 표현 감지 구조
  - **참고**: 실제 검사 로직은 LLM 통합 시 구현 예정

### 5.3 LLM Client
- [x] `LlmClient` 인터페이스
  - LLM 응답 구조 정의 (text, tokenUsage, costEstimate)
- [x] `SimpleLlmClient` (임시 구현)
  - **현재**: "LLM 서비스는 준비 중 입니다." 텍스트 반환
  - **후반 작업**: 실제 LLM Provider 구현체 (OpenAI, Anthropic 등)로 교체 예정
- [x] `LlmResponseSanitizer` (기본 구조)
  - 응답 정제 및 검증 구조
  - **참고**: 실제 정제 로직은 LLM 통합 시 구현 예정

### 5.4 Fallback
- [x] 기본 구조 완료
  - `SimpleLlmClient`가 항상 고정 텍스트 반환 (Fallback 역할)
  - **후반 작업**: LLM 실패 시 템플릿 미션 반환 로직 구현 예정
  - **후반 작업**: 정책 위반 시 처리 로직 구현 예정

### 5.5 별자리 처리
- [x] `ZodiacCalculator` 구현
  - 생년월일로부터 별자리 계산
  - 12개 별자리 지원
  - **보안 원칙**: 생년월일은 계산에만 사용되며 저장되지 않음
- [x] `MissionUseCase`에 별자리 계산 로직 통합
  - 생년월일 → 별자리 변환 후 프롬프트에 포함
  - 별자리 정보는 응답에만 포함 (DB 저장 안 함)

## 참고 사항
- **LLM 통합은 후반 작업으로 예정**
- 현재는 `SimpleLlmClient`가 "LLM 서비스는 준비 중 입니다." 텍스트를 반환
- 기본 구조는 완료되어 있어 나중에 실제 LLM Provider로 교체 가능
- **별자리 처리**: 생년월일은 API 요청에 포함되지만 DB에 저장되지 않음

## 참고
- 패키지: `io.appback.lottoguide.domain.mission`
- 패키지: `io.appback.lottoguide.domain.mission.zodiac`
- 패키지: `io.appback.lottoguide.infra.llm`
- 프로토타입에서는 Tone 1개만 (`LIGHT`)
