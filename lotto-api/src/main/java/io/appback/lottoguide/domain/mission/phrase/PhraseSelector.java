package io.appback.lottoguide.domain.mission.phrase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appback.lottoguide.domain.generator.model.Strategy;
import io.appback.lottoguide.domain.mission.combo.ComboFeatureExtractor;
import io.appback.lottoguide.domain.mission.tag.AlignTag;
import io.appback.lottoguide.domain.mission.tag.ComboTag;
import io.appback.lottoguide.domain.mission.tag.ToneTag;
import io.appback.lottoguide.domain.mission.tag.ZodiacTag;
import io.appback.lottoguide.infra.persistence.entity.MissionPhraseAEntity;
import io.appback.lottoguide.infra.persistence.entity.MissionPhraseBEntity;
import io.appback.lottoguide.infra.persistence.entity.MissionPhraseCEntity;
import io.appback.lottoguide.infra.persistence.repository.MissionPhraseARepository;
import io.appback.lottoguide.infra.persistence.repository.MissionPhraseBRepository;
import io.appback.lottoguide.infra.persistence.repository.MissionPhraseCRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A/B/C 멘트 선택 서비스
 * 
 * 가중치 기반 선택 로직:
 * - A: 번호 조합/전략 65%, 별자리 25%, 랜덤 10%
 * - B: A에 종속, alignTags/avoidTags 기반 필터링
 * - C: 완전 랜덤
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhraseSelector {
    
    private final MissionPhraseARepository phraseARepository;
    private final MissionPhraseBRepository phraseBRepository;
    private final MissionPhraseCRepository phraseCRepository;
    private final ComboFeatureExtractor comboFeatureExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 중복 방지 설정 (프론트엔드에서 관리)
    private static final int A_DUPLICATE_DAYS = 7;  // 최근 7일 동안 A 중복 최소화
    private static final int B_DUPLICATE_COUNT = 5;  // 최근 5회 동안 B 중복 강력 회피
    
    /**
     * A/B/C 멘트 선택
     * 
     * @param strategy 사용자가 선택한 생성 전략
     * @param numbers 생성된 6개 번호
     * @param zodiacSign 별자리 (선택적)
     * @param excludePhraseAIds 제외할 A 멘트 ID 목록 (프론트엔드 히스토리)
     * @param excludePhraseBIds 제외할 B 멘트 ID 목록 (프론트엔드 히스토리)
     * @return 선택된 A/B/C 멘트 (ID 포함)
     */
    public SelectedPhrases selectPhrases(Strategy strategy, List<Integer> numbers, String zodiacSign,
                                         List<Long> excludePhraseAIds, List<Long> excludePhraseBIds) {
        // 1. 번호 조합 특성 추출
        Set<ComboTag> comboTags = comboFeatureExtractor.extractComboTags(numbers);
        
        // 2. A 선택 (제외 목록 적용)
        MissionPhraseAEntity phraseA = selectPhraseA(strategy, comboTags, zodiacSign, excludePhraseAIds);
        
        // 3. B 선택 (A에 종속, 제외 목록 적용)
        MissionPhraseBEntity phraseB = selectPhraseB(phraseA, excludePhraseBIds);
        
        // 4. C 선택 (완전 랜덤)
        MissionPhraseCEntity phraseC = selectPhraseC();
        
        // 5. 결과 반환 (ID 포함하여 프론트엔드에서 히스토리 저장)
        return SelectedPhrases.builder()
            .phraseA(phraseA.getText())
            .phraseB(phraseB.getText())
            .phraseC(phraseC.getText())
            .phraseAId(phraseA.getId())
            .phraseBId(phraseB.getId())
            .phraseCId(phraseC.getId())
            .build();
    }
    
    /**
     * A 멘트 선택
     * 가중치: 번호 조합/전략 65%, 별자리 25%, 랜덤 10%
     */
    private MissionPhraseAEntity selectPhraseA(Strategy strategy, Set<ComboTag> comboTags, String zodiacSign,
                                               List<Long> excludePhraseAIds) {
        List<MissionPhraseAEntity> allPhrases = phraseARepository.findAll();
        
        if (allPhrases.isEmpty()) {
            throw new IllegalStateException("A 멘트가 없습니다");
        }
        
        // 제외할 A 멘트 ID 집합 (프론트엔드 히스토리)
        Set<Long> excludeSet = excludePhraseAIds != null ? new HashSet<>(excludePhraseAIds) : new HashSet<>();
        
        // 각 후보에 점수 부여 (제외 목록 제외)
        List<ScoredPhraseA> scoredPhrases = allPhrases.stream()
            .filter(phrase -> !excludeSet.contains(phrase.getId()))  // 제외 목록 필터링
            .map(phrase -> {
                double score = calculatePhraseAScore(phrase, strategy, comboTags, zodiacSign);
                return new ScoredPhraseA(phrase, score);
            })
            .sorted((a, b) -> Double.compare(b.score, a.score))  // 내림차순
            .collect(Collectors.toList());
        
        // 제외 목록으로 인해 후보가 없으면 제외 목록 무시하고 선택
        if (scoredPhrases.isEmpty()) {
            log.warn("제외 목록으로 인해 A 멘트 후보가 없어 제외 목록을 무시하고 선택합니다.");
            scoredPhrases = allPhrases.stream()
                .map(phrase -> {
                    double score = calculatePhraseAScore(phrase, strategy, comboTags, zodiacSign);
                    return new ScoredPhraseA(phrase, score);
                })
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .collect(Collectors.toList());
        }
        
        // 상위 10% 후보 중에서 가중치 랜덤 선택
        int topCount = Math.max(1, scoredPhrases.size() / 10);
        List<ScoredPhraseA> topCandidates = scoredPhrases.subList(0, Math.min(topCount, scoredPhrases.size()));
        
        return selectPhraseAByWeight(topCandidates.stream()
            .map(sp -> sp.phrase)
            .collect(Collectors.toList()));
    }
    
    /**
     * A 멘트 점수 계산
     * ScoreA = 0.65*(strategy+combo match) + 0.25*(zodiac match) + 0.10*(random variety)
     */
    private double calculatePhraseAScore(MissionPhraseAEntity phrase, Strategy strategy, 
                                         Set<ComboTag> comboTags, String zodiacSign) {
        double score = 0.0;
        
        // ① 번호 조합/전략 매칭 점수 (65%)
        Set<String> strategyTags = parseJsonArray(phrase.getStrategyTags());
        Set<String> phraseComboTags = parseJsonArray(phrase.getComboTags());
        
        double strategyComboScore = 0.0;
        if (strategyTags.contains(strategy.name())) {
            strategyComboScore += 0.4;  // 전략 일치
        }
        
        long comboMatchCount = phraseComboTags.stream()
            .filter(tag -> comboTags.stream().anyMatch(ct -> ct.name().equals(tag)))
            .count();
        if (comboMatchCount > 0) {
            strategyComboScore += 0.6 * (comboMatchCount / (double) Math.max(phraseComboTags.size(), 1));  // 조합 특성 일치
        }
        
        score += 0.65 * strategyComboScore;
        
        // ② 별자리 매칭 점수 (25%)
        if (zodiacSign != null && !zodiacSign.isEmpty()) {
            Set<String> zodiacTags = parseJsonArray(phrase.getZodiacTags());
            String zodiacCode = zodiacToCode(zodiacSign);
            if (zodiacTags.contains(zodiacCode)) {
                score += 0.25;
            }
        }
        
        // ③ 순수 랜덤 다양성 점수 (10%)
        score += 0.10 * (phrase.getWeightBase() != null ? phrase.getWeightBase() : 1) / 10.0;
        
        return score;
    }
    
    /**
     * B 멘트 선택 (A에 종속)
     */
    private MissionPhraseBEntity selectPhraseB(MissionPhraseAEntity phraseA, List<Long> excludePhraseBIds) {
        List<MissionPhraseBEntity> allPhrases = phraseBRepository.findAll();
        
        if (allPhrases.isEmpty()) {
            throw new IllegalStateException("B 멘트가 없습니다");
        }
        
        // A의 태그에서 alignTags 추출 (A의 분위기)
        Set<String> aTags = new HashSet<>();
        aTags.addAll(parseJsonArray(phraseA.getComboTags()));
        aTags.addAll(parseJsonArray(phraseA.getStrategyTags()));
        aTags.addAll(parseJsonArray(phraseA.getZodiacTags()));
        
        // 제외할 B 멘트 ID 집합 (프론트엔드 히스토리)
        Set<Long> excludeSet = excludePhraseBIds != null ? new HashSet<>(excludePhraseBIds) : new HashSet<>();
        
        // B 후보 필터링 (제외 목록 적용)
        List<MissionPhraseBEntity> candidates = allPhrases.stream()
            .filter(phraseB -> !excludeSet.contains(phraseB.getId()))  // 제외 목록 필터링
            .filter(phraseB -> {
                // alignTags와 A의 태그가 일치하는지 확인
                Set<String> alignTags = parseJsonArray(phraseB.getAlignTags());
                boolean hasMatch = alignTags.stream().anyMatch(aTags::contains);
                
                // avoidTags와 A의 태그가 충돌하는지 확인
                Set<String> avoidTags = parseJsonArray(phraseB.getAvoidTags());
                boolean hasConflict = avoidTags.stream().anyMatch(aTags::contains);
                
                return hasMatch && !hasConflict;
            })
            .collect(Collectors.toList());
        
        // 필터링 결과가 없으면 제외 목록만 무시하고 다시 시도
        if (candidates.isEmpty()) {
            log.warn("제외 목록으로 인해 B 멘트 후보가 없어 제외 목록을 무시하고 선택합니다.");
            candidates = allPhrases.stream()
                .filter(phraseB -> {
                    Set<String> alignTags = parseJsonArray(phraseB.getAlignTags());
                    boolean hasMatch = alignTags.stream().anyMatch(aTags::contains);
                    Set<String> avoidTags = parseJsonArray(phraseB.getAvoidTags());
                    boolean hasConflict = avoidTags.stream().anyMatch(aTags::contains);
                    return hasMatch && !hasConflict;
                })
                .collect(Collectors.toList());
        }
        
        // 여전히 없으면 전체에서 선택
        if (candidates.isEmpty()) {
            candidates = allPhrases;
        }
        
        return selectPhraseBByWeight(candidates);
    }
    
    /**
     * C 멘트 선택 (완전 랜덤)
     */
    private MissionPhraseCEntity selectPhraseC() {
        List<MissionPhraseCEntity> allPhrases = phraseCRepository.findAll();
        
        if (allPhrases.isEmpty()) {
            throw new IllegalStateException("C 멘트가 없습니다");
        }
        
        return selectPhraseCByWeight(allPhrases);
    }
    
    /**
     * 가중치 기반 랜덤 선택 (MissionPhraseAEntity)
     */
    private MissionPhraseAEntity selectPhraseAByWeight(List<MissionPhraseAEntity> items) {
        if (items.isEmpty()) {
            return null;
        }
        
        if (items.size() == 1) {
            return items.get(0);
        }
        
        Random random = new Random();
        int totalWeight = items.stream()
            .mapToInt(item -> item.getWeightBase() != null ? item.getWeightBase() : 1)
            .sum();
        
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (MissionPhraseAEntity item : items) {
            currentWeight += item.getWeightBase() != null ? item.getWeightBase() : 1;
            if (randomValue < currentWeight) {
                return item;
            }
        }
        
        return items.get(items.size() - 1);
    }
    
    /**
     * 가중치 기반 랜덤 선택 (MissionPhraseBEntity)
     */
    private MissionPhraseBEntity selectPhraseBByWeight(List<MissionPhraseBEntity> items) {
        if (items.isEmpty()) {
            return null;
        }
        
        if (items.size() == 1) {
            return items.get(0);
        }
        
        Random random = new Random();
        int totalWeight = items.stream()
            .mapToInt(item -> item.getWeightBase() != null ? item.getWeightBase() : 1)
            .sum();
        
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (MissionPhraseBEntity item : items) {
            currentWeight += item.getWeightBase() != null ? item.getWeightBase() : 1;
            if (randomValue < currentWeight) {
                return item;
            }
        }
        
        return items.get(items.size() - 1);
    }
    
    /**
     * 가중치 기반 랜덤 선택 (MissionPhraseCEntity)
     */
    private MissionPhraseCEntity selectPhraseCByWeight(List<MissionPhraseCEntity> items) {
        if (items.isEmpty()) {
            return null;
        }
        
        if (items.size() == 1) {
            return items.get(0);
        }
        
        Random random = new Random();
        int totalWeight = items.stream()
            .mapToInt(item -> item.getWeightBase() != null ? item.getWeightBase() : 1)
            .sum();
        
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (MissionPhraseCEntity item : items) {
            currentWeight += item.getWeightBase() != null ? item.getWeightBase() : 1;
            if (randomValue < currentWeight) {
                return item;
            }
        }
        
        return items.get(items.size() - 1);
    }
    
    /**
     * JSON 배열 파싱
     */
    private Set<String> parseJsonArray(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Set.of();
        }
        
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return new HashSet<>(list);
        } catch (Exception e) {
            log.warn("JSON 배열 파싱 실패: {}", json, e);
            return Set.of();
        }
    }
    
    /**
     * 별자리 이름을 코드로 변환
     */
    private String zodiacToCode(String zodiacSign) {
        return switch (zodiacSign) {
            case "염소자리" -> "CAPRICORN";
            case "물병자리" -> "AQUARIUS";
            case "물고기자리" -> "PISCES";
            case "양자리" -> "ARIES";
            case "황소자리" -> "TAURUS";
            case "쌍둥이자리" -> "GEMINI";
            case "게자리" -> "CANCER";
            case "사자자리" -> "LEO";
            case "처녀자리" -> "VIRGO";
            case "천칭자리" -> "LIBRA";
            case "전갈자리" -> "SCORPIO";
            case "사수자리" -> "SAGITTARIUS";
            default -> null;
        };
    }
    
    
    /**
     * 점수가 부여된 A 멘트
     */
    private static class ScoredPhraseA {
        final MissionPhraseAEntity phrase;
        final double score;
        
        ScoredPhraseA(MissionPhraseAEntity phrase, double score) {
            this.phrase = phrase;
            this.score = score;
        }
    }
}
