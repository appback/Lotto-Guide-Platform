package io.appback.lottoguide.domain.generator.preset;

import io.appback.lottoguide.domain.generator.model.Constraints;
import io.appback.lottoguide.domain.generator.preset.util.PatternAnalyzer;
import io.appback.lottoguide.domain.generator.preset.util.WeightedRandomUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 패턴 필터링 Preset
 * 과거 당첨 데이터의 패턴을 학습하여, 그 패턴과 일치하는 조합만 생성
 * 
 * 주의: 이 Preset은 DrawRepositoryPort를 통해 과거 당첨 데이터에 접근해야 하므로,
 * GeneratorEngine에서 DrawRepositoryPort를 주입받아 전달해야 합니다.
 * 현재는 metricsList만 사용하여 간단한 패턴 분석을 수행합니다.
 */
public class PatternMatcherPreset implements Preset {
    
    // 패턴 일치도 임계값 (0.0 ~ 1.0, 높을수록 엄격)
    private static final double PATTERN_THRESHOLD = 0.6;
    
    // 최대 시도 횟수
    private static final int MAX_ATTEMPTS = 1000;
    
    @Override
    public List<Integer> generate(Constraints constraints, Integer windowSize, List<NumberMetrics> metricsList) {
        // 과거 당첨 데이터 없이 호출 (기본 패턴 통계 사용)
        return generate(constraints, windowSize, metricsList, null);
    }
    
    /**
     * 패턴 통계 캐시를 사용하여 패턴 기반 생성
     */
    public List<Integer> generate(
            Constraints constraints,
            Integer windowSize,
            List<NumberMetrics> metricsList,
            io.appback.lottoguide.infra.refresh.PatternStatisticsCache patternStatisticsCache) {
        // 1. 후보 번호 풀 생성 (1~45)
        List<Integer> candidatePool = IntStream.rangeClosed(1, 45)
            .boxed()
            .collect(Collectors.toList());
        
        // 2. 제약 조건 적용
        candidatePool = applyConstraints(candidatePool, constraints);
        
        if (candidatePool.size() < 6) {
            candidatePool = IntStream.rangeClosed(1, 45).boxed().collect(Collectors.toList());
        }
        
        // 3. 패턴 기반 선택
        return selectByPattern(candidatePool, metricsList, patternStatisticsCache, windowSize, 6);
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
    
    /**
     * 패턴 기반 선택
     * 과거 당첨 패턴과 일치하는 조합을 생성
     */
    private List<Integer> selectByPattern(
            List<Integer> candidates,
            List<NumberMetrics> metricsList,
            io.appback.lottoguide.infra.refresh.PatternStatisticsCache patternStatisticsCache,
            Integer windowSize,
            int count) {
        
        // 패턴 통계 조회 (캐시 우선)
        PatternAnalyzer.PatternStatistics statistics;
        if (patternStatisticsCache != null) {
            // 캐시된 패턴 통계 사용 (데이터 추가 시에만 재계산됨)
            statistics = patternStatisticsCache.getPatternStatistics(windowSize);
        } else {
            // 캐시가 없으면 기본 패턴 통계 사용
            statistics = createDefaultStatistics();
        }
        
        // metricsList가 있으면 가중치 기반으로 생성하고 패턴 검증
        // 없으면 일반 랜덤으로 생성하고 패턴 검증
        Random random = new Random();
        int attempts = 0;
        
        while (attempts < MAX_ATTEMPTS) {
            List<Integer> candidate;
            
            if (metricsList != null && !metricsList.isEmpty()) {
                // 가중치 기반 랜덤 추출
                Map<Integer, Double> weightMap = WeightedRandomUtil.createCombinedWeightMap(
                        metricsList, 0.5, 0.5);
                candidate = WeightedRandomUtil.selectByWeight(candidates, weightMap, count);
            } else {
                // 일반 랜덤 추출
                Collections.shuffle(candidates);
                candidate = candidates.stream()
                        .limit(count)
                        .sorted()
                        .collect(Collectors.toList());
            }
            
            // 패턴 검증
            if (PatternAnalyzer.matchesPattern(candidate, statistics, PATTERN_THRESHOLD)) {
                return candidate;
            }
            
            attempts++;
        }
        
        // 최대 시도 횟수 내에 패턴 일치 조합을 찾지 못하면 마지막 생성 조합 반환
        Collections.shuffle(candidates);
        return candidates.stream()
                .limit(count)
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * 기본 패턴 통계 생성
     * 과거 당첨 데이터가 없을 경우 사용하는 기본값
     * 실제 로또 당첨 번호의 일반적인 패턴을 반영
     */
    private PatternAnalyzer.PatternStatistics createDefaultStatistics() {
        // 실제 로또 당첨 번호의 일반적인 패턴:
        // - 총합: 100~200 (평균 150)
        // - 홀수: 2~4개 (평균 3개)
        // - 고번호(31~45): 2~4개 (평균 3개)
        // - 연속수: 약 50% 확률로 포함
        
        List<PatternAnalyzer.PatternInfo> defaultPatterns = new ArrayList<>();
        
        // 예시 패턴들 (실제 데이터가 없을 때 사용)
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(1, 5, 12, 23, 28, 35)));
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(3, 8, 15, 22, 31, 40)));
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(2, 10, 18, 25, 33, 42)));
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(4, 11, 19, 27, 36, 44)));
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(6, 14, 21, 29, 38, 45)));
        
        return new PatternAnalyzer.PatternStatistics(defaultPatterns);
    }
}
