package io.appback.lottoguide.domain.mission.policy;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 금지된 표현 감지기
 * 확률/보장 관련 표현을 차단
 * 
 * 현재는 LLM 통합 전 단계이므로 기본 구조만 구현
 */
@Component
public class ForbiddenPhraseDetector {
    
    private static final List<String> FORBIDDEN_PHRASES = Arrays.asList(
        "보장",
        "확실",
        "확률 증가",
        "당첨 보장",
        "100%",
        "반드시",
        "절대",
        "확실히"
    );
    
    /**
     * 금지된 표현이 포함되어 있는지 확인
     * @param text 검사할 텍스트
     * @return 금지된 표현 포함 여부 (true: 포함, false: 없음)
     */
    public boolean containsForbiddenPhrase(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        
        for (String phrase : FORBIDDEN_PHRASES) {
            if (lowerText.contains(phrase.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 금지된 표현 목록 반환
     */
    public List<String> getForbiddenPhrases() {
        return FORBIDDEN_PHRASES;
    }
}
