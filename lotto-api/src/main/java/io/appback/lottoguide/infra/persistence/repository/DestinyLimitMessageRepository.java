package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.DestinyLimitMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 운명의 번호 추천 경고 메시지 Repository
 */
@Repository
public interface DestinyLimitMessageRepository extends JpaRepository<DestinyLimitMessageEntity, Long> {
    
    /**
     * orderIndex 순서로 정렬하여 모든 메시지 조회
     */
    List<DestinyLimitMessageEntity> findAllByOrderByOrderIndexAsc();
}
