package io.appback.lottoguide.infra.refresh;

import io.appback.lottoguide.infra.external.DonghaengLottoApiClient;
import io.appback.lottoguide.infra.external.dto.DrawApiResponse;
import io.appback.lottoguide.infra.persistence.entity.DrawEntity;
import io.appback.lottoguide.infra.persistence.repository.DrawRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 추첨 데이터 로드 서비스
 * 
 * 캐싱 전략:
 * 1. 요청 -> 캐싱 데이터 확인
 *    - 히트 -> 응답
 *    - 히트 실패 -> DB 검색
 *      - DB 있음 -> 캐싱 -> 응답
 *      - DB 없음 -> 외부로부터 얻어옴
 *        - 얻어옴 성공 -> DB 저장 -> 캐싱 -> 응답
 *        - 실패 -> 데이터 없이 랜덤하게 숫자 획득 -> 응답
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrawRefreshService {
    
    private final DrawRepository drawRepository;
    private final DonghaengLottoApiClient apiClient;
    private final MetricsRecomputeService metricsRecomputeService;
    private final PatternStatisticsCache patternStatisticsCache;
    
    // 메모리 캐시: 데이터 로드 여부 (true = 데이터 있음, false = 데이터 없음, null = 아직 확인 안 함)
    private volatile Boolean cacheDataLoaded = null;
    
    // 동시성 제어를 위한 Lock (여러 요청이 동시에 들어와도 1번만 로드)
    private final ReentrantLock loadLock = new ReentrantLock();
    private volatile boolean isLoading = false;
    
    // 수집 중단 플래그
    private volatile boolean isCancelled = false;
    
    /**
     * 데이터가 있는지 확인 (고객 API용)
     * 
     * 플로우:
     * 1. 메모리 캐시 확인 (히트면 바로 응답)
     * 2. 캐시 없음 -> DB 확인 (캐싱 후 응답)
     * 
     * 외부 API 호출 없음. DB 저장은 관리자 페이지에서만 수행.
     * 
     * @return 데이터 존재 여부 (true = 있음, false = 없음)
     */
    public boolean hasData() {
        // 1. 메모리 캐시 확인 (히트면 바로 응답)
        if (cacheDataLoaded != null && cacheDataLoaded) {
            log.debug("캐싱 히트: 메모리 캐시에 데이터 존재");
            return true;
        }
        
        if (cacheDataLoaded != null && !cacheDataLoaded) {
            log.debug("캐싱 히트: 메모리 캐시에 데이터 없음 확인됨");
            return false;
        }
        
        // 2. 캐시 없음 -> DB 확인 (캐싱 후 응답)
        long drawCount = drawRepository.count();
        
        if (drawCount > 0) {
            log.debug("DB에 추첨 데이터 존재: {}개 -> 캐싱", drawCount);
            cacheDataLoaded = true; // 캐싱
            return true;
        } else {
            // DB에 데이터 없음 -> 캐싱 (데이터 없음)
            cacheDataLoaded = false;
            return false;
        }
    }
    
    /**
     * 데이터가 있는지 확인하고, 없으면 로드 시도
     * 
     * @deprecated 고객 API에서는 사용하지 않음. 관리자 페이지에서만 사용.
     *             고객 API는 hasData()를 사용하세요.
     * 
     * @return 데이터 로드 성공 여부 (true = 성공, false = 실패)
     */
    @Deprecated
    @Transactional
    public boolean ensureDataLoaded() {
        // 1. 캐싱 데이터 확인
        if (cacheDataLoaded != null && cacheDataLoaded) {
            log.debug("캐싱 히트: 메모리 캐시에 데이터 존재");
            return true;
        }
        
        if (cacheDataLoaded != null && !cacheDataLoaded) {
            log.debug("캐싱 히트: 메모리 캐시에 데이터 없음 확인됨");
            return false;
        }
        
        // 2. 캐싱 히트 실패 -> DB 검색
        long drawCount = drawRepository.count();
        
        if (drawCount > 0) {
            log.debug("DB에 추첨 데이터 존재: {}개 -> 캐싱", drawCount);
            cacheDataLoaded = true; // 캐싱
            return true;
        }
        
        // 3. DB 없음 -> 외부로부터 얻어옴
        boolean loadSuccess = loadFromExternalApi();
        
        if (loadSuccess) {
            // 얻어옴 성공 -> DB 저장 -> 캐싱
            cacheDataLoaded = true;
            return true;
        } else {
            // 실패 -> 캐싱 (데이터 없음)
            cacheDataLoaded = false;
            return false;
        }
    }
    
    /**
     * 외부 API에서 데이터 로드 시도
     * 
     * @return 로드 성공 여부
     */
    private boolean loadFromExternalApi() {
        // 동시성 제어
        if (loadLock.tryLock()) {
            try {
                // 이중 체크 (Lock 획득 후 다시 확인)
                if (drawRepository.count() > 0) {
                    log.debug("다른 스레드가 이미 데이터 로드 완료");
                    return true;
                }
                
                if (isLoading) {
                    log.debug("다른 스레드가 로딩 중, 대기");
                    return false;
                }
                
                isLoading = true;
                log.info("DB에 추첨 데이터 없음, 외부 API에서 로드 시작");
                
                // 외부 API에서 데이터 로드 및 DB 저장
                try {
                    refreshDataFromExternalApi(false); // 고객 API용이므로 강제 업데이트 없음
                    log.info("추첨 데이터 로드 완료");
                    return true;
                } catch (Exception e) {
                    log.error("외부 API에서 추첨 데이터 로드 실패: {}", e.getMessage());
                    return false;
                }
            } finally {
                isLoading = false;
                loadLock.unlock();
            }
        } else {
            // 다른 스레드가 로딩 중이면 대기 후 재확인
            log.debug("다른 스레드가 로딩 중, 대기 후 재확인");
            int maxWait = 10; // 최대 10번 재시도 (약 1초)
            int waitCount = 0;
            
            while (waitCount < maxWait && drawRepository.count() == 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                waitCount++;
            }
            
            // 재확인
            if (drawRepository.count() > 0) {
                log.debug("다른 스레드 로딩 완료, 캐싱");
                return true;
            } else {
                log.warn("다른 스레드 로딩 대기 중 타임아웃");
                return false;
            }
        }
    }
    
    /**
     * 외부 API에서 추첨 데이터를 로드하여 DB에 저장 (관리자용, 프로덕션급)
     * 
     * 프로덕션급 수집 전략:
     * 1. 중단/재개 가능: DB에서 마지막 회차 확인 후 그 다음부터 시작
     * 2. 실패 회차 재시도: 실패한 회차는 별도 큐에 저장 후 재시도
     * 3. Rate Limiting: 회차 간 대기 시간으로 API 부하 방지
     * 4. 최신회차 탐지: 연속 실패 N회면 중단 (데이터 불일치 방지)
     * 
     * 관리자 페이지에서만 호출되어야 합니다.
     * 고객 API에서는 이 메서드를 호출하지 않습니다.
     * 
     * @return 수집 결과 (저장된 회차 수, 실패한 회차 수 포함)
     * @throws RuntimeException 최신 회차 번호 조회 실패 시
     */
    @Transactional
    public RefreshResult refreshDataFromExternalApi(boolean forceUpdate) {
        log.info("외부 API에서 추첨 데이터 로드 시작 (관리자 요청, 프로덕션급 수집 전략, forceUpdate={})", forceUpdate);
        
        // 중단 플래그 초기화
        isCancelled = false;
        
        try {
            // 1. DB에서 없는 최소 회차 찾기 (중간에 빈 회차가 있어도 채우기)
            int startDrawNo = findFirstMissingDrawNo();
            if (startDrawNo == -1) {
                // 모든 회차가 있거나 DB가 비어있음 -> 최신 회차 + 1부터 시작
                Optional<DrawEntity> lastDrawOpt = drawRepository.findFirstByOrderByDrawNoDesc();
                if (lastDrawOpt.isPresent()) {
                    startDrawNo = lastDrawOpt.get().getDrawNo() + 1;
                    log.info("DB에 모든 회차가 연속으로 존재, 마지막 회차: {} -> 다음 회차부터 수집 시작: {}", 
                        lastDrawOpt.get().getDrawNo(), startDrawNo);
                } else {
                    startDrawNo = 1;
                    log.info("DB에 데이터 없음 -> 1회차부터 전체 수집 시작");
                }
            } else {
                log.info("DB에 없는 최소 회차 발견: {} -> 이 회차부터 수집 시작", startDrawNo);
            }
            
            // 2. 최신 회차 번호 조회
            log.info("최신 회차 번호 조회 시도");
            Optional<Integer> latestDrawNoOpt = apiClient.findLatestDrawNo(0);
            
            if (latestDrawNoOpt.isEmpty()) {
                String errorMsg = "동행복권 API에서 최신 회차 번호를 찾을 수 없습니다. API가 변경되었거나 접근이 차단되었을 수 있습니다.";
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            int latestDrawNo = latestDrawNoOpt.get();
            log.info("최신 회차 번호 확인: {}", latestDrawNo);
            
            // 3. 수집 범위 확인
            if (startDrawNo > latestDrawNo) {
                log.info("이미 최신 데이터를 모두 수집함: DB 최신={}, API 최신={}", startDrawNo - 1, latestDrawNo);
                return new RefreshResult(0, 0, 0, "이미 최신 데이터를 모두 수집했습니다.");
            }
            
            int count = latestDrawNo - startDrawNo + 1;
            log.info("추첨 결과 {}개 수집 시작: {} ~ {}", count, startDrawNo, latestDrawNo);
            
            // 4. 회차별 수집 (1회차부터 순차적으로)
            int savedCount = 0;
            int failedCount = 0;
            List<Integer> failedDrawNos = new ArrayList<>(); // 실패한 회차 목록
            int consecutiveFailures = 0; // 연속 실패 횟수
            final int MAX_CONSECUTIVE_FAILURES = 10; // 최대 연속 실패 허용 횟수 (최신회차 탐지)
            final long RATE_LIMIT_DELAY_MS = 5000; // Rate limiting: 회차 간 5초 대기 (API 차단 방지)
            
            for (int drawNo = startDrawNo; drawNo <= latestDrawNo; drawNo++) {
                // 중단 플래그 확인
                if (isCancelled) {
                    log.warn("사용자 요청으로 수집 중단: drawNo={}", drawNo);
                    break;
                }
                
                // 강제 업데이트가 아닐 경우, 이미 존재하는 회차는 건너뛰기
                if (!forceUpdate && drawRepository.findByDrawNo(drawNo).isPresent()) {
                    log.debug("회차 {}는 이미 존재함, 건너뜀 (forceUpdate=false)", drawNo);
                    continue;
                }
                
                // 강제 업데이트일 경우 로그 출력
                if (forceUpdate && drawRepository.findByDrawNo(drawNo).isPresent()) {
                    log.info("회차 {} 강제 업데이트: 기존 데이터를 새로 수집하여 업데이트", drawNo);
                }
                
                // API 호출
                Optional<DrawApiResponse> apiResponseOpt = apiClient.fetchDraw(drawNo);
                
                if (apiResponseOpt.isPresent()) {
                    // 성공: DB에 저장 또는 업데이트
                    DrawApiResponse apiResponse = apiResponseOpt.get();
                    int[] numbers = apiResponse.getNumbers();
                    Arrays.sort(numbers); // 정렬
                    
                    // 기존 엔티티 조회 (강제 업데이트일 경우)
                    Optional<DrawEntity> existingOpt = drawRepository.findByDrawNo(drawNo);
                    DrawEntity drawEntity;
                    
                    if (existingOpt.isPresent() && forceUpdate) {
                        // 기존 데이터 업데이트
                        DrawEntity existing = existingOpt.get();
                        drawEntity = DrawEntity.builder()
                            .drawNo(existing.getDrawNo())
                            .drawDate(apiResponse.getDrawDate())
                            .n1(numbers[0])
                            .n2(numbers[1])
                            .n3(numbers[2])
                            .n4(numbers[3])
                            .n5(numbers[4])
                            .n6(numbers[5])
                            .bonus(apiResponse.getBnusNo())
                            .totalPrize(apiResponse.getTotalPrize())
                            .winnerCount(apiResponse.getWinnerCount())
                            .prizePerPerson(apiResponse.getPrizePerPerson())
                            .createdAt(existing.getCreatedAt()) // 기존 생성일 유지
                            .build();
                        log.debug("회차 {} 기존 데이터 업데이트", drawNo);
                    } else {
                        // 신규 저장
                        drawEntity = DrawEntity.builder()
                            .drawNo(apiResponse.getDrwNo())
                            .drawDate(apiResponse.getDrawDate())
                            .n1(numbers[0])
                            .n2(numbers[1])
                            .n3(numbers[2])
                            .n4(numbers[3])
                            .n5(numbers[4])
                            .n6(numbers[5])
                            .bonus(apiResponse.getBnusNo())
                            .totalPrize(apiResponse.getTotalPrize())
                            .winnerCount(apiResponse.getWinnerCount())
                            .prizePerPerson(apiResponse.getPrizePerPerson())
                            .build();
                    }
                    
                    drawRepository.save(drawEntity);
                    savedCount++;
                    consecutiveFailures = 0; // 성공 시 연속 실패 카운터 리셋
                    
                    // 저장 확인 (실제로 DB에 저장되었는지 확인)
                    Optional<DrawEntity> savedEntity = drawRepository.findByDrawNo(apiResponse.getDrwNo());
                    if (savedEntity.isPresent()) {
                        log.info("회차 {} 저장 완료 및 확인: drawDate={}, totalPrize={}, winnerCount={}, prizePerPerson={}", 
                            apiResponse.getDrwNo(), apiResponse.getDrawDate(), 
                            apiResponse.getTotalPrize(), apiResponse.getWinnerCount(), apiResponse.getPrizePerPerson());
                    } else {
                        log.error("회차 {} 저장 실패: DB에 저장되지 않음 (트랜잭션 롤백 가능성)", apiResponse.getDrwNo());
                    }
                    
                    // 진행 상황 로깅 (100회차마다)
                    if (savedCount % 100 == 0) {
                        log.info("수집 진행: {}개 저장 완료 (현재 회차: {}/{})", savedCount, drawNo, latestDrawNo);
                    }
                } else {
                    // 실패: 실패 목록에 추가
                    failedCount++;
                    failedDrawNos.add(drawNo);
                    consecutiveFailures++;
                    
                    log.warn("회차 {} 수집 실패 (연속 실패: {}/{})", drawNo, consecutiveFailures, MAX_CONSECUTIVE_FAILURES);
                    
                    // 연속 실패 시 더 긴 대기 시간 (API 차단 가능성 대비)
                    if (consecutiveFailures > 0 && consecutiveFailures % 3 == 0) {
                        long failureDelay = RATE_LIMIT_DELAY_MS * 2; // 실패 시 2배 대기
                        log.info("연속 실패 {}회, {}ms 대기 후 재시도", consecutiveFailures, failureDelay);
                        try {
                            Thread.sleep(failureDelay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("수집 중단: drawNo={}", drawNo);
                            break;
                        }
                    }
                    
                    // 연속 실패가 너무 많으면 중단 (최신회차 탐지 전략)
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        log.warn("연속 실패 횟수 초과 ({}회), 수집 중단. 최신 회차 도달로 판단: drawNo={}", 
                            consecutiveFailures, drawNo);
                        break;
                    }
                }
                
                // Rate limiting: API 부하 방지 (성공 시에도 대기)
                if (drawNo < latestDrawNo) {
                    try {
                        Thread.sleep(RATE_LIMIT_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("수집 중단: drawNo={}", drawNo);
                        break;
                    }
                }
                
                // 중단 플래그 재확인 (대기 중에 중단 요청이 들어올 수 있음)
                if (isCancelled) {
                    log.warn("사용자 요청으로 수집 중단: drawNo={}", drawNo);
                    break;
                }
            }
            
            // 중단된 경우 메시지 추가
            String statusMessage = isCancelled ? " (사용자 요청으로 중단됨)" : "";
            log.info("추첨 결과 수집 완료{}: 저장={}개, 실패={}개, 실패 회차={}", 
                statusMessage, savedCount, failedCount, failedDrawNos.size());
            
            // 5. 실패한 회차 재시도 (최대 1회, 중단되지 않은 경우에만)
            if (!failedDrawNos.isEmpty() && savedCount > 0 && !isCancelled) {
                log.info("실패한 회차 재시도 시작: {}개", failedDrawNos.size());
                int retrySavedCount = retryFailedDraws(failedDrawNos);
                savedCount += retrySavedCount;
                failedCount -= retrySavedCount;
                log.info("재시도 완료: 추가 저장={}개, 최종 실패={}개", retrySavedCount, failedCount);
            }
            
            // 6. 저장된 데이터가 없으면 실패로 처리
            if (savedCount == 0 && failedCount > 0) {
                String errorMsg = String.format("동행복권 API에서 추첨 데이터를 가져올 수 없습니다. 최신 회차: %d, 조회 시도: %d개, 모두 실패", 
                    latestDrawNo, failedCount);
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            // 7. 메트릭 재계산 트리거 (데이터 로드 성공 시 즉시 재계산)
            if (savedCount > 0) {
                log.info("메트릭 재계산 트리거: savedCount={}", savedCount);
                try {
                    metricsRecomputeService.recomputeAllMetrics();
                    // 패턴 통계도 재계산 (비동기)
                    patternStatisticsCache.recomputeAllPatternStatistics();
                } catch (Exception e) {
                    log.warn("메트릭 재계산 실패 (데이터는 저장됨): error={}", e.getMessage());
                    // 메트릭 재계산 실패는 치명적이지 않으므로 계속 진행
                }
            }
            
            // 8. 캐시 업데이트
            cacheDataLoaded = true;
            
            String message = isCancelled 
                ? String.format("수집 중단됨: 저장 %d개, 실패 %d개", savedCount, failedCount)
                : String.format("수집 완료: 저장 %d개, 실패 %d개", savedCount, failedCount);
            return new RefreshResult(savedCount, failedCount, latestDrawNo, message);
            
        } catch (RuntimeException e) {
            // 명시적으로 발생시킨 예외는 그대로 전파
            log.error("외부 API에서 추첨 데이터 로드 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // 예상치 못한 예외 (스택 트레이스 없이 메시지만)
            String errorMsg = String.format("외부 API에서 추첨 데이터 로드 중 예상치 못한 오류 발생: %s", e.getMessage());
            log.error(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }
    
    /**
     * DB에 없는 최소 회차 번호 찾기 (효율적인 방법)
     * 
     * 전제: 회차별 중복이 없음
     * 
     * 알고리즘:
     * 1. 총 개수와 최대 회차 번호 비교
     * 2. 같으면 모든 회차가 있음 (1부터 MAX까지 연속)
     * 3. 다르면 누락이 있음 -> 누락된 번호 검색
     * 4. 누락된 번호 중 최소값 반환
     * 
     * 예: DB에 1, 2, 3, 5, 6, 7이 있으면 (총 6개, MAX=7) -> 4 반환
     * 예: DB에 1, 2, 3이 있으면 (총 3개, MAX=3) -> -1 반환 (모든 회차가 연속)
     * 예: DB가 비어있으면 -> -1 반환
     * 
     * @return DB에 없는 최소 회차 번호, 없으면 -1
     */
    private int findFirstMissingDrawNo() {
        // 1. DB가 비어있는지 확인
        long totalCount = drawRepository.count();
        log.info("DB 상태 확인: 총 회차 수={}", totalCount);
        
        if (totalCount == 0) {
            log.info("DB에 데이터 없음");
            return -1; // DB가 비어있음
        }
        
        // 2. 최소 회차와 최대 회차 확인
        Optional<Integer> minDrawNoOpt = drawRepository.findMinDrawNo();
        Optional<Integer> maxDrawNoOpt = drawRepository.findMaxDrawNo();
        
        if (minDrawNoOpt.isEmpty() || maxDrawNoOpt.isEmpty()) {
            log.warn("DB에 데이터가 있다고 했지만 최소/최대 회차를 찾을 수 없음");
            return -1;
        }
        
        int minDrawNo = minDrawNoOpt.get();
        int maxDrawNo = maxDrawNoOpt.get();
        log.info("DB 상태 확인: 최소 회차={}, 최대 회차={}", minDrawNo, maxDrawNo);
        
        // 3. 최소 회차가 1이 아니면 1 반환 (1회차부터 빈 회차)
        if (minDrawNo > 1) {
            log.info("최소 회차가 1이 아님: {} -> 1회차부터 빈 회차", minDrawNo);
            return 1;
        }
        
        // 4. 총 개수와 최대 회차 번호 비교
        // 예상 개수 = maxDrawNo (1부터 maxDrawNo까지 모두 있으면 maxDrawNo개)
        long expectedCount = maxDrawNo;
        log.info("회차 연속성 확인: 총 개수={}, 최대 회차={}, 예상 개수={}", 
            totalCount, maxDrawNo, expectedCount);
        
        if (totalCount == expectedCount) {
            // 모든 회차가 연속으로 존재 (1부터 maxDrawNo까지)
            log.info("모든 회차가 연속으로 존재: 총 {}개, 최대 회차 {} (1부터 {}까지 모두 존재)", 
                totalCount, maxDrawNo, maxDrawNo);
            return -1;
        }
        
        // 5. 누락이 있음 -> 누락된 번호 검색
        long missingCount = expectedCount - totalCount;
        log.info("누락된 회차 발견: 총 {}개, 최대 회차 {}, 누락 개수 {} (예상: 1부터 {}까지 {}개 있어야 함)", 
            totalCount, maxDrawNo, missingCount, maxDrawNo, expectedCount);
        
        // 6. SQL로 첫 번째 누락된 회차 찾기
        Optional<Integer> missingDrawNoOpt = drawRepository.findFirstMissingDrawNo();
        if (missingDrawNoOpt.isPresent()) {
            int missingDrawNo = missingDrawNoOpt.get();
            log.info("첫 번째 누락된 회차 발견: {}", missingDrawNo);
            return missingDrawNo;
        }
        
        // 7. SQL로 찾지 못한 경우 (이론적으로는 발생하지 않아야 함)
        log.warn("누락이 있다고 판단했지만 SQL로 찾지 못함. 총 {}개, 최대 회차 {}", 
            totalCount, maxDrawNo);
        return -1;
    }
    
    /**
     * 실패한 회차 재시도
     * 
     * @param failedDrawNos 실패한 회차 번호 목록
     * @return 재시도로 저장된 회차 수
     */
    private int retryFailedDraws(List<Integer> failedDrawNos) {
        int retrySavedCount = 0;
        final long RETRY_DELAY_MS = 5000; // 재시도 시 5초 대기 시간
        
        for (Integer drawNo : failedDrawNos) {
            // 이미 저장되었는지 확인 (다른 스레드가 저장했을 수 있음)
            if (drawRepository.findByDrawNo(drawNo).isPresent()) {
                log.debug("회차 {}는 이미 저장됨 (재시도 불필요)", drawNo);
                continue;
            }
            
            Optional<DrawApiResponse> apiResponseOpt = apiClient.fetchDraw(drawNo);
            
            if (apiResponseOpt.isPresent()) {
                DrawApiResponse apiResponse = apiResponseOpt.get();
                int[] numbers = apiResponse.getNumbers();
                Arrays.sort(numbers);
                
                DrawEntity drawEntity = DrawEntity.builder()
                    .drawNo(apiResponse.getDrwNo())
                    .drawDate(apiResponse.getDrawDate())
                    .n1(numbers[0])
                    .n2(numbers[1])
                    .n3(numbers[2])
                    .n4(numbers[3])
                    .n5(numbers[4])
                    .n6(numbers[5])
                    .bonus(apiResponse.getBnusNo())
                    .totalPrize(apiResponse.getTotalPrize())
                    .winnerCount(apiResponse.getWinnerCount())
                    .prizePerPerson(apiResponse.getPrizePerPerson())
                    .build();
                
                drawRepository.save(drawEntity);
                retrySavedCount++;
                log.debug("회차 {} 재시도 성공: totalPrize={}, winnerCount={}, prizePerPerson={}", 
                    drawNo, apiResponse.getTotalPrize(), apiResponse.getWinnerCount(), apiResponse.getPrizePerPerson());
            } else {
                log.warn("회차 {} 재시도 실패", drawNo);
            }
            
            // Rate limiting
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return retrySavedCount;
    }
    
    /**
     * 수집 결과
     */
    public record RefreshResult(
        int savedCount,      // 저장된 회차 수
        int failedCount,     // 실패한 회차 수
        int latestDrawNo,    // 최신 회차 번호
        String message       // 결과 메시지
    ) {}
    
    /**
     * 수집 중단 요청
     */
    public void cancelRefresh() {
        isCancelled = true;
        log.info("수집 중단 요청 수신");
    }
    
    /**
     * 수집 중단 상태 확인
     */
    public boolean isCancelled() {
        return isCancelled;
    }
    
    /**
     * 캐시 초기화 (테스트용 또는 수동 갱신 시)
     */
    public void clearCache() {
        cacheDataLoaded = null;
        // 패턴 통계 캐시도 무효화
        patternStatisticsCache.invalidateCache();
        log.info("캐시 초기화 완료");
    }
}
