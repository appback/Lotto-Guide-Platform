package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 운명의 번호 추천 경고 메시지 엔티티
 * 테이블명: destiny_limit_message
 */
@Entity
@Table(name = "destiny_limit_message")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinyLimitMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "message_part_a", columnDefinition = "TEXT")
    private String messagePartA;

    @Column(name = "message_part_b", columnDefinition = "TEXT")
    private String messagePartB;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

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
