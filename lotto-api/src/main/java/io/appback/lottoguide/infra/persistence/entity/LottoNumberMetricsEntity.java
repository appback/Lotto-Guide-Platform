package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 로또 번호 메트릭 엔티티
 * 테이블명: lotto_number_metrics
 * windowSize별 번호의 빈도, 과거 데이터 등을 저장
 */
@Entity
@Table(name = "lotto_number_metrics", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"window_size", "number"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LottoNumberMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "window_size", nullable = false)
    private Integer windowSize; // 20, 50, 100

    @Column(name = "number", nullable = false)
    private Integer number; // 1..45

    @Column(name = "freq", nullable = false)
    private Integer freq; // 빈도

    @Column(name = "overdue", nullable = false)
    private Integer overdue; // 과거 데이터 (몇 회 추첨 동안 나오지 않음)

    @Column(name = "last_seen_draw_no")
    private Integer lastSeenDrawNo; // 마지막으로 나온 추첨 번호

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
