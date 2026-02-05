package io.appback.lottoguide.domain.generator.model;

/**
 * Explain Tag enum
 * 생성된 번호 세트의 특성을 설명하는 태그
 */
public enum ExplainTag {
    /**
     * 최근 50회 추첨 기준
     */
    WINDOW_50,
    
    /**
     * 홀수 3개, 짝수 3개
     */
    ODD_3_EVEN_3,
    
    /**
     * 합계 126
     */
    SUM_126,
    
    /**
     * 빈도 편향 (고빈도 번호 포함)
     */
    FREQ_BIAS,
    
    /**
     * 과거 데이터 편향 (오래 나오지 않은 번호 포함)
     */
    OVERDUE_BIAS,
    
    /**
     * 긴 연속 번호 없음
     */
    NO_LONG_CONSEC
}
