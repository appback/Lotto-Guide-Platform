package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.SystemOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 시스템 옵션 리포지토리
 */
@Repository
public interface SystemOptionRepository extends JpaRepository<SystemOptionEntity, Long> {
    
    /**
     * 옵션 키로 조회
     */
    Optional<SystemOptionEntity> findByOptionKey(String optionKey);
}
