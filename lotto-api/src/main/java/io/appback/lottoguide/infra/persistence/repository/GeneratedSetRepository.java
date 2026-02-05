package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.GeneratedSetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 생성된 번호 세트 Repository (Member 전용)
 */
@Repository
public interface GeneratedSetRepository extends JpaRepository<GeneratedSetEntity, Long> {

    /**
     * 사용자별 생성된 세트 조회 (최신순)
     */
    List<GeneratedSetEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자별 생성된 세트 조회 (페이징)
     */
    Page<GeneratedSetEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자별 세트 개수 조회
     */
    long countByUserId(Long userId);
}
