package io.appback.lottoguide.domain.generator.preset;

import io.appback.lottoguide.domain.generator.model.Constraints;
import io.appback.lottoguide.domain.generator.preset.util.PatternAnalyzer;
import io.appback.lottoguide.domain.generator.preset.util.WeightedRandomUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AI 시뮬레이션 Preset
 * 여러 조합을 생성하고 다차원 평가를 통해 최적 조합을 선별
 * 통계 기반 지능형 알고리즘으로 AI 수준의 추천 제공
 */
public class AiSimulationPreset implements Preset {
    
    // 시뮬레이션 생성 개수
    private static final int SIMULATION_COUNT = 500;
    
    // 상위 선별 개수
    private static final int TOP_SELECTION_COUNT = 50;
    
    @Override
    public List<Integer> generate(Constraints constraints, Integer windowSize, List<NumberMetrics> metricsList) {
        return generate(constraints, windowSize, metricsList, null);
    }
    
    /**
     * 패턴 통계 캐시를 사용하여 AI 시뮬레이션 기반 생성
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
        
        // 4. 시뮬레이션: 여러 조합 생성
        List<ScoredCombination> scoredCombinations = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < SIMULATION_COUNT; i++) {
            List<Integer> combination;
            
            // 다양한 방법으로 조합 생성 (다양성 확보)
            if (i % 3 == 0 && metricsList != null && !metricsList.isEmpty()) {
                // 가중치 기반 (빈도 우선)
                Map<Integer, Double> weightMap = WeightedRandomUtil.createCombinedWeightMap(
                        metricsList, 0.7, 0.3);
                combination = WeightedRandomUtil.selectByWeight(candidatePool, weightMap, 6);
            } else if (i % 3 == 1 && metricsList != null && !metricsList.isEmpty()) {
                // 가중치 기반 (과거 데이터 우선)
                Map<Integer, Double> weightMap = WeightedRandomUtil.createCombinedWeightMap(
                        metricsList, 0.3, 0.7);
                combination = WeightedRandomUtil.selectByWeight(candidatePool, weightMap, 6);
            } else {
                // 균형 가중치 또는 랜덤
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
            }
            
            // 5. 다차원 평가 점수 계산
            double score = calculateScore(combination, statistics, metricsList);
            scoredCombinations.add(new ScoredCombination(combination, score));
        }
        
        // 6. 점수 기준 정렬하여 상위 조합 선별
        scoredCombinations.sort((a, b) -> Double.compare(b.score, a.score));
        
        // 7. 상위 조합 중 랜덤 선택 (최상위만 선택하지 않고 다양성 확보)
        int topIndex = random.nextInt(Math.min(TOP_SELECTION_COUNT, scoredCombinations.size()));
        return scoredCombinations.get(topIndex).combination;
    }
    
    /**
     * 다차원 평가 점수 계산
     */
    private double calculateScore(
            List<Integer> combination,
            PatternAnalyzer.PatternStatistics statistics,
            List<NumberMetrics> metricsList) {
        
        double totalScore = 0.0;
        double weightSum = 0.0;
        
        // 1. 패턴 일치도 점수 (40%)
        double patternScore = calculatePatternScore(combination, statistics);
        totalScore += patternScore * 0.4;
        weightSum += 0.4;
        
        // 2. 빈도 점수 (30%)
        if (metricsList != null && !metricsList.isEmpty()) {
            double frequencyScore = calculateFrequencyScore(combination, metricsList);
            totalScore += frequencyScore * 0.3;
            weightSum += 0.3;
        }
        
        // 3. 과거 데이터 점수 (20%)
        if (metricsList != null && !metricsList.isEmpty()) {
            double overdueScore = calculateOverdueScore(combination, metricsList);
            totalScore += overdueScore * 0.2;
            weightSum += 0.2;
        }
        
        // 4. 다양성 점수 (10%)
        double diversityScore = calculateDiversityScore(combination);
        totalScore += diversityScore * 0.1;
        weightSum += 0.1;
        
        return weightSum > 0 ? totalScore / weightSum : 0.0;
    }
    
