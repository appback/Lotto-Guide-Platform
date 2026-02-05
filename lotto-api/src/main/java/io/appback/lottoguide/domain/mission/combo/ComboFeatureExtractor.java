package io.appback.lottoguide.domain.mission.combo;

import io.appback.lottoguide.domain.mission.tag.ComboTag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 번호 조합 특성 추출기
 * 생성된 6개 번호에서 ComboTag를 추출
 */
@Component
@Slf4j
public class ComboFeatureExtractor {
    
    /**
     * 번호 리스트에서 조합 특성 태그 추출
     * 
     * @param numbers 정렬된 번호 리스트 (1-45)
     * @return 추출된 ComboTag 집합
     */
    public Set<ComboTag> extractComboTags(List<Integer> numbers) {
        if (numbers == null || numbers.size() != 6) {
            log.warn("번호가 6개가 아닙니다: {}", numbers);
            return Set.of();
        }
        
        Set<ComboTag> tags = new HashSet<>();
        
        // 1. 홀짝 분석
        long oddCount = numbers.stream().filter(n -> n % 2 == 1).count();
        long evenCount = 6 - oddCount;
        
        if (oddCount >= 4) {
            tags.add(ComboTag.ODD_HEAVY);
        } else if (evenCount >= 4) {
            tags.add(ComboTag.EVEN_HEAVY);
        } else {
            tags.add(ComboTag.ODD_EVEN_BALANCED);
        }
        
        // 2. 합계 분석
        int sum = numbers.stream().mapToInt(Integer::intValue).sum();
        if (sum >= 150) {
            tags.add(ComboTag.SUM_HIGH);
        } else if (sum <= 100) {
            tags.add(ComboTag.SUM_LOW);
        } else {
            tags.add(ComboTag.SUM_MID);
        }
        
        // 3. 연속 번호 분석
        List<Integer> sorted = new ArrayList<>(numbers);
        sorted.sort(Integer::compareTo);
        boolean hasConsecutive = false;
        for (int i = 0; i < sorted.size() - 1; i++) {
            if (sorted.get(i + 1) - sorted.get(i) == 1) {
                hasConsecutive = true;
                break;
            }
        }
        if (hasConsecutive) {
            tags.add(ComboTag.CONSECUTIVE);
        } else {
            tags.add(ComboTag.NO_CONSECUTIVE);
        }
        
        // 4. 번호대 분석 (저/중/고)
        long lowCount = numbers.stream().filter(n -> n >= 1 && n <= 10).count();
        long midCount = numbers.stream().filter(n -> n >= 11 && n <= 30).count();
        long highCount = numbers.stream().filter(n -> n >= 31 && n <= 45).count();
        
        if (lowCount >= 3) {
            tags.add(ComboTag.LOW_HEAVY);
        } else if (midCount >= 3) {
            tags.add(ComboTag.MID_HEAVY);
        } else if (highCount >= 3) {
            tags.add(ComboTag.HIGH_HEAVY);
        } else {
            tags.add(ComboTag.MIXED);
        }
        
        // 5. 끝자리 패턴 분석
        Set<Integer> endDigits = new HashSet<>();
        for (int num : numbers) {
            endDigits.add(num % 10);
        }
        if (endDigits.size() >= 5) {
            tags.add(ComboTag.END_DIGIT_VARIED);
        } else {
            tags.add(ComboTag.END_DIGIT_CONCENTRATED);
        }
        
        log.debug("번호 조합 특성 추출: {} -> {}", numbers, tags);
        return tags;
    }
}
