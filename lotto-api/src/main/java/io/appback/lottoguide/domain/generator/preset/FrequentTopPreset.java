package io.appback.lottoguide.domain.generator.preset;

import io.appback.lottoguide.domain.generator.model.Constraints;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 고빈도 번호 우선 Preset
 * 최근 N회 추첨에서 고빈도 번호를 우선적으로 선택
 */
public class FrequentTopPreset implements Preset {
    
    @Override
    public List<Integer> generate(Constraints constraints, Integer windowSize, List<NumberMetrics> metricsList) {
        // 1. 후보 번호 풀 생성 (1~45)
        List<Integer> candidatePool = IntStream.rangeClosed(1, 45)
            .boxed()
            .collect(Collectors.toList());
        
        // 2. 제약 조건 적용
        candidatePool = applyConstraints(candidatePool, constraints);
        
        if (candidatePool.size() < 6) {
            // 후보가 부족하면 전체 풀에서 선택
            candidatePool = IntStream.rangeClosed(1, 45).boxed().collect(Collectors.toList());
        }
        
        // 3. 빈도 기반 가중치 적용하여 선택
        // metricsList가 비어있으면 랜덤 생성, 있으면 빈도 기반 선택
        return selectByFrequency(candidatePool, metricsList, 6);
    }
    
    private List<Integer> applyConstraints(List<Integer> candidates, Constraints constraints) {
        if (constraints == null) {
            return candidates;
        }
        
        // 포함할 번호가 있으면 필수 포함
        if (constraints.getIncludeNumbers() != null && !constraints.getIncludeNumbers().isEmpty()) {
            Set<Integer> includeSet = new HashSet<>(constraints.getIncludeNumbers());
            candidates = candidates.stream()
                .filter(n -> includeSet.contains(n) || !includeSet.contains(n))
                .collect(Collectors.toList());
        }
        
        // 제외할 번호 제거
        if (constraints.getExcludeNumbers() != null && !constraints.getExcludeNumbers().isEmpty()) {
            Set<Integer> excludeSet = new HashSet<>(constraints.getExcludeNumbers());
            candidates = candidates.stream()
                .filter(n -> !excludeSet.contains(n))
                .collect(Collectors.toList());
        }
        
        return candidates;
    }
    
    private List<Integer> selectByFrequency(List<Integer> candidates, List<NumberMetrics> metricsList, int count) {
        // metricsList가 비어있으면 랜덤 선택
        if (metricsList == null || metricsList.isEmpty()) {
            Collections.shuffle(candidates);
            return candidates.stream()
                .limit(count)
                .sorted()
                .collect(Collectors.toList());
        }
        
        // 빈도 기반 가중치 맵 생성
        Map<Integer, Double> frequencyMap = new HashMap<>();
        for (NumberMetrics metrics : metricsList) {
            // 빈도가 높을수록 높은 가중치 (최소 0.1로 설정하여 선택 가능하게)
            double weight = Math.max(0.1, metrics.frequency() + 1.0);
            frequencyMap.put(metrics.number(), weight);
        }
        
        // 후보 번호에 가중치 적용 (없으면 기본값 0.1)
        List<Integer> weightedCandidates = candidates.stream()
            .sorted((a, b) -> {
                double weightA = frequencyMap.getOrDefault(a, 0.1);
                double weightB = frequencyMap.getOrDefault(b, 0.1);
                // 가중치가 높은 순으로 정렬
                return Double.compare(weightB, weightA);
            })
            .collect(Collectors.toList());
        
        // 상위 count개 선택
        return weightedCandidates.stream()
            .limit(count)
            .sorted()
            .collect(Collectors.toList());
    }
}
