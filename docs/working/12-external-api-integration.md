# 12. 외부 API 연동 구현

## 목표
- 동행복권 API를 통한 추첨 결과 자동 수집
- 안정적인 데이터 갱신 및 에러 처리

## 작업 항목

### 12.1 동행복권 API Client 구현 ✅
- [x] `DonghaengLottoApiClient` 생성
  - 위치: `infra/external/DonghaengLottoApiClient.java
  - API 엔드포인트: `https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={회차번호}`
  - 메서드:
    - `fetchDraw(int drawNo)`: 특정 회차 조회
    - `fetchDraws(int startDrawNo, int count)`: 여러 회차 조회
    - `findLatestDrawNo(int estimatedLatestDrawNo)`: 최신 회차 번호 탐색
  - 재시도 로직: 최대 3회, 지수 백오프 (1초, 2초, 3초)
  - API 부하 방지: 회차 간 200ms 대기

### 12.2 API 응답 DTO 생성 ✅
- [x] `DrawApiResponse` 생성
  - 위치: `infra/external/dto/DrawApiResponse.java`
  - 필드:
    - `drwNo`: 회차 번호
    - `drwNoDate`: 추첨일 (yyyy-MM-dd)
    - `drwtNo1~6`: 당첨 번호
    - `bnusNo`: 보너스 번호
    - `returnValue`: 성공 여부
  - 유틸리티 메서드:
    - `isSuccess()`: 성공 여부 확인
    - `getDrawDate()`: LocalDate 변환
    - `getNumbers()`: 정렬된 번호 배열 반환

### 12.3 DrawRefreshService 통합 ✅
- [x] 실제 API 호출 로직 구현
  - 최신 회차 번호 탐색 (`findLatestDrawNo`)
  - 새로 추가된 회차만 조회 (현재 최신 회차 이후)
  - DB 저장 (중복 방지)
  - 상태 업데이트 (`asOfDrawNo`, `refreshedAt`)
  - 상세 로깅

### 12.4 에러 처리 및 재시도 ✅
- [x] 재시도 로직
  - 최대 3회 재시도
  - 지수 백오프 (1초, 2초, 3초)
  - 네트워크 오류 및 파싱 오류 처리
- [x] 쿨다운 정책
  - 실패 시 30분 쿨다운
  - `refreshLockUntil` 필드에 저장
- [x] 타임아웃 처리
  - 10분 초과 시 자동 복구
  - `refreshing` 플래그 해제

### 12.5 RestTemplate Bean 등록 ✅
- [x] `AppConfig`에 `RestTemplate` Bean 추가
  - 외부 API 호출용

## 구현 세부사항

### API 엔드포인트
```
GET https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={회차번호}
```

### 응답 형식 (JSON)
```json
{
  "returnValue": "success",
  "drwNo": 1234,
  "drwNoDate": "2024-01-06",
  "drwtNo1": 1,
  "drwtNo2": 2,
  "drwtNo3": 3,
  "drwtNo4": 4,
  "drwtNo5": 5,
  "drwtNo6": 6,
  "bnusNo": 45
}
```

### 데이터 저장 흐름
1. 현재 DB의 최신 회차 확인
2. 예상 최신 회차 계산 (현재 + 7일치)
3. 실제 최신 회차 번호 탐색
4. 새로 추가된 회차만 조회
5. DB에 저장 (중복 방지)
6. 상태 업데이트

## 주의사항
- 이 API는 비공식 엔드포인트일 수 있으며, 동행복권의 정책 변경 시 동작하지 않을 수 있습니다.
- API 부하를 방지하기 위해 회차 간 200ms 대기 시간을 두었습니다.
- 재시도 로직으로 일시적인 네트워크 오류를 처리합니다.

## 다음 단계
- Phase 3: 통계 재계산 통합
  - 데이터 갱신 성공 시 메트릭 즉시 재계산
  - 캐시 테이블 저장
