package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 미션 템플릿 엔티티
 * 타로/운세 톤의 문구를 저장
 */
@Entity
@Table(name = "mission_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionTemplateEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 카테고리 (ELEMENT_WATER, ELEMENT_FIRE, ZODIAC_GEMINI, NUM_PATTERN_BALANCE 등)
     */
    @Column(name = "category", nullable = false, length = 100)
    private String category;
    
    /**
     * 테마 (ELEMENT, ZODIAC, NUM_PATTERN)
     */
    @Column(name = "theme", nullable = false, length = 50)
    private String theme;
    
    /**
     * 톤 (TAROT)
     */
    @Column(name = "tone", nullable = false, length = 50)
    private String tone;
    
    /**
     * 장소 힌트 (RIVER, PARK, CROSSROAD 등, 선택적)
     */
    @Column(name = "place_hint", length = 50)
    private String placeHint;
    
    /**
     * 시간 힌트 (MORNING, EVENING 등, 선택적)
     */
    @Column(name = "time_hint", length = 50)
    private String timeHint;
    
    /**
     * 템플릿 문구 텍스트
     */
    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;
    
    /**
     * 가중치 (선택 우선순위)
     */
    @Column(name = "weight", nullable = false)
    @Builder.Default
    private Integer weight = 1;
    
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
