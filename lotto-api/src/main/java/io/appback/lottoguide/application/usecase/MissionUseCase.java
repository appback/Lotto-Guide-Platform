package io.appback.lottoguide.application.usecase;

import io.appback.lottoguide.domain.generator.model.Strategy;
import io.appback.lottoguide.domain.mission.model.Mission;
import io.appback.lottoguide.domain.mission.model.Tone;
import io.appback.lottoguide.domain.mission.phrase.PhraseSelector;
import io.appback.lottoguide.domain.mission.phrase.SelectedPhrases;
import io.appback.lottoguide.domain.mission.zodiac.ZodiacCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 미션 생성 UseCase
 * 
 * A/B/C 멘트 조합 엔진 사용
 * 주의: birthDate는 별자리 계산에만 사용되며 저장되지 않음
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MissionUseCase {
    
    private final PhraseSelector phraseSelector;
    private final ZodiacCalculator zodiacCalculator;
    
    /**
     * 미션 생성 실행 (A/B/C 멘트 조합)
     * 
     * @param strategy 사용자가 선택한 생성 전략
     * @param numbers 생성된 6개 번호
     * @param tone 톤
     * @param birthDate 생년월일 (선택적, 별자리 계산에만 사용)
     * @param excludePhraseAIds 제외할 A 멘트 ID 목록 (프론트엔드 히스토리)
     * @param excludePhraseBIds 제외할 B 멘트 ID 목록 (프론트엔드 히스토리)
     * @return 생성된 미션
     */
    public Mission execute(Strategy strategy, List<Integer> numbers, Tone tone, LocalDate birthDate,
                          List<Long> excludePhraseAIds, List<Long> excludePhraseBIds) {
        // 1. 생년월일로부터 별자리 계산 (저장하지 않음)
        String zodiacSign = null;
        if (birthDate != null) {
            zodiacSign = zodiacCalculator.calculateZodiac(birthDate);
            log.debug("생년월일로부터 별자리 계산: {} -> {}", birthDate, zodiacSign);
        }
        
        // 2. A/B/C 멘트 선택 (제외 목록 적용)
        SelectedPhrases phrases = phraseSelector.selectPhrases(strategy, numbers, zodiacSign,
                                                               excludePhraseAIds, excludePhraseBIds);
        
        // 3. 전체 메시지 조합
        String missionText = phrases.getFullMessage();
        
        // 4. Mission 객체 생성 (별자리 정보는 응답에만 포함, DB 저장 안 함)
        return Mission.builder()
            .missionText(missionText)
            .tone(tone)
            .inputTagsJson(null)  // A/B/C 시스템에서는 사용하지 않음
            .createdAt(LocalDateTime.now())
            .tokenUsage(null)  // LLM 사용 안 함
            .costEstimate(null)  // LLM 사용 안 함
            .zodiacSign(zodiacSign)  // 별자리 정보 포함 (DB 저장 안 함)
            .phraseAId(phrases.getPhraseAId())  // 프론트엔드 히스토리 저장용
            .phraseBId(phrases.getPhraseBId())  // 프론트엔드 히스토리 저장용
            .phraseCId(phrases.getPhraseCId())  // 프론트엔드 히스토리 저장용
            .build();
    }
    
    /**
     * 미션 생성 실행 (별자리 없이)
     * 
     * @param strategy 사용자가 선택한 생성 전략
     * @param numbers 생성된 6개 번호
     * @param tone 톤
     * @param excludePhraseAIds 제외할 A 멘트 ID 목록
     * @param excludePhraseBIds 제외할 B 멘트 ID 목록
     * @return 생성된 미션
     */
    public Mission execute(Strategy strategy, List<Integer> numbers, Tone tone,
                          List<Long> excludePhraseAIds, List<Long> excludePhraseBIds) {
        return execute(strategy, numbers, tone, null, excludePhraseAIds, excludePhraseBIds);
    }
}
