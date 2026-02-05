package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.AiLoadingMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI 로딩 메시지 Repository
 */
@Repository
public interface AiLoadingMessageRepository extends JpaRepository<AiLoadingMessageEntity, Long> {
    
    /**
     * orderIndex 기준 오름차순 정렬하여 모든 메시지 조회
     */
    List<AiLoadingMessageEntity> findAllByOrderByOrderIndexAsc();
}
