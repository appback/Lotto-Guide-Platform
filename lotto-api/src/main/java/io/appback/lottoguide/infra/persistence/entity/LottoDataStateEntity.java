package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 로또 데이터 갱신 상태 엔티티
 * 테이블명: lotto_data_state
 * 
 * 이 엔티티는 항상 1행만 유지되며 (id=1),
 * 데이터 최신 상태와 갱신 진행 여부를 관리합니다.
 */
@Entity
@Table(name = "lotto_data_state")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LottoDataStateEntity {

    /**
     * PK (항상 1)
     */
    @Id
    @Column(name = "id", nullable = false)
    private Integer id = 1;

    /**
     * 마지막으로 반영된 회차 번호
     */
    @Column(name = "as_of_draw_no")
    private Integer asOfDrawNo;

    /**
     * 마지막 갱신 완료 시각
     */
    @Column(name = "refreshed_at")
    private LocalDateTime refreshedAt;

    /**
     * 현재 갱신 진행 여부
     */
    @Column(name = "refreshing", nullable = false)
    @Builder.Default
    @Setter
    private Boolean refreshing = false;

    /**
     * 갱신 시작 시각
     */
    @Column(name = "refresh_started_at")
    private LocalDateTime refreshStartedAt;

    /**
     * 실패 시 쿨다운 종료 시각
     * 이 시각 이전에는 갱신 시도 금지
     */
    @Column(name = "refresh_lock_until")
    private LocalDateTime refreshLockUntil;

    /**
     * 마지막 에러 메시지 (nullable)
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
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
        if (id == null) {
            id = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
