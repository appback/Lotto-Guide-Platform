package io.appback.lottoguide.infra.llm;

import org.springframework.stereotype.Component;

/**
 * LLM 응답 정제 및 검증
 * 
 * 현재는 LLM 통합 전 단계이므로 기본 구조만 구현
 */
@Component
public class LlmResponseSanitizer {
    
    /**
     * LLM 응답 정제
     * @param response 원본 응답
     * @return 정제된 응답
     */
    public String sanitize(String response) {
        if (response == null) {
            return "";
        }
        
        // 기본적인 정제 (앞뒤 공백 제거)
        String sanitized = response.trim();
        
        // TODO: LLM 통합 시 추가 정제 로직 구현
        // - 특수 문자 제거
        // - 길이 제한
        // - 인코딩 문제 해결 등
        
        return sanitized;
    }
    
    /**
     * 응답 검증
     * @param response 검증할 응답
     * @return 유효성 여부
     */
    public boolean isValid(String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }
        
        // 기본 검증: 최소 길이 체크
        if (response.trim().length() < 10) {
            return false;
        }
        
        // TODO: LLM 통합 시 추가 검증 로직 구현
        
        return true;
    }
}
