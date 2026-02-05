package io.appback.lottoguide.infra.refresh;

import io.appback.lottoguide.application.port.out.DrawRepositoryPort;
import io.appback.lottoguide.domain.generator.preset.util.PatternAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 패턴 통계 캐시 서비스
 * 
 * 과거 당첨 데이터의 패턴 통계를 계산하여 캐싱합니다.
 * 데이터가 1주일에 1회 추가되므로, 데이터 추가 시에만 재계산하면 됩니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatternStatisticsCache {
    
    private final DrawRepositoryPort drawRepositoryPort;
    
    // windowSize별 패턴 통계 캐시 (메모리 캐시)
    private final Map<Integer, PatternAnalyzer.PatternStatistics> cache = new ConcurrentHashMap<>();
    
    // 캐시 무효화를 위한 최신 추첨 번호 추적
    private Integer cachedLatestDrawNo = null;
    
    /**
     * windowSize에 대한 패턴 통계 조회 (캐시 우선)
     * 
     * @param windowSize 윈도우 크기 (20, 50, 100)
     * @return 패턴 통계 (캐시에 없으면 계산하여 캐싱)
     */
    public PatternAnalyzer.PatternStatistics getPatternStatistics(Integer windowSize) {
        if (windowSize == null) {
            windowSize = 50; // 기본값
        }
        
        // 캐시 무효화 확인
        if (isCacheInvalid()) {
            log.debug("패턴 통계 캐시 무효화, 재계산 필요");
            invalidateCache();
        }
        
        // 캐시에서 조회
        PatternAnalyzer.PatternStatistics cached = cache.get(windowSize);
        if (cached != null) {
            log.debug("패턴 통계 캐시 히트: windowSize={}", windowSize);
            return cached;
        }
        
        // 캐시 미스: 계산하여 캐싱
        log.info("패턴 통계 캐시 미스, 계산 시작: windowSize={}", windowSize);
        PatternAnalyzer.PatternStatistics statistics = computePatternStatistics(windowSize);
        cache.put(windowSize, statistics);
        
        // 최신 추첨 번호 업데이트
        updateCachedLatestDrawNo();
        
        return statistics;
    }
    
    /**
     * 패턴 통계 계산
     * 
     * @param windowSize 윈도우 크기
     * @return 패턴 통계
     */
    private PatternAnalyzer.PatternStatistics computePatternStatistics(Integer windowSize) {
        try {
            // 최근 N회 추첨 데이터 조회
            List<DrawRepositoryPort.DrawInfo> draws = drawRepositoryPort.findRecentDraws(windowSize);
            
            if (draws == null || draws.isEmpty()) {
                log.warn("과거 당첨 데이터가 없어 기본 패턴 통계 사용: windowSize={}", windowSize);
                return createDefaultStatistics();
            }
            
            // 번호 리스트로 변환
            List<List<Integer>> pastDrawNumbers = draws.stream()
                    .map(draw -> draw.numbers())
                    .toList();
            
            // 패턴 통계 계산
            return PatternAnalyzer.analyzeStatistics(pastDrawNumbers);
            
        } catch (Exception e) {
            log.error("패턴 통계 계산 실패, 기본 통계 사용: windowSize={}, error={}", windowSize, e.getMessage(), e);
            return createDefaultStatistics();
        }
    }
    
    /**
     * 기본 패턴 통계 생성
     */
    private PatternAnalyzer.PatternStatistics createDefaultStatistics() {
        List<PatternAnalyzer.PatternInfo> defaultPatterns = new ArrayList<>();
        
        // 예시 패턴들 (실제 데이터가 없을 때 사용)
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(1, 5, 12, 23, 28, 35)));
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(3, 8, 15, 22, 31, 40)));
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(2, 10, 18, 25, 33, 42)));
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(4, 11, 19, 27, 36, 44)));
        defaultPatterns.add(new PatternAnalyzer.PatternInfo(Arrays.asList(6, 14, 21, 29, 38, 45)));
        
        return new PatternAnalyzer.PatternStatistics(defaultPatterns);
    }
    
    /**
     * 캐시 무효화 확인
     * 최신 추첨 번호가 변경되었는지 확인
     */
    private boolean isCacheInvalid() {
        try {
            Optional<DrawRepositoryPort.DrawInfo> latestDrawOpt = drawRepositoryPort.findLatestDraw();
            if (latestDrawOpt.isEmpty()) {
                return false; // 데이터가 없으면 무효화하지 않음
            }
            
            Integer currentLatestDrawNo = latestDrawOpt.get().drawNo();
            
            // 캐시된 최신 추첨 번호가 없거나 변경되었으면 무효화
            if (cachedLatestDrawNo == null || !cachedLatestDrawNo.equals(currentLatestDrawNo)) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.warn("캐시 무효화 확인 실패: {}", e.getMessage());
            return false; // 에러 발생 시 무효화하지 않음
        }
    }
    
    /**
     * 캐시된 최신 추첨 번호 업데이트
     */
    private void updateCachedLatestDrawNo() {
        try {
            Optional<DrawRepositoryPort.DrawInfo> latestDrawOpt = drawRepositoryPort.findLatestDraw();
            if (latestDrawOpt.isPresent()) {
                cachedLatestDrawNo = latestDrawOpt.get().drawNo();
            }
        } catch (Exception e) {
            log.warn("최신 추첨 번호 업데이트 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 캐시 무효화 (데이터 추가 시 호출)
     */
    public void invalidateCache() {
        log.info("패턴 통계 캐시 무효화");
        cache.clear();
        cachedLatestDrawNo = null;
    }
    
    /**
     * 모든 windowSize에 대해 패턴 통계 재계산
     * 데이터 추가 시 호출
     */
    public void recomputeAllPatternStatistics() {
        log.info("패턴 통계 재계산 시작");
        
        // 캐시 무효화
        invalidateCache();
        
        // 모든 windowSize에 대해 계산하여 캐싱
        List<Integer> windowSizes = Arrays.asList(20, 50, 100);
        for (Integer windowSize : windowSizes) {
            try {
                PatternAnalyzer.PatternStatistics statistics = computePatternStatistics(windowSize);
                cache.put(windowSize, statistics);
                log.debug("windowSize {} 패턴 통계 재계산 완료", windowSize);
            } catch (Exception e) {
                log.error("windowSize {} 패턴 통계 재계산 실패: {}", windowSize, e.getMessage(), e);
            }
        }
        
        // 최신 추첨 번호 업데이트
        updateCachedLatestDrawNo();
        
        log.info("패턴 통계 재계산 완료");
    }
    
    /**
     * 캐시 상태 조회 (디버깅용)
     */
    public Map<String, Object> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("cachedWindowSizes", new ArrayList<>(cache.keySet()));
        status.put("cachedLatestDrawNo", cachedLatestDrawNo);
        status.put("cacheSize", cache.size());
        return status;
    }
}
