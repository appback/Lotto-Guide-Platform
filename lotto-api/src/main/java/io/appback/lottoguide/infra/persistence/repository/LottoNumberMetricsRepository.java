package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.LottoNumberMetricsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 로또 번호 메트릭 Repository
 */
@Repository
public interface LottoNumberMetricsRepository extends JpaRepository<LottoNumberMetricsEntity, Long> {

    /**
     * windowSize와 number로 조회
     */
    Optional<LottoNumberMetricsEntity> findByWindowSizeAndNumber(Integer windowSize, Integer number);

    /**
     * windowSize별 모든 메트릭 조회
     */
    List<LottoNumberMetricsEntity> findByWindowSizeOrderByNumberAsc(Integer windowSize);

    /**
     * windowSize별 빈도 높은 순으로 조회
     */
    @Query("SELECT m FROM LottoNumberMetricsEntity m WHERE m.windowSize = :windowSize ORDER BY m.freq DESC, m.number ASC")
    List<LottoNumberMetricsEntity> findByWindowSizeOrderByFreqDesc(Integer windowSize);

    /**
     * windowSize별 과거 데이터 높은 순으로 조회
     */
    @Query("SELECT m FROM LottoNumberMetricsEntity m WHERE m.windowSize = :windowSize ORDER BY m.overdue DESC, m.number ASC")
    List<LottoNumberMetricsEntity> findByWindowSizeOrderByOverdueDesc(Integer windowSize);
}
