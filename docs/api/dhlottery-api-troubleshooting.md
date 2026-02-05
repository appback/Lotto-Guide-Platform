# 동행복권 API HTML 응답 문제 해결 가이드

**문제**: 동행복권 API 호출 시 JSON 대신 HTML 응답(166KB)을 받고 있음

**로그 예시**:
```
동행복권 API가 HTML 응답 반환 (API 변경 또는 회차 없음 가능): drawNo=1206, responseLength=166305
```

---

## 확인이 필요한 사항

### 1. 브라우저에서 직접 테스트

**테스트 URL**: 
```
https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=1206
```

**확인 사항**:
- [ ] 브라우저에서 위 URL을 직접 열었을 때 JSON이 표시되는가?
- [ ] HTML 페이지가 표시되는가?
- [ ] 다른 오류 메시지가 표시되는가?

**브라우저 개발자 도구 확인**:
1. F12로 개발자 도구 열기
2. Network 탭 열기
3. 위 URL로 요청
4. 요청 헤더(Request Headers) 확인:
   - `User-Agent` 값
   - `Referer` 값
   - `Cookie` 값 (있는지 확인)
   - `Accept` 값
5. 응답 헤더(Response Headers) 확인:
   - `Content-Type` 값
   - `Set-Cookie` 값 (있는지 확인)

### 2. 실제 동행복권 사이트에서의 API 호출 방식

**확인 방법**:
1. https://www.dhlottery.co.kr 접속
2. 특정 회차 조회 페이지로 이동
3. 개발자 도구(F12) → Network 탭 열기
4. 회차 조회 버튼 클릭
5. 네트워크 요청 중 `getLottoNumber` 관련 요청 찾기
6. 해당 요청의 헤더와 쿠키 확인

**확인할 항목**:
- [ ] 실제 요청 URL (파라미터 포함)
- [ ] 모든 Request Headers (특히 Cookie)
- [ ] 요청 방식 (GET/POST)
- [ ] 추가 파라미터가 있는지

### 3. 실행 환경 확인

**확인 사항**:
- [ ] 로컬 환경에서 실행 중인가?
- [ ] 서버(AWS 등)에서 실행 중인가?
- [ ] 프록시나 방화벽이 있는가?
- [ ] IP 차단 가능성이 있는가?

### 4. HTML 응답 내용 확인

현재 코드는 HTML 응답의 첫 글자만 확인하고 있습니다. 실제 HTML 내용을 확인하면 원인을 파악할 수 있습니다.

**확인 방법**:
- HTML 응답의 처음 500자 정도를 로그에 출력
- HTML 내용에서 오류 메시지나 리다이렉트 정보 확인

---

## 가능한 원인들

### 1. API 엔드포인트 변경
- 동행복권에서 API URL이나 파라미터 형식이 변경되었을 수 있음
- 실제 사이트에서 사용하는 URL과 비교 필요

### 2. 쿠키/세션 필요
- 동행복권 사이트가 세션 쿠키를 요구할 수 있음
- 먼저 메인 페이지에 접속하여 쿠키를 받은 후 API 호출 필요

### 3. CSRF 토큰 필요
- 보안을 위해 CSRF 토큰이나 특정 헤더가 필요할 수 있음

### 4. User-Agent 검증 강화
- 현재 User-Agent가 더 이상 유효하지 않을 수 있음
- 실제 브라우저의 User-Agent와 비교 필요

### 5. Referer 검증 강화
- Referer가 정확한 경로여야 할 수 있음
- 예: `https://www.dhlottery.co.kr/gameResult.do?method=byWin` 등

### 6. IP 차단
- 과도한 요청으로 인한 일시적 차단
- 다른 IP에서 테스트 필요

### 7. 회차 번호 문제
- 1206, 1205 회차가 아직 발표되지 않았을 수 있음
- 과거 회차(예: 1000)로 테스트 필요

---

## 디버깅을 위한 코드 수정 제안

HTML 응답의 일부를 로그에 출력하여 실제 내용을 확인할 수 있도록 수정:

```java
if (responseBody.trim().startsWith("<")) {
    // HTML 응답의 처음 1000자 출력
    String htmlPreview = responseBody.length() > 1000 
        ? responseBody.substring(0, 1000) 
        : responseBody;
    log.warn("동행복권 API가 HTML 응답 반환: drawNo={}, responseLength={}, htmlPreview=\n{}", 
        drawNo, responseBody.length(), htmlPreview);
    return Optional.empty();
}
```

---

## 해결 방법 (구현 완료)

WAF/봇 방어 시스템을 우회하기 위해 다음과 같은 개선을 적용했습니다:

### 1. 세션 쿠키 관리
- 메인 페이지(`gameResult.do?method=byWin`)에 먼저 접속하여 세션 쿠키 획득
- 쿠키를 저장하여 이후 API 호출에 사용
- HTML 응답 시 쿠키 만료로 판단하고 재획득

### 2. 브라우저 헤더 강화
- 최신 Chrome User-Agent 사용
- `Sec-Fetch-*` 헤더 추가 (최신 브라우저 보안 헤더)
- `Accept`, `Accept-Language`, `Accept-Encoding` 등 브라우저와 동일하게 설정
- `Referer`를 실제 게임 결과 페이지로 설정
- `Origin`, `X-Requested-With` 헤더 추가

### 3. 자연스러운 요청 패턴
- 메인 페이지 접속 후 500ms 대기 (자연스러운 브라우저 동작 모방)
- 쿠키 획득 후 API 호출

### 구현된 코드 변경사항
- `ensureSessionCookies()`: 세션 쿠키 획득 메서드
- `createBrowserHeaders()`: 브라우저처럼 보이는 헤더 생성
- `createApiHeaders()`: API 호출용 헤더 생성 (JSON 요청)
- HTML 응답 시 쿠키 재획득 로직

---

## 테스트 방법

1. 애플리케이션 재시작
2. 어드민 페이지에서 회차 조회 테스트
3. 로그 확인:
   - "세션 쿠키 획득 성공" 메시지 확인
   - API 호출 성공 여부 확인

---

## 추가 개선 가능 사항

만약 여전히 HTML 응답을 받는다면:

1. **실제 브라우저의 요청 헤더 확인**
   - 개발자 도구에서 실제 요청 헤더 복사
   - 코드에 반영

2. **쿠키 값 확인**
   - 실제 브라우저에서 받은 쿠키 값을 코드에 하드코딩 (임시 테스트용)

3. **요청 타이밍 조정**
   - 메인 페이지 접속 후 대기 시간 증가
   - API 호출 간격 조정

4. **대안: Selenium 사용**
   - RestTemplate 대신 Selenium WebDriver 사용
   - 실제 브라우저를 제어하여 데이터 수집
   - 더 높은 성공률이지만 성능 저하
