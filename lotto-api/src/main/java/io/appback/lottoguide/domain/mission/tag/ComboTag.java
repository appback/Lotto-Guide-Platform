package io.appback.lottoguide.domain.mission.tag;

/**
 * 번호 조합 특성 태그
 * 생성된 6개 번호의 특성을 나타냄
 */
public enum ComboTag {
    /**
     * 홀수가 많음 (4개 이상)
     */
    ODD_HEAVY,
    
    /**
     * 짝수가 많음 (4개 이상)
     */
    EVEN_HEAVY,
    
    /**
     * 홀짝 균형 (3:3)
     */
    ODD_EVEN_BALANCED,
    
    /**
     * 합계가 높음 (150 이상)
     */
    SUM_HIGH,
    
    /**
     * 합계가 낮음 (100 이하)
     */
    SUM_LOW,
    
    /**
     * 합계가 중간 (100-150)
     */
    SUM_MID,
    
    /**
     * 연속 번호 포함 (2개 이상 연속)
     */
    CONSECUTIVE,
    
    /**
     * 연속 번호 없음
     */
    NO_CONSECUTIVE,
    
    /**
     * 저번대(1-10) 번호가 많음 (3개 이상)
     */
    LOW_HEAVY,
    
    /**
     * 중번대(11-30) 번호가 많음 (3개 이상)
     */
    MID_HEAVY,
    
    /**
     * 고번대(31-45) 번호가 많음 (3개 이상)
     */
    HIGH_HEAVY,
    
    /**
     * 번호대가 고르게 분산됨
     */
    MIXED,
    
    /**
     * 끝자리 패턴이 다양함
     */
    END_DIGIT_VARIED,
    
    /**
     * 끝자리 패턴이 집중됨
     */
    END_DIGIT_CONCENTRATED
}
