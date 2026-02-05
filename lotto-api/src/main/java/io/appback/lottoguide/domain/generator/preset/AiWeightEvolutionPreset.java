package io.appback.lottoguide.domain.generator.preset;

import io.appback.lottoguide.domain.generator.model.Constraints;
import io.appback.lottoguide.domain.generator.preset.util.PatternAnalyzer;
import io.appback.lottoguide.domain.generator.preset.util.WeightedRandomUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AI 가중치 진화 Preset
 * AI가 빈도·과거 데이터 가중치를 스스로 조정
 * 패턴 적합도에 따라 가중치 변화하는 적응형 전략
 */
public class AiWeightEvolutionPreset implements Preset {
    
    // 시뮬레이션 생성 개수
    private static final int SIMULATION_COUNT = 400;
    
    // 상위 선별 개수
    private static final int TOP_SELECTION_COUNT = 30;
    
    @Override
    public List<Integer> generate(Constraints constraints, Integer windowSize, List<NumberMetrics> metricsList) {
        return generate(constraints, windowSize, metricsList, null);
    }
    
    /**
     * 패턴 통계 캐시를 사용하여 AI 가중치 진화 기반 생성
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
        
        // 4. 적응형 가중치 계산 (패턴 적합도 기반)
        double[] optimalWeights = calculateOptimalWeights(statistics, metricsList);
        double frequencyWeight = optimalWeights[0];
        double overdueWeight = optimalWeights[1];
        
        // 5. 시뮬레이션: 여러 조합 생성
        List<ScoredCombination> scoredCombinations = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < SIMULATION_COUNT; i++) {
            List<Integer> combination;
            
            // 적응형 가중치를 사용하여 조합 생성
            if (metricsList != null && !metricsList.isEmpty()) {
                Map<Integer, Double> weightMap = WeightedRandomUtil.createCombinedWeightMap(
                        metricsList, frequencyWeight, overdueWeight);
                combination = WeightedRandomUtil.selectByWeight(candidatePool, weightMap, 6);
            } else {
                Collections.shuffle(candidatePool);
                combination = candidatePool.stream()
                        .limit(6)
                        .sorted()
                        .collect(Collectors.toList());
            }
            
            // 6. 다차원 평가 점수 계산
            double score = calculateScore(combination, statistics, metricsList);
            scoredCombinations.add(new ScoredCombination(combination, score));
        }
        
        // 7. 점수 기준 정렬하여 상위 조합 선별
        scoredCombinations.sort((a, b) -> Double.compare(b.score, a.score));
        
        // 8. 상위 조합 중 랜덤 선택
        int topIndex = random.nextInt(Math.min(TOP_SELECTION_COUNT, scoredCombinations.size()));
        return scoredCombinations.get(topIndex).combination;
    }
    
    /**
     * 패턴 적합도 기반 최적 가중치 계산
     * @return [frequencyWeight, overdueWeight]
     */
    private double[] calculateOptimalWeights(
            PatternAnalyzer.PatternStatistics statistics,
            List<NumberMetrics> metricsList) {
        
        // 기본 가중치 (50:50)
        double frequencyWeight = 0.5;
        double overdueWeight = 0.5;
        
        if (metricsList == null || metricsList.isEmpty()) {
            return new double[]{frequencyWeight, overdueWeight};
        }
        
        // 패턴 통계를 기반으로 가중치 조정
        // 총합이 평균보다 작으면 과거 데이터에 더 가중치
        // 총합이 평균보다 크면 빈도에 더 가중치
        double avgSum = statistics.getAvgSum();
        double midSum = 135.0; // 1~45 평균 합계
        
        if (avgSum < midSum) {
            // 평균보다 작으면 과거 데이터 강조
            frequencyWeight = 0.3;
            overdueWeight = 0.7;
        } else if (avgSum > midSum) {
            // 평균보다 크면 빈도 강조
            frequencyWeight = 0.7;
            overdueWeight = 0.3;
        }
        
        return new double[]{frequencyWeight, overdueWeight};
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
        
        // 가중 평균
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
        
        if (count == 0) return 0.5;
        
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
        
        if (count == 0) return 0.5;
        
        double avgOverdue = totalOverdue / count;
        double maxOverdue = metricsList.stream()
                .mapToInt(NumberMetrics::overdue)
                .max()
                .orElse(1);
        
        return Math.min(avgOverdue / maxOverdue, 1.0);
    }
    
    /**
     * 다양성 점수 계산
     */
    private double calculateDiversityScore(List<Integer> combination) {
        Collections.sort(combination);
        
        List<Integer> gaps = new ArrayList<>();
        for (int i = 1; i < combination.size(); i++) {
            gaps.add(combination.get(i) - combination.get(i - 1));
        }
        
        if (gaps.isEmpty()) return 0.5;
        
        double avgGap = gaps.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double idealGap = 45.0 / 6.0;
        double gapScore = 1.0 - Math.min(Math.abs(avgGap - idealGap) / idealGap, 1.0);
        
        return gapScore;
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
