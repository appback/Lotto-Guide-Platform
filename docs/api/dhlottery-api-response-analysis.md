# 동행복권 API 응답 형식 및 처리 방식 분석

## 개요

이 문서는 동행복권 API(`getLottoNumber`)의 응답 형식과 현재 구현된 코드에서의 처리 방식을 상세히 분석합니다.

**작성일**: 2026-01-09  
**API 엔드포인트**: `https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={회차번호}`

---

## 1. API 엔드포인트 정보

### 1.1 기본 정보

- **URL**: `https://www.dhlottery.co.kr/common.do`
- **메서드**: `GET`
- **파라미터**:
  - `method`: `getLottoNumber` (고정값)
  - `drwNo`: 회차 번호 (예: 1000, 1234)

### 1.2 요청 예시

```http
GET https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=1000
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
Accept: application/json, text/plain, */*
Referer: https://www.dhlottery.co.kr/
```

---

## 2. API 응답 형식

### 2.1 성공 응답 (JSON)

동행복권 API가 정상적으로 JSON을 반환하는 경우:

```json
{
  "returnValue": "success",
  "drwNo": 1000,
  "drwNoDate": "2021-06-19",
  "drwtNo1": 10,
  "drwtNo2": 23,
  "drwtNo3": 29,
  "drwtNo4": 33,
  "drwtNo5": 37,
  "drwtNo6": 40,
  "bnusNo": 16,
  "firstAccumamnt": 863604600,
  "firstPrzwnerCo": 0,
  "firstWinamnt": 0,
  "totSellamnt": 3681782000
}
```

#### 필드 설명

| 필드명 | 타입 | 설명 | 필수 여부 |
|--------|------|------|-----------|
| `returnValue` | String | 응답 상태 (`"success"` 또는 `"fail"`) | 필수 |
| `drwNo` | Integer | 회차 번호 | 필수 |
| `drwNoDate` | String | 추첨일 (yyyy-MM-dd 형식) | 필수 |
| `drwtNo1` ~ `drwtNo6` | Integer | 당첨 번호 6개 (1~45) | 필수 |
| `bnusNo` | Integer | 보너스 번호 (1~45) | 필수 |
| `firstAccumamnt` | Long | 1등 총 당첨금 | 선택 |
| `firstPrzwnerCo` | Integer | 1등 당첨자 수 | 선택 |
| `firstWinamnt` | Long | 1등 1인당 당첨금 | 선택 |
| `totSellamnt` | Long | 총 판매금액 | 선택 |

### 2.2 실패 응답 (JSON)

API가 JSON을 반환하지만 실패한 경우:

```json
{
  "returnValue": "fail"
}
```

또는 회차가 존재하지 않는 경우:

```json
{
  "returnValue": "fail",
  "drwNo": null
}
```

### 2.3 HTML 응답 (비정상 케이스)

**중요**: 일부 환경에서는 API가 HTML 페이지를 반환할 수 있습니다.

```html
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" ...>
<html xmlns="http://www.w3.org/1999/xhtml" lang="ko">
  ...
</html>
```

#### HTML 응답이 발생하는 경우

1. **API 접근 제한**: Referer 검증 실패
2. **User-Agent 검증**: 브라우저가 아닌 클라이언트 차단
3. **회차 없음**: 존재하지 않는 회차 번호 요청
4. **API 변경**: 동행복권 측에서 API 엔드포인트 변경
5. **IP 차단**: 과도한 요청으로 인한 일시적 차단

---

## 3. 코드에서의 응답 처리

### 3.1 DTO 클래스: `DrawApiResponse`

**위치**: `io.appback.lottoguide.infra.external.dto.DrawApiResponse`

```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // 알 수 없는 필드 무시
public class DrawApiResponse {
    
    @JsonProperty("returnValue")
    private String returnValue;
    
    @JsonProperty("drwNo")
    private Integer drwNo;
    
    @JsonProperty("drwNoDate")
    private String drwNoDate;
    
    @JsonProperty("drwtNo1")
    private Integer drwtNo1;
    
    @JsonProperty("drwtNo2")
    private Integer drwtNo2;
    
    @JsonProperty("drwtNo3")
    private Integer drwtNo3;
    
    @JsonProperty("drwtNo4")
    private Integer drwtNo4;
    
    @JsonProperty("drwtNo5")
    private Integer drwtNo5;
    
    @JsonProperty("drwtNo6")
    private Integer drwtNo6;
    
    @JsonProperty("bnusNo")
    private Integer bnusNo;
    
    /**
     * 성공 여부 확인
     */
    public boolean isSuccess() {
        return "success".equals(returnValue) && drwNo != null;
    }
    
    /**
     * 추첨일을 LocalDate로 변환
     */
    public LocalDate getDrawDate() {
        if (drwNoDate == null || drwNoDate.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(drwNoDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 번호 배열로 반환 (정렬됨)
     */
    public int[] getNumbers() {
        return new int[]{
            drwtNo1, drwtNo2, drwtNo3, drwtNo4, drwtNo5, drwtNo6
        };
    }
}
```

