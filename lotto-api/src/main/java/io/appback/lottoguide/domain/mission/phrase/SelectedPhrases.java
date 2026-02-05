package io.appback.lottoguide.domain.mission.phrase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 선택된 A/B/C 멘트
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectedPhrases {
    /**
     * A 멘트 (해석/은유)
     */
    private String phraseA;
    
    /**
     * B 멘트 (행동/장소/색감)
     */
    private String phraseB;
    
    /**
     * C 멘트 (추천도/마무리)
     */
    private String phraseC;
    
    /**
     * A 멘트 ID (프론트엔드 히스토리 저장용)
     */
    private Long phraseAId;
    
    /**
     * B 멘트 ID (프론트엔드 히스토리 저장용)
     */
    private Long phraseBId;
    
    /**
     * C 멘트 ID (프론트엔드 히스토리 저장용)
     */
    private Long phraseCId;
    
    /**
     * 전체 메시지 조합
     */
    public String getFullMessage() {
        return String.format("%s\n%s\n%s", phraseA, phraseB, phraseC);
    }
}
