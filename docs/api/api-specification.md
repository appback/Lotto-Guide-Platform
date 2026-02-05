# Lotto Guide Platform API 명세서

**작성일**: 2026-01-11  
**Base URL**: `http://localhost:8083/lotto` (로컬 개발 환경)  
**Context Path**: `/lotto`  
**API 버전**: `v1`

---

## 목차

1. [고객 API](#고객-api)
   - [번호 생성](#1-번호-생성)
   - [히스토리 조회](#2-히스토리-조회)
   - [미션 생성](#3-미션-생성)
2. [관리자 API](#관리자-api)
   - [데이터 수집](#1-데이터-수집)
   - [데이터 상태 확인](#2-데이터-상태-확인)
   - [캐시 초기화](#3-캐시-초기화)
   - [범위 수집 (테스트용)](#4-범위-수집-테스트용)
   - [누락 회차 자동 저장](#5-누락-회차-자동-저장)
   - [수동 저장](#6-수동-저장)
   - [CSV 다운로드](#7-csv-다운로드)
   - [CSV 업로드](#8-csv-업로드)
   - [단일 회차 조회](#9-단일-회차-조회)
   - [전체 회차 목록 조회](#10-전체-회차-목록-조회)

---

## 고객 API

### 1. 번호 생성

**엔드포인트**: `POST /api/v1/generate`

**설명**: 로또 번호를 생성합니다. DB와 캐싱만 사용하며, 외부 API 호출은 하지 않습니다.

**Headers**:
- `Content-Type: application/json`
- `X-User-Id: {userId}` (선택적, Member인 경우)

**Request Body**:
```json
{
  "strategy": "BALANCED",  // 선택적: "FREQUENT_TOP" | "OVERDUE_TOP" | "BALANCED" (기본값: BALANCED)
  "constraints": {  // 선택적
    "includeNumbers": [1, 2, 3],  // 포함할 번호
    "excludeNumbers": [40, 41, 42],  // 제외할 번호
    "oddEvenRatioRange": {  // 홀짝 비율 범위
      "min": 2,
      "max": 4
    },
    "sumRange": {  // 합계 범위
      "min": 100,
      "max": 200
    },
    "similarityThreshold": 0.5  // 유사도 임계값
  },
  "count": 5,  // 생성할 세트 개수 (기본값: 1)
  "windowSize": 50  // 윈도우 크기: 20, 50, 100 (기본값: 50)
}
```

**Response (200 OK)**:
```json
{
  "generatedSets": [
    {
      "index": 0,
      "numbers": [1, 2, 3, 33, 36, 37],
      "tags": ["WINDOW_50", "ODD_3_EVEN_3"]
    }
  ],
  "setId": 123  // Member인 경우에만 값이 있음, Guest는 null
}
```

**에러 응답**:
- `400 Bad Request`: 잘못된 요청
- `500 Internal Server Error`: 서버 오류

**예시**:
```bash
curl -X POST http://localhost:8083/lotto/api/v1/generate \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "strategy": "BALANCED",
    "count": 5,
    "windowSize": 50
  }'
```

---

### 2. 히스토리 조회

**엔드포인트**: `GET /api/v1/history`

**설명**: 사용자가 생성한 번호 히스토리를 페이징하여 조회합니다. Member 전용입니다.

**Headers**:
- `X-User-Id: {userId}` (필수)

**Query Parameters**:
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 10)

**Response (200 OK)**:
```json
{
  "content": [
    {
      "setId": 123,
      "strategyCode": "BALANCED",
      "generatedSets": [
        {
          "index": 0,
          "numbers": [1, 2, 3, 33, 36, 37],
          "tags": ["WINDOW_50"]
        }
      ],
      "createdAt": "2026-01-11T10:30:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 50,
  "totalPages": 5
}
```

**에러 응답**:
- `401 Unauthorized`: X-User-Id 헤더가 없는 경우
- `500 Internal Server Error`: 서버 오류

**예시**:
```bash
curl -X GET "http://localhost:8083/lotto/api/v1/history?page=0&size=10" \
  -H "X-User-Id: 1"
```

---

### 3. 미션 생성

**엔드포인트**: `POST /api/v1/mission`

**설명**: Explain Tags와 별자리 정보를 기반으로 LLM을 통해 미션 텍스트를 생성합니다.

**Headers**:
- `Content-Type: application/json`

**Request Body**:
```json
{
  "explainTags": ["WINDOW_50", "ODD_3_EVEN_3", "SUM_126"],  // 선택적
  "tone": "LIGHT",  // 선택적 (기본값: LIGHT)
  "birthDate": "1990-05-15"  // 선택적 (YYYY-MM-DD 형식, 별자리 계산에만 사용, 저장되지 않음)
}
```

**Response (200 OK)**:
```json
{
  "missionText": "LLM 서비스는 준비 중 입니다.\n\n※ 본 서비스는 번호 생성 도구일 뿐이며, 당첨을 보장하지 않습니다.",
  "tokenUsage": null,
  "costEstimate": null,
  "zodiacSign": "황소자리"  // birthDate가 제공된 경우에만 값이 있음
}
```

**에러 응답**:
- `400 Bad Request`: 잘못된 요청
- `500 Internal Server Error`: 서버 오류

**예시**:
```bash
curl -X POST http://localhost:8083/lotto/api/v1/mission \
  -H "Content-Type: application/json" \
  -d '{
    "birthDate": "1990-05-15",
    "explainTags": ["WINDOW_50", "ODD_3_EVEN_3"],
    "tone": "LIGHT"
  }'
```

**참고**:
- 생년월일은 저장되지 않으며, 별자리 계산에만 사용됩니다.
- 별자리 정보는 응답에 포함되지만 DB에 저장되지 않습니다.

---

## 관리자 API

### 1. 데이터 수집

**엔드포인트**: `POST /api/v1/admin/refresh-data`

**설명**: 동행복권 API에서 최신 추첨 데이터를 가져와서 DB에 저장합니다. 날짜 기반으로 최신 회차를 계산하여, DB의 마지막 회차 다음부터 최신 회차까지 수집합니다.

**Request Body**: 없음

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "수집 완료: 저장 10개, 실패 0개",
  "savedCount": 10,
  "failedCount": 0,
  "latestDrawNo": 1207
}
```

**에러 응답**:
- `500 Internal Server Error`: 데이터 수집 실패

**예시**:
```bash
curl -X POST http://localhost:8083/lotto/api/v1/admin/refresh-data
```

**참고**:
- 이미 존재하는 회차는 건너뜁니다.
- 날짜 기반으로 최신 회차를 계산합니다 (첫 회차: 2002-12-07, 매주 토요일 추첨).

**주의사항**:
- 동행복권 API가 HTML을 반환하는 경우 수집이 실패할 수 있습니다. 이는 다음 원인일 수 있습니다:
  - API 엔드포인트 변경
  - 접근 차단 (User-Agent, IP 등)
  - 회차 데이터 없음
- 외부 API 실패 시 대안:
  1. **수동 저장 API** (`POST /api/v1/admin/save-draw`) 사용
  2. **CSV 업로드 API** (`POST /api/v1/admin/import-csv`) 사용
  3. **누락 회차 자동 저장 API** (`POST /api/v1/admin/refresh-missing`) 사용 (일부 회차만 실패하는 경우)

**에러 예시**:
```json
{
  "success": false,
  "message": "데이터 수집 실패: 동행복권 API에서 추첨 데이터를 가져올 수 없습니다. 최신 회차: 1206, 조회 시도: 10개, 모두 실패",
  "error": "RuntimeException"
}
```

---

### 2. 데이터 상태 확인

**엔드포인트**: `GET /api/v1/admin/data-status`

**설명**: DB와 캐시에 데이터가 있는지 확인합니다. 외부 API 호출은 하지 않습니다.

**Response (200 OK)**:
```json
{
  "hasData": true,
  "message": "데이터가 존재합니다"
}
```

**예시**:
```bash
curl -X GET http://localhost:8083/lotto/api/v1/admin/data-status
```

---

### 3. 캐시 초기화

**엔드포인트**: `POST /api/v1/admin/clear-cache`

**설명**: 메모리 캐시를 초기화합니다. 다음 요청 시 DB에서 다시 확인합니다.

**Request Body**: 없음

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "캐시 초기화 완료"
}
```

**예시**:
```bash
curl -X POST http://localhost:8083/lotto/api/v1/admin/clear-cache
```

---

### 4. 범위 수집 (테스트용)

**엔드포인트**: `POST /api/v1/admin/collect-range`

**설명**: 지정된 범위의 회차를 순차적으로 수집하여 DB에 저장합니다. 이미 저장된 회차는 자동으로 스킵하며, 실패한 회차는 건너뛰고 계속 진행합니다. 재실행 시 실패한 회차만 다시 시도할 수 있습니다.

**Query Parameters**:
- `from`: 시작 회차 (기본값: 1)
- `to`: 종료 회차 (기본값: 1206)

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "범위 수집 완료: 1회차 ~ 1206회차, 성공 1000개, 스킵 0개, 실패 206개 (총 1206개)",
  "from": 1,
  "to": 1206,
  "success": 1000,
  "skip": 0,
  "fail": 206,
  "total": 1206
}
```

**에러 응답**:
- `400 Bad Request`: 잘못된 범위 (from < 1 또는 to < from)
- `500 Internal Server Error`: 수집 실패

**예시**:
```bash
# 1회차부터 1206회차까지 수집
curl -X POST "http://localhost:8083/lotto/api/v1/admin/collect-range?from=1&to=1206"

# 100회차부터 200회차까지 수집
curl -X POST "http://localhost:8083/lotto/api/v1/admin/collect-range?from=100&to=200"
```

**참고**:
- 이미 저장된 회차는 자동으로 스킵됩니다.
- HTML 응답 등 실패한 회차는 건너뛰고 계속 진행합니다.
- 재실행 시 실패한 회차만 다시 시도할 수 있습니다.
- Rate limiting 적용 (200ms 대기).

---

### 5. 누락 회차 자동 저장

**엔드포인트**: `POST /api/v1/admin/refresh-missing`

**설명**: 1회차부터 최신 회차까지 모든 회차를 확인하여, DB에 없는 누락된 회차를 외부 API에서 가져와 저장합니다.

**Request Body**: 없음

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "누락 회차 수집 완료: 저장 50개, 실패 2개 (총 누락 52개)",
  "savedCount": 50,
  "failedCount": 2,
  "missingCount": 52,
  "latestDrawNo": 1207,
  "failedDrawNos": [100, 200]
}
```

**에러 응답**:
- `500 Internal Server Error`: 수집 실패

**예시**:
```bash
curl -X POST http://localhost:8083/lotto/api/v1/admin/refresh-missing
```

**참고**:
- 1회차부터 최신 회차까지 모든 회차를 확인합니다.
- 누락된 회차만 외부 API에서 가져와 저장합니다.
- Rate limiting 적용 (300ms 대기).

**주의사항**:
- 동행복권 API가 HTML을 반환하는 경우 해당 회차는 실패로 처리됩니다.
- 실패한 회차는 `failedDrawNos` 배열에 포함되어 반환됩니다.
- 일부 회차만 실패하는 경우, 성공한 회차는 저장되고 실패한 회차는 나중에 수동 저장하거나 CSV로 업로드할 수 있습니다.

---

### 6. 수동 저장

**엔드포인트**: `POST /api/v1/admin/save-draw`

**설명**: 관리자가 직접 회차별 데이터를 JSON으로 전달하여 저장합니다.

**Headers**:
- `Content-Type: application/json`

**Request Body**:
```json
{
  "drawNo": 1207,
  "drawDate": "2026-01-19",
  "numbers": [1, 2, 3, 33, 36, 37],
  "bonus": 16,
  "totalPrize": 280.3,
  "winnerCount": 15,
  "prizePerPerson": 18.7
}
```

**필드 설명**:
- `drawNo`: 회차 번호 (필수, 1 이상)
- `drawDate`: 추첨일 (필수, YYYY-MM-DD 형식)
- `numbers`: 번호 6개 (필수, 배열)
- `bonus`: 보너스 번호 (필수)
- `totalPrize`: 당첨금 (억 단위, 선택적)
- `winnerCount`: 당첨인원 (선택적)
- `prizePerPerson`: 인당당첨금 (억 단위, 선택적)

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "회차 1207 저장 완료",
  "drawNo": 1207
}
```

**에러 응답**:
- `400 Bad Request`: 잘못된 요청 (회차 번호, 추첨일, 번호, 보너스 번호 검증 실패)
- `409 Conflict`: 이미 존재하는 회차
- `500 Internal Server Error`: 저장 실패

**예시**:
```bash
curl -X POST http://localhost:8083/lotto/api/v1/admin/save-draw \
  -H "Content-Type: application/json" \
  -d '{
    "drawNo": 1207,
    "drawDate": "2026-01-19",
    "numbers": [1, 2, 3, 33, 36, 37],
    "bonus": 16
  }'
```

**참고**:
- 번호는 자동으로 정렬됩니다.
- 기존 데이터가 있으면 자동으로 업데이트됩니다 (409 Conflict 반환하지 않음).

---

### 7. CSV 다운로드

**엔드포인트**: `GET /api/v1/admin/export-csv`

**설명**: DB에 저장된 모든 추첨 데이터를 CSV 파일로 다운로드합니다.

**Response (200 OK)**:
- Content-Type: `text/csv; charset=UTF-8`
- Content-Disposition: `attachment; filename="lotto_draws_YYYYMMDD.csv"`
- UTF-8 BOM 포함 (Excel 호환)

**CSV 형식**:
```csv
drawNo,drawDate,n1,n2,n3,n4,n5,n6,bonus,totalPrize,winnerCount,prizePerPerson
1,2002-12-07,10,23,29,33,37,40,16,280.3,15,18.7
2,2002-12-14,9,13,21,25,32,42,5,,
```

**예시**:
```bash
# 브라우저에서 직접 접근
http://localhost:8083/lotto/api/v1/admin/export-csv

# 또는 curl
curl -O http://localhost:8083/lotto/api/v1/admin/export-csv
```

**참고**:
- 회차 순으로 정렬되어 다운로드됩니다.
- UTF-8 BOM이 포함되어 Excel에서 바로 열 수 있습니다.
- `totalPrize`, `winnerCount`, `prizePerPerson`은 선택적 필드입니다 (없으면 빈 값).

---

### 8. CSV 업로드

**엔드포인트**: `POST /api/v1/admin/import-csv`

**설명**: CSV 파일을 업로드하여 DB에 저장합니다.

**Headers**:
- `Content-Type: multipart/form-data`

**Request Body**:
- `file`: CSV 파일 (multipart/form-data)

**CSV 형식**:
```csv
drawNo,drawDate,n1,n2,n3,n4,n5,n6,bonus,totalPrize,winnerCount,prizePerPerson
1,2002-12-07,10,23,29,33,37,40,16,280.3,15,18.7
2,2002-12-14,9,13,21,25,32,42,5,,
```

**필드 설명**:
- `drawNo`: 회차 번호 (필수)
- `drawDate`: 추첨일 (필수, YYYY-MM-DD 형식)
- `n1` ~ `n6`: 번호 6개 (필수)
- `bonus`: 보너스 번호 (필수)
- `totalPrize`: 당첨금 (억 단위, 선택적)
- `winnerCount`: 당첨인원 (선택적, "15명" 형식도 지원)
- `prizePerPerson`: 인당당첨금 (억 단위, 선택적)

**Response (200 OK)**:
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

**에러 응답**:
- `400 Bad Request`: 파일이 비어있거나 CSV 파일이 아닌 경우
- `500 Internal Server Error`: 업로드 실패

**예시**:
```bash
curl -X POST http://localhost:8083/lotto/api/v1/admin/import-csv \
  -F "file=@lotto_draws.csv"
```

**참고**:
- 헤더는 자동으로 감지하여 건너뜁니다.
- 중복 회차는 건너뛰고 계속 진행합니다.
- 오류가 있는 라인은 건너뛰고 계속 진행합니다.
- BOM이 있는 경우 자동으로 처리합니다.
- `totalPrize`, `winnerCount`, `prizePerPerson`은 선택적 필드입니다 (없으면 빈 값).
- `winnerCount`는 "15명" 형식도 지원합니다 (자동으로 "명" 제거).

---

### 9. 단일 회차 조회

**엔드포인트**: `GET /api/v1/admin/draw/{drawNo}`

**설명**: 특정 회차의 데이터를 조회합니다.

**Path Parameters**:
- `drawNo`: 회차 번호

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "drawNo": 1206,
    "drawDate": "2026-01-11",
    "numbers": [1, 3, 17, 26, 27, 42],
    "bonus": 23,
    "totalPrize": 280.3,
    "winnerCount": 15,
    "prizePerPerson": 18.7,
    "createdAt": "2026-01-11T10:30:00"
  }
}
```

**에러 응답**:
- `404 Not Found`: 회차를 찾을 수 없는 경우
- `500 Internal Server Error`: 조회 실패

**예시**:
```bash
curl -X GET http://localhost:8083/lotto/api/v1/admin/draw/1206
```

---

### 10. 전체 회차 목록 조회

**엔드포인트**: `GET /api/v1/admin/draws`

**설명**: 저장된 모든 회차 목록을 조회합니다. 최신 회차부터 정렬됩니다.

**Query Parameters**:
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 100)

**Response (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "drawNo": 1206,
      "drawDate": "2026-01-11",
      "numbers": [1, 3, 17, 26, 27, 42],
      "bonus": 23,
      "totalPrize": 280.3,
      "winnerCount": 15,
      "prizePerPerson": 18.7
    }
  ],
  "total": 1206,
  "page": 0,
  "size": 100,
  "totalPages": 13
}
```

**에러 응답**:
- `500 Internal Server Error`: 조회 실패

**예시**:
```bash
# 첫 페이지 (최신 100개)
curl -X GET "http://localhost:8083/lotto/api/v1/admin/draws?page=0&size=100"

# 두 번째 페이지
curl -X GET "http://localhost:8083/lotto/api/v1/admin/draws?page=1&size=100"
```

**참고**:
- 회차 번호 기준 내림차순 정렬 (최신 회차부터).

---

## 공통 사항

### 인증

- **고객 API**: `X-User-Id` 헤더로 Guest/Member 구분
  - Guest: 헤더 없음 또는 null
  - Member: 헤더에 사용자 ID 포함
- **관리자 API**: 현재 프로토타입에서는 인증 없음 (후반 작업 예정)

### 에러 응답 형식

```json
{
  "success": false,
  "message": "에러 메시지",
  "error": "에러 클래스명"
}
```

### Context Path

모든 API는 `/lotto` context path를 포함합니다:
- 로컬 개발: `http://localhost:8083/lotto/api/v1/...`
- 프로덕션: `https://your-domain.com/lotto/api/v1/...`

### Rate Limiting

- 외부 API 호출 시 Rate limiting 적용 (300ms 대기)
- 동시성 제어를 위한 Lock 사용

### 외부 API 제한사항

**동행복권 API (`https://www.dhlottery.co.kr/common.do`)**

동행복권 API는 비공식 엔드포인트이며, 다음 제한사항이 있습니다:

1. **HTML 응답 반환 가능성**
   - API가 JSON 대신 HTML을 반환할 수 있습니다.
   - 이는 API 엔드포인트 변경, 접근 차단, 또는 회차 데이터 없음을 의미할 수 있습니다.
   - 로그 예시: `동행복권 API가 HTML 응답 반환 (API 변경 또는 회차 없음 가능)`

2. **대응 방법**
   - **수동 저장**: `POST /api/v1/admin/save-draw` API로 직접 데이터 입력
   - **CSV 업로드**: `POST /api/v1/admin/import-csv` API로 대량 데이터 업로드
   - **CSV 다운로드**: `GET /api/v1/admin/export-csv` API로 기존 데이터 백업

3. **재시도 정책**
   - 각 API 호출은 최대 3회 재시도합니다.
   - HTML 응답은 재시도해도 동일하므로 즉시 실패 처리됩니다.
   - 연속 실패 10회 시 수집이 중단됩니다.

4. **User-Agent 헤더**
   - 브라우저처럼 보이도록 User-Agent 헤더를 설정합니다.
   - 하지만 접근 차단을 완전히 방지하지는 못할 수 있습니다.

---

## 참고 문서

- [동행복권 API 응답 분석](./dhlottery-api-response-analysis.md)
- [미션 API 상세 문서](./mission-api.md)
