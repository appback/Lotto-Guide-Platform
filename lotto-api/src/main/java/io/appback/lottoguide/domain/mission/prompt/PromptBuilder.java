package io.appback.lottoguide.domain.mission.prompt;

import io.appback.lottoguide.domain.generator.model.ExplainTag;
import io.appback.lottoguide.domain.mission.model.Tone;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mission 생성 프롬프트 빌더
 * Explain Tags와 Tone을 기반으로 프롬프트 생성
 * 
 * 현재는 LLM 통합 전 단계이므로 기본 구조만 구현
 */
@Component
public class PromptBuilder {
    
    private static final int PROMPT_VERSION = 1;
    
    /**
     * 프롬프트 생성
     * @param explainTags Explain Tags 리스트
     * @param tone 톤
     * @param zodiacSign 별자리 (선택적)
     * @return 생성된 프롬프트
     */
    public String build(List<ExplainTag> explainTags, Tone tone, String zodiacSign) {
        // TODO: LLM 통합 시 실제 프롬프트 생성 로직 구현
        // 현재는 기본 구조만 제공
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a mission text based on the following explain tags: ");
        
        if (explainTags != null && !explainTags.isEmpty()) {
            prompt.append(explainTags.toString());
        }
        
        if (zodiacSign != null && !zodiacSign.isEmpty()) {
            prompt.append("\nZodiac Sign: ").append(zodiacSign);
        }
        
        prompt.append("\nTone: ").append(tone.name());
        prompt.append("\nPrompt Version: ").append(PROMPT_VERSION);
        
        return prompt.toString();
    }
    
    /**
     * 프롬프트 생성 (별자리 없이)
     * @param explainTags Explain Tags 리스트
     * @param tone 톤
     * @return 생성된 프롬프트
     */
    public String build(List<ExplainTag> explainTags, Tone tone) {
        return build(explainTags, tone, null);
    }
    
    /**
     * 프롬프트 버전 반환
     */
    public int getPromptVersion() {
        return PROMPT_VERSION;
    }
}
