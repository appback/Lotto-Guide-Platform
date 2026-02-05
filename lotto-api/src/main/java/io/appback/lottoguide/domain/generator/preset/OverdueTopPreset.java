package io.appback.lottoguide.domain.generator.preset;

import io.appback.lottoguide.domain.generator.model.Constraints;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 과거 데이터 번호 우선 Preset
 * 최근에 나오지 않은 번호를 우선적으로 선택
 */
public class OverdueTopPreset implements Preset {
    
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
        
        // 3. 과거 데이터 기반 가중치 적용하여 선택
        // metricsList가 비어있으면 랜덤 생성, 있으면 과거 데이터 기반 선택
        return selectByOverdue(candidatePool, metricsList, 6);
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
    
    private List<Integer> selectByOverdue(List<Integer> candidates, List<NumberMetrics> metricsList, int count) {
        // metricsList가 비어있으면 랜덤 선택
        if (metricsList == null || metricsList.isEmpty()) {
            Collections.shuffle(candidates);
            return candidates.stream()
                .limit(count)
                .sorted()
                .collect(Collectors.toList());
        }
        
        // 과거 데이터 기반 가중치 맵 생성
        Map<Integer, Double> overdueMap = new HashMap<>();
        for (NumberMetrics metrics : metricsList) {
            // overdue가 클수록 (오래 안 나온 번호일수록) 높은 가중치
            // overdue가 0이면 최근에 나온 번호이므로 낮은 가중치
            double weight = Math.max(0.1, metrics.overdue() + 1.0);
            overdueMap.put(metrics.number(), weight);
        }
        
        // 후보 번호에 가중치 적용 (없으면 기본값 0.1)
        List<Integer> weightedCandidates = candidates.stream()
            .sorted((a, b) -> {
                double weightA = overdueMap.getOrDefault(a, 0.1);
                double weightB = overdueMap.getOrDefault(b, 0.1);
                // 가중치가 높은 순으로 정렬 (오래 안 나온 번호 우선)
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
