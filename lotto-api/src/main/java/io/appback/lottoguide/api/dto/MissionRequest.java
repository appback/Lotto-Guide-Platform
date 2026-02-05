package io.appback.lottoguide.api.dto;

import io.appback.lottoguide.domain.mission.model.Tone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 미션 생성 요청 DTO
 * 
 * 주의: birthDate는 별자리 계산에만 사용되며 저장되지 않음
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionRequest {
    
    /**
     * 생성 전략 (A/B/C 시스템에서 사용)
     */
    private String strategy;
    
    /**
     * 생성된 6개 번호 (A/B/C 시스템에서 사용)
     */
    private List<Integer> numbers;
    
    /**
     * 제외할 A 멘트 ID 목록 (로컬 스토리지에서 가져온 히스토리)
     */
    private List<Long> excludePhraseAIds;
    
    /**
     * 제외할 B 멘트 ID 목록 (로컬 스토리지에서 가져온 히스토리)
     */
    private List<Long> excludePhraseBIds;
    
    /**
     * Explain Tags (문자열 리스트) - 하위 호환성 유지
     */
    private List<String> explainTags;
    
    /**
     * 톤
     */
    private Tone tone;
    
    /**
     * 생년월일 (선택적)
     * 별자리 계산에만 사용되며 저장되지 않음
     */
    private LocalDate birthDate;
}
