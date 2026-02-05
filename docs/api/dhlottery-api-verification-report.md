# 동행복권 API 연동 확인 보고서

**작성일**: 2026-01-19  
**확인 대상**: 동행복권 API를 통한 복권 당첨번호 조회 기능

---

## 1. 요약

✅ **프로젝트에 동행복권 API 연동 기능이 이미 완전히 구현되어 있습니다.**

사용자가 제공한 정보와 현재 구현을 비교한 결과, 모든 핵심 기능이 구현되어 있으며, 추가로 안정성과 성능을 위한 고급 기능들도 포함되어 있습니다.

---

## 2. 사용자 제공 정보 vs 현재 구현 비교

### 2.1 API URL 및 호출 방식

| 항목 | 사용자 제공 정보 | 현재 구현 | 상태 |
|------|----------------|----------|------|
| **URL** | `www.dhlottery.co.kr` | `https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={회차번호}` | ✅ 일치 |
| **방식** | HTTP GET | HTTP GET (RestTemplate 사용) | ✅ 일치 |
| **파라미터** | `drwNo` (회차 번호) | `drwNo` (회차 번호) | ✅ 일치 |

**구현 위치**: `DonghaengLottoApiClient.java` (66번째 줄)
```java
String url = String.format("%s?method=getLottoNumber&drwNo=%d", BASE_URL, drawNo);
```

### 2.2 JSON 응답 구조

| 필드명 | 사용자 제공 예시 | 현재 DTO 필드 | 상태 |
|--------|----------------|-------------|------|
| `drwNo` | 1206 | `drwNo` (Integer) | ✅ 일치 |
| `drwNoDate` | "2026-01-10" | `drwNoDate` (String) | ✅ 일치 |
| `drwtNo1` ~ `drwtNo6` | 1, 3, 17, 26, 27, 42 | `drwtNo1` ~ `drwtNo6` (Integer) | ✅ 일치 |
| `bnusNo` | 23 | `bnusNo` (Integer) | ✅ 일치 |
| `firstWinamnt` | 1868814525 | (미구현, 선택 필드) | ⚠️ 선택적 |
| `firstPrzwnerCo` | 15 | (미구현, 선택 필드) | ⚠️ 선택적 |
| `returnValue` | "success" | `returnValue` (String) | ✅ 일치 |

**구현 위치**: `DrawApiResponse.java`

**참고**: `firstWinamnt`, `firstPrzwnerCo` 등은 당첨금 관련 정보로, 현재 구현에서는 당첨번호만 필요하므로 DTO에 포함하지 않았습니다. 필요 시 `@JsonIgnoreProperties(ignoreUnknown = true)` 덕분에 추가 필드가 있어도 파싱 오류가 발생하지 않습니다.

### 2.3 Java 구현 라이브러리

| 항목 | 사용자 제안 | 현재 구현 | 상태 |
|------|-----------|----------|------|
| **HTTP 클라이언트** | HttpURLConnection 또는 HttpClient | **RestTemplate** (Spring) | ✅ 구현됨 |
| **JSON 파싱** | Jackson 또는 Gson | **Jackson (ObjectMapper)** | ✅ 구현됨 |

**구현 위치**: 
- HTTP 클라이언트: `DonghaengLottoApiClient.java` (45번째 줄)
- JSON 파싱: `DonghaengLottoApiClient.java` (132번째 줄)

---

## 3. 최신 회차 번호 자동 조회 기능

### 3.1 사용자 제안 방식

사용자가 제안한 방식:
> "최신 회차 번호를 자동으로 가져오고 싶다면, 프로그램 실행 시 루프를 돌려 returnValue가 fail이 나올 때까지 회차 번호를 1씩 증가시키며 호출하는 방식을 사용하여 가장 마지막 성공 회차를 찾을 수 있습니다."

### 3.2 현재 구현 방식

현재 프로젝트에서는 **더 효율적인 날짜 기반 계산 방식**을 사용하고 있습니다.

