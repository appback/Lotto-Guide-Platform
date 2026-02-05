package io.appback.lottoguide.domain.generator.preset;

import io.appback.lottoguide.domain.generator.model.Constraints;
import io.appback.lottoguide.domain.generator.preset.util.WeightedRandomUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AI 판단 필터 Preset
 * AI가 "이 조합은 버린다 / 남긴다"를 판단
 * 극단값 조합 자동 제거 및 말이 안 되는 조합 사전 차단
 */
public class AiDecisionFilterPreset implements Preset {
    
    // 생성 후보 개수
    private static final int CANDIDATE_COUNT = 300;
    
    // 최대 시도 횟수
    private static final int MAX_ATTEMPTS = 1000;
    
    @Override
    public List<Integer> generate(Constraints constraints, Integer windowSize, List<NumberMetrics> metricsList) {
        // 1. 후보 번호 풀 생성
        List<Integer> candidatePool = IntStream.rangeClosed(1, 45)
            .boxed()
            .collect(Collectors.toList());
        
        // 2. 제약 조건 적용
        candidatePool = applyConstraints(candidatePool, constraints);
        
        if (candidatePool.size() < 6) {
            candidatePool = IntStream.rangeClosed(1, 45).boxed().collect(Collectors.toList());
        }
        
        // 3. 여러 후보 조합 생성
        List<List<Integer>> candidates = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < CANDIDATE_COUNT; i++) {
            List<Integer> combination;
            
            if (metricsList != null && !metricsList.isEmpty()) {
                Map<Integer, Double> weightMap = WeightedRandomUtil.createCombinedWeightMap(
                        metricsList, 0.5, 0.5);
                combination = WeightedRandomUtil.selectByWeight(candidatePool, weightMap, 6);
            } else {
                Collections.shuffle(candidatePool);
                combination = candidatePool.stream()
                        .limit(6)
                        .sorted()
                        .collect(Collectors.toList());
            }
            
            candidates.add(combination);
        }
        
        // 4. AI 판단 필터링: 극단값 조합 제거
        List<List<Integer>> filtered = candidates.stream()
                .filter(this::isValidCombination)
                .collect(Collectors.toList());
        
        // 5. 필터링된 조합이 없으면 기본 랜덤
        if (filtered.isEmpty()) {
            Collections.shuffle(candidatePool);
            return candidatePool.stream()
                    .limit(6)
                    .sorted()
                    .collect(Collectors.toList());
        }
        
        // 6. 필터링된 조합 중 랜덤 선택
        return filtered.get(random.nextInt(filtered.size()));
    }
    
    /**
     * AI 판단: 이 조합이 유효한지 판단
     * 극단값 조합이나 말이 안 되는 조합을 제거
     */
    private boolean isValidCombination(List<Integer> combination) {
        Collections.sort(combination);
        
        // 1. 총합이 너무 작거나 큰 경우 제거 (60 ~ 200 범위)
        int sum = combination.stream().mapToInt(Integer::intValue).sum();
        if (sum < 60 || sum > 200) {
            return false;
        }
        
        // 2. 번호 간격이 너무 좁은 경우 제거 (연속수 4개 이상)
        int consecutiveCount = 0;
        int maxConsecutive = 0;
        for (int i = 1; i < combination.size(); i++) {
            if (combination.get(i) - combination.get(i - 1) == 1) {
                consecutiveCount++;
                maxConsecutive = Math.max(maxConsecutive, consecutiveCount);
            } else {
                consecutiveCount = 0;
            }
        }
        if (maxConsecutive >= 4) {
            return false;
        }
        
        // 3. 번호가 너무 집중된 경우 제거 (10개 범위에 5개 이상)
        int min = combination.get(0);
        int max = combination.get(combination.size() - 1);
        if (max - min < 10 && combination.size() >= 5) {
            return false;
        }
        
        // 4. 홀짝 비율이 극단적인 경우 제거 (0:6 또는 6:0)
        long oddCount = combination.stream().filter(n -> n % 2 == 1).count();
        if (oddCount == 0 || oddCount == 6) {
            return false;
        }
        
        // 5. 고저 비율이 극단적인 경우 제거 (0:6 또는 6:0)
        long highCount = combination.stream().filter(n -> n > 22).count();
        if (highCount == 0 || highCount == 6) {
            return false;
        }
        
        return true;
    }
    
    private List<Integer> applyConstraints(List<Integer> candidates, Constraints constraints) {
        if (constraints == null) {
            return candidates;
        }
        
        if (constraints.getExcludeNumbers() != null && !constraints.getExcludeNumbers().isEmpty()) {
            Set<Integer> excludeSet = new HashSet<>(constraints.getExcludeNumbers());
            candidates = candidates.stream()
                    .filter(n -> !excludeSet.contains(n))
                    .collect(Collectors.toList());
        }
        
        return candidates;
    }
}
