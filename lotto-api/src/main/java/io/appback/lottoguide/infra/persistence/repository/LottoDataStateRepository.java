package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.LottoDataStateEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 로또 데이터 갱신 상태 Repository
 * 항상 1행만 유지됨 (id=1)
 */
@Repository
public interface LottoDataStateRepository extends JpaRepository<LottoDataStateEntity, Integer> {

    /**
     * 상태 조회 (동시성 제어를 위한 Lock)
     * @Lock(PESSIMISTIC_WRITE)로 동시 갱신 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM LottoDataStateEntity s WHERE s.id = 1")
    Optional<LottoDataStateEntity> findByIdWithLock();

    /**
     * 일반 조회 (Lock 없음)
     */
    @Query("SELECT s FROM LottoDataStateEntity s WHERE s.id = 1")
    Optional<LottoDataStateEntity> findByIdWithoutLock();
}
