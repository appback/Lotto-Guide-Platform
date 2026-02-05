package io.appback.lottoguide.domain.mission.tag;

/**
 * A와 B의 호환성을 나타내는 정렬 태그
 * B는 A의 분위기를 따라가야 하므로 이 태그로 필터링
 */
public enum AlignTag {
    /**
     * 물의 기운 (차분, 흐름, 유연)
     */
    WATER,
    
    /**
     * 불의 기운 (열정, 활력, 변화)
     */
    FIRE,
    
    /**
     * 바람의 기운 (자유, 변화, 소통)
     */
    AIR,
    
    /**
     * 땅의 기운 (안정, 실용, 견고)
     */
    EARTH,
    
    /**
     * 고독/혼자만의 시간
     */
    LONELY,
    
    /**
     * 전환/변화의 시기
     */
    TRANSITION,
    
    /**
     * 안정/균형
     */
    STABLE,
    
    /**
     * 대담/도전
     */
    BOLD,
    
    /**
     * 부드러움/온화
     */
    GENTLE,
    
    /**
     * 역동적/활발
     */
    DYNAMIC,
    
    /**
     * 신비/탐구
     */
    MYSTICAL,
    
    /**
     * 실용/현실
     */
    PRACTICAL
}
