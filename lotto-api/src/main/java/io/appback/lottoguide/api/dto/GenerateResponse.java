package io.appback.lottoguide.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 번호 생성 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResponse {
    
    /**
     * 생성된 번호 세트 리스트
     */
    private List<GeneratedSetDto> generatedSets;
    
    /**
     * 세트 ID (Member인 경우에만 값이 있음, Guest는 null)
     */
    private Long setId;
    
    /**
     * 생성된 번호 세트 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedSetDto {
        /**
         * 세트 내 인덱스
         */
        private Integer index;
        
        /**
         * 생성된 번호 6개
         */
        private List<Integer> numbers;
        
        /**
         * Explain Tags
         */
        private List<String> tags; // ExplainTag enum을 String으로 변환
    }
}
