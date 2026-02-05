package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 전략 설명 엔티티
 * 테이블명: strategy_description
 */
@Entity
@Table(name = "strategy_description")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyDescriptionEntity {

    @Id
    @Column(name = "strategy_code", nullable = false, length = 50)
    private String strategyCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "short_description", nullable = false, columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "features", nullable = false, columnDefinition = "TEXT")
    private String features; // JSON 배열 형태로 저장

    @Column(name = "algorithm", nullable = false, columnDefinition = "TEXT")
    private String algorithm; // JSON 배열 형태로 저장

    @Column(name = "scenarios", nullable = false, columnDefinition = "TEXT")
    private String scenarios; // JSON 배열 형태로 저장

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // JSON 배열 형태로 저장 (선택적)

    @Column(name = "content_hash", length = 64)
    private String contentHash; // 전략 설명 내용 기반 해시 일련번호 (SHA-256)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
