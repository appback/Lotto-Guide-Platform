# 03. Domain 모델 생성

## 목표
- 핵심 비즈니스 도메인 모델 정의
- Generator와 Mission 도메인 모델 구현

## 작업 항목

### 3.1 Generator Domain
- [x] `Strategy` enum
  - `FREQUENT_TOP`, `OVERDUE_TOP`, `BALANCED`
- [x] `Constraints` 클래스
  - `includeNumbers`, `excludeNumbers`
  - `oddEvenRatioRange` (내부 클래스: OddEvenRatioRange)
  - `sumRange` (내부 클래스: SumRange)
  - `similarityThreshold`
- [x] `GeneratedSet` 클래스
  - 생성된 번호 세트
  - 메타데이터 포함 (strategy, constraints, tags, createdAt)
  - 유틸리티 메서드: getSum(), getOddCount(), getEvenCount()
- [x] `ExplainTag` enum
  - `WINDOW_50`, `ODD_3_EVEN_3`, `SUM_126`
  - `FREQ_BIAS`, `OVERDUE_BIAS`, `NO_LONG_CONSEC`

### 3.2 Mission Domain
- [x] `Tone` enum
  - `LIGHT` (프로토타입에서는 1개만)
- [x] `Mission` 클래스
  - 미션 텍스트
  - 메타데이터 (tone, inputTagsJson, createdAt, tokenUsage, costEstimate)
  - `zodiacSign` (별자리 정보, 생년월일로부터 계산되지만 저장되지 않음)
- [x] `ZodiacCalculator` (별자리 계산기)
  - 생년월일(`LocalDate`)로부터 별자리 계산
  - 12개 별자리 지원 (염소자리, 물병자리, 물고기자리, 양자리, 황소자리, 쌍둥이자리, 게자리, 사자자리, 처녀자리, 천칭자리, 전갈자리, 사수자리)
  - **원칙**: 생년월일 정보는 계산에만 사용되며 저장되지 않음

## 참고
- 패키지: `io.appback.lottoguide.domain.generator.model`
- 패키지: `io.appback.lottoguide.domain.mission.model`
- 패키지: `io.appback.lottoguide.domain.mission.zodiac`
