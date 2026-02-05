package io.appback.lottoguide.domain.generator.engine;

import java.util.*;

/**
 * 유사도 기반 다양성 필터
 * 유사한 번호 세트를 제거하여 다양성 확보
 */
public class DiversityFilter {
    
    /**
     * 유사도 임계값 기반 필터링
     * @param sets 생성된 번호 세트 리스트
     * @param similarityThreshold 유사도 임계값 (0.0 ~ 1.0)
     * @return 필터링된 세트 리스트
     */
    public List<List<Integer>> filter(List<List<Integer>> sets, Double similarityThreshold) {
        if (similarityThreshold == null || similarityThreshold <= 0.0) {
            return sets;
        }
        
        List<List<Integer>> filtered = new ArrayList<>();
        
        for (List<Integer> set : sets) {
            boolean isSimilar = false;
            
            for (List<Integer> existing : filtered) {
                double similarity = calculateSimilarity(set, existing);
                if (similarity >= similarityThreshold) {
                    isSimilar = true;
                    break;
                }
            }
            
            if (!isSimilar) {
                filtered.add(new ArrayList<>(set));
            }
        }
        
        return filtered;
    }
    
    /**
     * 두 세트 간 유사도 계산 (Jaccard 유사도)
     * @return 0.0 ~ 1.0 사이의 유사도 값
     */
    private double calculateSimilarity(List<Integer> set1, List<Integer> set2) {
        Set<Integer> union = new HashSet<>(set1);
        union.addAll(set2);
        
        Set<Integer> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        if (union.isEmpty()) {
            return 0.0;
        }
        
        return (double) intersection.size() / (double) union.size();
    }
    
    /**
     * 중복 세트 제거
     */
    public List<List<Integer>> removeDuplicates(List<List<Integer>> sets) {
        Set<String> seen = new HashSet<>();
        List<List<Integer>> unique = new ArrayList<>();
        
        for (List<Integer> set : sets) {
            List<Integer> sorted = new ArrayList<>(set);
            Collections.sort(sorted);
            String key = sorted.toString();
            
            if (!seen.contains(key)) {
                seen.add(key);
                unique.add(new ArrayList<>(set));
            }
        }
        
        return unique;
    }
}
