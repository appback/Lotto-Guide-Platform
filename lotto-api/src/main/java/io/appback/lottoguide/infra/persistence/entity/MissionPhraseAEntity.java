package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A 멘트 엔티티 (해석/은유)
 * 
 * "오늘의 메시지"의 핵심. 번호 조합/전략이 1순위, 별자리 2순위, 그 외 랜덤 3순위
 */
@Entity
@Table(name = "mission_phrase_a")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionPhraseAEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * A 문장 텍스트
     */
    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;
    
    /**
     * 전략 태그 (JSON 배열: ["FREQUENT_TOP", "OVERDUE_TOP", ...])
     */
    @Column(name = "strategy_tags", columnDefinition = "TEXT")
    private String strategyTags;
    
    /**
     * 조합 특성 태그 (JSON 배열: ["ODD_HEAVY", "SUM_HIGH", ...])
     */
    @Column(name = "combo_tags", columnDefinition = "TEXT")
    private String comboTags;
    
    /**
     * 별자리 태그 (JSON 배열: ["ARIES", "GEMINI", ...])
     */
    @Column(name = "zodiac_tags", columnDefinition = "TEXT")
    private String zodiacTags;
    
    /**
     * 톤 태그 (JSON 배열: ["TAROT", "FORTUNE", ...])
     */
    @Column(name = "tone_tags", columnDefinition = "TEXT")
    private String toneTags;
    
    /**
     * 기본 가중치
     */
    @Column(name = "weight_base", nullable = false)
    @Builder.Default
    private Integer weightBase = 1;
    
    /**
     * 생성일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 수정일시
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
