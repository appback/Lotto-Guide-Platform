# Mission API 문서

## 개요

미션 생성 API는 Explain Tags와 별자리 정보를 기반으로 LLM을 통해 미션 텍스트를 생성합니다.

## 엔드포인트

### POST /api/v1/mission

미션 텍스트를 생성합니다.

#### 요청

**Headers:**
- `Content-Type: application/json`

**Body:**
```json
{
  "explainTags": ["WINDOW_50", "ODD_3_EVEN_3", "SUM_126"],  // 선택적
  "tone": "LIGHT",  // 선택적 (기본값: LIGHT)
  "birthDate": "1990-05-15"  // 선택적 (YYYY-MM-DD 형식)
}
```

**필드 설명:**
- `explainTags`: ExplainTag 문자열 리스트 (선택적)
- `tone`: 미션 톤 (선택적, 기본값: LIGHT)
- `birthDate`: 생년월일 (선택적, 별자리 계산에만 사용, **저장되지 않음**)

#### 응답

**성공 (200 OK):**
```json
{
  "missionText": "LLM 서비스는 준비 중 입니다.\n\n※ 본 서비스는 번호 생성 도구일 뿐이며, 당첨을 보장하지 않습니다.",
  "tokenUsage": null,
  "costEstimate": null,
  "zodiacSign": "황소자리"  // birthDate가 제공된 경우에만 값이 있음
}
```

**필드 설명:**
- `missionText`: 생성된 미션 텍스트 (Disclaimer 포함)
- `tokenUsage`: LLM 토큰 사용량 (LLM 통합 시 사용)
- `costEstimate`: 예상 비용 (LLM 통합 시 사용)
- `zodiacSign`: 계산된 별자리 (생년월일이 제공된 경우에만 값이 있음)

#### 처리 흐름

1. 생년월일이 제공된 경우 → 별자리 계산 (저장하지 않음)
2. 프롬프트 생성 (Explain Tags + Tone + 별자리)
3. LLM 호출
4. 응답 정제
5. 정책 검사 (확률/보장 관련 표현 차단)
6. Disclaimer 자동 추가
7. 응답 반환

#### 보안/프라이버시

- **생년월일은 저장되지 않음**: API 요청에 포함되지만 DB에 저장되지 않습니다.
- **별자리만 계산**: 생년월일로부터 별자리를 계산한 후 생년월일 정보는 버립니다.
- **별자리 정보**: 응답에 포함되지만 DB에 저장되지 않습니다.

#### 예시

**별자리 포함 요청:**
```bash
curl -X POST http://localhost:8083/api/v1/mission \
  -H "Content-Type: application/json" \
  -d '{
    "birthDate": "1990-05-15",
    "explainTags": ["WINDOW_50", "ODD_3_EVEN_3"],
    "tone": "LIGHT"
  }'
```

**별자리 없이 요청:**
```bash
curl -X POST http://localhost:8083/api/v1/mission \
  -H "Content-Type: application/json" \
  -d '{
    "explainTags": ["WINDOW_50"],
    "tone": "LIGHT"
  }'
```

## 참고

- 현재는 `SimpleLlmClient`로 고정 텍스트를 반환합니다.
- LLM 통합은 후반 작업으로 예정되어 있습니다.
- 별자리 계산은 `ZodiacCalculator`에서 수행됩니다.
