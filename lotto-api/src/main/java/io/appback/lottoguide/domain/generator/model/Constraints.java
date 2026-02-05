package io.appback.lottoguide.domain.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 번호 생성 제약 조건
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Constraints {
    
    /**
     * 포함할 번호 목록
     */
    private List<Integer> includeNumbers;
    
    /**
     * 제외할 번호 목록
     */
    private List<Integer> excludeNumbers;
    
    /**
     * 홀수/짝수 비율 범위
     * [minOddCount, maxOddCount] 형태
     * 예: [3, 3] = 홀수 3개, 짝수 3개
     */
    private OddEvenRatioRange oddEvenRatioRange;
    
    /**
     * 합계 범위
     * [minSum, maxSum] 형태
     */
    private SumRange sumRange;
    
    /**
     * 유사도 임계값 (0.0 ~ 1.0)
     * 이 값보다 유사한 세트는 제외
     */
    private Double similarityThreshold;
    
    /**
     * 홀수/짝수 비율 범위
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OddEvenRatioRange {
        private Integer minOddCount;
        private Integer maxOddCount;
    }
    
    /**
     * 합계 범위
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SumRange {
        private Integer minSum;
        private Integer maxSum;
    }
}
