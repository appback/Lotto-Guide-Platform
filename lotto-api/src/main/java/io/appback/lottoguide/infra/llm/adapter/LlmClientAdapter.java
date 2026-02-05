package io.appback.lottoguide.infra.llm.adapter;

import io.appback.lottoguide.application.port.out.LlmClientPort;
import io.appback.lottoguide.infra.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LlmClientPort 구현체
 * Infrastructure의 LlmClient를 Application Port로 어댑팅
 */
@Component
@RequiredArgsConstructor
public class LlmClientAdapter implements LlmClientPort {
    
    private final LlmClient llmClient;
    
    @Override
    public LlmResponse generateMission(String prompt) {
        LlmClient.LlmResponse response = llmClient.generateMission(prompt);
        
        return new LlmResponse(
            response.text(),
            response.tokenUsage(),
            response.costEstimate()
        );
    }
}
