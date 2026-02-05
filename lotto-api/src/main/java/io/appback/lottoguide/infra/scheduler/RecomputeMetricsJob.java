package io.appback.lottoguide.infra.scheduler;

import io.appback.lottoguide.infra.refresh.MetricsRecomputeService;
import io.appback.lottoguide.infra.refresh.PatternStatisticsCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 번호 메트릭 재계산 Job
 * 
 * 매일 새벽 3시에 메트릭 재계산 (보조 수단)
 * 주 전략은 Lazy Refresh에서 데이터 갱신 성공 시 즉시 재계산
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecomputeMetricsJob {
    
    private final MetricsRecomputeService metricsRecomputeService;
    private final PatternStatisticsCache patternStatisticsCache;
    
    /**
     * 매일 새벽 3시에 메트릭 재계산
     * 보조 수단: 장기간 요청이 없을 때를 대비
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void recomputeMetrics() {
        log.info("RecomputeMetricsJob 실행: 번호 메트릭 재계산 시작");
        
        metricsRecomputeService.recomputeAllMetrics();
        // 패턴 통계도 재계산
        patternStatisticsCache.recomputeAllPatternStatistics();
        
        log.info("RecomputeMetricsJob 완료: 번호 메트릭 재계산 완료");
    }
}
