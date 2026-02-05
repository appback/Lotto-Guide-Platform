package io.appback.lottoguide.application.port.out;

/**
 * LLM Client Port (Outbound)
 * Infrastructure 레이어에서 구현
 */
public interface LlmClientPort {
    
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
