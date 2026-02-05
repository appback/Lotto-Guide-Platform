# 09. Config 레이어 구현

## 목표
- Spring Boot 설정 클래스 구현
- Feature Flags 설정

## 작업 항목

### 9.1 Configuration Classes
- [x] `WebConfig`
  - CORS 설정 (프로토타입에서는 모든 Origin 허용)
  - 인터셉터 설정 (필요 시 추가 가능)
- [x] `JacksonConfig`
  - JSON 직렬화/역직렬화 설정
  - JavaTimeModule 등록
  - 날짜 형식 설정 (WRITE_DATES_AS_TIMESTAMPS 비활성화)
- [x] `AppConfig` (업데이트)
  - LlmClient Bean 등록
  - llm.provider 설정에 따른 구현체 선택 (프로토타입: simple)

### 9.2 Feature Flags
- [x] `FeatureFlags`
  - `@ConfigurationProperties`로 설정 관리
  - 프로토타입/MVP 전환 플래그
  - Rate limiting 활성화 여부
  - LLM 캐싱 활성화 여부
  - 광고 플래그
  - LLM Budget Cap 활성화 여부
  - Mission Cache 활성화 여부
  - Monitoring 활성화 여부

### 9.3 Application Properties
- [x] `application.yml` 설정
  - 데이터베이스 연결 설정 (dev/prod 프로파일)
  - LLM Provider 설정 (dev: simple, prod: openai)
  - Feature Flags 설정 (dev: 모두 false, prod: 모두 true)
  - 프로파일별 설정 (dev, prod)

## 참고
- 패키지: `io.appback.lottoguide.config`
- 프로토타입 → MVP 전환은 설정만으로 가능하도록 설계