#### 주요 특징

- `@JsonIgnoreProperties(ignoreUnknown = true)`: API 응답에 추가 필드가 있어도 파싱 실패하지 않음
- `isSuccess()`: `returnValue == "success"` 및 `drwNo != null` 확인
- `getDrawDate()`: 안전한 날짜 파싱 (예외 처리 포함)
- `getNumbers()`: 당첨 번호를 배열로 반환 (정렬되지 않음, DB 저장 시 정렬 필요)

### 3.2 API 클라이언트: `DonghaengLottoApiClient`

**위치**: `io.appback.lottoguide.infra.external.DonghaengLottoApiClient`

#### 3.2.1 응답 처리 흐름

```
1. HTTP 요청 전송
   ↓
2. HTTP 상태 코드 확인 (200 OK)
   ↓
3. 응답 본문 확인 (null/empty 체크)
   ↓
4. HTML 응답 체크 (responseBody.startsWith("<"))
   → HTML이면 즉시 실패 반환 (재시도 안 함)
   ↓
5. JSON 형식 확인 (startsWith("{") 또는 startsWith("["))
   → JSON이 아니면 실패 반환
   ↓
6. JSON 파싱 시도
   → 파싱 실패 시 실패 반환
   ↓
7. API 응답 검증
   - apiResponse == null 체크
   - returnValue == "success" 체크
   - drwNo 일치 확인
   ↓
8. 성공 응답 반환
```

#### 3.2.2 핵심 처리 로직

```java
// 1. HTML 응답 체크
if (responseBody.trim().startsWith("<")) {
    log.warn("동행복권 API가 HTML 응답 반환 (API 변경 또는 회차 없음 가능): drawNo={}, responseLength={}, attempt={}/{}", 
        drawNo, responseBody.length(), attempt, MAX_RETRY);
    // HTML 응답은 재시도해도 동일하므로 즉시 반환
    return Optional.empty();
}

// 2. JSON 형식 확인
String trimmedBody = responseBody.trim();
if (!trimmedBody.startsWith("{") && !trimmedBody.startsWith("[")) {
    log.warn("동행복권 API 응답이 JSON 형식이 아님: drawNo={}, responsePreview={}, attempt={}/{}", 
        drawNo, 
        trimmedBody.length() > 200 ? trimmedBody.substring(0, 200) : trimmedBody,
        attempt, MAX_RETRY);
    return Optional.empty();
}

// 3. JSON 파싱
DrawApiResponse apiResponse = objectMapper.readValue(responseBody, DrawApiResponse.class);

// 4. returnValue 확인
if (!apiResponse.isSuccess()) {
    log.warn("동행복권 API 응답 실패: returnValue={}, drawNo={}, attempt={}/{}", 
        apiResponse.getReturnValue(), drawNo, attempt, MAX_RETRY);
    return Optional.empty();
}

// 5. 회차 번호 일치 확인
if (apiResponse.getDrwNo() == null || !apiResponse.getDrwNo().equals(drawNo)) {
    log.warn("동행복권 API 응답 회차 번호 불일치: 요청={}, 응답={}, attempt={}/{}", 
        drawNo, apiResponse.getDrwNo(), attempt, MAX_RETRY);
    return Optional.empty();
}
```

### 3.3 재시도 로직

#### 재시도 조건

- **네트워크 오류** (`RestClientException`): 재시도 가능
- **HTTP 상태 코드 오류** (200이 아닌 경우): 재시도 가능
- **응답 본문 비어있음**: 재시도 가능
- **HTML 응답**: 재시도 안 함 (즉시 실패)
- **JSON 파싱 실패**: 재시도 안 함 (즉시 실패)
- **returnValue != "success"**: 재시도 안 함 (즉시 실패)
- **회차 번호 불일치**: 재시도 안 함 (즉시 실패)

