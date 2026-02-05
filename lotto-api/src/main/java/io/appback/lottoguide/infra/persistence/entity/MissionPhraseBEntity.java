package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * B 멘트 엔티티 (행동/장소/색감)
 * 
 * A의 분위기를 따라가 어색하지 않은 행동 유도. 데이터가 가장 많아야 함
 */
@Entity
@Table(name = "mission_phrase_b")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionPhraseBEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * B 문장 텍스트
     */
    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;
    
    /**
     * 장소 힌트 (RIVER, PARK, CAFE, ...)
     */
    @Column(name = "place_hint", length = 50)
    private String placeHint;
    
    /**
     * 색감 힌트 (BLUE, RED, GREEN, ...)
     */
    @Column(name = "color_hint", length = 50)
    private String colorHint;
    
    /**
     * 정렬 태그 (JSON 배열: ["WATER", "FIRE", "LONELY", ...])
     * A와 맞는 분위기 태그
     */
    @Column(name = "align_tags", columnDefinition = "TEXT")
    private String alignTags;
    
    /**
     * 회피 태그 (JSON 배열: ["FIRE", "BOLD", ...])
     * 같이 나오면 어색한 조합 태그
     */
    @Column(name = "avoid_tags", columnDefinition = "TEXT")
    private String avoidTags;
    
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
