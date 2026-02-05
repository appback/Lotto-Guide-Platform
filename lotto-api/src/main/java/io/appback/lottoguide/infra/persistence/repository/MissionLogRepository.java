package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.MissionLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 미션 로그 Repository (Observability)
 */
@Repository
public interface MissionLogRepository extends JpaRepository<MissionLogEntity, Long> {

    /**
     * 사용자별 미션 로그 조회 (최신순, 페이징)
     */
    Page<MissionLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 익명 ID별 미션 로그 조회 (최신순, 페이징)
     */
    Page<MissionLogEntity> findByAnonIdOrderByCreatedAtDesc(String anonId, Pageable pageable);

    /**
     * 기간별 미션 로그 조회
     */
    @Query("SELECT m FROM MissionLogEntity m WHERE m.createdAt BETWEEN :startDate AND :endDate ORDER BY m.createdAt DESC")
    Page<MissionLogEntity> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * 사용자별 미션 로그 개수 조회
     */
    long countByUserId(Long userId);

    /**
     * 익명 ID별 미션 로그 개수 조회
     */
    long countByAnonId(String anonId);
}