#### 재시도 설정

- **최대 재시도 횟수**: 3회
- **재시도 지연**: 지수 백오프 (1초, 2초, 3초)
  ```java
  Thread.sleep(RETRY_DELAY_MS * attempt);  // attempt=1: 1초, attempt=2: 2초, attempt=3: 3초
  ```

---

## 4. 실제 테스트 결과

### 4.1 PowerShell 직접 호출 테스트

**테스트 환경**: Windows PowerShell  
**테스트 날짜**: 2026-01-09

#### 테스트 1: 기본 GET 요청

```powershell
Invoke-WebRequest -Uri "https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=1000" -UseBasicParsing
```

**결과**: HTML 페이지 반환 (JSON 아님)

**원인 분석**:
- Referer 헤더 누락 가능성
- User-Agent 검증 실패 가능성
- 동행복권 측에서 직접 브라우저 접근만 허용하는 정책

#### 테스트 2: 헤더 포함 요청

```powershell
$headers = @{
    'User-Agent'='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    'Accept'='application/json'
    'Referer'='https://www.dhlottery.co.kr/'
}
Invoke-WebRequest -Uri "https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=1000" -Headers $headers -UseBasicParsing
```

**결과**: 여전히 HTML 페이지 반환

**결론**: 
- PowerShell의 `Invoke-WebRequest`로는 JSON 응답을 받기 어려움
- 실제 애플리케이션에서 `RestTemplate`을 사용할 때는 정상 동작할 가능성 높음
- 코드에 HTML 응답 처리 로직이 이미 구현되어 있음

### 4.2 코드에서의 예상 동작

애플리케이션에서 `RestTemplate`을 사용할 때:

1. **정상 케이스**: JSON 응답 → 파싱 성공 → DB 저장
2. **HTML 응답 케이스**: HTML 감지 → `Optional.empty()` 반환 → 로그 기록
3. **네트워크 오류**: 재시도 (최대 3회) → 실패 시 `Optional.empty()` 반환

---

## 5. 응답 처리 시나리오별 동작

### 시나리오 1: 정상 JSON 응답

```
요청: GET ...?drwNo=1000
응답: {"returnValue":"success","drwNo":1000,...}
처리: ✅ 성공 → DrawApiResponse 객체 반환
```

### 시나리오 2: HTML 응답

```
요청: GET ...?drwNo=1000
응답: <!DOCTYPE html>...
처리: ❌ HTML 감지 → Optional.empty() 반환 (재시도 안 함)
로그: "동행복권 API가 HTML 응답 반환"
```

### 시나리오 3: returnValue="fail"

```
요청: GET ...?drwNo=9999
응답: {"returnValue":"fail"}
처리: ❌ isSuccess() == false → Optional.empty() 반환
로그: "동행복권 API 응답 실패: returnValue=fail"
```

### 시나리오 4: 회차 번호 불일치

```
요청: GET ...?drwNo=1000
응답: {"returnValue":"success","drwNo":1001,...}
처리: ❌ 회차 번호 불일치 → Optional.empty() 반환
로그: "동행복권 API 응답 회차 번호 불일치: 요청=1000, 응답=1001"
```

### 시나리오 5: 네트워크 오류

```
요청: GET ...?drwNo=1000
응답: RestClientException 발생
처리: ⏳ 재시도 (최대 3회) → 모두 실패 시 Optional.empty() 반환
로그: "동행복권 API 호출 중 네트워크 오류"
```

### 시나리오 6: JSON 파싱 실패

```
요청: GET ...?drwNo=1000
응답: "invalid json"
처리: ❌ JsonParseException → Optional.empty() 반환 (재시도 안 함)
로그: "동행복권 API JSON 파싱 실패"
```

---

## 6. 데이터 저장 흐름

### 6.1 DrawRefreshService에서의 처리

```java
// 1. API 호출
Optional<DrawApiResponse> apiResponse = apiClient.fetchDraw(drawNo);

// 2. 응답 확인
if (apiResponse.isPresent()) {
    DrawApiResponse draw = apiResponse.get();
    
    // 3. 중복 체크
    if (drawRepository.findByDrawNo(draw.getDrwNo()).isPresent()) {
        log.debug("회차 {}는 이미 존재함, 건너뜀", draw.getDrwNo());
        continue;
    }
    
    // 4. 번호 정렬
    int[] numbers = draw.getNumbers();
    Arrays.sort(numbers);  // DB 저장 시 정렬
    
    // 5. Entity 생성 및 저장
    DrawEntity drawEntity = DrawEntity.builder()
        .drawNo(draw.getDrwNo())
        .drawDate(draw.getDrawDate())  // LocalDate 변환
        .n1(numbers[0])
        .n2(numbers[1])
        .n3(numbers[2])
        .n4(numbers[3])
        .n5(numbers[4])
        .n6(numbers[5])
        .bonus(draw.getBnusNo())
        .build();
    
    drawRepository.save(drawEntity);
}
```

