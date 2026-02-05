# 동행복권 API Network 탭 확인 가이드

## 현재 상황
- `gameResult.do?method=byWin` → 302 리다이렉트 (에러 페이지)
- 실제 API 호출은 JavaScript로 동적으로 발생할 가능성

---

## 확인 방법

### 1. 브라우저에서 실제 동작 확인

1. **브라우저에서 접속**
   ```
   https://www.dhlottery.co.kr/gameResult.do?method=byWin
   ```

2. **F12 → Network 탭 열기**
   - "Preserve log" 체크 (리다이렉트 후에도 로그 유지)
   - "Disable cache" 체크

3. **회차 번호 입력 후 조회 버튼 클릭**
   - 예: 회차 1 입력 후 조회

4. **Network 탭에서 다음 요청 찾기**:
   - `common.do` 포함된 요청
   - `getLottoNumber` 포함된 요청
   - `drwNo` 파라미터가 있는 요청

---

## 확인할 정보 (가장 중요!)

### A. Request URL (요청 URL)
```
예: https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=1
```

### B. Request Headers (요청 헤더) - 전체 복사
**특히 중요한 헤더들**:
- `Referer`: [정확한 값 - 매우 중요!]
- `Cookie`: [전체 쿠키 값 - 매우 중요!]
- `User-Agent`: [실제 브라우저 값]
- `Accept`: [실제 값]
- `X-Requested-With`: [있는지 확인]
- `Origin`: [있는지 확인]
- `Sec-Fetch-*`: [모든 Sec-Fetch 헤더]

### C. Request Method
```
GET 또는 POST
```

### D. Response
- Content-Type: [실제 값]
- 응답 본문이 JSON인지 확인

---

## 예상되는 문제

1. **Referer 불일치**
   - 현재 코드: `https://www.dhlottery.co.kr`
   - 실제 필요: `https://www.dhlottery.co.kr/gameResult.do?method=byWin` 또는 다른 값

2. **추가 쿠키 필요**
   - 현재: `DHJSESSIONID`만 전송
   - 실제: 다른 쿠키도 필요할 수 있음

3. **POST 요청일 수 있음**
   - 현재 코드: GET 요청
   - 실제: POST 요청일 수 있음

4. **추가 헤더 필요**
   - 현재 코드에 없는 헤더가 필요할 수 있음

---

## 다음 단계

**Network 탭에서 `common.do` 또는 `getLottoNumber` 요청을 찾아서**:
1. Request URL 전체 복사
2. Request Headers 전체 복사 (특히 Referer, Cookie)
3. Request Method 확인
4. Response Content-Type 확인
5. 응답 본문이 JSON인지 확인

이 정보를 제공해주시면 정확한 해결책을 제시하겠습니다.
