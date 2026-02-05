package io.appback.lottoguide.infra.persistence.init;

import io.appback.lottoguide.infra.persistence.entity.MissionTemplateEntity;
import io.appback.lottoguide.infra.persistence.repository.MissionTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 미션 템플릿 초기 데이터 삽입
 * 애플리케이션 시작 시 실행
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)  // 다른 초기화보다 먼저 실행
public class MissionTemplateInitializer implements CommandLineRunner {
    
    private final MissionTemplateRepository templateRepository;
    
    @Override
    @Transactional
    public void run(String... args) {
        // 이미 데이터가 있으면 스킵
        if (templateRepository.count() > 0) {
            log.info("미션 템플릿 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }
        
        log.info("미션 템플릿 초기 데이터 삽입 시작");
        
        List<MissionTemplateEntity> templates = createInitialTemplates();
        templateRepository.saveAll(templates);
        
        log.info("미션 템플릿 초기 데이터 삽입 완료: {}개", templates.size());
    }
    
    private List<MissionTemplateEntity> createInitialTemplates() {
        LocalDateTime now = LocalDateTime.now();
        
        return List.of(
            // 원소 기반 (물)
            MissionTemplateEntity.builder()
                .category("ELEMENT_WATER")
                .theme("ELEMENT")
                .tone("TAROT")
                .placeHint("RIVER")
                .text("이 조합은 물의 기운이 강하게 느껴져요. 분수나 강가를 스쳐 지나갈 때, 가볍게 한 장 담아보는 건 어때요?")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("ELEMENT_WATER")
                .theme("ELEMENT")
                .tone("TAROT")
                .placeHint("CAFE")
                .text("물의 흐름처럼 자연스럽게 흘러가는 조합이에요. 비가 오거나 습한 공기가 느껴질 때 마음이 더 잘 맞을 수 있어요.")
                .weight(1)
                .createdAt(now)
                .build(),
            
            // 원소 기반 (불)
            MissionTemplateEntity.builder()
                .category("ELEMENT_FIRE")
                .theme("ELEMENT")
                .tone("TAROT")
                .placeHint("RED_SIGN")
                .text("불의 기운이 살아있는 조합이에요. 붉은 간판이 보이는 길목에서 한 번쯤 시도해보는 건 어떨까요?")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("ELEMENT_FIRE")
                .theme("ELEMENT")
                .tone("TAROT")
                .timeHint("EVENING")
                .text("오늘은 불꽃처럼 '빠른 결정'이 어울려요. 오래 고민하지 말고 첫 선택으로 가볍게 담아보세요.")
                .weight(1)
                .createdAt(now)
                .build(),
            
            // 원소 기반 (바람)
            MissionTemplateEntity.builder()
                .category("ELEMENT_WIND")
                .theme("ELEMENT")
                .tone("TAROT")
                .placeHint("CROSSROAD")
                .text("바람의 기운이 스치는 조합이에요. 교차로나 언덕길처럼 바람이 잘 드는 곳에서 느낌이 맞을 수 있어요.")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("ELEMENT_WIND")
                .theme("ELEMENT")
                .tone("TAROT")
                .text("바람처럼 가벼운 숫자들이 섞였어요. 이동 중에 한 장 정도가 딱 좋아요.")
                .weight(1)
                .createdAt(now)
                .build(),
            
            // 원소 기반 (흙)
            MissionTemplateEntity.builder()
                .category("ELEMENT_EARTH")
                .theme("ELEMENT")
                .tone("TAROT")
                .placeHint("PARK")
                .text("흙의 기운이 단단하게 깔린 조합이에요. 공원이나 나무 많은 길을 걷다 들른 곳이 잘 맞을 수 있어요.")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("ELEMENT_EARTH")
                .theme("ELEMENT")
                .tone("TAROT")
                .text("안정감이 느껴지는 조합이에요. 익숙한 동네에서 '늘 가던 자리'가 오히려 편할 수 있어요.")
                .weight(1)
                .createdAt(now)
                .build(),
            
            // 별자리 기반 (쌍둥이)
            MissionTemplateEntity.builder()
                .category("ZODIAC_GEMINI")
                .theme("ZODIAC")
                .tone("TAROT")
                .text("이 조합은 쌍둥이의 기운이 느껴져요. 오늘은 '짝을 이루는 선택'이 어울려요. 짝수 느낌이 드는 날에 가볍게 시도해보세요.")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("ZODIAC_GEMINI")
                .theme("ZODIAC")
                .tone("TAROT")
                .timeHint("MORNING")
                .text("쌍둥이처럼 빠르게 흐름이 바뀌는 조합이에요. 오전/오후 중 마음이 더 가벼운 시간대에 한 장 담아보세요.")
                .weight(1)
                .createdAt(now)
                .build(),
            
            // 별자리 기반 (사자)
            MissionTemplateEntity.builder()
                .category("ZODIAC_LEO")
                .theme("ZODIAC")
                .tone("TAROT")
                .text("사자의 기운이 살짝 보여요. 사람 많은 곳에서 오히려 기세가 붙을 수 있어요.")
                .weight(1)
                .createdAt(now)
                .build(),
            
            // 별자리 기반 (처녀)
            MissionTemplateEntity.builder()
                .category("ZODIAC_VIRGO")
                .theme("ZODIAC")
                .tone("TAROT")
                .text("처녀자리 느낌이 있어요. 정리정돈을 마친 뒤, 마음이 깔끔할 때 한 장이 잘 맞을 수 있어요.")
                .weight(1)
                .createdAt(now)
                .build(),
            
            // 별자리 기반 (전갈)
            MissionTemplateEntity.builder()
                .category("ZODIAC_SCORPIO")
                .theme("ZODIAC")
                .tone("TAROT")
                .text("전갈자리의 직감이 스치는 조합이에요. 오늘은 '첫 느낌'을 더 믿어보세요.")
                .weight(1)
                .createdAt(now)
                .build(),
            
            // 별자리 기반 (물병)
            MissionTemplateEntity.builder()
                .category("ZODIAC_AQUARIUS")
                .theme("ZODIAC")
                .tone("TAROT")
                .text("물병자리처럼 색다른 흐름이에요. 평소 안 가던 길에서 한 번쯤 시도해보는 것도 재미 있어요.")
                .weight(1)
                .createdAt(now)
                .build(),
            
            // 숫자 패턴 기반
            MissionTemplateEntity.builder()
                .category("NUM_PATTERN_BALANCE")
                .theme("NUM_PATTERN")
                .tone("TAROT")
                .text("고저(낮은 수/높은 수)의 대비가 선명해요. 균형을 찾는 날에 어울리는 조합이에요.")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("NUM_PATTERN_ODD_EVEN")
                .theme("NUM_PATTERN")
                .tone("TAROT")
                .text("홀짝이 엇갈리게 섞였어요. 한쪽으로 치우치지 않게 '가볍게' 담아보세요.")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("NUM_PATTERN_CONSECUTIVE")
                .theme("NUM_PATTERN")
                .tone("TAROT")
                .text("연속의 기운이 은근히 보여요. 오늘은 흐름을 끊지 말고, 이동 중에 자연스럽게 한 장.")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("NUM_PATTERN_SYMMETRY")
                .theme("NUM_PATTERN")
                .tone("TAROT")
                .text("대칭이 느껴지는 조합이에요. 마음이 안정될수록 더 편하게 느껴질 수 있어요.")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("NUM_PATTERN_RELEASE")
                .theme("NUM_PATTERN")
                .tone("TAROT")
                .text("'비워내기'가 키워드인 조합이에요. 집착을 내려놓는 날에 잘 맞을 수 있어요.")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("NUM_PATTERN_TRANSITION")
                .theme("NUM_PATTERN")
                .tone("TAROT")
                .text("'전환'의 기운이 있어요. 오늘은 작은 변화가 즐겁게 이어질 수 있어요.")
                .weight(1)
                .createdAt(now)
                .build(),
            
            // 가벼운 마무리 (디스클레이머 톤)
            MissionTemplateEntity.builder()
                .category("DISCLAIMER_LIGHT")
                .theme("DISCLAIMER")
                .tone("TAROT")
                .text("오늘의 조언은 재미로만 가볍게 가져가 주세요. 마음이 끌리는 쪽이 정답일 때도 있거든요.")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("DISCLAIMER_LIGHT")
                .theme("DISCLAIMER")
                .tone("TAROT")
                .text("너무 큰 의미를 두기보다, 기분 전환으로 한 장 담아보는 정도가 딱 좋아요.")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("DISCLAIMER_LIGHT")
                .theme("DISCLAIMER")
                .tone("TAROT")
                .text("부담 없이, 지나가는 김에. 오늘은 '가벼움'이 흐름을 좋게 만들 수 있어요.")
                .weight(1)
                .createdAt(now)
                .build(),
            MissionTemplateEntity.builder()
                .category("DISCLAIMER_LIGHT")
                .theme("DISCLAIMER")
                .tone("TAROT")
                .text("이 조합은 \"오늘의 무드\"에 가까워요. 결과보다 과정이 재밌어지는 날일 수 있어요.")
                .weight(1)
                .createdAt(now)
                .build()
        );
    }
}