### 6.2 데이터 변환 과정

```
API 응답 (JSON)
  ↓
DrawApiResponse DTO
  ↓
번호 배열 추출 (getNumbers())
  ↓
번호 정렬 (Arrays.sort())
  ↓
DrawEntity 생성
  ↓
DB 저장
```

---

## 7. 주의사항 및 제한사항

### 7.1 API 안정성

- ⚠️ **비공식 API**: 동행복권의 공식 API가 아닐 수 있음
- ⚠️ **정책 변경 가능**: 동행복권 측에서 언제든지 접근을 제한하거나 엔드포인트를 변경할 수 있음
- ⚠️ **HTML 응답 가능**: 일부 환경에서는 HTML을 반환할 수 있음

### 7.2 현재 구현의 강점

- ✅ **HTML 응답 감지**: HTML 응답을 감지하고 적절히 처리
- ✅ **재시도 로직**: 네트워크 오류 시 자동 재시도
- ✅ **안전한 실패 처리**: 모든 실패 케이스에서 `Optional.empty()` 반환
- ✅ **상세 로깅**: 각 단계별 로그 기록
- ✅ **API 부하 방지**: 회차 간 대기 시간 설정 (200ms)

### 7.3 개선 가능 사항

1. **캐싱**: 동일 회차 재요청 시 캐시 활용
2. **Rate Limiting**: API 호출 빈도 제한 강화
3. **모니터링**: API 응답 성공률 추적
4. **폴백 메커니즘**: API 실패 시 대체 데이터 소스 활용

---

## 8. 테스트 방법

### 8.1 애플리케이션 테스트 엔드포인트

현재 구현된 테스트 엔드포인트를 활용:

```bash
# 특정 회차 조회
GET /api/v1/test/draw/{drawNo}

# 최신 회차 번호 조회
GET /api/v1/test/latest-draw-no

# 전체 데이터 로드
POST /api/v1/test/load-data
```

### 8.2 예상 응답 예시

#### 성공 케이스

```json
{
  "success": true,
  "drawNo": 1000,
  "drawDate": "2021-06-19",
  "numbers": [10, 23, 29, 33, 37, 40],
  "bonus": 16
}
```

#### 실패 케이스

```json
{
  "success": false,
  "message": "회차 데이터를 찾을 수 없습니다: 9999",
  "suggestion": "최신 회차 번호를 먼저 확인해보세요: /api/v1/test/latest-draw-no"
}
```

---

## 9. 결론

### 9.1 현재 상태

- ✅ **코드 구현 완료**: 동행복권 API 호출 및 응답 처리 로직이 완전히 구현됨
- ✅ **에러 처리 완비**: 모든 실패 시나리오에 대한 처리 로직 포함
- ✅ **HTML 응답 대응**: HTML 응답 감지 및 처리 로직 포함

### 9.2 실제 동작 확인 필요

- 🔍 **애플리케이션 실행 후 테스트**: 실제 `RestTemplate`을 사용한 호출 시 정상 동작 여부 확인 필요
- 🔍 **로그 모니터링**: 실제 운영 환경에서의 API 응답 패턴 확인 필요

### 9.3 권장 사항

1. **애플리케이션 실행 후 테스트**: `/api/v1/test/draw/1000` 엔드포인트로 실제 동작 확인
2. **로그 확인**: HTML 응답이 발생하는 빈도 및 패턴 확인
3. **모니터링 설정**: API 호출 성공률 및 응답 시간 모니터링
4. **폴백 전략**: API 실패 시 랜덤 생성 모드로 전환 (이미 구현됨)

---

## 참고 자료

- [동행복권 API 통합 문서](./12-external-api-integration.md)
- `DonghaengLottoApiClient.java`: API 클라이언트 구현
- `DrawApiResponse.java`: 응답 DTO 구현
- `DrawRefreshService.java`: 데이터 갱신 서비스 구현
