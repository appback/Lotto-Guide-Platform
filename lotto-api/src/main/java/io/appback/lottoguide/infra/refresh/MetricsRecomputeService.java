package io.appback.lottoguide.infra.refresh;

import io.appback.lottoguide.infra.persistence.entity.DrawEntity;
import io.appback.lottoguide.infra.persistence.entity.LottoNumberMetricsEntity;
import io.appback.lottoguide.infra.persistence.repository.DrawRepository;
import io.appback.lottoguide.infra.persistence.repository.LottoNumberMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 메트릭 재계산 서비스
 * 
 * windowSize별로 번호의 빈도, 과거 데이터 등을 계산하여 캐시 테이블에 저장합니다.
 * 실시간 계산은 금지되며, 캐시만 조회합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsRecomputeService {
    
    private static final List<Integer> WINDOW_SIZES = Arrays.asList(20, 50, 100);
    
    private final DrawRepository drawRepository;
    private final LottoNumberMetricsRepository metricsRepository;
    
    /**
     * 모든 windowSize에 대해 메트릭 재계산
     * @Async로 Non-blocking 처리
     */
    @Async
    @Transactional
    public void recomputeAllMetrics() {
        log.info("메트릭 재계산 시작");
        
        for (Integer windowSize : WINDOW_SIZES) {
            try {
                recomputeMetricsForWindowSize(windowSize);
                log.info("windowSize {} 메트릭 재계산 완료", windowSize);
            } catch (Exception e) {
                log.error("windowSize {} 메트릭 재계산 실패: {}", windowSize, e.getMessage());
            }
        }
        
        log.info("메트릭 재계산 완료");
    }
    
    /**
     * 특정 windowSize에 대해 메트릭 재계산
     */
    @Transactional
    public void recomputeMetricsForWindowSize(Integer windowSize) {
        log.debug("windowSize {} 메트릭 재계산 시작", windowSize);
        
        // 1. 최근 N회 추첨 데이터 조회 (최신 순)
        List<DrawEntity> recentDraws = drawRepository.findFirstByOrderByDrawNoDesc()
            .map(latestDraw -> {
                int startDrawNo = Math.max(1, latestDraw.getDrawNo() - windowSize + 1);
                return drawRepository.findByDrawNoRange(startDrawNo, latestDraw.getDrawNo());
            })
            .orElse(Collections.emptyList());
        
        if (recentDraws.isEmpty()) {
            log.warn("windowSize {}에 대한 추첨 데이터가 없습니다", windowSize);
            return;
        }
        
        // 최신 회차 번호
        int latestDrawNo = recentDraws.get(0).getDrawNo();
        
        // 2. 각 번호(1-45)의 메트릭 계산
        Map<Integer, NumberMetrics> metricsMap = new HashMap<>();
        
        // 초기화: 모든 번호에 대해 기본값 설정
        for (int number = 1; number <= 45; number++) {
            metricsMap.put(number, new NumberMetrics(number, 0, latestDrawNo, 0));
        }
        
        // 빈도 계산 및 마지막 출현 회차 추적
        Set<Integer> numbersInWindow = new HashSet<>();
        for (DrawEntity draw : recentDraws) {
            numbersInWindow.add(draw.getN1());
            numbersInWindow.add(draw.getN2());
            numbersInWindow.add(draw.getN3());
            numbersInWindow.add(draw.getN4());
            numbersInWindow.add(draw.getN5());
            numbersInWindow.add(draw.getN6());
        }
        
        // 각 번호별 빈도 및 마지막 출현 회차 계산
        for (int number = 1; number <= 45; number++) {
            int frequency = 0;
            int lastSeenDrawNo = 0;
            
            for (DrawEntity draw : recentDraws) {
                if (containsNumber(draw, number)) {
                    frequency++;
                    if (lastSeenDrawNo == 0) {
                        lastSeenDrawNo = draw.getDrawNo();
                    }
                }
            }
            
            // 과거 데이터 계산 (마지막 출현 회차로부터 현재까지의 회차 차이)
            int overdue = lastSeenDrawNo > 0 ? latestDrawNo - lastSeenDrawNo : latestDrawNo;
            
            metricsMap.put(number, new NumberMetrics(number, frequency, lastSeenDrawNo, overdue));
        }
        
        // 3. DB에 저장/업데이트
        for (NumberMetrics metrics : metricsMap.values()) {
            Optional<LottoNumberMetricsEntity> existingOpt = 
                metricsRepository.findByWindowSizeAndNumber(windowSize, metrics.number);
            
            if (existingOpt.isPresent()) {
                // 업데이트
                LottoNumberMetricsEntity entity = existingOpt.get();
                entity.setFreq(metrics.frequency);
                entity.setLastSeenDrawNo(metrics.lastSeenDrawNo);
                entity.setOverdue(metrics.overdue);
                metricsRepository.save(entity);
            } else {
                // 신규 생성
                LottoNumberMetricsEntity entity = LottoNumberMetricsEntity.builder()
                    .windowSize(windowSize)
                    .number(metrics.number)
                    .freq(metrics.frequency)
                    .lastSeenDrawNo(metrics.lastSeenDrawNo)
                    .overdue(metrics.overdue)
                    .build();
                metricsRepository.save(entity);
            }
        }
        
        log.debug("windowSize {} 메트릭 재계산 완료: {}개 번호 업데이트", windowSize, metricsMap.size());
    }
    
    /**
     * DrawEntity에 특정 번호가 포함되어 있는지 확인
     */
    private boolean containsNumber(DrawEntity draw, int number) {
        return draw.getN1() == number ||
               draw.getN2() == number ||
               draw.getN3() == number ||
               draw.getN4() == number ||
               draw.getN5() == number ||
               draw.getN6() == number;
    }
    
    /**
     * 메트릭 계산 결과를 담는 내부 클래스
     */
    private static class NumberMetrics {
        final int number;
        final int frequency;
        final int lastSeenDrawNo;
        final int overdue;
        
        NumberMetrics(int number, int frequency, int lastSeenDrawNo, int overdue) {
            this.number = number;
            this.frequency = frequency;
            this.lastSeenDrawNo = lastSeenDrawNo;
            this.overdue = overdue;
        }
    }
}
