# 동행복권 API 디버깅 가이드

**현재 상태**: 쿠키는 획득되지만 여전히 HTML 응답 반환

---

## 현재 상황 분석

### 로그 분석
```
✅ 세션 쿠키 획득 성공: DHJSESSIONID=XW2bY8uhEKfj8SvuIwUZCQUsaJh20VC8lCHSRSHMlURN4LEutmjYLBisn6ekgVa0...
❌ 동행복권 API가 HTML 응답 반환: drawNo=1206, responseLength=166305
```

**확인된 사항**:
- ✅ 쿠키 획득 성공 (DHJSESSIONID)
- ✅ 쿠키 재획득 로직 동작
- ❌ 여전히 HTML 응답 반환

---

## 다음 단계: 실제 브라우저 요청 확인

### 1. 브라우저 개발자 도구에서 실제 요청 확인

**방법**:
1. https://www.dhlottery.co.kr 접속
2. F12 → Network 탭 열기
3. 회차 조회 페이지로 이동 (예: https://www.dhlottery.co.kr/gameResult.do?method=byWin)
4. 회차 번호 입력 후 조회 버튼 클릭
5. Network 탭에서 `getLottoNumber` 또는 `common.do` 관련 요청 찾기

**확인할 항목**:

#### Request Headers (요청 헤더)
다음 정보를 복사해주세요:
```
User-Agent: [실제 값]
Referer: [실제 값]
Accept: [실제 값]
Accept-Language: [실제 값]
Accept-Encoding: [실제 값]
Cookie: [실제 값 - 모든 쿠키]
Origin: [실제 값]
X-Requested-With: [있는지 확인]
Sec-Fetch-*: [모든 Sec-Fetch 헤더]
```

#### Request URL
```
정확한 URL과 파라미터 형식
```

#### Response Headers (응답 헤더)
```
Content-Type: [실제 값]
Set-Cookie: [있는지 확인]
```

---

## 가능한 원인

### 1. 쿠키 형식 문제
- 현재: `DHJSESSIONID=xxx`만 전송
- 필요: 다른 쿠키도 함께 전송해야 할 수 있음

### 2. 헤더 순서 문제
- 일부 WAF는 헤더 순서를 검증할 수 있음

### 3. 추가 보안 토큰 필요
- JavaScript 실행 후 생성되는 토큰이 필요할 수 있음
- CSRF 토큰 등

### 4. API 엔드포인트 변경
- 실제 브라우저에서 사용하는 URL이 다를 수 있음

### 5. 요청 타이밍 문제
- 메인 페이지 접속 후 바로 API 호출하면 차단될 수 있음
- 더 긴 대기 시간 필요할 수 있음

---

## 임시 해결 방법

### 방법 1: 실제 브라우저 헤더 복사
브라우저 개발자 도구에서 실제 요청 헤더를 복사하여 코드에 반영

### 방법 2: 쿠키 확인
메인 페이지 접속 시 받은 모든 쿠키를 확인하고 모두 전송

### 방법 3: 요청 순서 변경
1. 메인 페이지 접속
2. 게임 결과 페이지 접속 (쿠키 유지)
3. API 호출

---

## 확인 요청 사항

다음 정보를 제공해주시면 정확한 해결책을 제시할 수 있습니다:

1. **브라우저 개발자 도구에서 실제 요청 헤더** (전체)
2. **실제 요청 URL** (파라미터 포함)
3. **응답 Content-Type** (JSON인지 확인)
4. **모든 쿠키 값** (Cookie 헤더 전체)

특히 **Cookie 헤더의 전체 내용**이 중요합니다. 현재는 `DHJSESSIONID`만 전송하고 있는데, 다른 쿠키도 필요할 수 있습니다.
