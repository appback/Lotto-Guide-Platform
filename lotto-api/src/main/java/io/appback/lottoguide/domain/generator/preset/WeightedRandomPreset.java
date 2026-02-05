package io.appback.lottoguide.domain.generator.preset;

import io.appback.lottoguide.domain.generator.model.Constraints;
import io.appback.lottoguide.domain.generator.preset.util.WeightedRandomUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 가중치 기반 랜덤 Preset
 * 빈도와 과거 데이터를 결합한 가중치를 사용하여 랜덤하게 번호를 추출
 */
public class WeightedRandomPreset implements Preset {
    
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
        
        // 3. 가중치 기반 랜덤 추출
        // 빈도와 과거 데이터를 50:50으로 결합한 가중치 사용
        return selectByWeightedRandom(candidatePool, metricsList, 6);
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
    
    /**
     * 가중치 기반 랜덤 추출
     * 빈도와 과거 데이터를 결합한 가중치를 사용
     */
    private List<Integer> selectByWeightedRandom(
            List<Integer> candidates,
            List<NumberMetrics> metricsList,
            int count) {
        
        // metricsList가 비어있으면 일반 랜덤 선택
        if (metricsList == null || metricsList.isEmpty()) {
            Collections.shuffle(candidates);
            return candidates.stream()
                    .limit(count)
                    .sorted()
                    .collect(Collectors.toList());
        }
        
        // 빈도와 과거 데이터를 50:50으로 결합한 가중치 맵 생성
        Map<Integer, Double> weightMap = WeightedRandomUtil.createCombinedWeightMap(
                metricsList,
                0.5,  // 빈도 가중치 50%
                0.5   // 과거 데이터 가중치 50%
        );
        
        // 가중치 기반 랜덤 추출
        return WeightedRandomUtil.selectByWeight(candidates, weightMap, count);
    }
}
