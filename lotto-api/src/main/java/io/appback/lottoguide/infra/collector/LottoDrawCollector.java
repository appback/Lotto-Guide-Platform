package io.appback.lottoguide.infra.collector;

import io.appback.lottoguide.infra.external.DonghaengLottoApiClient;
import io.appback.lottoguide.infra.external.dto.DrawApiResponse;
import io.appback.lottoguide.infra.persistence.entity.DrawEntity;
import io.appback.lottoguide.infra.persistence.repository.DrawRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * 로또 추첨 데이터 수집기
 * 
 * 특징:
 * - 1회차부터 지정된 회차까지 순차적으로 수집
 * - 이미 저장된 회차는 자동으로 스킵
 * - HTML 응답 등 실패한 회차는 건너뛰고 계속 진행
 * - 재실행 시 실패한 회차만 다시 시도 가능
 * 
 * 사용 예시:
 * - collectRange(1, 1206): 1회차부터 1206회차까지 수집
 * - 중간에 실패해도 재실행하면 남은 회차만 계속 채움
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LottoDrawCollector {
    
    private final DrawRepository drawRepository;
    private final DonghaengLottoApiClient apiClient;
    
    /**
     * 지정된 범위의 회차를 수집하여 DB에 저장
     * 
     * @param from 시작 회차 (1 이상)
     * @param to 종료 회차 (from 이상)
     * @return 수집 결과 통계
     */
    @Transactional
    public CollectResult collectRange(int from, int to) {
        if (from < 1 || to < from) {
            log.warn("잘못된 범위: from={}, to={}", from, to);
            return CollectResult.builder()
                    .from(from)
                    .to(to)
                    .success(0)
                    .skip(0)
                    .fail(0)
                    .build();
        }
        
        log.info("회차 수집 시작: {}회차 ~ {}회차", from, to);
        
        int success = 0;
        int skip = 0;
        int fail = 0;
        
        for (int drawNo = from; drawNo <= to; drawNo++) {
            // 이미 저장된 회차는 스킵
            if (drawRepository.existsByDrawNo(drawNo)) {
                skip++;
                if (drawNo % 100 == 0) {
                    log.debug("회차 {} 이미 저장됨 (스킵)", drawNo);
                }
                continue;
            }
            
            // 회차 데이터 수집 시도
            boolean saved = fetchAndSave(drawNo);
            if (saved) {
                success++;
                // 진행 상황 로깅 (100회차마다)
                if (success % 100 == 0) {
                    log.info("수집 진행: {}회차까지 저장 완료 (성공: {}개, 스킵: {}개, 실패: {}개)", 
                            drawNo, success, skip, fail);
                }
            } else {
                fail++;
                if (fail % 10 == 0) {
                    log.warn("수집 실패 누적: {}회차까지 실패 {}개", drawNo, fail);
                }
            }
            
            // Rate limiting (API 부하 방지)
            if (drawNo < to) {
                sleepSilently(200); // 200ms 대기
            }
        }
        
        CollectResult result = CollectResult.builder()
                .from(from)
                .to(to)
                .success(success)
                .skip(skip)
                .total(to - from + 1)
                .fail(fail)
                .build();
        
        log.info("회차 수집 완료: 범위={}~{}, 성공={}개, 스킵={}개, 실패={}개, 총={}개", 
                from, to, success, skip, fail, result.getTotal());
        
        return result;
    }
    
    /**
     * 특정 회차를 외부 API에서 가져와서 DB에 저장
     * 
     * @param drawNo 회차 번호
     * @return 저장 성공 여부
     */
    private boolean fetchAndSave(int drawNo) {
        try {
            // 외부 API에서 데이터 가져오기
            // DonghaengLottoApiClient는 이미 HTML 감지, 재시도 로직이 포함되어 있음
            var apiResponseOpt = apiClient.fetchDraw(drawNo);
            
            if (apiResponseOpt.isEmpty()) {
                log.debug("회차 {} 수집 실패: API 응답 없음", drawNo);
                return false;
            }
            
            DrawApiResponse apiResponse = apiResponseOpt.get();
            
            // 응답 검증
            if (!apiResponse.isSuccess()) {
                log.debug("회차 {} 수집 실패: API 응답 실패 (returnValue={})", 
                        drawNo, apiResponse.getReturnValue());
                return false;
            }
            
            // 회차 번호 일치 확인
            if (apiResponse.getDrwNo() == null || !apiResponse.getDrwNo().equals(drawNo)) {
                log.warn("회차 {} 수집 실패: 회차 번호 불일치 (응답 회차: {})", 
                        drawNo, apiResponse.getDrwNo());
                return false;
            }
            
            // 번호 배열 가져오기 및 정렬
            int[] numbers = apiResponse.getNumbers();
            Arrays.sort(numbers);
            
            // 추첨일 가져오기
            var drawDate = apiResponse.getDrawDate();
            if (drawDate == null) {
                log.warn("회차 {} 수집 실패: 추첨일 없음", drawNo);
                return false;
            }
            
            // 엔티티 생성 및 저장
            DrawEntity entity = DrawEntity.builder()
                    .drawNo(drawNo)
                    .drawDate(drawDate)
                    .n1(numbers[0])
                    .n2(numbers[1])
                    .n3(numbers[2])
                    .n4(numbers[3])
                    .n5(numbers[4])
                    .n6(numbers[5])
                    .bonus(apiResponse.getBnusNo())
                    .build();
            
            drawRepository.save(entity);
            log.debug("회차 {} 저장 완료: 날짜={}, 번호={}, 보너스={}", 
                    drawNo, drawDate, Arrays.toString(numbers), apiResponse.getBnusNo());
            
            return true;
            
        } catch (Exception e) {
            log.warn("회차 {} 수집 중 예외 발생: {}", drawNo, e.getMessage());
            return false;
        }
    }
    
    /**
     * 안전한 대기 (InterruptedException 처리)
     */
    private static void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("수집 중단: 인터럽트 발생");
        }
    }
    
    /**
     * 수집 결과 통계
     */
    @lombok.Data
    @lombok.Builder
    public static class CollectResult {
        private int from;
        private int to;
        private int success;
        private int skip;
        private int fail;
        private int total;
    }
}
