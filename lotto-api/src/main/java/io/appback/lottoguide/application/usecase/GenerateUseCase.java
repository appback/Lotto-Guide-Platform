package io.appback.lottoguide.application.usecase;

import io.appback.lottoguide.application.port.out.*;
import io.appback.lottoguide.domain.generator.engine.GeneratorEngine;
import io.appback.lottoguide.domain.generator.explain.ExplainTagBuilder;
import io.appback.lottoguide.domain.generator.model.*;
import io.appback.lottoguide.domain.generator.preset.Preset;
import io.appback.lottoguide.infra.refresh.DrawRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import io.appback.lottoguide.domain.exception.AiServiceBusyException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 번호 생성 UseCase
 * 
 * Lazy Refresh: 요청 시 데이터 갱신 필요 여부를 확인하고,
 * 필요 시 백그라운드로 갱신을 수행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateUseCase {
    
    private final GeneratorEngine generatorEngine;
    private final ExplainTagBuilder explainTagBuilder;
    private final DrawRepositoryPort drawRepositoryPort;
    private final MetricsRepositoryPort metricsRepositoryPort;
    private final GeneratedSetRepositoryPort generatedSetRepositoryPort;
    private final DrawRefreshService drawRefreshService;
    private final io.appback.lottoguide.infra.refresh.PatternStatisticsCache patternStatisticsCache;
    
    // AI 대기 시간 처리를 위한 스레드 풀 설정
    private static final int MAX_CONCURRENT_AI_REQUESTS = 20; // 최대 동시 처리 수
    private static final int QUEUE_CAPACITY = 10; // 대기 큐 크기
    private static final ThreadPoolExecutor aiDelayExecutor;
    
    // 동시 요청 그룹화를 위한 시간 윈도우 (밀리초)
    // 같은 시간 윈도우에 들어온 요청들은 같은 대기 시간을 가짐
    private static final long TIME_WINDOW_MS = 2000; // 2초 윈도우
    private static final long MIN_DELAY_MS = 3000; // 최소 대기 시간 3초
    private static final long MAX_DELAY_MS = 5000; // 최대 대기 시간 5초
    
    // 시간 윈도우별 대기 시간 캐시 (윈도우 시작 시간 -> 대기 시간)
    private static final ConcurrentHashMap<Long, Long> delayCache = new ConcurrentHashMap<>();
    
    static {
        // ThreadPoolExecutor를 사용하여 큐 크기 제한 및 거부 정책 설정
        aiDelayExecutor = new ThreadPoolExecutor(
            MAX_CONCURRENT_AI_REQUESTS,  // corePoolSize: 최소 스레드 수
            MAX_CONCURRENT_AI_REQUESTS,  // maximumPoolSize: 최대 스레드 수
            60L,                         // keepAliveTime: 60초
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY), // 제한된 큐 크기
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "ai-delay-" + (++counter));
                    t.setDaemon(true);
                    return t;
                }
            },
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    // 큐가 가득 차면 즉시 거부 (사용자에게 메시지 전달)
                    throw new AiServiceBusyException(
                        "현재 이용하는 사용자가 많아 지금은 사용할 수 없습니다. 잠시 후 다시 시도해주세요."
                    );
                }
            }
        );
    }
    
    /**
     * 번호 생성 실행
     * @param strategy 생성 전략
     * @param constraints 제약 조건
     * @param count 생성할 세트 개수
     * @param windowSize 윈도우 크기 (20, 50, 100)
     * @param userId 사용자 ID (null이면 Guest)
     * @return 생성 결과
     */
    public GenerateResult execute(
            Strategy strategy,
            Constraints constraints,
            int count,
            Integer windowSize,
            Long userId) {
        
        // 0. 데이터 확인 플로우: 메모리 캐시 (히트 응답) -> 없음 -> DB (캐싱 후 응답)
        // 외부 API 호출 없음. DB 저장은 관리자 페이지에서만 수행.
        boolean dataLoaded = drawRefreshService.hasData();
        
        // 1. 메트릭 데이터 조회 (데이터가 없으면 빈 리스트로 랜덤 생성)
        List<Preset.NumberMetrics> metricsList;
        if (dataLoaded) {
            metricsList = getMetrics(windowSize);
        } else {
            log.warn("추첨 데이터 없음, 랜덤 생성 모드로 전환");
            metricsList = List.of(); // 빈 메트릭으로 랜덤 생성
        }
        
        // 2. AI 전략인 경우: 계산은 즉시 실행, 대기 시간만 비동기 처리
        // 모든 AI 전략 (AI_로 시작하는 전략)에 동일한 대기 시간 적용
        if (strategy.name().startsWith("AI_")) {
            // 2-1. 계산 로직 즉시 실행 (빠르게 완료)
            List<GeneratedSet> generatedSets = generatorEngine.generate(
                strategy, constraints, count, windowSize, metricsList, null, patternStatisticsCache
            );
            
            // Explain Tags 생성
            List<GeneratedSet> setsWithTags = new ArrayList<>();
            for (GeneratedSet set : generatedSets) {
                List<ExplainTag> tags = explainTagBuilder.buildTags(set, windowSize);
                GeneratedSet setWithTags = GeneratedSet.builder()
                    .index(set.getIndex())
                    .numbers(set.getNumbers())
                    .tags(tags)
                    .strategy(set.getStrategy())
                    .constraints(set.getConstraints())
                    .createdAt(set.getCreatedAt())
                    .build();
                setsWithTags.add(setWithTags);
            }
            final List<GeneratedSet> finalGeneratedSets = setsWithTags;
            
            // Member인 경우 DB 저장
            final Long finalSetId;
            if (userId != null) {
                finalSetId = generatedSetRepositoryPort.save(
                    userId,
                    finalGeneratedSets,
                    strategy.name(),
                    null, // strategyParamsJson (나중에 구현)
                    null  // constraintsJson (나중에 구현)
                );
            } else {
                finalSetId = null;
            }
            
            // 2-2. 대기 시간만 비동기로 처리 (별도 스레드에서 대기)
            // 동시에 들어온 요청들은 같은 시간 윈도우에 속하므로 같은 대기 시간을 가짐
            long delay = getDelayForTimeWindow();
            log.info("AI 시뮬레이션 전략: 계산 완료, {}ms 대기 후 응답...", delay);
            
            try {
                // CompletableFuture를 사용하여 비동기 대기 (메인 스레드 블로킹 없음)
                // 스레드 풀이 가득 차면 RejectedExecutionHandler에서 AiServiceBusyException 발생
                CompletableFuture<GenerateResult> future = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            Thread.sleep(delay);
                            return new GenerateResult(finalGeneratedSets, finalSetId);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("AI 시뮬레이션 대기 중 인터럽트 발생", e);
                            return new GenerateResult(finalGeneratedSets, finalSetId);
                        }
                    }, aiDelayExecutor);
                
                // 대기 완료 후 결과 반환 (비동기이지만 동기적으로 기다림)
                // 타임아웃 설정: 최대 10초 (정상 대기 시간 3-5초 + 여유)
                return future.get(10, TimeUnit.SECONDS);
            } catch (AiServiceBusyException e) {
                // 서비스 사용량 초과 예외는 그대로 전파
                log.warn("AI 서비스 사용량 초과: {}", e.getMessage());
                throw e;
            } catch (TimeoutException e) {
                log.error("AI 시뮬레이션 타임아웃 발생", e);
                // 타임아웃 시 계산된 결과라도 반환
                return new GenerateResult(finalGeneratedSets, finalSetId);
            } catch (Exception e) {
                log.error("AI 시뮬레이션 비동기 처리 실패", e);
                // 실패 시 즉시 반환
                return new GenerateResult(finalGeneratedSets, finalSetId);
            }
        }
        
        // 3. 일반 전략: 즉시 처리
        // Pattern Matcher의 경우 캐시된 패턴 통계를 사용 (과거 당첨 데이터 직접 전달 불필요)
        // 패턴 통계는 PatternStatisticsCache에서 캐싱되어 있음
        List<GeneratedSet> generatedSets = generatorEngine.generate(
            strategy, constraints, count, windowSize, metricsList, null, patternStatisticsCache
        );
        
        // 4. Explain Tags 생성
        List<GeneratedSet> setsWithTags = new ArrayList<>();
        for (GeneratedSet set : generatedSets) {
            List<ExplainTag> tags = explainTagBuilder.buildTags(set, windowSize);
            GeneratedSet setWithTags = GeneratedSet.builder()
                .index(set.getIndex())
                .numbers(set.getNumbers())
                .tags(tags)
                .strategy(set.getStrategy())
                .constraints(set.getConstraints())
                .createdAt(set.getCreatedAt())
                .build();
            setsWithTags.add(setWithTags);
        }
        generatedSets = setsWithTags;
        
        // 5. Member인 경우 DB 저장
        Long setId = null;
        if (userId != null) {
            setId = generatedSetRepositoryPort.save(
                userId,
                generatedSets,
                strategy.name(),
                null, // strategyParamsJson (나중에 구현)
                null  // constraintsJson (나중에 구현)
            );
        }
        
        return new GenerateResult(generatedSets, setId);
    }
    
    /**
     * 현재 시간 윈도우에 대한 대기 시간을 가져옴
     * 같은 시간 윈도우에 들어온 요청들은 같은 대기 시간을 가짐
     */
    private long getDelayForTimeWindow() {
        long currentTime = System.currentTimeMillis();
        long windowStart = (currentTime / TIME_WINDOW_MS) * TIME_WINDOW_MS;
        
        // 캐시에서 대기 시간 조회, 없으면 새로 생성
        return delayCache.computeIfAbsent(windowStart, k -> {
            // 랜덤 대기 시간 생성 (5-10초)
            long delay = MIN_DELAY_MS + (long)(Math.random() * (MAX_DELAY_MS - MIN_DELAY_MS));
            log.debug("새로운 시간 윈도우 {}에 대한 대기 시간 생성: {}ms", windowStart, delay);
            
            // 오래된 캐시 항목 정리 (메모리 누수 방지)
            // 현재 윈도우보다 1분 이상 오래된 항목 제거
            long expireTime = currentTime - 60000; // 1분 전
            delayCache.entrySet().removeIf(entry -> entry.getKey() < expireTime);
            
            return delay;
        });
    }
    
    /**
     * 메트릭 데이터 조회
     */
    private List<Preset.NumberMetrics> getMetrics(Integer windowSize) {
        List<MetricsRepositoryPort.NumberMetrics> metrics = 
            metricsRepositoryPort.findByWindowSize(windowSize != null ? windowSize : 50);
        
        return metrics.stream()
            .map(m -> new Preset.NumberMetrics(
                m.number(),
                m.frequency(),
                m.overdue(),
                m.lastSeenDrawNo()
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * 생성 결과
     */
    public record GenerateResult(
        List<GeneratedSet> generatedSets,
        Long setId  // Member인 경우에만 값이 있음
    ) {}
}
