package io.appback.lottoguide.infra.llm;

/**
 * 간단한 LLM Client 구현체
 * LLM 통합 전 단계에서 사용하는 임시 구현
 * 실제 LLM 호출 대신 고정된 텍스트 반환
 */
public class SimpleLlmClient implements LlmClient {
    
    private static final String PLACEHOLDER_MESSAGE = "LLM 서비스는 준비 중 입니다.";
    
    @Override
    public LlmResponse generateMission(String prompt) {
        // LLM 통합 전 단계: 고정된 텍스트 반환
        return new LlmResponse(
            PLACEHOLDER_MESSAGE,
            null, // tokenUsage
            null  // costEstimate
        );
    }
}
