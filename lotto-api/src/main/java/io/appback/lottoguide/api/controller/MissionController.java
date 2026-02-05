package io.appback.lottoguide.api.controller;

import io.appback.lottoguide.api.dto.MissionRequest;
import io.appback.lottoguide.api.dto.MissionResponse;
import io.appback.lottoguide.application.usecase.MissionUseCase;
import io.appback.lottoguide.domain.generator.model.Strategy;
import io.appback.lottoguide.domain.mission.model.Mission;
import io.appback.lottoguide.domain.mission.model.Tone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 미션 생성 API Controller
 */
@RestController
@RequestMapping("/api/v1/mission")
@RequiredArgsConstructor
@Slf4j
public class MissionController {
    
    private final MissionUseCase missionUseCase;
    
    /**
     * 미션 생성
     * POST /api/v1/mission
     * 
     * @param request 미션 생성 요청
     * @return 생성된 미션
     */
    @PostMapping
    public ResponseEntity<?> createMission(@RequestBody MissionRequest request) {
        try {
            // A/B/C 시스템: strategy와 numbers가 필수
            if (request.getStrategy() == null || request.getNumbers() == null || request.getNumbers().size() != 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", "잘못된 요청입니다.", 
                        "message", "strategy와 numbers(6개)는 필수입니다."));
            }
            
            // Strategy 파싱
            Strategy strategy;
            try {
                strategy = Strategy.valueOf(request.getStrategy());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", "잘못된 전략입니다.", "message", e.getMessage()));
            }
            
            Tone tone = request.getTone() != null ? request.getTone() : Tone.LIGHT;
            
            // UseCase 실행 (생년월일은 별자리 계산에만 사용되며 저장되지 않음)
            // 제외 목록: 프론트엔드 로컬 스토리지에서 가져온 히스토리
            Mission mission = missionUseCase.execute(
                strategy, 
                request.getNumbers(), 
                tone, 
                request.getBirthDate(),
                request.getExcludePhraseAIds(),
                request.getExcludePhraseBIds()
            );
            
            // Domain 모델을 DTO로 변환
            // 멘트 ID는 프론트엔드에서 히스토리 저장용으로 사용
            MissionResponse response = MissionResponse.builder()
                .missionText(mission.getMissionText())
                .tokenUsage(mission.getTokenUsage())
                .costEstimate(mission.getCostEstimate())
                .zodiacSign(mission.getZodiacSign())  // 별자리 정보 포함
                .phraseAId(mission.getPhraseAId())  // 프론트엔드 히스토리 저장용
                .phraseBId(mission.getPhraseBId())  // 프론트엔드 히스토리 저장용
                .phraseCId(mission.getPhraseCId())  // 프론트엔드 히스토리 저장용
                .build();
            
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (IllegalArgumentException e) {
            // 잘못된 파라미터
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(java.util.Map.of("error", "잘못된 요청입니다.", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("미션 생성 중 오류 발생", e);
            // 기타 예외
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of("error", "미션 생성 중 오류가 발생했습니다.", "message", e.getMessage()));
        }
    }
}
