# 07. Application 레이어 구현

## 목표
- UseCase 구현
- Port 인터페이스 정의
- 비즈니스 로직 조합

## 작업 항목

### 7.1 UseCases
- [x] `GenerateUseCase`
  - 번호 생성 로직 조합
  - Guest/Member 구분 처리 (userId가 null이면 Guest)
  - DB 저장 (Member만)
  - Explain Tags 생성
- [x] `MissionUseCase`
  - 프롬프트 생성 → LLM 호출 → 정책 검사 → Disclaimer 추가
  - Fallback 처리 (정책 위반 시)
- [x] `HistoryUseCase`
  - Member 히스토리 조회
  - 페이징 처리 지원

### 7.2 Ports (Interfaces)
- [x] `out` 포트 (Infrastructure가 구현)
  - `DrawRepositoryPort`: 추첨 결과 조회
  - `MetricsRepositoryPort`: 번호 메트릭 조회
  - `GeneratedSetRepositoryPort`: 생성된 세트 저장/조회
  - `LlmClientPort`: LLM 미션 생성

## 참고
- 패키지: `io.appback.lottoguide.application.usecase`
- 패키지: `io.appback.lottoguide.application.port`
- Hexagonal Architecture 패턴 적용
