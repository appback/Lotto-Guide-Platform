package io.appback.lottoguide.api.dto;

import io.appback.lottoguide.domain.generator.model.Constraints;
import io.appback.lottoguide.domain.generator.model.Strategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 번호 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRequest {
    
    /**
     * 생성 전략
     */
    private Strategy strategy;
    
    /**
     * 제약 조건
     */
    private Constraints constraints;
    
    /**
     * 생성할 세트 개수
     */
    private Integer count;
    
    /**
     * 윈도우 크기 (20, 50, 100)
     * 기본값: 50
     */
    private Integer windowSize;
}
