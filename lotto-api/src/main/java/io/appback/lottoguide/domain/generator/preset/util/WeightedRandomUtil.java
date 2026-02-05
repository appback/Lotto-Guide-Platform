package io.appback.lottoguide.domain.generator.preset.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 가중치 기반 랜덤 추출 유틸리티
 * 공통 로직을 제공하여 여러 Preset에서 재사용
 */
public class WeightedRandomUtil {
    
    /**
     * 가중치 맵을 기반으로 랜덤하게 번호를 추출
     * 
     * @param candidates 후보 번호 리스트
     * @param weightMap 번호별 가중치 맵 (번호 -> 가중치)
     * @param count 추출할 개수
     * @return 추출된 번호 리스트 (정렬됨)
     */
    public static List<Integer> selectByWeight(
            List<Integer> candidates,
            Map<Integer, Double> weightMap,
            int count) {
        
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (weightMap == null || weightMap.isEmpty()) {
            // 가중치가 없으면 일반 랜덤 선택
            Collections.shuffle(candidates);
            return candidates.stream()
                    .limit(count)
                    .sorted()
                    .collect(Collectors.toList());
        }
        
        Random random = new Random();
        List<Integer> selected = new ArrayList<>();
        List<Integer> remainingCandidates = new ArrayList<>(candidates);
        
        // count개만큼 가중치 기반으로 추출
        for (int i = 0; i < count && !remainingCandidates.isEmpty(); i++) {
            // 총 가중치 계산
            double totalWeight = remainingCandidates.stream()
                    .mapToDouble(num -> weightMap.getOrDefault(num, 0.1))
                    .sum();
            
            if (totalWeight <= 0) {
                // 가중치가 모두 0 이하면 일반 랜덤 선택
                Collections.shuffle(remainingCandidates);
                selected.add(remainingCandidates.remove(0));
                continue;
            }
            
            // 랜덤 값 생성 (0 ~ totalWeight)
            double randomValue = random.nextDouble() * totalWeight;
            
            // 가중치 누적합을 따라 번호 선택
            double cumulativeWeight = 0.0;
            Integer selectedNumber = null;
            
            for (Integer num : remainingCandidates) {
                double weight = weightMap.getOrDefault(num, 0.1);
                cumulativeWeight += weight;
                
                if (randomValue <= cumulativeWeight) {
                    selectedNumber = num;
                    break;
                }
            }
            
            // 선택된 번호가 없으면 마지막 번호 선택 (안전장치)
            if (selectedNumber == null) {
                selectedNumber = remainingCandidates.get(remainingCandidates.size() - 1);
            }
            
            selected.add(selectedNumber);
            remainingCandidates.remove(selectedNumber);
        }
        
        return selected.stream().sorted().collect(Collectors.toList());
    }
    
    /**
     * 빈도 기반 가중치 맵 생성
     * 
     * @param metricsList 메트릭 데이터 리스트
     * @return 번호별 가중치 맵 (빈도가 높을수록 높은 가중치)
     */
    public static Map<Integer, Double> createFrequencyWeightMap(
            List<io.appback.lottoguide.domain.generator.preset.Preset.NumberMetrics> metricsList) {
        
        Map<Integer, Double> weightMap = new HashMap<>();
        
        if (metricsList == null || metricsList.isEmpty()) {
            return weightMap;
        }
        
        for (io.appback.lottoguide.domain.generator.preset.Preset.NumberMetrics metrics : metricsList) {
            // 빈도가 높을수록 높은 가중치 (최소 0.1로 설정하여 선택 가능하게)
            double weight = Math.max(0.1, metrics.frequency() + 1.0);
            weightMap.put(metrics.number(), weight);
        }
        
        return weightMap;
    }
    
    /**
     * 과거 데이터(overdue) 기반 가중치 맵 생성
     * 
     * @param metricsList 메트릭 데이터 리스트
     * @return 번호별 가중치 맵 (overdue가 클수록 높은 가중치)
     */
    public static Map<Integer, Double> createOverdueWeightMap(
            List<io.appback.lottoguide.domain.generator.preset.Preset.NumberMetrics> metricsList) {
        
        Map<Integer, Double> weightMap = new HashMap<>();
        
        if (metricsList == null || metricsList.isEmpty()) {
            return weightMap;
        }
        
        for (io.appback.lottoguide.domain.generator.preset.Preset.NumberMetrics metrics : metricsList) {
            // overdue가 클수록 (오래 안 나온 번호일수록) 높은 가중치
            // overdue가 0이면 최근에 나온 번호이므로 낮은 가중치
            double weight = Math.max(0.1, metrics.overdue() + 1.0);
            weightMap.put(metrics.number(), weight);
        }
        
        return weightMap;
    }
    
    /**
     * 빈도와 과거 데이터를 결합한 가중치 맵 생성
     * 
     * @param metricsList 메트릭 데이터 리스트
     * @param frequencyWeight 빈도 가중치 비율 (0.0 ~ 1.0)
     * @param overdueWeight 과거 데이터 가중치 비율 (0.0 ~ 1.0)
     * @return 번호별 가중치 맵
     */
    public static Map<Integer, Double> createCombinedWeightMap(
            List<io.appback.lottoguide.domain.generator.preset.Preset.NumberMetrics> metricsList,
            double frequencyWeight,
            double overdueWeight) {
        
        Map<Integer, Double> weightMap = new HashMap<>();
        
        if (metricsList == null || metricsList.isEmpty()) {
            return weightMap;
        }
        
        // 정규화를 위한 최대값 계산
        int maxFrequency = metricsList.stream()
                .mapToInt(io.appback.lottoguide.domain.generator.preset.Preset.NumberMetrics::frequency)
                .max()
                .orElse(1);
        
        int maxOverdue = metricsList.stream()
                .mapToInt(io.appback.lottoguide.domain.generator.preset.Preset.NumberMetrics::overdue)
                .max()
                .orElse(1);
        
        for (io.appback.lottoguide.domain.generator.preset.Preset.NumberMetrics metrics : metricsList) {
            // 정규화된 빈도 점수 (0.0 ~ 1.0)
            double normalizedFrequency = maxFrequency > 0 
                    ? (double) metrics.frequency() / maxFrequency 
                    : 0.0;
            
            // 정규화된 overdue 점수 (0.0 ~ 1.0)
            double normalizedOverdue = maxOverdue > 0 
                    ? (double) metrics.overdue() / maxOverdue 
                    : 0.0;
            
            // 가중치 결합 (최소 0.1로 설정)
            double combinedWeight = frequencyWeight * normalizedFrequency 
                    + overdueWeight * normalizedOverdue;
            weightMap.put(metrics.number(), Math.max(0.1, combinedWeight + 0.1));
        }
        
        return weightMap;
    }
}
