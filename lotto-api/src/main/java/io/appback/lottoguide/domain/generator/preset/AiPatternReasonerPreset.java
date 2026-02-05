package io.appback.lottoguide.domain.generator.preset;

import io.appback.lottoguide.domain.generator.model.Constraints;
import io.appback.lottoguide.domain.generator.preset.util.PatternAnalyzer;
import io.appback.lottoguide.domain.generator.preset.util.WeightedRandomUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AI 패턴 분석 Preset
 * 과거 당첨 패턴과 유사도가 낮은 조합을 AI가 자동 제거
 * 총합/홀짝/고저/연속수 패턴 학습 및 패턴 일치도 기반 선별
 */
public class AiPatternReasonerPreset implements Preset {
    
    // 패턴 일치도 임계값 (0.0 ~ 1.0, 높을수록 엄격)
    private static final double PATTERN_THRESHOLD = 0.65; // AI 전략이므로 더 엄격하게
    
    // 최대 시도 횟수
    private static final int MAX_ATTEMPTS = 1000;
    
    // 생성 후보 개수
    private static final int CANDIDATE_COUNT = 200;
    
    @Override
    public List<Integer> generate(Constraints constraints, Integer windowSize, List<NumberMetrics> metricsList) {
        return generate(constraints, windowSize, metricsList, null);
    }
    
    /**
     * 패턴 통계 캐시를 사용하여 AI 패턴 분석 기반 생성
     */
    public List<Integer> generate(
            Constraints constraints,
            Integer windowSize,
            List<NumberMetrics> metricsList,
            io.appback.lottoguide.infra.refresh.PatternStatisticsCache patternStatisticsCache) {
        
        // 1. 후보 번호 풀 생성
        List<Integer> candidatePool = IntStream.rangeClosed(1, 45)
            .boxed()
            .collect(Collectors.toList());
        
        // 2. 제약 조건 적용
        candidatePool = applyConstraints(candidatePool, constraints);
        
        if (candidatePool.size() < 6) {
            candidatePool = IntStream.rangeClosed(1, 45).boxed().collect(Collectors.toList());
        }
        
        // 3. 패턴 통계 조회
        PatternAnalyzer.PatternStatistics statistics;
        if (patternStatisticsCache != null) {
            statistics = patternStatisticsCache.getPatternStatistics(windowSize);
        } else {
            statistics = createDefaultStatistics();
        }
        
        // 4. 여러 후보 조합 생성 및 패턴 일치도 평가
        List<ScoredCombination> scoredCombinations = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < CANDIDATE_COUNT; i++) {
            List<Integer> combination;
            
            // 다양한 방법으로 조합 생성
            if (metricsList != null && !metricsList.isEmpty()) {
                Map<Integer, Double> weightMap = WeightedRandomUtil.createCombinedWeightMap(
                        metricsList, 0.6, 0.4);
                combination = WeightedRandomUtil.selectByWeight(candidatePool, weightMap, 6);
            } else {
                Collections.shuffle(candidatePool);
                combination = candidatePool.stream()
                        .limit(6)
                        .sorted()
                        .collect(Collectors.toList());
            }
            
            // 5. 패턴 일치도 점수 계산
            double patternScore = calculatePatternScore(combination, statistics);
            
            // 패턴 일치도가 임계값 이상인 경우만 추가
            if (patternScore >= PATTERN_THRESHOLD) {
                scoredCombinations.add(new ScoredCombination(combination, patternScore));
            }
        }
        
        // 6. 패턴 일치도 기준 정렬
        scoredCombinations.sort((a, b) -> Double.compare(b.score, a.score));
        
        // 7. 상위 조합 중 랜덤 선택
        if (scoredCombinations.isEmpty()) {
            // 패턴 일치 조합이 없으면 기본 랜덤
            Collections.shuffle(candidatePool);
            return candidatePool.stream()
                    .limit(6)
                    .sorted()
                    .collect(Collectors.toList());
        }
        
        int topIndex = random.nextInt(Math.min(10, scoredCombinations.size()));
        return scoredCombinations.get(topIndex).combination;
    }
    
    /**
     * 패턴 일치도 점수 계산
     */
    private double calculatePatternScore(
            List<Integer> combination,
            PatternAnalyzer.PatternStatistics statistics) {
        
        PatternAnalyzer.PatternInfo patternInfo = new PatternAnalyzer.PatternInfo(combination);
        
        double totalScore = 0.0;
        double weightSum = 0.0;
        
        // 총합 점수 (30%)
        double sumScore = 0.0;
        if (patternInfo.getSum() >= statistics.getMinSum() && 
            patternInfo.getSum() <= statistics.getMaxSum()) {
            double avgDiff = Math.abs(patternInfo.getSum() - statistics.getAvgSum());
            double range = statistics.getMaxSum() - statistics.getMinSum();
            sumScore = 1.0 - Math.min(avgDiff / range, 1.0);
        }
        totalScore += sumScore * 0.3;
        weightSum += 0.3;
        
        // 홀짝비 점수 (25%)
        double oddEvenScore = 0.0;
        if (patternInfo.getOddCount() >= statistics.getMinOddCount() && 
            patternInfo.getOddCount() <= statistics.getMaxOddCount()) {
            double avgDiff = Math.abs(patternInfo.getOddCount() - statistics.getAvgOddCount());
            double range = statistics.getMaxOddCount() - statistics.getMinOddCount();
            oddEvenScore = 1.0 - Math.min(avgDiff / range, 1.0);
        }
        totalScore += oddEvenScore * 0.25;
        weightSum += 0.25;
        
        // 고저비 점수 (25%)
        double highLowScore = 0.0;
        if (patternInfo.getHighCount() >= statistics.getMinHighCount() && 
            patternInfo.getHighCount() <= statistics.getMaxHighCount()) {
            double avgDiff = Math.abs(patternInfo.getHighCount() - statistics.getAvgHighCount());
            double range = statistics.getMaxHighCount() - statistics.getMinHighCount();
            highLowScore = 1.0 - Math.min(avgDiff / range, 1.0);
        }
        totalScore += highLowScore * 0.25;
        weightSum += 0.25;
        
        // 연속수 점수 (20%)
        double consecutiveScore = patternInfo.hasConsecutive() ? 
            statistics.getConsecutiveRatio() : (1.0 - statistics.getConsecutiveRatio());
        totalScore += consecutiveScore * 0.2;
        weightSum += 0.2;
        
        return weightSum > 0 ? totalScore / weightSum : 0.0;
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
    
    private PatternAnalyzer.PatternStatistics createDefaultStatistics() {
        List<PatternAnalyzer.PatternInfo> defaultPatterns = new ArrayList<>();
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(1, 5, 12, 23, 28, 35)));
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(3, 8, 15, 22, 31, 40)));
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(2, 10, 18, 25, 33, 42)));
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(4, 11, 19, 27, 36, 44)));
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(6, 14, 21, 29, 38, 45)));
        return new PatternAnalyzer.PatternStatistics(defaultPatterns);
    }
    
    /**
     * 점수가 매겨진 조합
     */
    private static class ScoredCombination {
        final List<Integer> combination;
        final double score;
        
        ScoredCombination(List<Integer> combination, double score) {
            this.combination = combination;
            this.score = score;
        }
    }
}
