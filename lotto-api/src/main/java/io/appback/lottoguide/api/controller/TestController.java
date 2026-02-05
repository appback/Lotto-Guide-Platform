package io.appback.lottoguide.api.controller;

import io.appback.lottoguide.infra.external.DonghaengLottoApiClient;
import io.appback.lottoguide.infra.external.dto.DrawApiResponse;
import io.appback.lottoguide.infra.refresh.DrawRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 테스트용 컨트롤러
 * 외부 API 연동 테스트용
 */
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {
    
    private final DonghaengLottoApiClient apiClient;
    private final DrawRefreshService drawRefreshService;
    
    /**
     * 특정 회차 조회 테스트
     */
    @GetMapping("/draw/{drawNo}")
    public ResponseEntity<Map<String, Object>> testFetchDraw(@PathVariable int drawNo) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("테스트: 회차 {} 조회 시작", drawNo);
            Optional<DrawApiResponse> response = apiClient.fetchDraw(drawNo);
            
            if (response.isPresent()) {
                DrawApiResponse draw = response.get();
                result.put("success", true);
                result.put("drawNo", draw.getDrwNo());
                result.put("drawDate", draw.getDrwNoDate());
                result.put("numbers", draw.getNumbers());
                result.put("bonus", draw.getBnusNo());
                log.info("테스트: 회차 {} 조회 성공", drawNo);
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("message", "회차 데이터를 찾을 수 없습니다: " + drawNo);
                result.put("suggestion", "최신 회차 번호를 먼저 확인해보세요: /api/v1/test/latest-draw-no");
                log.warn("테스트: 회차 {} 조회 실패 - 데이터 없음 (아직 발표되지 않았거나 존재하지 않는 회차일 수 있습니다)", drawNo);
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("테스트: 회차 {} 조회 중 예외 발생", drawNo, e);
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 최신 회차 번호 조회 테스트
     */
    @GetMapping("/latest-draw-no")
    public ResponseEntity<Map<String, Object>> testFindLatestDrawNo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("테스트: 최신 회차 번호 조회 시작");
            Optional<Integer> latestDrawNo = apiClient.findLatestDrawNo(0);
            
            if (latestDrawNo.isPresent()) {
                result.put("success", true);
                result.put("latestDrawNo", latestDrawNo.get());
                
                // 최신 회차의 실제 데이터도 함께 조회
                Optional<DrawApiResponse> latestDraw = apiClient.fetchDraw(latestDrawNo.get());
                if (latestDraw.isPresent()) {
                    DrawApiResponse draw = latestDraw.get();
                    result.put("latestDrawDate", draw.getDrwNoDate());
                    result.put("latestDrawNumbers", draw.getNumbers());
                }
                
                log.info("테스트: 최신 회차 번호 조회 성공: {}", latestDrawNo.get());
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("message", "최신 회차 번호를 찾을 수 없습니다");
                log.warn("테스트: 최신 회차 번호 조회 실패");
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("테스트: 최신 회차 번호 조회 중 예외 발생", e);
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 데이터 로드 테스트 (전체 플로우)
     */
    @PostMapping("/load-data")
    public ResponseEntity<Map<String, Object>> testLoadData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean loaded = drawRefreshService.ensureDataLoaded();
            
            result.put("success", loaded);
            result.put("message", loaded ? "데이터 로드 성공" : "데이터 로드 실패 (랜덤 생성 모드)");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