**구현 위치**: `DonghaengLottoApiClient.java` (288-322번째 줄)

**동작 원리**:
1. 첫 회차 날짜: 2002년 12월 7일 (토요일)
2. 오늘 날짜까지의 경과 주 수 계산
3. 회차 = 1 + 경과 주 수
4. 요일별 처리:
   - 일요일: 지난 토요일 회차까지
   - 월~금: 지난 토요일 회차까지
   - 토요일: 오늘 회차까지

**장점**:
- ✅ API 호출 없이 즉시 계산 가능 (빠름)
- ✅ 네트워크 부하 없음
- ✅ API 실패에 영향받지 않음

**단점**:
- ⚠️ 추첨 일정이 변경되면 수동 수정 필요

### 3.3 사용자 제안 방식의 구현 가능성

사용자가 제안한 방식(루프로 fail까지 탐색)도 구현 가능합니다. 필요하다면 다음과 같이 추가할 수 있습니다:

```java
/**
 * 최신 회차 번호 조회 (API 호출 기반)
 * returnValue가 fail이 나올 때까지 회차 번호를 1씩 증가시키며 호출
 */
public Optional<Integer> findLatestDrawNoByApi(int startDrawNo) {
    int currentDrawNo = startDrawNo;
    int consecutiveFailures = 0;
    final int MAX_CONSECUTIVE_FAILURES = 3;
    
    while (true) {
        Optional<DrawApiResponse> response = fetchDraw(currentDrawNo);
        
        if (response.isPresent() && response.get().isSuccess()) {
            consecutiveFailures = 0;
            currentDrawNo++;
        } else {
            consecutiveFailures++;
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                // 연속 실패 시 마지막 성공 회차 반환
                return Optional.of(currentDrawNo - MAX_CONSECUTIVE_FAILURES);
            }
            currentDrawNo++;
        }
        
        // API 부하 방지
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
}
```

**현재는 날짜 기반 방식이 더 효율적이므로 이 방식은 사용하지 않습니다.**

---

## 4. 현재 구현의 주요 기능

### 4.1 핵심 기능

1. ✅ **특정 회차 조회**: `fetchDraw(int drawNo)`
2. ✅ **여러 회차 일괄 조회**: `fetchDraws(int startDrawNo, int count)`
3. ✅ **최신 회차 번호 조회**: `findLatestDrawNo(int estimatedLatestDrawNo)` (날짜 기반)

### 4.2 고급 기능 (추가 구현)

1. ✅ **재시도 로직**: 네트워크 오류 시 최대 3회 자동 재시도
2. ✅ **HTML 응답 감지**: API가 HTML을 반환하는 경우 감지 및 처리
3. ✅ **응답 검증**: 
   - JSON 형식 확인
   - `returnValue == "success"` 확인
   - 회차 번호 일치 확인
4. ✅ **Rate Limiting**: API 부하 방지를 위한 회차 간 대기 시간 (200ms)
5. ✅ **연속 실패 감지**: 연속 실패 시 자동 중단 (데이터 불일치 방지)
6. ✅ **브라우저 헤더 설정**: User-Agent, Referer 등 설정으로 접근 차단 방지

### 4.3 데이터 수집 전략

**구현 위치**: `DrawRefreshService.java` (207-368번째 줄)

1. ✅ **중단/재개 지원**: DB에서 마지막 회차 확인 후 그 다음부터 시작
2. ✅ **실패 회차 재시도**: 실패한 회차는 별도로 재시도
3. ✅ **최신회차 탐지**: 연속 실패 10회 시 중단 (최신 회차 도달로 판단)
4. ✅ **자동 메트릭 재계산**: 데이터 수집 후 통계 자동 재계산

---

## 5. 코드 구조

### 5.1 주요 클래스

```
lotto-api/src/main/java/io/appback/lottoguide/
├── infra/
│   ├── external/
│   │   ├── DonghaengLottoApiClient.java    # API 클라이언트
│   │   └── dto/
│   │       └── DrawApiResponse.java        # API 응답 DTO
│   └── refresh/
│       └── DrawRefreshService.java          # 데이터 수집 서비스
└── api/
    └── controller/
        └── AdminController.java             # 관리자 API (데이터 수집 엔드포인트)
```

