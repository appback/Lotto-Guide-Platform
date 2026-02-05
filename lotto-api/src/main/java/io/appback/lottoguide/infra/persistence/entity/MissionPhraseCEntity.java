package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * C 멘트 엔티티 (추천도/마무리)
 * 
 * 완전 랜덤. 데이터 적어도 됨
 */
@Entity
@Table(name = "mission_phrase_c")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionPhraseCEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * C 문장 텍스트
     */
    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;
    
    /**
     * 톤 태그 (JSON 배열: ["TAROT", "FORTUNE", ...])
     * 선택적
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
