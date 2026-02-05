package io.appback.lottoguide.domain.mission.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 미션 도메인 모델
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mission {
    
    /**
     * 미션 텍스트
     */
    private String missionText;
    
    /**
     * 톤
     */
    private Tone tone;
    
    /**
     * 입력 Explain Tags (JSON 문자열)
     */
    private String inputTagsJson;
    
    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * LLM 토큰 사용량 (nullable)
     */
    private Integer tokenUsage;
    
    /**
     * 비용 추정 (nullable)
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
