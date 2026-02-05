package io.appback.lottoguide.domain.generator.preset.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 과거 당첨 데이터 패턴 분석 유틸리티
 * 공통 로직을 제공하여 Pattern Matcher에서 사용
 */
public class PatternAnalyzer {
    
    /**
     * 당첨 번호 조합의 패턴 정보
     */
    public static class PatternInfo {
        private final int sum;                    // 총합
        private final int oddCount;                // 홀수 개수
        private final int evenCount;               // 짝수 개수
        private final int highCount;               // 고번호 개수 (31~45)
        private final int lowCount;                // 저번호 개수 (1~30)
        private final boolean hasConsecutive;      // 연속수 포함 여부
        private final int maxConsecutiveLength;    // 최대 연속수 길이
        
        public PatternInfo(List<Integer> numbers) {
            this.sum = numbers.stream().mapToInt(Integer::intValue).sum();
            this.oddCount = (int) numbers.stream().filter(n -> n % 2 == 1).count();
            this.evenCount = numbers.size() - oddCount;
            this.highCount = (int) numbers.stream().filter(n -> n >= 31).count();
            this.lowCount = numbers.size() - highCount;
            
            // 연속수 체크
            List<Integer> sorted = new ArrayList<>(numbers);
            Collections.sort(sorted);
            boolean hasConsecutive = false;
            int maxConsecutive = 1;
            int currentConsecutive = 1;
            
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i) == sorted.get(i - 1) + 1) {
                    currentConsecutive++;
                    hasConsecutive = true;
                    maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
                } else {
                    currentConsecutive = 1;
                }
            }
            
            this.hasConsecutive = hasConsecutive;
            this.maxConsecutiveLength = maxConsecutive;
        }
        
        public int getSum() { return sum; }
        public int getOddCount() { return oddCount; }
        public int getEvenCount() { return evenCount; }
        public int getHighCount() { return highCount; }
        public int getLowCount() { return lowCount; }
        public boolean hasConsecutive() { return hasConsecutive; }
        public int getMaxConsecutiveLength() { return maxConsecutiveLength; }
    }
    
    /**
     * 과거 당첨 데이터의 패턴 통계
     */
    public static class PatternStatistics {
        private final int minSum;
        private final int maxSum;
        private final double avgSum;
        private final int minOddCount;
        private final int maxOddCount;
        private final double avgOddCount;
        private final int minHighCount;
        private final int maxHighCount;
        private final double avgHighCount;
        private final double consecutiveRatio;  // 연속수 포함 비율
        private final int maxConsecutiveLength;  // 최대 연속수 길이
        
        public PatternStatistics(List<PatternInfo> patterns) {
            if (patterns == null || patterns.isEmpty()) {
                // 기본값 설정
                this.minSum = 100;
                this.maxSum = 200;
                this.avgSum = 150;
                this.minOddCount = 2;
                this.maxOddCount = 4;
                this.avgOddCount = 3;
                this.minHighCount = 2;
                this.maxHighCount = 4;
                this.avgHighCount = 3;
                this.consecutiveRatio = 0.5;
                this.maxConsecutiveLength = 3;
                return;
            }
            
            // 총합 통계
            List<Integer> sums = patterns.stream()
                    .map(PatternInfo::getSum)
                    .collect(Collectors.toList());
            this.minSum = Collections.min(sums);
            this.maxSum = Collections.max(sums);
            this.avgSum = sums.stream().mapToInt(Integer::intValue).average().orElse(150);
            
            // 홀수 개수 통계
            List<Integer> oddCounts = patterns.stream()
                    .map(PatternInfo::getOddCount)
                    .collect(Collectors.toList());
            this.minOddCount = Collections.min(oddCounts);
            this.maxOddCount = Collections.max(oddCounts);
            this.avgOddCount = oddCounts.stream().mapToInt(Integer::intValue).average().orElse(3);
            
            // 고번호 개수 통계
            List<Integer> highCounts = patterns.stream()
                    .map(PatternInfo::getHighCount)
                    .collect(Collectors.toList());
            this.minHighCount = Collections.min(highCounts);
            this.maxHighCount = Collections.max(highCounts);
            this.avgHighCount = highCounts.stream().mapToInt(Integer::intValue).average().orElse(3);
            
            // 연속수 통계
            long consecutiveCount = patterns.stream()
                    .filter(PatternInfo::hasConsecutive)
                    .count();
            this.consecutiveRatio = (double) consecutiveCount / patterns.size();
            this.maxConsecutiveLength = patterns.stream()
                    .mapToInt(PatternInfo::getMaxConsecutiveLength)
                    .max()
                    .orElse(3);
        }
        
        public int getMinSum() { return minSum; }
        public int getMaxSum() { return maxSum; }
        public double getAvgSum() { return avgSum; }
        public int getMinOddCount() { return minOddCount; }
        public int getMaxOddCount() { return maxOddCount; }
        public double getAvgOddCount() { return avgOddCount; }
        public int getMinHighCount() { return minHighCount; }
        public int getMaxHighCount() { return maxHighCount; }
        public double getAvgHighCount() { return avgHighCount; }
        public double getConsecutiveRatio() { return consecutiveRatio; }
        public int getMaxConsecutiveLength() { return maxConsecutiveLength; }
    }
    
    /**
     * 번호 리스트로부터 패턴 정보 생성
     */
    public static PatternInfo analyzePattern(List<Integer> numbers) {
        return new PatternInfo(numbers);
    }
    
    /**
     * 여러 번호 조합으로부터 패턴 통계 생성
     */
    public static PatternStatistics analyzeStatistics(List<List<Integer>> numberSets) {
        List<PatternInfo> patterns = numberSets.stream()
                .map(PatternAnalyzer::analyzePattern)
                .collect(Collectors.toList());
        return new PatternStatistics(patterns);
    }
    
    /**
     * 생성된 조합이 과거 당첨 패턴과 일치하는지 검증
     * 
     * @param numbers 생성된 번호 조합
     * @param statistics 과거 당첨 데이터 통계
     * @param threshold 일치도 임계값 (0.0 ~ 1.0, 높을수록 엄격)
     * @return 패턴 일치 여부
     */
    public static boolean matchesPattern(
            List<Integer> numbers,
            PatternStatistics statistics,
            double threshold) {
        
        PatternInfo pattern = analyzePattern(numbers);
        
        // 각 패턴 요소별 점수 계산 (0.0 ~ 1.0)
        double score = 0.0;
        int criteriaCount = 0;
        
        // 1. 총합 검증 (30% 가중치)
        double sumScore = calculateSumScore(pattern.getSum(), statistics);
        score += sumScore * 0.3;
        criteriaCount++;
        
        // 2. 홀짝비 검증 (25% 가중치)
        double oddEvenScore = calculateOddEvenScore(pattern.getOddCount(), statistics);
        score += oddEvenScore * 0.25;
        criteriaCount++;
        
        // 3. 고저비 검증 (25% 가중치)
        double highLowScore = calculateHighLowScore(pattern.getHighCount(), statistics);
        score += highLowScore * 0.25;
        criteriaCount++;
        
        // 4. 연속수 검증 (20% 가중치)
        double consecutiveScore = calculateConsecutiveScore(
                pattern.hasConsecutive(),
                pattern.getMaxConsecutiveLength(),
                statistics);
        score += consecutiveScore * 0.2;
        criteriaCount++;
        
        // 평균 점수가 임계값 이상이면 패턴 일치
        return score >= threshold;
    }
    
    /**
     * 총합 점수 계산
     */
    private static double calculateSumScore(int sum, PatternStatistics stats) {
        if (sum < stats.getMinSum() || sum > stats.getMaxSum()) {
            return 0.0; // 범위 밖이면 0점
        }
        
        // 평균에 가까울수록 높은 점수
        double distanceFromAvg = Math.abs(sum - stats.getAvgSum());
        double range = stats.getMaxSum() - stats.getMinSum();
        return Math.max(0.0, 1.0 - (distanceFromAvg / range));
    }
    
    /**
     * 홀짝비 점수 계산
     */
    private static double calculateOddEvenScore(int oddCount, PatternStatistics stats) {
        if (oddCount < stats.getMinOddCount() || oddCount > stats.getMaxOddCount()) {
            return 0.0;
        }
        
        double distanceFromAvg = Math.abs(oddCount - stats.getAvgOddCount());
        double range = stats.getMaxOddCount() - stats.getMinOddCount();
        return Math.max(0.0, 1.0 - (distanceFromAvg / range));
    }
    
    /**
     * 고저비 점수 계산
     */
    private static double calculateHighLowScore(int highCount, PatternStatistics stats) {
        if (highCount < stats.getMinHighCount() || highCount > stats.getMaxHighCount()) {
            return 0.0;
        }
        
        double distanceFromAvg = Math.abs(highCount - stats.getAvgHighCount());
        double range = stats.getMaxHighCount() - stats.getMinHighCount();
        return Math.max(0.0, 1.0 - (distanceFromAvg / range));
    }
    
    /**
     * 연속수 점수 계산
     */
    private static double calculateConsecutiveScore(
            boolean hasConsecutive,
            int maxConsecutiveLength,
            PatternStatistics stats) {
        
        // 연속수 포함 비율이 높으면 연속수가 있어야 하고, 낮으면 없어야 함
        if (stats.getConsecutiveRatio() > 0.5) {
            // 대부분 연속수 포함 -> 연속수가 있으면 점수 높음
            if (!hasConsecutive) {
                return 0.3; // 연속수 없으면 낮은 점수
            }
            // 연속수 길이가 통계 범위 내면 높은 점수
            if (maxConsecutiveLength <= stats.getMaxConsecutiveLength()) {
                return 1.0;
            } else {
                return 0.5; // 너무 긴 연속수면 중간 점수
            }
        } else {
            // 대부분 연속수 없음 -> 연속수가 없으면 점수 높음
            if (!hasConsecutive) {
                return 1.0;
            } else {
                // 짧은 연속수면 중간 점수, 긴 연속수면 낮은 점수
                return maxConsecutiveLength <= 2 ? 0.5 : 0.2;
            }
        }
    }
}
