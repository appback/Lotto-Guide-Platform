package io.appback.lottoguide.domain.mission.template;

import io.appback.lottoguide.domain.mission.model.MissionTemplate;
import io.appback.lottoguide.infra.persistence.entity.MissionTemplateEntity;
import io.appback.lottoguide.infra.persistence.repository.MissionTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 미션 템플릿 선택 서비스
 * 별자리, 원소, 숫자 패턴 기반으로 템플릿 선택
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MissionTemplateSelector {
    
    private final MissionTemplateRepository templateRepository;
    private final Random random = new Random();
    
    /**
     * 템플릿 선택
     * 
     * 선택 우선순위:
     * 1. 생년월일이 있으면 별자리 템플릿 우선
     * 2. 없으면 원소 템플릿 랜덤
     * 3. 숫자 패턴 템플릿으로 보조 문장 추가 가능
     * 
     * @param zodiacSign 별자리 (선택적)
     * @param explainTags Explain Tags (숫자 패턴 분석용)
     * @return 선택된 템플릿
     */
    public MissionTemplate selectTemplate(String zodiacSign, List<String> explainTags) {
        // 1. 별자리가 있으면 별자리 템플릿 우선
        if (zodiacSign != null && !zodiacSign.isEmpty()) {
            String category = "ZODIAC_" + zodiacToCategoryCode(zodiacSign);
            List<MissionTemplateEntity> zodiacTemplates = templateRepository.findByCategoryOrderByWeightDesc(category);
            
            if (!zodiacTemplates.isEmpty()) {
                MissionTemplateEntity selected = selectByWeight(zodiacTemplates);
                log.debug("별자리 템플릿 선택: {} -> {}", zodiacSign, selected.getCategory());
                return toMissionTemplate(selected);
            }
        }
        
        // 2. 별자리 템플릿이 없으면 원소 템플릿 랜덤
        List<MissionTemplateEntity> elementTemplates = templateRepository.findElementTemplates();
        if (!elementTemplates.isEmpty()) {
            MissionTemplateEntity selected = selectByWeight(elementTemplates);
            log.debug("원소 템플릿 선택: {}", selected.getCategory());
            return toMissionTemplate(selected);
        }
        
        // 3. 원소 템플릿도 없으면 숫자 패턴 템플릿
        List<MissionTemplateEntity> patternTemplates = templateRepository.findNumberPatternTemplates();
        if (!patternTemplates.isEmpty()) {
            MissionTemplateEntity selected = selectByWeight(patternTemplates);
            log.debug("숫자 패턴 템플릿 선택: {}", selected.getCategory());
            return toMissionTemplate(selected);
        }
        
        // 4. 모든 템플릿이 없으면 기본 메시지
        log.warn("템플릿이 없어 기본 메시지 반환");
        return MissionTemplate.builder()
            .text("오늘의 조언은 재미로만 가볍게 가져가 주세요. 마음이 끌리는 쪽이 정답일 때도 있거든요.")
            .category("DEFAULT")
            .theme("DEFAULT")
            .build();
    }
    
    /**
     * 별자리 이름을 카테고리 코드로 변환
     */
    private String zodiacToCategoryCode(String zodiacSign) {
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
            default -> "UNKNOWN";
        };
    }
    
    /**
     * 가중치 기반 선택
     */
    private MissionTemplateEntity selectByWeight(List<MissionTemplateEntity> templates) {
        if (templates.isEmpty()) {
            return null;
        }
        
        if (templates.size() == 1) {
            return templates.get(0);
        }
        
        // 가중치 합계 계산
        int totalWeight = templates.stream()
            .mapToInt(t -> t.getWeight() != null ? t.getWeight() : 1)
            .sum();
        
        // 랜덤 선택
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (MissionTemplateEntity template : templates) {
            currentWeight += template.getWeight() != null ? template.getWeight() : 1;
            if (randomValue < currentWeight) {
                return template;
            }
        }
        
        // 마지막 템플릿 반환 (이론적으로 도달하지 않음)
        return templates.get(templates.size() - 1);
    }
    
    /**
     * Entity를 Domain 모델로 변환
     */
    private MissionTemplate toMissionTemplate(MissionTemplateEntity entity) {
        return MissionTemplate.builder()
            .text(entity.getText())
            .category(entity.getCategory())
            .theme(entity.getTheme())
            .tone(entity.getTone())
            .placeHint(entity.getPlaceHint())
            .timeHint(entity.getTimeHint())
            .build();
    }
}
