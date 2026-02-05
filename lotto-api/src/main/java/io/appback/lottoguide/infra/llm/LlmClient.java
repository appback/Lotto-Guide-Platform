package io.appback.lottoguide.infra.llm;

/**
 * LLM Client 인터페이스
 * 
 * 현재는 LLM 통합 전 단계이므로 기본 구조만 정의
 * 나중에 실제 LLM Provider (OpenAI, Anthropic 등) 구현체로 교체 예정
 */
public interface LlmClient {
    
    /**
     * LLM을 통한 미션 생성
     * @param prompt 프롬프트
     * @return LLM 응답 결과
     */
    LlmResponse generateMission(String prompt);
    
    /**
     * LLM 응답 결과
     */
    record LlmResponse(
        String text,
        Integer tokenUsage,
        Double costEstimate
    ) {}
}
