# 동행복권 API HTML 응답 근본 원인 분석

**현재 상황**: 1회차 조회 시에도 HTML 응답 반환 (API 접근 자체가 차단된 것으로 보임)

**로그 분석**:
- ✅ 쿠키 획득 성공
- ✅ 헤더 설정 완료
- ❌ 모든 회차에서 HTML 응답 반환

---

## 근본 원인 파악을 위한 확인 사항

### 1. 브라우저에서 실제 API 호출 확인 (가장 중요)

**방법**:
1. https://www.dhlottery.co.kr 접속
2. F12 → Network 탭 열기
3. 회차 조회 페이지로 이동: https://www.dhlottery.co.kr/gameResult.do?method=byWin
4. 회차 번호 입력 (예: 1) 후 조회 버튼 클릭
5. Network 탭에서 `common.do` 또는 `getLottoNumber` 관련 요청 찾기

**확인할 항목**:

#### A. Request URL (요청 URL)
```
정확한 전체 URL (파라미터 포함)
예: https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=1
```

#### B. Request Headers (요청 헤더) - 전체 복사
```
User-Agent: [실제 값]
Referer: [실제 값 - 매우 중요!]
Accept: [실제 값]
Accept-Language: [실제 값]
Accept-Encoding: [실제 값]
Cookie: [전체 쿠키 값 - 매우 중요!]
Origin: [실제 값]
Host: [실제 값]
Connection: [실제 값]
X-Requested-With: [있는지 확인]
Sec-Fetch-*: [모든 Sec-Fetch 헤더]
기타 모든 헤더
```

#### C. Response (응답)
```
Content-Type: [실제 값]
응답 본문이 JSON인지 확인
```

---

## 가능한 원인들

### 1. API 엔드포인트 변경
- 실제 브라우저에서 사용하는 URL이 다를 수 있음
- 파라미터 형식이 다를 수 있음

### 2. Referer 헤더 불일치
- 현재 코드: `https://www.dhlottery.co.kr`
- 실제 브라우저: 다른 값일 수 있음
- **가장 가능성 높음**

### 3. 쿠키 부족
- 현재: `DHJSESSIONID`만 전송
- 실제: 다른 쿠키도 필요할 수 있음

### 4. 추가 헤더 필요
- 현재 코드에 없는 헤더가 필요할 수 있음
- 헤더 순서가 중요할 수 있음

### 5. JavaScript 실행 필요
- HTML에 `rsaModulus`가 있는 것으로 보아 JavaScript 실행 후 토큰 생성이 필요할 수 있음
- 이 경우 RestTemplate으로는 불가능, Selenium 등 필요

---

## 다음 단계

**브라우저 개발자 도구에서 다음 정보를 제공해주세요**:

1. **실제 요청 URL** (전체)
2. **Request Headers 전체** (특히 Referer, Cookie)
3. **Response Headers** (Content-Type)
4. **응답 본문** (JSON인지 확인)

이 정보를 바탕으로 정확한 원인을 파악하고 해결책을 제시하겠습니다.
