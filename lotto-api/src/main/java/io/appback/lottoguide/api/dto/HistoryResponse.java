package io.appback.lottoguide.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 히스토리 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryResponse {
    
    /**
     * 히스토리 아이템 리스트
     */
    private List<HistoryItemDto> content;
    
    /**
     * 현재 페이지 (0부터 시작)
     */
    private Integer page;
    
    /**
     * 페이지 크기
     */
    private Integer size;
    
    /**
     * 전체 아이템 수
     */
    private Long totalElements;
    
    /**
     * 전체 페이지 수
     */
    private Integer totalPages;
    
    /**
     * 히스토리 아이템 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItemDto {
        /**
         * 세트 ID
         */
        private Long setId;
        
        /**
         * 생성 전략 코드
         */
        private String strategyCode;
        
        /**
         * 생성된 번호 세트 리스트
         */
        private List<GenerateResponse.GeneratedSetDto> generatedSets;
        
        /**
         * 생성 시간
         */
        private LocalDateTime createdAt;
    }
}
