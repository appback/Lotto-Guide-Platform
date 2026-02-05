package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.StrategyDescriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 전략 설명 Repository
 */
@Repository
public interface StrategyDescriptionRepository extends JpaRepository<StrategyDescriptionEntity, String> {
    
    /**
     * 전략 코드로 조회
     */
    Optional<StrategyDescriptionEntity> findByStrategyCode(String strategyCode);
    
    /**
     * 전략 코드 존재 여부 확인
     */
    boolean existsByStrategyCode(String strategyCode);
}
