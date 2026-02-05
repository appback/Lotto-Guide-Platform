# CSV 업로드 가이드

## 방법 1: 프론트엔드 관리자 페이지 사용 (권장)

### 접속 방법
1. Spring Boot 애플리케이션 실행 (프론트엔드가 빌드되어 포함된 상태)
2. 브라우저에서 `http://localhost:8083/lotto/admin` 접속
3. 또는 메인 페이지(`http://localhost:8083/lotto/generate`)에서 "관리자" 버튼 클릭

**참고**: Vite 개발 서버(5173)는 사용하지 않습니다. Spring Boot가 빌드된 프론트엔드를 서빙합니다.

### CSV 업로드
1. "CSV 업로드" 섹션에서 "CSV 파일 업로드" 버튼 클릭
2. CSV 파일 선택
3. 업로드 완료 후 결과 확인

### CSV 다운로드
1. "CSV 다운로드" 섹션에서 "모든 데이터 CSV 다운로드" 버튼 클릭
2. 파일이 자동으로 다운로드됩니다

### 범위 수집
1. "범위 수집" 섹션에서 시작/종료 회차 입력
2. "수집 시작" 버튼 클릭
3. 결과 확인

---

## 방법 2: curl 사용

### CSV 업로드
```bash
curl -X POST "http://localhost:8083/lotto/api/v1/admin/import-csv" \
  -F "file=@lotto_draws.csv"
```

### CSV 다운로드
```bash
curl -O "http://localhost:8083/lotto/api/v1/admin/export-csv"
```

### 범위 수집
```bash
curl -X POST "http://localhost:8083/lotto/api/v1/admin/collect-range?from=1&to=1206"
```

---

## 방법 3: Postman 사용

### CSV 업로드
1. Method: `POST`
2. URL: `http://localhost:8083/lotto/api/v1/admin/import-csv`
3. Body 탭 → form-data 선택
4. Key: `file` (타입: File)
5. Value: CSV 파일 선택
6. Send 클릭

### CSV 다운로드
1. Method: `GET`
2. URL: `http://localhost:8083/lotto/api/v1/admin/export-csv`
3. Send and Download 클릭

### 범위 수집
1. Method: `POST`
2. URL: `http://localhost:8083/lotto/api/v1/admin/collect-range`
3. Params 탭:
   - `from`: 1
   - `to`: 1206
4. Send 클릭

---

## CSV 파일 형식

### 헤더 (필수)
```csv
drawNo,drawDate,n1,n2,n3,n4,n5,n6,bonus,totalPrize,winnerCount,prizePerPerson
```

### 데이터 예시
```csv
drawNo,drawDate,n1,n2,n3,n4,n5,n6,bonus,totalPrize,winnerCount,prizePerPerson
1,2002-12-07,10,23,29,33,37,40,16,280.3,15,18.7
2,2002-12-14,9,13,21,25,32,42,5,,
1206,2026-01-11,1,3,17,26,27,42,23,280.3,15,18.7
```

### 주의사항
- 헤더는 자동으로 감지되어 건너뜁니다
- 날짜 형식: `YYYY-MM-DD` (예: `2002-12-07`)
- 번호는 자동으로 정렬됩니다
- 중복 회차는 건너뛰고 계속 진행합니다
- UTF-8 BOM이 있어도 자동으로 처리됩니다
- `totalPrize`, `winnerCount`, `prizePerPerson`은 선택적 필드입니다 (없으면 빈 값)
- `winnerCount`는 "15명" 형식도 지원합니다 (자동으로 "명" 제거)

---

## CSV 파일 준비 방법

### 온라인 데이터 소스
1. 공개된 로또 데이터 사이트에서 CSV 다운로드
2. Excel에서 데이터 정리 후 CSV로 저장
3. 직접 작성

### Excel에서 CSV 저장
1. Excel에서 데이터 입력
2. "다른 이름으로 저장" → "CSV UTF-8(쉼표로 구분)(*.csv)" 선택
3. 저장

---

## 응답 예시

### CSV 업로드 성공
```json
{
  "success": true,
  "message": "CSV 업로드 완료: 저장 100개, 건너뜀 5개, 오류 2개",
  "savedCount": 100,
  "skippedCount": 5,
  "errorCount": 2,
  "errors": [
    "라인 10: 컬럼 수가 부족합니다 (예상: 9개, 실제: 8개)",
    "라인 25: 파싱 실패 - Invalid date format"
  ]
}
```

### 범위 수집 성공
```json
{
  "success": true,
  "message": "범위 수집 완료: 1회차 ~ 1206회차, 성공 1000개, 스킵 0개, 실패 206개 (총 1206개)",
  "from": 1,
  "to": 1206,
  "successCount": 1000,
  "skip": 0,
  "fail": 206,
  "total": 1206
}
```

---

## 문제 해결

### 업로드 실패
- CSV 파일 형식 확인 (헤더, 컬럼 수)
- 날짜 형식 확인 (`YYYY-MM-DD`)
- 파일 인코딩 확인 (UTF-8 권장)

### 다운로드 실패
- 브라우저에서 직접 접근 시도
- curl 사용 시 `-O` 옵션 확인

### 범위 수집 실패
- 동행복권 API 차단 가능성 확인
- 작은 범위로 먼저 테스트 (예: 1~10회차)
- 로그 확인