    /**
     * 패턴 일치도 점수 계산
     */
    private double calculatePatternScore(
            List<Integer> combination,
            PatternAnalyzer.PatternStatistics statistics) {
        
        PatternAnalyzer.PatternInfo patternInfo = new PatternAnalyzer.PatternInfo(combination);
        
        // 총합 점수
        double sumScore = 0.0;
        if (patternInfo.getSum() >= statistics.getMinSum() && 
            patternInfo.getSum() <= statistics.getMaxSum()) {
            double avgDiff = Math.abs(patternInfo.getSum() - statistics.getAvgSum());
            double range = statistics.getMaxSum() - statistics.getMinSum();
            sumScore = 1.0 - Math.min(avgDiff / range, 1.0);
        }
        
        // 홀짝비 점수
        double oddEvenScore = 0.0;
        if (patternInfo.getOddCount() >= statistics.getMinOddCount() && 
            patternInfo.getOddCount() <= statistics.getMaxOddCount()) {
            double avgDiff = Math.abs(patternInfo.getOddCount() - statistics.getAvgOddCount());
            double range = statistics.getMaxOddCount() - statistics.getMinOddCount();
            oddEvenScore = 1.0 - Math.min(avgDiff / range, 1.0);
        }
        
        // 고저비 점수
        double highLowScore = 0.0;
        if (patternInfo.getHighCount() >= statistics.getMinHighCount() && 
            patternInfo.getHighCount() <= statistics.getMaxHighCount()) {
            double avgDiff = Math.abs(patternInfo.getHighCount() - statistics.getAvgHighCount());
            double range = statistics.getMaxHighCount() - statistics.getMinHighCount();
            highLowScore = 1.0 - Math.min(avgDiff / range, 1.0);
        }
        
        // 연속수 점수
        double consecutiveScore = patternInfo.hasConsecutive() ? 
            statistics.getConsecutiveRatio() : (1.0 - statistics.getConsecutiveRatio());
        
        // 가중 평균 (총합 30%, 홀짝비 25%, 고저비 25%, 연속수 20%)
        return sumScore * 0.3 + oddEvenScore * 0.25 + highLowScore * 0.25 + consecutiveScore * 0.2;
    }
    
    /**
     * 빈도 점수 계산
     */
    private double calculateFrequencyScore(List<Integer> combination, List<NumberMetrics> metricsList) {
        Map<Integer, NumberMetrics> metricsMap = metricsList.stream()
                .collect(Collectors.toMap(NumberMetrics::number, m -> m));
        
        double totalFrequency = 0.0;
        int count = 0;
        
        for (Integer num : combination) {
            NumberMetrics metrics = metricsMap.get(num);
            if (metrics != null && metrics.frequency() > 0) {
                totalFrequency += metrics.frequency();
                count++;
            }
        }
        
        if (count == 0) return 0.5; // 데이터 없으면 중간 점수
        
        double avgFrequency = totalFrequency / count;
        double maxFrequency = metricsList.stream()
                .mapToDouble(NumberMetrics::frequency)
                .max()
                .orElse(1.0);
        
        return Math.min(avgFrequency / maxFrequency, 1.0);
    }
    
    /**
     * 과거 데이터 점수 계산
     */
    private double calculateOverdueScore(List<Integer> combination, List<NumberMetrics> metricsList) {
        Map<Integer, NumberMetrics> metricsMap = metricsList.stream()
                .collect(Collectors.toMap(NumberMetrics::number, m -> m));
        
        double totalOverdue = 0.0;
        int count = 0;
        
        for (Integer num : combination) {
            NumberMetrics metrics = metricsMap.get(num);
            if (metrics != null && metrics.overdue() > 0) {
                totalOverdue += metrics.overdue();
                count++;
            }
        }
        
        if (count == 0) return 0.5; // 데이터 없으면 중간 점수
        
        double avgOverdue = totalOverdue / count;
        double maxOverdue = metricsList.stream()
                .mapToInt(NumberMetrics::overdue)
                .max()
                .orElse(1);
        
        return Math.min(avgOverdue / maxOverdue, 1.0);
    }
    
    /**
     * 다양성 점수 계산 (번호 분산도)
     */
    private double calculateDiversityScore(List<Integer> combination) {
        Collections.sort(combination);
        
        // 번호 간 간격의 표준편차 계산
        List<Integer> gaps = new ArrayList<>();
        for (int i = 1; i < combination.size(); i++) {
            gaps.add(combination.get(i) - combination.get(i - 1));
        }
        
        if (gaps.isEmpty()) return 0.5;
        
        double avgGap = gaps.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = gaps.stream()
                .mapToDouble(g -> Math.pow(g - avgGap, 2))
                .average()
                .orElse(0.0);
        
        // 적절한 간격이면 높은 점수 (너무 밀집하거나 너무 분산되지 않음)
        double idealGap = 45.0 / 6.0; // 평균 간격
        double gapScore = 1.0 - Math.min(Math.abs(avgGap - idealGap) / idealGap, 1.0);
        
        return gapScore * 0.7 + (1.0 - Math.min(variance / 100.0, 1.0)) * 0.3;
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
