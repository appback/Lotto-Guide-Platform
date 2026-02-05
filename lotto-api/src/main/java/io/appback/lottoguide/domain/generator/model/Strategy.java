package io.appback.lottoguide.domain.generator.model;

/**
 * 번호 생성 전략 enum
 */
public enum Strategy {
    /**
     * 최근 N회 추첨에서 고빈도 번호 우선
     */
    FREQUENT_TOP,
    
    /**
     * 최근에 나오지 않은 번호 우선 (과거 데이터)
     */
    OVERDUE_TOP,
    
    /**
     * 제약 조건 기반 균형 잡힌 랜덤
     */
    BALANCED,
    
    /**
     * Wheeling System: 5등(번호 3개 일치) 보장 조합
     * 통계적으로 가장 안 나온 9개 번호를 제외한 36개 번호로 14개 조합 생성
     */
    WHEELING_SYSTEM,
    
    /**
     * 가중치 기반 랜덤: 빈도와 과거 데이터를 결합한 가중치로 랜덤 추출
     */
    WEIGHTED_RANDOM,
    
    /**
     * 패턴 필터링: 과거 당첨 데이터의 패턴과 일치하는 조합만 생성
     */
    PATTERN_MATCHER,
    
    /**
     * AI 기반 추천: 시뮬레이션과 다차원 평가를 통한 최적 조합 선별
     * 통계 기반 지능형 알고리즘으로 여러 조합을 생성하고 평가하여 최상의 조합을 선택
     */
    AI_SIMULATION,
    
    /**
     * AI 패턴 분석 추천: 과거 당첨 패턴과 유사도가 낮은 조합을 AI가 자동 제거
     * 총합/홀짝/고저/연속수 패턴 학습 및 패턴 일치도 기반 선별
     */
    AI_PATTERN_REASONER,
    
    /**
     * AI 판단 필터 추천: AI가 "이 조합은 버린다 / 남긴다"를 판단
     * 극단값 조합 자동 제거 및 말이 안 되는 조합 사전 차단
     */
    AI_DECISION_FILTER,
    
    /**
     * AI 가중치 진화 추천: AI가 빈도·과거 데이터 가중치를 스스로 조정
     * 패턴 적합도에 따라 가중치 변화하는 적응형 전략
     */
    AI_WEIGHT_EVOLUTION
}
