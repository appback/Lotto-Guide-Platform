package io.appback.lottoguide.domain.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 생성된 번호 세트
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedSet {
    
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
    private List<ExplainTag> tags;
    
    /**
     * 생성 전략
     */
    private Strategy strategy;
    
    /**
     * 사용된 제약 조건
     */
    private Constraints constraints;
    
    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 번호 합계
     */
    public Integer getSum() {
        return numbers != null ? numbers.stream().mapToInt(Integer::intValue).sum() : 0;
    }
    
    /**
     * 홀수 개수
     */
    public Integer getOddCount() {
        return numbers != null ? (int) numbers.stream().filter(n -> n % 2 == 1).count() : 0;
    }
    
    /**
     * 짝수 개수
     */
    public Integer getEvenCount() {
        return numbers != null ? (int) numbers.stream().filter(n -> n % 2 == 0).count() : 0;
    }
}
