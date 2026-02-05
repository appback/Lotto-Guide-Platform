package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 생성된 번호 세트 엔티티 (Member 전용)
 * 테이블명: generated_set
 */
@Entity
@Table(name = "generated_set")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedSetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Member ID (FK)

    @Column(name = "strategy_code", nullable = false, length = 50)
    private String strategyCode; // FREQUENT_TOP, OVERDUE_TOP, BALANCED

    @Column(name = "strategy_params_json", columnDefinition = "TEXT")
    private String strategyParamsJson; // 전략 파라미터 JSON

    @Column(name = "constraints_json", columnDefinition = "TEXT")
    private String constraintsJson; // 제약 조건 JSON

    @Column(name = "generated_count", nullable = false)
    private Integer generatedCount; // 생성된 번호 세트 개수

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
