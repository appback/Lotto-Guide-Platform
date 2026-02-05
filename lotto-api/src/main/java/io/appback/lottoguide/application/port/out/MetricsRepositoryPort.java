package io.appback.lottoguide.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * 번호 메트릭 조회 Port (Outbound)
 * Infrastructure 레이어에서 구현
 */
public interface MetricsRepositoryPort {
    
    /**
     * windowSize와 number로 메트릭 조회
     */
    Optional<NumberMetrics> findByWindowSizeAndNumber(Integer windowSize, Integer number);
    
    /**
     * windowSize별 모든 메트릭 조회
     */
    List<NumberMetrics> findByWindowSize(Integer windowSize);
    
    /**
     * windowSize별 빈도 높은 순으로 조회
     */
    List<NumberMetrics> findByWindowSizeOrderByFreqDesc(Integer windowSize);
    
    /**
     * windowSize별 과거 데이터 높은 순으로 조회
     */
    List<NumberMetrics> findByWindowSizeOrderByOverdueDesc(Integer windowSize);
    
    /**
     * 번호 메트릭 정보
     */
    record NumberMetrics(
        Integer number,
        Integer frequency,
        Integer overdue,
        Integer lastSeenDrawNo
    ) {}
}
