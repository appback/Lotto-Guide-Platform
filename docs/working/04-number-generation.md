# 04. Number Generation 엔진 구현

## 목표
- 번호 생성 엔진 구현
- 3가지 Preset 구현
- Constraints 처리
- Diversity 필터링
- Explain Tags 생성

## 작업 항목

### 4.1 Preset 구현
- [x] `Preset` 인터페이스
  - 번호 생성 인터페이스 정의
- [x] `FrequentTopPreset`
  - 최근 N회 추첨에서 고빈도 번호 우선
- [x] `OverdueTopPreset`
  - 최근에 나오지 않은 번호 우선
- [x] `BalancedPreset`
  - 제약 조건 기반 균형 잡힌 랜덤
  - 홀수/짝수 비율, 합계 범위 제약 조건 검증

### 4.2 Engine 구현
- [x] `GeneratorEngine`
  - Preset 선택 및 실행
  - Constraints 적용
  - Diversity 필터링
  - GeneratedSet 리스트 생성
- [x] `CandidateSelector`
  - 후보 번호 선택 로직
  - 제약 조건 적용
  - 가중치 기반 선택
- [x] `DiversityFilter`
  - 유사도 임계값 기반 필터링 (Jaccard 유사도)
  - 중복 세트 제거

### 4.3 Explain Tags
- [x] `ExplainTagBuilder`
  - 생성된 세트 분석
  - 태그 생성 (`WINDOW_50`, `ODD_3_EVEN_3`, `SUM_126` 등)
  - 전략 기반 태그 (FREQ_BIAS, OVERDUE_BIAS)
  - 연속 번호 체크 (NO_LONG_CONSEC)

## 참고
- 패키지: `io.appback.lottoguide.domain.generator`
- Metrics 데이터를 활용하여 빈도/과거 데이터 계산
