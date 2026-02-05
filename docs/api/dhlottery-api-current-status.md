# 동행복권 API 현재 상태 및 동작 플로우

**작성일**: 2026-01-16  
**상태**: 정상 동작 중 (폴백 메커니즘 활성화)

---

## 현재 동작 상태 요약

### ✅ 정상 동작하는 부분

1. **폴백 메커니즘**: `common.do` 엔드포인트가 HTML을 반환하면 자동으로 `selectPstLt645Info.do` 엔드포인트로 폴백
2. **전체 리스트 조회**: `selectPstLt645Info.do?srchLtEpsd=all` 엔드포인트는 정상적으로 JSON 반환
3. **회차 필터링**: 전체 리스트에서 특정 회차를 찾아서 반환
4. **날짜 형식 변환**: `20021207` → `2002-12-07` 형식으로 자동 변환
5. **DB 저장**: 폴백 엔드포인트에서 가져온 데이터가 정상적으로 DB에 저장됨

### ❌ 동작하지 않는 부분

1. **기본 엔드포인트**: `common.do?method=getLottoNumber&drwNo={회차}` 엔드포인트는 HTML 응답 반환 (차단됨)
   - 모든 회차에서 HTML 응답 (166KB)
   - `rsaModulus` 포함된 보안 페이지로 리다이렉트
   - WAF/봇 방어 시스템에 의해 차단된 것으로 추정

---

## 정상 동작 플로우 (현재)

### 1. API 호출 시도 (`fetchDraw` 메서드)

```
[시작]
  ↓
1. 세션 쿠키 확인 및 획득
   - 메인 페이지 접속: https://www.dhlottery.co.kr/
   - 당첨번호 조회 페이지 접속: https://www.dhlottery.co.kr/lt645/result
   - 세션 쿠키 저장 (DHJSESSIONID 등)
  ↓
2. 기본 엔드포인트 호출 시도
   - URL: https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={회차}
   - 헤더: Referer=https://www.dhlottery.co.kr/lt645/result
   - 헤더: Accept=application/json, text/javascript, */*; q=0.01
   - 헤더: X-Requested-With=XMLHttpRequest
  ↓
3. 응답 확인
   ├─ [JSON 응답] → ✅ 성공 → DrawApiResponse 반환
   └─ [HTML 응답] → ❌ 실패 → 폴백 메커니즘 실행
  ↓
4. 폴백 메커니즘 (HTML 응답 시)
   - fetchDrawFromListApi() 메서드 호출
   - URL: https://www.dhlottery.co.kr/lt645/selectPstLt645Info.do?srchLtEpsd=all
   - 전체 리스트 조회 (약 700KB JSON)
  ↓
5. 전체 리스트에서 특정 회차 찾기
   - JSON 파싱: {"resultCode":null,"resultMessage":null,"data":{"list":[...]}}
   - data.list 배열에서 ltEpsd == drawNo인 항목 검색
  ↓
6. 필드 매핑 및 변환
   - ltEpsd → drwNo
   - tm1WnNo ~ tm6WnNo → drwtNo1 ~ drwtNo6
   - bnsWnNo → bnusNo
   - ltRflYmd (20021207) → drwNoDate (2002-12-07)
  ↓
7. DrawApiResponse 생성 및 반환
   - returnValue = "success"
   - 모든 필드 매핑 완료
  ↓
[완료] ✅
```

### 2. 데이터 수집 플로우 (`refreshDataFromExternalApi` 메서드)

```
[시작]
  ↓
1. DB에서 없는 최소 회차 찾기
   - findAllByOrderByDrawNoAsc()로 모든 회차 조회
   - 1회차부터 순차적으로 확인
   - 첫 번째 빈 회차 발견 시 해당 회차부터 시작
   - 모든 회차가 연속이면 마지막 회차 + 1부터 시작
  ↓
2. API 최신 회차 확인
   - findLatestDrawNo() 호출 (날짜 기반 계산)
  ↓
3. 회차별 수집 루프
   for (drawNo = startDrawNo; drawNo <= latestDrawNo; drawNo++) {
     ↓
     3-1. 이미 존재하는 회차 확인
          - findByDrawNo(drawNo)로 DB 확인
          - 있으면 → skip (continue)
          - 없으면 → 계속 진행
     ↓
     3-2. API 호출
          - fetchDraw(drawNo) 호출
          - 기본 엔드포인트 시도 → HTML 응답 → 폴백 엔드포인트로 자동 전환
          - 폴백 엔드포인트에서 데이터 획득 성공
     ↓
     3-3. DB 저장
          - DrawApiResponse → DrawEntity 변환
          - 번호 정렬 (n1 ~ n6)
          - drawRepository.save()
     ↓
     3-4. Rate Limiting
          - 5초 대기 (Thread.sleep(5000))
          - API 차단 방지
   }
  ↓
4. 실패한 회차 재시도
   - 실패 목록에 있는 회차들 재시도
   - 5초 간격으로 재시도
  ↓
5. 메트릭 재계산
   - 저장된 데이터가 있으면 메트릭 재계산 트리거
  ↓
[완료] ✅
```

---

## 성공 케이스 예시 (로그 기반)

### 회차 22 수집 성공

```
1. 기본 엔드포인트 호출
   → HTML 응답 (166KB, rsaModulus 포함)
   → 폴백 메커니즘 실행

2. 폴백 엔드포인트 호출
   → selectPstLt645Info.do?srchLtEpsd=all
   → JSON 응답 (700KB+)
   → 전체 리스트에서 회차 22 찾기 성공

3. 데이터 변환
   - ltRflYmd: "20030503" → drwNoDate: "2003-05-03"
   - tm1WnNo ~ tm6WnNo → drwtNo1 ~ drwtNo6
   - bnsWnNo → bnusNo

4. DB 저장
   → INSERT 성공
   → 회차 22 저장 완료
```

---

## 실패 케이스 (현재 없음)

- 모든 회차가 폴백 메커니즘을 통해 성공적으로 수집됨
- 기본 엔드포인트는 항상 HTML을 반환하지만, 폴백으로 해결됨

---

## 성능 특성

### 폴백 메커니즘 비용

- **기본 엔드포인트 호출**: 약 0.5초 (HTML 응답)
- **폴백 엔드포인트 호출**: 약 1-2초 (700KB JSON 다운로드 및 파싱)
- **총 소요 시간**: 회차당 약 1.5-2.5초
- **Rate Limiting**: 5초 대기
- **실제 회차당 소요 시간**: 약 6.5-7.5초

### 최적화 가능성

현재는 매 회차마다 전체 리스트(700KB)를 다운로드하고 있습니다.  
최적화 방안:
- 전체 리스트를 한 번만 다운로드하여 메모리에 캐싱
- 여러 회차를 한 번에 처리
- 캐시 만료 시간 설정 (예: 1시간)

---

## 현재 설정값

- **Rate Limiting**: 5초 (5000ms)
- **재시도 횟수**: 3회
- **최대 연속 실패 허용**: 10회
- **폴백 엔드포인트**: `selectPstLt645Info.do?srchLtEpsd=all`

---

## 결론

현재 시스템은 **정상 동작** 중입니다:
- ✅ 모든 회차 데이터 수집 성공
- ✅ 폴백 메커니즘이 안정적으로 작동
- ✅ DB 저장 정상
- ⚠️ 기본 엔드포인트는 차단되어 있으나, 폴백으로 해결됨

**권장사항**: 현재 상태 유지. 폴백 메커니즘이 안정적으로 작동하고 있으므로 추가 수정 불필요.
