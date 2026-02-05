package io.appback.lottoguide.domain.mission.policy;

import org.springframework.stereotype.Component;

/**
 * Mission 정책 검증
 * 
 * 현재는 LLM 통합 전 단계이므로 기본 구조만 구현
 */
@Component
public class MissionPolicy {
    
    /**
     * 미션 텍스트 정책 검증
     * @param missionText 미션 텍스트
     * @return 정책 위반 여부 (true: 위반, false: 정상)
     */
    public boolean violatesPolicy(String missionText) {
        if (missionText == null || missionText.trim().isEmpty()) {
            return true;
        }
        
        // TODO: LLM 통합 시 실제 정책 검증 로직 구현
        // - 확률/보장 관련 표현 차단
        // - 금지된 의미론 검사
        
        return false;
    }
    
    /**
     * 고정 Disclaimer 텍스트
     */
    public String getDisclaimer() {
        return "\n\n※ 본 서비스는 번호 생성 도구일 뿐이며, 당첨을 보장하지 않습니다.";
    }
}
