package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 로또 추첨 결과 엔티티
 * 테이블명: lotto_draw
 */
@Entity
@Table(name = "lotto_draw")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrawEntity {

    @Id
    @Column(name = "draw_no", nullable = false)
    private Integer drawNo;

    @Column(name = "draw_date", nullable = false)
    private LocalDate drawDate;

    @Column(name = "n1", nullable = false)
    private Integer n1;

    @Column(name = "n2", nullable = false)
    private Integer n2;

    @Column(name = "n3", nullable = false)
    private Integer n3;

    @Column(name = "n4", nullable = false)
    private Integer n4;

    @Column(name = "n5", nullable = false)
    private Integer n5;

    @Column(name = "n6", nullable = false)
    private Integer n6;

    @Column(name = "bonus", nullable = false)
    private Integer bonus;

    @Column(name = "total_prize")
    private Double totalPrize; // 당첨금 (억 단위)

    @Column(name = "winner_count")
    private Integer winnerCount; // 당첨인원

    @Column(name = "prize_per_person")
    private Double prizePerPerson; // 인당당첨금 (억 단위)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
