package io.appback.lottoguide.domain.generator.preset;

import io.appback.lottoguide.domain.generator.model.Constraints;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 균형 잡힌 랜덤 Preset
 * 제약 조건 기반으로 균형 잡힌 번호 선택
 */
public class BalancedPreset implements Preset {
    
    @Override
    public List<Integer> generate(Constraints constraints, Integer windowSize, List<NumberMetrics> metricsList) {
        // 1. 후보 번호 풀 생성 (1~45)
        List<Integer> candidatePool = IntStream.rangeClosed(1, 45)
            .boxed()
            .collect(Collectors.toList());
        
        // 2. 제약 조건 적용
        candidatePool = applyConstraints(candidatePool, constraints);
        
        if (candidatePool.size() < 6) {
            candidatePool = IntStream.rangeClosed(1, 45).boxed().collect(Collectors.toList());
        }
        
        // 3. 제약 조건을 만족하는 조합 생성
        return selectBalanced(candidatePool, constraints, 6);
    }
    
    private List<Integer> applyConstraints(List<Integer> candidates, Constraints constraints) {
        if (constraints == null) {
            return candidates;
        }
        
        if (constraints.getIncludeNumbers() != null && !constraints.getIncludeNumbers().isEmpty()) {
            Set<Integer> includeSet = new HashSet<>(constraints.getIncludeNumbers());
            candidates = candidates.stream()
                .filter(n -> includeSet.contains(n) || !includeSet.contains(n))
                .collect(Collectors.toList());
        }
        
        if (constraints.getExcludeNumbers() != null && !constraints.getExcludeNumbers().isEmpty()) {
            Set<Integer> excludeSet = new HashSet<>(constraints.getExcludeNumbers());
            candidates = candidates.stream()
                .filter(n -> !excludeSet.contains(n))
                .collect(Collectors.toList());
        }
        
        return candidates;
    }
    
    private List<Integer> selectBalanced(List<Integer> candidates, Constraints constraints, int count) {
        List<Integer> selected = new ArrayList<>();
        Random random = new Random();
        int maxAttempts = 1000;
        int attempts = 0;
        
        while (selected.size() < count && attempts < maxAttempts) {
            Collections.shuffle(candidates);
            List<Integer> temp = new ArrayList<>(selected);
            
            for (Integer candidate : candidates) {
                if (temp.size() >= count) break;
                if (!temp.contains(candidate)) {
                    temp.add(candidate);
                }
            }
            
            if (temp.size() == count && satisfiesConstraints(temp, constraints)) {
                selected = temp;
                break;
            }
            
            attempts++;
        }
        
        // 제약 조건을 만족하지 못하면 기본 선택
        if (selected.size() < count) {
            Collections.shuffle(candidates);
            selected = candidates.stream()
                .limit(count)
                .sorted()
                .collect(Collectors.toList());
        }
        
        return selected.stream().sorted().collect(Collectors.toList());
    }
    
    private boolean satisfiesConstraints(List<Integer> numbers, Constraints constraints) {
        if (constraints == null) {
            return true;
        }
        
        // 홀수/짝수 비율 체크
        if (constraints.getOddEvenRatioRange() != null) {
            int oddCount = (int) numbers.stream().filter(n -> n % 2 == 1).count();
            Integer minOdd = constraints.getOddEvenRatioRange().getMinOddCount();
            Integer maxOdd = constraints.getOddEvenRatioRange().getMaxOddCount();
            
            if (minOdd != null && oddCount < minOdd) return false;
            if (maxOdd != null && oddCount > maxOdd) return false;
        }
        
        // 합계 범위 체크
        if (constraints.getSumRange() != null) {
            int sum = numbers.stream().mapToInt(Integer::intValue).sum();
            Integer minSum = constraints.getSumRange().getMinSum();
            Integer maxSum = constraints.getSumRange().getMaxSum();
            
            if (minSum != null && sum < minSum) return false;
            if (maxSum != null && sum > maxSum) return false;
        }
        
        return true;
    }
}
