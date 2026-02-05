package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.MissionPhraseBEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * B 멘트 리포지토리
 * 
 * 히스토리는 프론트엔드 로컬 스토리지에서 관리하므로
 * DB 조회 메서드 제거
 */
@Repository
public interface MissionPhraseBRepository extends JpaRepository<MissionPhraseBEntity, Long> {
}
