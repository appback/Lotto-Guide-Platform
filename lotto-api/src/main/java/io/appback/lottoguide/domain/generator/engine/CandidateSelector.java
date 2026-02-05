package io.appback.lottoguide.domain.generator.engine;

import io.appback.lottoguide.domain.generator.model.Constraints;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 후보 번호 선택 로직
 */
public class CandidateSelector {
    
    /**
     * 제약 조건을 만족하는 후보 번호 선택
     */
    public List<Integer> selectCandidates(Constraints constraints, int poolSize) {
        List<Integer> pool = IntStream.rangeClosed(1, 45)
            .boxed()
            .collect(Collectors.toList());
        
        if (constraints == null) {
            return pool;
        }
        
        // 포함할 번호 필수 포함
        if (constraints.getIncludeNumbers() != null && !constraints.getIncludeNumbers().isEmpty()) {
            Set<Integer> includeSet = new HashSet<>(constraints.getIncludeNumbers());
            // 포함 번호는 유지하고 나머지에서 선택
        }
        
        // 제외할 번호 제거
        if (constraints.getExcludeNumbers() != null && !constraints.getExcludeNumbers().isEmpty()) {
            Set<Integer> excludeSet = new HashSet<>(constraints.getExcludeNumbers());
            pool = pool.stream()
                .filter(n -> !excludeSet.contains(n))
                .collect(Collectors.toList());
        }
        
        return pool;
    }
    
    /**
     * 가중치 기반 선택 (빈도 또는 과거 데이터)
     */
    public List<Integer> selectByWeight(List<Integer> candidates, Map<Integer, Double> weights, int count) {
        // 가중치 기반 확률 선택
        List<WeightedCandidate> weightedCandidates = candidates.stream()
            .map(n -> new WeightedCandidate(n, weights.getOrDefault(n, 1.0)))
            .sorted(Comparator.comparing(WeightedCandidate::weight).reversed())
            .collect(Collectors.toList());
        
        return weightedCandidates.stream()
            .limit(count)
            .map(WeightedCandidate::number)
            .sorted()
            .collect(Collectors.toList());
    }
    
    private record WeightedCandidate(Integer number, Double weight) {}
}
