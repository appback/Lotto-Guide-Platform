package io.appback.lottoguide.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 미션 생성 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionResponse {
    
    /**
     * 미션 텍스트 (Disclaimer 포함)
     */
    private String missionText;
    
    /**
     * 사용된 토큰 수 (nullable)
     */
    private Integer tokenUsage;
    
    /**
     * 예상 비용 (nullable)
     */
    private Double costEstimate;
    
    /**
     * 별자리 정보 (nullable)
     * 생년월일로부터 계산되지만 저장되지 않음
     */
    private String zodiacSign;
    
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
}
