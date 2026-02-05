package io.appback.lottoguide.domain.generator.explain;

import io.appback.lottoguide.domain.generator.model.ExplainTag;
import io.appback.lottoguide.domain.generator.model.GeneratedSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Explain Tag 빌더
 * 생성된 번호 세트를 분석하여 태그 생성
 */
@Component
public class ExplainTagBuilder {
    
    /**
     * 생성된 세트에 대한 Explain Tags 생성
     */
    public List<ExplainTag> buildTags(GeneratedSet set, Integer windowSize) {
        List<ExplainTag> tags = new ArrayList<>();
        
        if (set.getNumbers() == null || set.getNumbers().size() != 6) {
            return tags;
        }
        
        // WINDOW 태그 (윈도우 크기)
        if (windowSize != null) {
            if (windowSize == 50) {
                tags.add(ExplainTag.WINDOW_50);
            }
            // 다른 윈도우 크기는 필요시 추가
        }
        
        // 홀수/짝수 비율 태그
        int oddCount = set.getOddCount();
        int evenCount = set.getEvenCount();
        if (oddCount == 3 && evenCount == 3) {
            tags.add(ExplainTag.ODD_3_EVEN_3);
        }
        
        // 합계 태그
        int sum = set.getSum();
        if (sum == 126) {
            tags.add(ExplainTag.SUM_126);
        }
        
        // 전략 기반 태그
        if (set.getStrategy() != null) {
            switch (set.getStrategy()) {
                case FREQUENT_TOP -> tags.add(ExplainTag.FREQ_BIAS);
                case OVERDUE_TOP -> tags.add(ExplainTag.OVERDUE_BIAS);
                case BALANCED -> {
                    // BALANCED는 특별한 태그 없음
                }
                case AI_SIMULATION -> {
                    // AI 시뮬레이션은 다차원 평가를 통해 생성되므로 특별한 태그 추가 가능
                    tags.add(ExplainTag.FREQ_BIAS); // 빈도 기반 평가 포함
                }
                case AI_PATTERN_REASONER -> {
                    // AI 패턴 분석은 패턴 일치도 기반
                    tags.add(ExplainTag.FREQ_BIAS);
                }
                case AI_DECISION_FILTER -> {
                    // AI 판단 필터는 극단값 제거 기반
                    tags.add(ExplainTag.FREQ_BIAS);
                }
                case AI_WEIGHT_EVOLUTION -> {
                    // AI 가중치 진화는 적응형 가중치 기반
                    tags.add(ExplainTag.FREQ_BIAS);
                }
                default -> {
                    // 다른 전략은 기본 처리
                }
            }
        }
        
        // 연속 번호 체크
        if (!hasLongConsecutive(set.getNumbers())) {
            tags.add(ExplainTag.NO_LONG_CONSEC);
        }
        
        return tags;
    }
    
    /**
     * 긴 연속 번호가 있는지 확인
     */
    private boolean hasLongConsecutive(List<Integer> numbers) {
        if (numbers == null || numbers.size() < 3) {
            return false;
        }
        
        List<Integer> sorted = new ArrayList<>(numbers);
        sorted.sort(Integer::compareTo);
        
        int consecutiveCount = 1;
        int maxConsecutive = 1;
        
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i) - sorted.get(i - 1) == 1) {
                consecutiveCount++;
                maxConsecutive = Math.max(maxConsecutive, consecutiveCount);
            } else {
                consecutiveCount = 1;
            }
        }
        
        // 3개 이상 연속이면 긴 연속으로 간주
        return maxConsecutive >= 3;
    }
}