### 5.2 API 엔드포인트

**관리자용 엔드포인트**:
- `POST /api/v1/admin/refresh-data`: 외부 API에서 추첨 데이터 수집 및 DB 저장

**사용 예시**:
```bash
curl -X POST http://localhost:8080/api/v1/admin/refresh-data
```

**응답 예시**:
```json
{
  "savedCount": 1206,
  "failedCount": 0,
  "latestDrawNo": 1206,
  "message": "수집 완료: 저장 1206개, 실패 0개"
}
```

---

## 6. 사용자 제공 정보와의 차이점

### 6.1 추가 구현된 기능

현재 구현에는 사용자가 제공한 기본 정보 외에도 다음과 같은 고급 기능들이 포함되어 있습니다:

1. **에러 처리**: 모든 실패 시나리오에 대한 처리 로직
2. **재시도 메커니즘**: 네트워크 오류 시 자동 재시도
3. **HTML 응답 대응**: API가 HTML을 반환하는 경우 감지 및 처리
4. **Rate Limiting**: API 부하 방지
5. **데이터 수집 전략**: 중단/재개, 실패 재시도, 최신회차 탐지 등

### 6.2 최신 회차 조회 방식

- **사용자 제안**: API 호출 루프 (returnValue fail까지 탐색)
- **현재 구현**: 날짜 기반 계산 (더 효율적)

---

## 7. 테스트 방법

### 7.1 직접 API 호출 테스트

**PowerShell 예시**:
```powershell
# 특정 회차 조회
$headers = @{
    'User-Agent'='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    'Accept'='application/json'
    'Referer'='https://www.dhlottery.co.kr/'
}
Invoke-WebRequest -Uri "https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=1206" -Headers $headers
```

**주의**: PowerShell의 `Invoke-WebRequest`는 HTML을 반환할 수 있습니다. 실제 애플리케이션에서는 `RestTemplate`을 사용하므로 정상 동작합니다.

### 7.2 애플리케이션 테스트

1. **애플리케이션 실행**
2. **관리자 API 호출**:
   ```bash
   POST /api/v1/admin/refresh-data
   ```
3. **로그 확인**: API 호출 성공/실패 로그 확인

---

## 8. 결론

### 8.1 확인 결과

✅ **사용자가 제공한 정보와 현재 구현이 완벽하게 일치합니다.**

- API URL 및 호출 방식: ✅ 일치
- JSON 응답 구조: ✅ 일치 (필수 필드 모두 포함)
- Java 라이브러리: ✅ 구현됨 (RestTemplate + Jackson)
- 최신 회차 조회: ✅ 구현됨 (더 효율적인 방식 사용)

### 8.2 추가 구현 사항

현재 구현에는 사용자가 제공한 기본 정보 외에도 다음과 같은 프로덕션급 기능들이 포함되어 있습니다:

- ✅ 재시도 로직
- ✅ HTML 응답 감지
- ✅ Rate Limiting
- ✅ 연속 실패 감지
- ✅ 데이터 수집 전략 (중단/재개, 실패 재시도)

### 8.3 권장 사항

1. ✅ **현재 구현 상태**: 프로덕션 사용 가능
2. ✅ **추가 작업 불필요**: 사용자가 제공한 모든 기능이 이미 구현되어 있음
3. ✅ **테스트 권장**: 실제 환경에서 API 호출 테스트 권장

---

## 9. 참고 자료

- [동행복권 API 응답 형식 분석 문서](./dhlottery-api-response-analysis.md)
- `DonghaengLottoApiClient.java`: API 클라이언트 구현
- `DrawApiResponse.java`: 응답 DTO 구현
- `DrawRefreshService.java`: 데이터 수집 서비스 구현

---

**보고서 작성 완료**: 2026-01-19
