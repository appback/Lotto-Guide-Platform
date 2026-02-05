package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.MissionPhraseCEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * C 멘트 리포지토리
 */
@Repository
public interface MissionPhraseCRepository extends JpaRepository<MissionPhraseCEntity, Long> {
}
