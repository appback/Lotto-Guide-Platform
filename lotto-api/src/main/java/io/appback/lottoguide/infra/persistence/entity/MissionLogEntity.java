package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 미션 로그 엔티티 (Observability)
 * 테이블명: mission_log
 * Guest와 Member 모두 로깅 가능 (userId는 nullable)
 */
@Entity
@Table(name = "mission_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId; // Member ID (nullable)

    @Column(name = "anon_id", length = 100)
    private String anonId; // Guest ID (nullable)

    @Column(name = "tone", nullable = false, length = 50)
    private String tone; // LIGHT 등

    @Column(name = "input_tags_json", columnDefinition = "TEXT")
    private String inputTagsJson; // 입력 Explain Tags JSON

    @Column(name = "mission_text", nullable = false, columnDefinition = "TEXT")
    private String missionText; // 생성된 미션 텍스트

    @Column(name = "token_usage")
    private Integer tokenUsage; // LLM 토큰 사용량 (nullable)

    @Column(name = "cost_estimate", precision = 10, scale = 6)
    private BigDecimal costEstimate; // 비용 추정 (nullable)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
