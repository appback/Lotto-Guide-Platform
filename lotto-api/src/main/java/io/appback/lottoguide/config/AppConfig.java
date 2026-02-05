package io.appback.lottoguide.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appback.lottoguide.infra.llm.LlmClient;
import io.appback.lottoguide.infra.llm.SimpleLlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 애플리케이션 설정
 */
@Configuration
public class AppConfig {
    
    @Value("${llm.provider:simple}")
    private String llmProvider;
    
    @Value("${llm.simple.placeholder-message:LLM 서비스는 준비 중 입니다.}")
    private String placeholderMessage;
    
    /**
     * RestTemplate Bean 등록
     * 외부 API 호출용
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    /**
     * LlmClient Bean 등록
     * 프로토타입에서는 SimpleLlmClient 사용
     * MVP에서는 llm.provider 설정에 따라 다른 구현체 사용
     */
    @Bean
    public LlmClient llmClient() {
        // 프로토타입에서는 SimpleLlmClient만 사용
        // MVP에서는 llm.provider 값에 따라 다른 구현체 반환
        if ("simple".equals(llmProvider)) {
            return new SimpleLlmClient();
        }
        
        // TODO: MVP에서 실제 LLM Provider 구현체 반환
        // if ("openai".equals(llmProvider)) {
        //     return new OpenAILlmClient(...);
        // }
        
        return new SimpleLlmClient();
    }
}
