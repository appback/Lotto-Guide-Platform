package io.appback.lottoguide.domain.generator.engine;

import io.appback.lottoguide.domain.generator.model.*;
import io.appback.lottoguide.domain.generator.preset.*;
import io.appback.lottoguide.domain.generator.preset.util.PatternAnalyzer;
import org.springframework.stereotype.Component;

import java.util.List;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 번호 생성 엔진
 * Preset 선택, Constraints 적용, Diversity 필터링을 수행
 */
@Component
public class GeneratorEngine {
    
    private final CandidateSelector candidateSelector;
    private final DiversityFilter diversityFilter;
    
    public GeneratorEngine() {
        this.candidateSelector = new CandidateSelector();
        this.diversityFilter = new DiversityFilter();
    }
    
    /**
     * 번호 생성
     * 
     * DB가 없는 경우 (빈 metricsList): 모든 Preset이 랜덤 생성으로 동작
     * - BalancedPreset: 랜덤 선택
     * - FrequentTopPreset: 랜덤 선택 (빈도 데이터 없음)
     * - OverdueTopPreset: 랜덤 선택 (과거 데이터 없음)
     * 
     * @param strategy 생성 전략
     * @param constraints 제약 조건
     * @param count 생성할 세트 개수
     * @param windowSize 윈도우 크기 (20, 50, 100)
     * @param metricsList 메트릭 데이터 리스트 (번호별 빈도, 과거 데이터)
     *                     빈 리스트인 경우 랜덤 생성 모드로 동작
     * @param pastDrawNumbers 과거 당첨 번호 리스트 (사용 안 함, 하위 호환성 유지)
     * @param patternStatisticsCache 패턴 통계 캐시 (Pattern Matcher용, null 가능)
     * @return 생성된 번호 세트 리스트
     */
    public List<GeneratedSet> generate(
            Strategy strategy,
            Constraints constraints,
            int count,
            Integer windowSize,
            List<Preset.NumberMetrics> metricsList,
            List<List<Integer>> pastDrawNumbers,
            io.appback.lottoguide.infra.refresh.PatternStatisticsCache patternStatisticsCache) {
        
        // 1. Preset 선택
        Preset preset = selectPreset(strategy);
        
        // 2. 각 세트 생성
        // metricsList가 비어있으면 (데이터 없음) 랜덤 생성, 있으면 메트릭 기반 생성
        List<List<Integer>> rawSets = new ArrayList<>();
        
        // Wheeling System의 경우 특별 처리
        if (strategy == Strategy.WHEELING_SYSTEM) {
            WheelingSystemPreset wheelingPreset = (WheelingSystemPreset) preset;
            // 요청한 개수만큼 조합 생성 (5등 보장을 위해서는 14개 권장)
            rawSets = wheelingPreset.generateWheelingSets(metricsList, count);
        } else if (strategy == Strategy.PATTERN_MATCHER) {
            // Pattern Matcher의 경우 캐시된 패턴 통계 사용
            PatternMatcherPreset patternPreset = (PatternMatcherPreset) preset;
            for (int i = 0; i < count; i++) {
                List<Integer> numbers = patternPreset.generate(
                        constraints, windowSize, metricsList, patternStatisticsCache);
                rawSets.add(numbers);
            }
        } else if (strategy == Strategy.AI_SIMULATION) {
            // AI Simulation의 경우 캐시된 패턴 통계 사용
            AiSimulationPreset aiPreset = (AiSimulationPreset) preset;
            for (int i = 0; i < count; i++) {
                List<Integer> numbers = aiPreset.generate(
                        constraints, windowSize, metricsList, patternStatisticsCache);
                rawSets.add(numbers);
            }
        } else if (strategy == Strategy.AI_PATTERN_REASONER) {
            // AI Pattern Reasoner의 경우 캐시된 패턴 통계 사용
            AiPatternReasonerPreset aiPreset = (AiPatternReasonerPreset) preset;
            for (int i = 0; i < count; i++) {
                List<Integer> numbers = aiPreset.generate(
                        constraints, windowSize, metricsList, patternStatisticsCache);
                rawSets.add(numbers);
            }
        } else if (strategy == Strategy.AI_DECISION_FILTER) {
            // AI Decision Filter의 경우 일반 생성
            AiDecisionFilterPreset aiPreset = (AiDecisionFilterPreset) preset;
            for (int i = 0; i < count; i++) {
                List<Integer> numbers = aiPreset.generate(
                        constraints, windowSize, metricsList);
                rawSets.add(numbers);
            }
        } else if (strategy == Strategy.AI_WEIGHT_EVOLUTION) {
            // AI Weight Evolution의 경우 캐시된 패턴 통계 사용
            AiWeightEvolutionPreset aiPreset = (AiWeightEvolutionPreset) preset;
            for (int i = 0; i < count; i++) {
                List<Integer> numbers = aiPreset.generate(
                        constraints, windowSize, metricsList, patternStatisticsCache);
                rawSets.add(numbers);
            }
        } else {
            for (int i = 0; i < count; i++) {
                // metricsList를 Preset에 전달 (비어있으면 랜덤 생성)
                List<Integer> numbers = preset.generate(constraints, windowSize, metricsList);
                rawSets.add(numbers);
            }
        }
        
        // 3. Diversity 필터링
        if (constraints != null && constraints.getSimilarityThreshold() != null) {
            rawSets = diversityFilter.filter(rawSets, constraints.getSimilarityThreshold());
        }
        
        // 4. 중복 제거
        rawSets = diversityFilter.removeDuplicates(rawSets);
        
        // 5. GeneratedSet 객체 생성
        List<GeneratedSet> generatedSets = new ArrayList<>();
        for (int i = 0; i < rawSets.size(); i++) {
            GeneratedSet set = GeneratedSet.builder()
                .index(i)
                .numbers(rawSets.get(i))
                .strategy(strategy)
                .constraints(constraints)
                .createdAt(LocalDateTime.now())
                .build();
            generatedSets.add(set);
        }
        
        return generatedSets;
    }
    
    /**
     * 전략에 따른 Preset 선택
     */
    private Preset selectPreset(Strategy strategy) {
        return switch (strategy) {
            case FREQUENT_TOP -> new FrequentTopPreset();
            case OVERDUE_TOP -> new OverdueTopPreset();
            case BALANCED -> new BalancedPreset();
            case WHEELING_SYSTEM -> new WheelingSystemPreset();
            case WEIGHTED_RANDOM -> new WeightedRandomPreset();
            case PATTERN_MATCHER -> new PatternMatcherPreset();
            case AI_SIMULATION -> new AiSimulationPreset();
            case AI_PATTERN_REASONER -> new AiPatternReasonerPreset();
            case AI_DECISION_FILTER -> new AiDecisionFilterPreset();
            case AI_WEIGHT_EVOLUTION -> new AiWeightEvolutionPreset();
        };
    }
}
