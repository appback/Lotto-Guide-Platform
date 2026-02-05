package io.appback.lottoguide.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appback.lottoguide.infra.collector.LottoDrawCollector;
import io.appback.lottoguide.infra.external.DonghaengLottoApiClient;
import io.appback.lottoguide.infra.external.dto.DrawApiResponse;
import io.appback.lottoguide.infra.persistence.entity.DrawEntity;
import io.appback.lottoguide.infra.persistence.entity.StrategyDescriptionEntity;
import io.appback.lottoguide.infra.persistence.entity.DestinyLimitMessageEntity;
import io.appback.lottoguide.infra.persistence.entity.AiLoadingMessageEntity;
import io.appback.lottoguide.infra.persistence.repository.DrawRepository;
import io.appback.lottoguide.infra.persistence.repository.StrategyDescriptionRepository;
import io.appback.lottoguide.infra.persistence.repository.DestinyLimitMessageRepository;
import io.appback.lottoguide.infra.persistence.repository.AiLoadingMessageRepository;
import io.appback.lottoguide.infra.persistence.repository.MissionTemplateRepository;
import io.appback.lottoguide.infra.persistence.entity.MissionTemplateEntity;
import io.appback.lottoguide.infra.persistence.repository.MissionPhraseARepository;
import io.appback.lottoguide.infra.persistence.repository.MissionPhraseBRepository;
import io.appback.lottoguide.infra.persistence.repository.MissionPhraseCRepository;
import io.appback.lottoguide.infra.persistence.entity.MissionPhraseAEntity;
import io.appback.lottoguide.infra.persistence.entity.MissionPhraseBEntity;
import io.appback.lottoguide.infra.persistence.entity.MissionPhraseCEntity;
import io.appback.lottoguide.infra.persistence.repository.SystemOptionRepository;
import io.appback.lottoguide.infra.persistence.entity.SystemOptionEntity;
import io.appback.lottoguide.infra.refresh.DrawRefreshService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.LinkedHashSet;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 관리자 API Controller
 * 
 * 데이터 수집 및 관리 기능을 제공합니다.
 * 고객 API와 달리 외부 API 호출을 통한 데이터 수집이 가능합니다.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final DrawRefreshService drawRefreshService;
    private final DrawRepository drawRepository;
    private final DonghaengLottoApiClient apiClient;
    private final LottoDrawCollector lottoDrawCollector;
    private final StrategyDescriptionRepository strategyDescriptionRepository;
    private final DestinyLimitMessageRepository destinyLimitMessageRepository;
    private final AiLoadingMessageRepository aiLoadingMessageRepository;
    private final MissionTemplateRepository missionTemplateRepository;
    private final MissionPhraseARepository missionPhraseARepository;
    private final MissionPhraseBRepository missionPhraseBRepository;
    private final MissionPhraseCRepository missionPhraseCRepository;
    private final SystemOptionRepository systemOptionRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 외부 API에서 추첨 데이터 수집 및 DB 저장
     * 
     * POST /api/v1/admin/refresh-data
     * 
     * 동행복권 API에서 최신 추첨 데이터를 가져와서 DB에 저장합니다.
     * 이미 존재하는 회차는 건너뜁니다.
     * 
     * @return 수집 결과
     */
    @PostMapping("/refresh-data")
    public ResponseEntity<Map<String, Object>> refreshData(
            @RequestParam(defaultValue = "false") boolean forceUpdate) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 외부 API에서 추첨 데이터 수집 시작 (forceUpdate={})", forceUpdate);
            
            DrawRefreshService.RefreshResult refreshResult = drawRefreshService.refreshDataFromExternalApi(forceUpdate);
            
            result.put("success", true);
            result.put("message", refreshResult.message());
            result.put("savedCount", refreshResult.savedCount());
            result.put("failedCount", refreshResult.failedCount());
            result.put("latestDrawNo", refreshResult.latestDrawNo());
            
            log.info("관리자 요청: 데이터 수집 완료 - 저장: {}개, 실패: {}개", 
                refreshResult.savedCount(), refreshResult.failedCount());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 데이터 수집 실패", e);
            
            result.put("success", false);
            result.put("message", "데이터 수집 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 수집 중단 요청
     * 
     * POST /api/v1/admin/refresh-data/cancel
     * 
     * 현재 진행 중인 데이터 수집 작업을 중단합니다.
     * 
     * @return 중단 요청 결과
     */
    @PostMapping("/refresh-data/cancel")
    public ResponseEntity<Map<String, Object>> cancelRefreshData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 데이터 수집 중단 요청");
            
            drawRefreshService.cancelRefresh();
            
            result.put("success", true);
            result.put("message", "수집 중단 요청이 전달되었습니다. 현재 진행 중인 회차 처리 후 중단됩니다.");
            
            log.info("관리자 요청: 데이터 수집 중단 요청 완료");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 데이터 수집 중단 요청 실패", e);
            
            result.put("success", false);
            result.put("message", "중단 요청 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 데이터 존재 여부 확인
     * 
     * GET /api/v1/admin/data-status
     * 
     * DB와 캐시에 데이터가 있는지 확인합니다.
     * 외부 API 호출은 하지 않습니다.
     * 
     * @return 데이터 상태
     */
    @GetMapping("/data-status")
    public ResponseEntity<Map<String, Object>> getDataStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean hasData = drawRefreshService.hasData();
            
            result.put("hasData", hasData);
            result.put("message", hasData ? "데이터가 존재합니다" : "데이터가 없습니다");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("데이터 상태 확인 실패", e);
            
            result.put("hasData", false);
            result.put("message", "데이터 상태 확인 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 캐시 초기화
     * 
     * POST /api/v1/admin/clear-cache
     * 
     * 메모리 캐시를 초기화합니다.
     * 다음 요청 시 DB에서 다시 확인합니다.
     * 
     * @return 캐시 초기화 결과
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<Map<String, Object>> clearCache() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            drawRefreshService.clearCache();
            
            result.put("success", true);
            result.put("message", "캐시 초기화 완료");
            
            log.info("관리자 요청: 캐시 초기화 완료");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("캐시 초기화 실패", e);
            
            result.put("success", false);
            result.put("message", "캐시 초기화 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 누락된 회차 자동 저장
     * 
     * POST /api/v1/admin/refresh-missing
     * 
     * 1회차부터 최신 회차까지 모든 회차를 확인하여,
     * DB에 없는 누락된 회차를 외부 API에서 가져와 저장합니다.
     * 
     * @return 수집 결과
     */
    @PostMapping("/refresh-missing")
    @Transactional
    public ResponseEntity<Map<String, Object>> refreshMissing() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 누락된 회차 자동 저장 시작");
            
            // 1. 최신 회차 번호 조회
            Optional<Integer> latestDrawNoOpt = apiClient.findLatestDrawNo(0);
            if (latestDrawNoOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "최신 회차 번호를 계산할 수 없습니다.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            
            int latestDrawNo = latestDrawNoOpt.get();
            log.info("최신 회차 번호: {}", latestDrawNo);
            
            // 2. DB에 있는 모든 회차 번호 조회
            List<Integer> existingDrawNos = drawRepository.findAll().stream()
                    .map(DrawEntity::getDrawNo)
                    .collect(Collectors.toSet())
                    .stream()
                    .sorted()
                    .collect(Collectors.toList());
            
            Set<Integer> existingDrawNoSet = new HashSet<>(existingDrawNos);
            log.info("DB에 저장된 회차 수: {}개", existingDrawNos.size());
            
            // 3. 누락된 회차 찾기 (1회차부터 최신 회차까지)
            List<Integer> missingDrawNos = new ArrayList<>();
            for (int drawNo = 1; drawNo <= latestDrawNo; drawNo++) {
                if (!existingDrawNoSet.contains(drawNo)) {
                    missingDrawNos.add(drawNo);
                }
            }
            
            log.info("누락된 회차 수: {}개", missingDrawNos.size());
            
            if (missingDrawNos.isEmpty()) {
                result.put("success", true);
                result.put("message", "누락된 회차가 없습니다.");
                result.put("savedCount", 0);
                result.put("failedCount", 0);
                result.put("missingCount", 0);
                result.put("latestDrawNo", latestDrawNo);
                return ResponseEntity.ok(result);
            }
            
            // 4. 누락된 회차를 외부 API에서 가져와 저장
            int savedCount = 0;
            int failedCount = 0;
            List<Integer> failedDrawNos = new ArrayList<>();
            final long RATE_LIMIT_DELAY_MS = 300; // Rate limiting
            
            for (Integer drawNo : missingDrawNos) {
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
                            .build();
                    
                    drawRepository.save(drawEntity);
                    savedCount++;
                    
                    log.debug("누락 회차 {} 저장 완료", drawNo);
                    
                    // 진행 상황 로깅 (100회차마다)
                    if (savedCount % 100 == 0) {
                        log.info("누락 회차 수집 진행: {}개 저장 완료", savedCount);
                    }
                } else {
                    failedCount++;
                    failedDrawNos.add(drawNo);
                    log.warn("누락 회차 {} 수집 실패", drawNo);
                }
                
                // Rate limiting
                if (drawNo < missingDrawNos.get(missingDrawNos.size() - 1)) {
                    try {
                        Thread.sleep(RATE_LIMIT_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("누락 회차 수집 중단: drawNo={}", drawNo);
                        break;
                    }
                }
            }
            
            // 5. 캐시 업데이트
            drawRefreshService.clearCache();
            
            String message = String.format("누락 회차 수집 완료: 저장 %d개, 실패 %d개 (총 누락 %d개)", 
                    savedCount, failedCount, missingDrawNos.size());
            
            result.put("success", true);
            result.put("message", message);
            result.put("savedCount", savedCount);
            result.put("failedCount", failedCount);
            result.put("missingCount", missingDrawNos.size());
            result.put("latestDrawNo", latestDrawNo);
            result.put("failedDrawNos", failedDrawNos);
            
            log.info("관리자 요청: 누락 회차 수집 완료 - 저장: {}개, 실패: {}개", savedCount, failedCount);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 누락 회차 수집 실패", e);
            
            result.put("success", false);
            result.put("message", "누락 회차 수집 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 수동 저장 (JSON 데이터 직접 전달)
     * 
     * POST /api/v1/admin/save-draw
     * 
     * 관리자가 직접 회차별 데이터를 JSON으로 전달하여 저장합니다.
     * 
     * @param request 저장할 추첨 데이터
     * @return 저장 결과
     */
    @PostMapping("/save-draw")
    @Transactional
    public ResponseEntity<Map<String, Object>> saveDraw(@RequestBody SaveDrawRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 수동 저장 시작 - 회차: {}", request.getDrawNo());
            
            // 1. 요청 데이터 검증
            if (request.getDrawNo() == null || request.getDrawNo() < 1) {
                result.put("success", false);
                result.put("message", "회차 번호가 올바르지 않습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            if (request.getDrawDate() == null) {
                result.put("success", false);
                result.put("message", "추첨일이 필요합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            if (request.getNumbers() == null || request.getNumbers().length != 6) {
                result.put("success", false);
                result.put("message", "번호는 6개가 필요합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            if (request.getBonus() == null) {
                result.put("success", false);
                result.put("message", "보너스 번호가 필요합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            // 2. 이미 존재하는 회차 확인 (업데이트 지원)
            Optional<DrawEntity> existingOpt = drawRepository.findByDrawNo(request.getDrawNo());
            boolean isUpdate = existingOpt.isPresent();
            
            // 3. 번호 정렬
            int[] numbers = request.getNumbers().clone();
            Arrays.sort(numbers);
            
            // 4. 엔티티 생성 또는 업데이트
            DrawEntity drawEntity;
            if (isUpdate) {
                // 기존 엔티티 업데이트
                DrawEntity existing = existingOpt.get();
                drawEntity = DrawEntity.builder()
                        .drawNo(existing.getDrawNo())
                        .drawDate(request.getDrawDate())
                        .n1(numbers[0])
                        .n2(numbers[1])
                        .n3(numbers[2])
                        .n4(numbers[3])
                        .n5(numbers[4])
                        .n6(numbers[5])
                        .bonus(request.getBonus())
                        .totalPrize(request.getTotalPrize() != null ? request.getTotalPrize() : existing.getTotalPrize())
                        .winnerCount(request.getWinnerCount() != null ? request.getWinnerCount() : existing.getWinnerCount())
                        .prizePerPerson(request.getPrizePerPerson() != null ? request.getPrizePerPerson() : existing.getPrizePerPerson())
                        .createdAt(existing.getCreatedAt()) // 기존 생성일 유지
                        .build();
                log.info("관리자 요청: 회차 {} 업데이트", request.getDrawNo());
            } else {
                // 새 엔티티 생성
                drawEntity = DrawEntity.builder()
                        .drawNo(request.getDrawNo())
                        .drawDate(request.getDrawDate())
                        .n1(numbers[0])
                        .n2(numbers[1])
                        .n3(numbers[2])
                        .n4(numbers[3])
                        .n5(numbers[4])
                        .n6(numbers[5])
                        .bonus(request.getBonus())
                        .totalPrize(request.getTotalPrize())
                        .winnerCount(request.getWinnerCount())
                        .prizePerPerson(request.getPrizePerPerson())
                        .build();
                log.info("관리자 요청: 회차 {} 신규 저장", request.getDrawNo());
            }
            
            drawRepository.save(drawEntity);
            
            // 5. 캐시 초기화
            drawRefreshService.clearCache();
            
            result.put("success", true);
            result.put("message", String.format("회차 %d %s 완료", request.getDrawNo(), isUpdate ? "업데이트" : "저장"));
            result.put("drawNo", request.getDrawNo());
            result.put("isUpdate", isUpdate);
            
            log.info("관리자 요청: 수동 저장 완료 - 회차: {}", request.getDrawNo());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 수동 저장 실패", e);
            
            result.put("success", false);
            result.put("message", "수동 저장 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 공공데이터 API에서 특정 회차 조회 및 저장
     * 
     * POST /api/v1/admin/fetch-and-save-draw
     * 
     * 동행복권 API에서 특정 회차의 데이터를 조회하고, 성공 시 DB에 저장합니다.
     * 
     * @param drawNo 회차 번호
     * @return 조회 및 저장 결과
     */
    @PostMapping("/fetch-and-save-draw")
    @Transactional
    public ResponseEntity<Map<String, Object>> fetchAndSaveDraw(@RequestParam int drawNo) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 공공데이터 API에서 회차 {} 조회 및 저장 시작", drawNo);
            
            // 1. 회차 번호 검증
            if (drawNo < 1) {
                result.put("success", false);
                result.put("message", "회차 번호는 1 이상이어야 합니다.");
                result.put("drawNo", drawNo);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            // 2. 동행복권 API에서 조회
            Optional<DrawApiResponse> apiResponseOpt = apiClient.fetchDraw(drawNo);
            
            if (apiResponseOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", String.format("회차 %d의 데이터를 동행복권 API에서 가져올 수 없습니다. 회차가 존재하지 않거나 API 오류가 발생했을 수 있습니다.", drawNo));
                result.put("drawNo", drawNo);
                result.put("fetched", false);
                result.put("saved", false);
                return ResponseEntity.ok(result);
            }
            
            DrawApiResponse apiResponse = apiResponseOpt.get();
            log.info("동행복권 API 조회 성공: 회차 {}, 추첨일 {}", apiResponse.getDrwNo(), apiResponse.getDrwNoDate());
            
            // 3. 이미 존재하는 회차 확인
            Optional<DrawEntity> existingOpt = drawRepository.findByDrawNo(drawNo);
            boolean isUpdate = existingOpt.isPresent();
            
            // 4. 번호 정렬
            int[] numbers = apiResponse.getNumbers();
            Arrays.sort(numbers);
            
            // 5. 엔티티 생성 또는 업데이트
            DrawEntity drawEntity;
            if (isUpdate) {
                // 기존 엔티티 업데이트
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
                        .totalPrize(existing.getTotalPrize())
                        .winnerCount(existing.getWinnerCount())
                        .prizePerPerson(existing.getPrizePerPerson())
                        .createdAt(existing.getCreatedAt())
                        .build();
                log.info("회차 {} 업데이트", drawNo);
            } else {
                // 새 엔티티 생성
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
                        .build();
                log.info("회차 {} 신규 저장", drawNo);
            }
            
            drawRepository.save(drawEntity);
            
            // 6. 캐시 초기화
            drawRefreshService.clearCache();
            
            // 7. 결과 반환
            Map<String, Object> drawData = new HashMap<>();
            drawData.put("drawNo", apiResponse.getDrwNo());
            drawData.put("drawDate", apiResponse.getDrwNoDate());
            drawData.put("numbers", Arrays.asList(numbers[0], numbers[1], numbers[2], numbers[3], numbers[4], numbers[5]));
            drawData.put("bonus", apiResponse.getBnusNo());
            
            result.put("success", true);
            result.put("message", String.format("회차 %d 조회 및 %s 완료", drawNo, isUpdate ? "업데이트" : "저장"));
            result.put("drawNo", drawNo);
            result.put("fetched", true);
            result.put("saved", true);
            result.put("isUpdate", isUpdate);
            result.put("data", drawData);
            
            log.info("관리자 요청: 회차 {} 조회 및 저장 완료", drawNo);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 회차 {} 조회 및 저장 실패", drawNo, e);
            
            result.put("success", false);
            result.put("message", "조회 및 저장 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            result.put("drawNo", drawNo);
            result.put("fetched", false);
            result.put("saved", false);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * CSV 다운로드
     * 
     * GET /api/v1/admin/export-csv
     * 
     * DB에 저장된 모든 추첨 데이터를 CSV 파일로 다운로드합니다.
     * 
     * CSV 형식:
     * drawNo,drawDate,n1,n2,n3,n4,n5,n6,bonus,totalPrize,winnerCount,prizePerPerson
     * 1,2002-12-07,10,23,29,33,37,40,16,280.3,15,18.7
     * 
     * 주의: totalPrize, winnerCount, prizePerPerson은 선택적 필드입니다 (없으면 빈 값)
     * 
     * @return CSV 파일 다운로드
     */
    @GetMapping("/export-csv")
    public ResponseEntity<byte[]> exportCsv() {
        try {
            log.info("관리자 요청: CSV 다운로드 시작");
            
            // 1. DB에서 모든 데이터 조회 (회차 순으로 정렬)
            List<DrawEntity> draws = drawRepository.findAll().stream()
                    .sorted(Comparator.comparing(DrawEntity::getDrawNo))
                    .collect(Collectors.toList());
            
            log.info("CSV 다운로드: 총 {}개 회차 데이터", draws.size());
            
            // 2. CSV 생성
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8);
            
            // BOM 추가 (Excel에서 UTF-8 인식)
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);
            
            // 헤더 작성
            writer.println("drawNo,drawDate,n1,n2,n3,n4,n5,n6,bonus,totalPrize,winnerCount,prizePerPerson");
            
            // 데이터 작성
            DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
            for (DrawEntity draw : draws) {
                writer.printf("%d,%s,%d,%d,%d,%d,%d,%d,%d,%s,%s,%s%n",
                        draw.getDrawNo(),
                        draw.getDrawDate().format(dateFormatter),
                        draw.getN1(),
                        draw.getN2(),
                        draw.getN3(),
                        draw.getN4(),
                        draw.getN5(),
                        draw.getN6(),
                        draw.getBonus(),
                        draw.getTotalPrize() != null ? draw.getTotalPrize() : "",
                        draw.getWinnerCount() != null ? draw.getWinnerCount() : "",
                        draw.getPrizePerPerson() != null ? draw.getPrizePerPerson() : "");
            }
            
            writer.flush();
            byte[] csvBytes = baos.toByteArray();
            writer.close();
            
            // 3. 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", 
                    String.format("lotto_draws_%s.csv", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))));
            headers.setContentLength(csvBytes.length);
            
            log.info("관리자 요청: CSV 다운로드 완료 - {}개 회차, {} bytes", draws.size(), csvBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvBytes);
            
        } catch (Exception e) {
            log.error("관리자 요청: CSV 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * CSV 업로드
     * 
     * POST /api/v1/admin/import-csv
     * 
     * CSV 파일을 업로드하여 DB에 저장합니다.
     * 
     * CSV 형식:
     * drawNo,drawDate,n1,n2,n3,n4,n5,n6,bonus,totalPrize,winnerCount,prizePerPerson
     * 1,2002-12-07,10,23,29,33,37,40,16,280.3,15,18.7
     * 2,2002-12-14,9,13,21,25,32,42,5,,
     * 
     * 주의: totalPrize, winnerCount, prizePerPerson은 선택적 필드입니다 (없으면 빈 값)
     * winnerCount는 "15명" 형식도 지원합니다 (자동으로 "명" 제거)
     * 
     * @param file CSV 파일
     * @param includeHeader 헤더 포함 여부 (기본값: true)
     * @param delimiter 구분자 (기본값: 쉼표)
     * @return 업로드 결과
     */
    @PostMapping("/import-csv")
    @Transactional
    public ResponseEntity<Map<String, Object>> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "includeHeader", defaultValue = "true") boolean includeHeader,
            @RequestParam(value = "delimiter", defaultValue = ",") String delimiter) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: CSV 업로드 시작 - 파일명: {}, 크기: {} bytes, 헤더 포함: {}, 구분자: '{}'", 
                    file.getOriginalFilename(), file.getSize(), includeHeader, delimiter);
            
            // 1. 파일 검증
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "파일이 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                result.put("success", false);
                result.put("message", "CSV 파일만 업로드 가능합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            // 2. CSV 파싱 및 저장 (회차 번호가 Primary Key이므로 저장 순서와 무관)
            int savedCount = 0;
            int skippedCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                int lineNumber = 0;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    
                    // BOM 제거 (첫 줄에 BOM이 있을 수 있음)
                    if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    
                    // 빈 줄 건너뛰기
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // 헤더 건너뛰기
                    if (isFirstLine) {
                        isFirstLine = false;
                        if (includeHeader) {
                            // 헤더 포함 옵션이 true이면 첫 줄 건너뛰기
                            continue;
                        }
                        // 헤더 포함 옵션이 false이면 첫 줄도 데이터로 처리
                    }
                    
                    // CSV 파싱 (구분자 사용)
                    String[] columns = line.split(delimiter.equals("\t") ? "\t" : Pattern.quote(delimiter));
                    if (columns.length < 9) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 컬럼 수가 부족합니다 (최소 9개 필요, 실제: %d개)", lineNumber, columns.length));
                        continue;
                    }
                    
                    try {
                        // 필수 데이터 파싱 및 검증 (비어있으면 스킵)
                        String drawNoStr = columns[0].trim();
                        String drawDateStr = columns[1].trim();
                        String n1Str = columns[2].trim();
                        String n2Str = columns[3].trim();
                        String n3Str = columns[4].trim();
                        String n4Str = columns[5].trim();
                        String n5Str = columns[6].trim();
                        String n6Str = columns[7].trim();
                        String bonusStr = columns[8].trim();
                        
                        // 필수 필드가 비어있으면 스킵 (회차만 있고 내용이 없는 경우)
                        if (drawNoStr.isEmpty() || drawDateStr.isEmpty() || 
                            n1Str.isEmpty() || n2Str.isEmpty() || n3Str.isEmpty() || 
                            n4Str.isEmpty() || n5Str.isEmpty() || n6Str.isEmpty() || bonusStr.isEmpty()) {
                            errorCount++;
                            errors.add(String.format("라인 %d: 필수 필드가 비어있습니다 (회차만 있고 내용이 없는 경우 저장하지 않음)", lineNumber));
                            continue;
                        }
                        
                        int drawNo = Integer.parseInt(drawNoStr);
                        LocalDate drawDate = LocalDate.parse(drawDateStr, dateFormatter);
                        int n1 = Integer.parseInt(n1Str);
                        int n2 = Integer.parseInt(n2Str);
                        int n3 = Integer.parseInt(n3Str);
                        int n4 = Integer.parseInt(n4Str);
                        int n5 = Integer.parseInt(n5Str);
                        int n6 = Integer.parseInt(n6Str);
                        int bonus = Integer.parseInt(bonusStr);
                        
                        // 선택적 데이터 파싱 (당첨금 정보)
                        Double totalPrize = null;
                        Integer winnerCount = null;
                        Double prizePerPerson = null;
                        
                        if (columns.length >= 10 && !columns[9].trim().isEmpty()) {
                            try {
                                totalPrize = Double.parseDouble(columns[9].trim());
                            } catch (NumberFormatException e) {
                                log.debug("라인 {}: 당첨금 파싱 실패 (무시): {}", lineNumber, columns[9].trim());
                            }
                        }
                        
                        if (columns.length >= 11 && !columns[10].trim().isEmpty()) {
                            try {
                                String winnerCountStr = columns[10].trim();
                                // "15명" 형식에서 "명" 제거
                                winnerCountStr = winnerCountStr.replaceAll("[^0-9]", "");
                                if (!winnerCountStr.isEmpty()) {
                                    winnerCount = Integer.parseInt(winnerCountStr);
                                }
                            } catch (NumberFormatException e) {
                                log.debug("라인 {}: 당첨인원 파싱 실패 (무시): {}", lineNumber, columns[10].trim());
                            }
                        }
                        
                        if (columns.length >= 12 && !columns[11].trim().isEmpty()) {
                            try {
                                prizePerPerson = Double.parseDouble(columns[11].trim());
                            } catch (NumberFormatException e) {
                                log.debug("라인 {}: 인당당첨금 파싱 실패 (무시): {}", lineNumber, columns[11].trim());
                            }
                        }
                        
                        // 번호 정렬
                        int[] numbers = {n1, n2, n3, n4, n5, n6};
                        Arrays.sort(numbers);
                        
                        // 이미 존재하는 회차 확인 (회차 번호가 Primary Key이므로 중복 체크)
                        Optional<DrawEntity> existingOpt = drawRepository.findByDrawNo(drawNo);
                        if (existingOpt.isPresent()) {
                            skippedCount++;
                            log.debug("회차 {}는 이미 존재함, 건너뜀", drawNo);
                            continue;
                        }
                        
                        // 엔티티 생성 및 저장 (회차 번호를 key로 사용하므로 순서와 무관)
                        DrawEntity drawEntity = DrawEntity.builder()
                                .drawNo(drawNo)
                                .drawDate(drawDate)
                                .n1(numbers[0])
                                .n2(numbers[1])
                                .n3(numbers[2])
                                .n4(numbers[3])
                                .n5(numbers[4])
                                .n6(numbers[5])
                                .bonus(bonus)
                                .totalPrize(totalPrize)
                                .winnerCount(winnerCount)
                                .prizePerPerson(prizePerPerson)
                                .build();
                        
                        drawRepository.save(drawEntity);
                        savedCount++;
                        
                    } catch (Exception e) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 파싱 실패 - %s", lineNumber, e.getMessage()));
                        log.warn("CSV 라인 {} 파싱 실패: {}", lineNumber, e.getMessage());
                    }
                }
            }
            
            // 3. 캐시 초기화
            drawRefreshService.clearCache();
            
            String message = String.format("CSV 업로드 완료: 저장 %d개, 건너뜀 %d개, 오류 %d개", 
                    savedCount, skippedCount, errorCount);
            
            result.put("success", true);
            result.put("message", message);
            result.put("savedCount", savedCount);
            result.put("skippedCount", skippedCount);
            result.put("errorCount", errorCount);
            result.put("errors", errors);
            
            log.info("관리자 요청: CSV 업로드 완료 - 저장: {}개, 건너뜀: {}개, 오류: {}개", 
                    savedCount, skippedCount, errorCount);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: CSV 업로드 실패", e);
            
            result.put("success", false);
            result.put("message", "CSV 업로드 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 범위 수집 (제안된 방법 테스트용)
     * 
     * POST /api/v1/admin/collect-range
     * 
     * 지정된 범위의 회차를 순차적으로 수집하여 DB에 저장합니다.
     * 이미 저장된 회차는 자동으로 스킵하며, 실패한 회차는 건너뛰고 계속 진행합니다.
     * 재실행 시 실패한 회차만 다시 시도할 수 있습니다.
     * 
     * @param from 시작 회차 (기본값: 1)
     * @param to 종료 회차 (기본값: 1206)
     * @return 수집 결과
     */
    @PostMapping("/collect-range")
    @Transactional
    public ResponseEntity<Map<String, Object>> collectRange(
            @RequestParam(defaultValue = "1") int from,
            @RequestParam(defaultValue = "1206") int to) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 범위 수집 시작 - {}회차 ~ {}회차", from, to);
            
            // 범위 검증
            if (from < 1 || to < from) {
                result.put("success", false);
                result.put("message", String.format("잘못된 범위: from=%d, to=%d (from은 1 이상, to는 from 이상이어야 함)", from, to));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            // 수집 실행
            LottoDrawCollector.CollectResult collectResult = lottoDrawCollector.collectRange(from, to);
            
            // 캐시 초기화
            drawRefreshService.clearCache();
            
            String message = String.format("범위 수집 완료: %d회차 ~ %d회차, 성공 %d개, 스킵 %d개, 실패 %d개 (총 %d개)", 
                    from, to, collectResult.getSuccess(), collectResult.getSkip(), 
                    collectResult.getFail(), collectResult.getTotal());
            
            result.put("success", true);
            result.put("message", message);
            result.put("from", collectResult.getFrom());
            result.put("to", collectResult.getTo());
            result.put("successCount", collectResult.getSuccess());
            result.put("skip", collectResult.getSkip());
            result.put("fail", collectResult.getFail());
            result.put("total", collectResult.getTotal());
            
            log.info("관리자 요청: 범위 수집 완료 - {}", message);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 범위 수집 실패", e);
            
            result.put("success", false);
            result.put("message", "범위 수집 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 단일 회차 조회
     * 
     * GET /api/v1/admin/draw/{drawNo}
     * 
     * 특정 회차의 데이터를 조회합니다.
     * 
     * @param drawNo 회차 번호
     * @return 추첨 데이터
     */
    @GetMapping("/draw/{drawNo}")
    public ResponseEntity<Map<String, Object>> getDraw(@PathVariable Integer drawNo) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 회차 {} 조회", drawNo);
            
            Optional<DrawEntity> drawOpt = drawRepository.findByDrawNo(drawNo);
            
            if (drawOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", String.format("회차 %d를 찾을 수 없습니다.", drawNo));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
            
            DrawEntity draw = drawOpt.get();
            
            // 응답 데이터 구성
            Map<String, Object> drawData = new HashMap<>();
            drawData.put("drawNo", draw.getDrawNo());
            drawData.put("drawDate", draw.getDrawDate().toString());
            drawData.put("numbers", Arrays.asList(draw.getN1(), draw.getN2(), draw.getN3(), draw.getN4(), draw.getN5(), draw.getN6()));
            drawData.put("bonus", draw.getBonus());
            drawData.put("totalPrize", draw.getTotalPrize());
            drawData.put("winnerCount", draw.getWinnerCount());
            drawData.put("prizePerPerson", draw.getPrizePerPerson());
            drawData.put("createdAt", draw.getCreatedAt() != null ? draw.getCreatedAt().toString() : null);
            
            result.put("success", true);
            result.put("data", drawData);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 회차 조회 실패", e);
            
            result.put("success", false);
            result.put("message", "회차 조회 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 전체 회차 목록 조회
     * 
     * GET /api/v1/admin/draws
     * 
     * 저장된 모든 회차 목록을 조회합니다.
     * 
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 100)
     * @return 회차 목록
     */
    @GetMapping("/draws")
    public ResponseEntity<Map<String, Object>> getDraws(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 전체 회차 목록 조회 - page={}, size={}", page, size);
            
            // 전체 데이터 조회 (회차 번호 순으로 정렬)
            List<DrawEntity> allDraws = drawRepository.findAll().stream()
                    .sorted(Comparator.comparing(DrawEntity::getDrawNo).reversed()) // 최신순
                    .collect(Collectors.toList());
            
            // 페이징 처리
            int total = allDraws.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<DrawEntity> pagedDraws = start < total ? allDraws.subList(start, end) : new ArrayList<>();
            
            // 응답 데이터 구성
            List<Map<String, Object>> drawList = pagedDraws.stream().map(draw -> {
                Map<String, Object> drawData = new HashMap<>();
                drawData.put("drawNo", draw.getDrawNo());
                drawData.put("drawDate", draw.getDrawDate().toString());
                drawData.put("numbers", Arrays.asList(draw.getN1(), draw.getN2(), draw.getN3(), draw.getN4(), draw.getN5(), draw.getN6()));
                drawData.put("bonus", draw.getBonus());
                drawData.put("totalPrize", draw.getTotalPrize());
                drawData.put("winnerCount", draw.getWinnerCount());
                drawData.put("prizePerPerson", draw.getPrizePerPerson());
                return drawData;
            }).collect(Collectors.toList());
            
            result.put("success", true);
            result.put("data", drawList);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (total + size - 1) / size);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 전체 회차 목록 조회 실패", e);
            
            result.put("success", false);
            result.put("message", "회차 목록 조회 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 전략 설명 조회 (관리자용)
     * 
     * GET /api/v1/admin/strategy-descriptions
     * 
     * 모든 전략 설명을 조회합니다.
     * 
     * @return 전략 설명 목록
     */
    @GetMapping("/strategy-descriptions")
    public ResponseEntity<Map<String, Object>> getStrategyDescriptions() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 전략 설명 조회");
            
            List<StrategyDescriptionEntity> entities = strategyDescriptionRepository.findAll();
            
            List<Map<String, Object>> descriptions = entities.stream().map(entity -> {
                Map<String, Object> desc = new HashMap<>();
                desc.put("strategyCode", entity.getStrategyCode());
                desc.put("title", entity.getTitle());
                desc.put("shortDescription", entity.getShortDescription());
                desc.put("description", entity.getDescription());
                desc.put("contentHash", entity.getContentHash() != null ? entity.getContentHash() : ""); // 해시 일련번호 추가
                try {
                    desc.put("features", objectMapper.readValue(entity.getFeatures(), new TypeReference<List<String>>() {}));
                    desc.put("algorithm", objectMapper.readValue(entity.getAlgorithm(), new TypeReference<List<String>>() {}));
                    desc.put("scenarios", objectMapper.readValue(entity.getScenarios(), new TypeReference<List<String>>() {}));
                    if (entity.getNotes() != null && !entity.getNotes().isEmpty()) {
                        desc.put("notes", objectMapper.readValue(entity.getNotes(), new TypeReference<List<String>>() {}));
                    }
                } catch (Exception e) {
                    log.error("전략 설명 JSON 파싱 실패: {}", entity.getStrategyCode(), e);
                }
                desc.put("createdAt", entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
                desc.put("updatedAt", entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
                return desc;
            }).collect(Collectors.toList());
            
            result.put("success", true);
            result.put("data", descriptions);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 전략 설명 조회 실패", e);
            
            result.put("success", false);
            result.put("message", "전략 설명 조회 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 전략 설명 수정 (관리자용)
     * 
     * PUT /api/v1/admin/strategy-descriptions/{strategyCode}
     * 
     * 특정 전략의 설명을 수정합니다.
     * 
     * @param strategyCode 전략 코드
     * @param request 수정 요청
     * @return 수정 결과
     */
    @PutMapping("/strategy-descriptions/{strategyCode}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateStrategyDescription(
            @PathVariable String strategyCode,
            @RequestBody UpdateStrategyDescriptionRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 전략 설명 수정 - 전략: {}", strategyCode);
            
            // 1. 기존 엔티티 조회 또는 생성
            Optional<StrategyDescriptionEntity> existingOpt = strategyDescriptionRepository.findByStrategyCode(strategyCode);
            StrategyDescriptionEntity entity;
            
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
            } else {
                // 새로 생성
                entity = StrategyDescriptionEntity.builder()
                        .strategyCode(strategyCode)
                        .build();
            }
            
            // 2. 데이터 업데이트 (부분 업데이트 지원)
            // 최종 데이터 준비
            String finalTitle = request.getTitle() != null ? request.getTitle() : entity.getTitle();
            String finalShortDescription = request.getShortDescription() != null ? request.getShortDescription() : entity.getShortDescription();
            String finalDescription = request.getDescription() != null ? request.getDescription() : entity.getDescription();
            
            // JSON 필드 업데이트
            String finalFeatures;
            if (request.getFeatures() != null) {
                finalFeatures = objectMapper.writeValueAsString(request.getFeatures());
            } else {
                finalFeatures = entity.getFeatures();
            }
            
            String finalAlgorithm;
            if (request.getAlgorithm() != null) {
                finalAlgorithm = objectMapper.writeValueAsString(request.getAlgorithm());
            } else {
                finalAlgorithm = entity.getAlgorithm();
            }
            
            String finalScenarios;
            if (request.getScenarios() != null) {
                finalScenarios = objectMapper.writeValueAsString(request.getScenarios());
            } else {
                finalScenarios = entity.getScenarios();
            }
            
            String finalNotes;
            if (request.getNotes() != null) {
                finalNotes = objectMapper.writeValueAsString(request.getNotes());
            } else {
                finalNotes = entity.getNotes();
            }
            
            // 내용 기반 해시 일련번호 생성
            String contentHash = generateContentHash(
                    finalTitle,
                    finalShortDescription,
                    finalDescription,
                    finalFeatures,
                    finalAlgorithm,
                    finalScenarios,
                    finalNotes != null ? finalNotes : ""
            );
            
            StrategyDescriptionEntity.StrategyDescriptionEntityBuilder builder = StrategyDescriptionEntity.builder()
                    .strategyCode(entity.getStrategyCode())
                    .title(finalTitle)
                    .shortDescription(finalShortDescription)
                    .description(finalDescription)
                    .contentHash(contentHash) // 해시 일련번호 업데이트
                    .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now())
                    .updatedAt(LocalDateTime.now());
            
            // JSON 필드 설정
            builder.features(finalFeatures)
                    .algorithm(finalAlgorithm)
                    .scenarios(finalScenarios)
                    .notes(finalNotes);
            
            entity = builder.build();
            
            strategyDescriptionRepository.save(entity);
            
            result.put("success", true);
            result.put("message", String.format("전략 설명 %s 수정 완료", strategyCode));
            result.put("strategyCode", strategyCode);
            
            log.info("관리자 요청: 전략 설명 수정 완료 - 전략: {}", strategyCode);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 전략 설명 수정 실패", e);
            
            result.put("success", false);
            result.put("message", "전략 설명 수정 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 수동 저장 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveDrawRequest {
        private Integer drawNo;           // 회차 번호
        private LocalDate drawDate;      // 추첨일 (yyyy-MM-dd)
        private int[] numbers;           // 번호 6개
        private Integer bonus;           // 보너스 번호
        private Double totalPrize;       // 당첨금 (억 단위, 선택적)
        private Integer winnerCount;     // 당첨인원 (선택적)
        private Double prizePerPerson;   // 인당당첨금 (억 단위, 선택적)
    }
    
    /**
     * CSV 값 이스케이프 (쉼표, 따옴표, 줄바꿈 포함 시)
     */
    private String escapeCsvValue(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    /**
     * 전략 설명 CSV 다운로드
     * 
     * GET /api/v1/admin/strategy-descriptions/export-csv
     * 
     * 모든 전략 설명을 CSV 파일로 다운로드합니다.
     * 
     * CSV 형식:
     * strategyCode,title,shortDescription,description,features,algorithm,scenarios,notes
     * FREQUENT_TOP,고빈도 우선,간단 설명,상세 설명,"특징1|특징2","알고리즘1|알고리즘2","시나리오1|시나리오2","주의1|주의2"
     * 
     * 배열 필드(features, algorithm, scenarios, notes)는 파이프(|)로 구분됩니다.
     * 
     * @return CSV 파일 다운로드
     */
    @GetMapping("/strategy-descriptions/export-csv")
    public ResponseEntity<byte[]> exportStrategyDescriptionsCsv() {
        try {
            log.info("관리자 요청: 전략 설명 CSV 다운로드 시작");
            
            List<StrategyDescriptionEntity> entities = strategyDescriptionRepository.findAll();
            
            log.info("CSV 다운로드: 총 {}개 전략 설명", entities.size());
            
            // CSV 생성
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8);
            
            // BOM 추가 (Excel에서 UTF-8 인식)
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);
            
            // 헤더 작성
            writer.println("strategyCode,title,shortDescription,description,features,algorithm,scenarios,notes,contentHash");
            
            // 데이터 작성
            for (StrategyDescriptionEntity entity : entities) {
                try {
                    // JSON 배열을 파이프(|)로 구분된 문자열로 변환
                    List<String> features = objectMapper.readValue(entity.getFeatures(), new TypeReference<List<String>>() {});
                    List<String> algorithm = objectMapper.readValue(entity.getAlgorithm(), new TypeReference<List<String>>() {});
                    List<String> scenarios = objectMapper.readValue(entity.getScenarios(), new TypeReference<List<String>>() {});
                    List<String> notes = entity.getNotes() != null && !entity.getNotes().isEmpty() 
                        ? objectMapper.readValue(entity.getNotes(), new TypeReference<List<String>>() {})
                        : new ArrayList<>();
                    
                    String featuresStr = String.join("|", features);
                    String algorithmStr = String.join("|", algorithm);
                    String scenariosStr = String.join("|", scenarios);
                    String notesStr = notes.isEmpty() ? "" : String.join("|", notes);
                    
                    String contentHashStr = entity.getContentHash() != null ? entity.getContentHash() : "";
                    writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        escapeCsvValue(entity.getStrategyCode()),
                        escapeCsvValue(entity.getTitle()),
                        escapeCsvValue(entity.getShortDescription()),
                        escapeCsvValue(entity.getDescription()),
                        escapeCsvValue(featuresStr),
                        escapeCsvValue(algorithmStr),
                        escapeCsvValue(scenariosStr),
                        escapeCsvValue(notesStr),
                        escapeCsvValue(contentHashStr));
                } catch (Exception e) {
                    log.error("전략 설명 CSV 변환 실패: {}", entity.getStrategyCode(), e);
                }
            }
            
            writer.flush();
            byte[] csvBytes = baos.toByteArray();
            writer.close();
            
            // 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", 
                    String.format("strategy_descriptions_%s.csv", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))));
            headers.setContentLength(csvBytes.length);
            
            log.info("관리자 요청: 전략 설명 CSV 다운로드 완료 - {}개 전략, {} bytes", entities.size(), csvBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvBytes);
            
        } catch (Exception e) {
            log.error("관리자 요청: 전략 설명 CSV 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 전략 설명 CSV 업로드
     * 
     * POST /api/v1/admin/strategy-descriptions/import-csv
     * 
     * CSV 파일을 업로드하여 전략 설명을 일괄 업데이트합니다.
     * 
     * CSV 형식:
     * strategyCode,title,shortDescription,description,features,algorithm,scenarios,notes
     * FREQUENT_TOP,고빈도 우선,간단 설명,상세 설명,"특징1|특징2","알고리즘1|알고리즘2","시나리오1|시나리오2","주의1|주의2"
     * 
     * 배열 필드(features, algorithm, scenarios, notes)는 파이프(|)로 구분됩니다.
     * 
     * @param file CSV 파일
     * @param includeHeader 헤더 포함 여부 (기본값: true)
     * @param delimiter 구분자 (기본값: 쉼표)
     * @return 업로드 결과
     */
    @PostMapping("/strategy-descriptions/import-csv")
    @Transactional
    public ResponseEntity<Map<String, Object>> importStrategyDescriptionsCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "includeHeader", defaultValue = "true") boolean includeHeader,
            @RequestParam(value = "delimiter", defaultValue = ",") String delimiter) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 전략 설명 CSV 업로드 시작 - 파일명: {}, 크기: {} bytes, 헤더 포함: {}, 구분자: '{}'", 
                    file.getOriginalFilename(), file.getSize(), includeHeader, delimiter);
            
            // 1. 파일 검증
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "파일이 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                result.put("success", false);
                result.put("message", "CSV 파일만 업로드 가능합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            // 2. CSV 파싱 및 저장
            int savedCount = 0;
            int updatedCount = 0;
            int skippedCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                int lineNumber = 0;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    
                    // BOM 제거
                    if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    
                    // 빈 줄 건너뛰기
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // 헤더 건너뛰기
                    if (isFirstLine) {
                        isFirstLine = false;
                        if (includeHeader) {
                            continue;
                        }
                    }
                    
                    // CSV 파싱 (쉼표로 구분, 따옴표 처리)
                    List<String> columns = parseCsvLine(line, delimiter);
                    if (columns.size() < 4) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 컬럼 수가 부족합니다 (최소 4개 필요, 실제: %d개)", lineNumber, columns.size()));
                        continue;
                    }
                    
                    try {
                        String strategyCode = columns.get(0).trim();
                        String title = columns.size() > 1 ? columns.get(1).trim() : "";
                        String shortDescription = columns.size() > 2 ? columns.get(2).trim() : "";
                        String description = columns.size() > 3 ? columns.get(3).trim() : "";
                        String featuresStr = columns.size() > 4 ? columns.get(4).trim() : "";
                        String algorithmStr = columns.size() > 5 ? columns.get(5).trim() : "";
                        String scenariosStr = columns.size() > 6 ? columns.get(6).trim() : "";
                        String notesStr = columns.size() > 7 ? columns.get(7).trim() : "";
                        // CSV의 contentHash는 무시 (내용 기반으로 자동 생성)
                        
                        // 필수 필드 검증
                        if (strategyCode.isEmpty() || title.isEmpty() || shortDescription.isEmpty() || description.isEmpty()) {
                            errorCount++;
                            errors.add(String.format("라인 %d: 필수 필드가 비어있습니다", lineNumber));
                            continue;
                        }
                        
                        // 파이프(|)로 구분된 문자열을 리스트로 변환
                        List<String> features = featuresStr.isEmpty() ? new ArrayList<>() : Arrays.asList(featuresStr.split("\\|"));
                        List<String> algorithm = algorithmStr.isEmpty() ? new ArrayList<>() : Arrays.asList(algorithmStr.split("\\|"));
                        List<String> scenarios = scenariosStr.isEmpty() ? new ArrayList<>() : Arrays.asList(scenariosStr.split("\\|"));
                        List<String> notes = notesStr.isEmpty() ? new ArrayList<>() : Arrays.asList(notesStr.split("\\|"));
                        
                        // JSON 문자열로 변환
                        String featuresJson = objectMapper.writeValueAsString(features);
                        String algorithmJson = objectMapper.writeValueAsString(algorithm);
                        String scenariosJson = objectMapper.writeValueAsString(scenarios);
                        String notesJson = notes.isEmpty() ? null : objectMapper.writeValueAsString(notes);
                        
                        // 내용 기반 해시 일련번호 생성
                        String contentHash = generateContentHash(
                                title,
                                shortDescription,
                                description,
                                featuresJson,
                                algorithmJson,
                                scenariosJson,
                                notesJson != null ? notesJson : ""
                        );
                        
                        // 기존 엔티티 확인
                        Optional<StrategyDescriptionEntity> existingOpt = strategyDescriptionRepository.findByStrategyCode(strategyCode);
                        boolean isUpdate = existingOpt.isPresent();
                        
                        // 해시 비교: CSV 해시와 생성된 해시가 다르거나, 서버 해시와 다르면 업데이트
                        boolean shouldUpdate = true;
                        if (existingOpt.isPresent()) {
                            String existingHash = existingOpt.get().getContentHash();
                            // 서버 해시와 동일하면 업데이트 불필요
                            if (existingHash != null && existingHash.equals(contentHash)) {
                                shouldUpdate = false;
                                log.info("전략 설명 {}의 내용이 변경되지 않았습니다. 해시: {}", strategyCode, contentHash);
                            }
                        }
                        
                        if (!shouldUpdate) {
                            skippedCount++;
                            continue;
                        }
                        
                        // 엔티티 생성 또는 업데이트
                        StrategyDescriptionEntity entity = StrategyDescriptionEntity.builder()
                                .strategyCode(strategyCode)
                                .title(title)
                                .shortDescription(shortDescription)
                                .description(description)
                                .features(featuresJson)
                                .algorithm(algorithmJson)
                                .scenarios(scenariosJson)
                                .notes(notesJson)
                                .contentHash(contentHash) // 내용 기반 해시 일련번호
                                .createdAt(existingOpt.map(StrategyDescriptionEntity::getCreatedAt).orElse(LocalDateTime.now()))
                                .updatedAt(LocalDateTime.now())
                                .build();
                        
                        strategyDescriptionRepository.save(entity);
                        
                        if (isUpdate) {
                            updatedCount++;
                        } else {
                            savedCount++;
                        }
                        
                    } catch (Exception e) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 처리 실패 - %s", lineNumber, e.getMessage()));
                        log.warn("CSV 라인 {} 처리 실패: {}", lineNumber, e.getMessage());
                    }
                }
            }
            
            String message = String.format("CSV 업로드 완료: 신규 %d개, 업데이트 %d개, 스킵 %d개, 오류 %d개", 
                    savedCount, updatedCount, skippedCount, errorCount);
            
            result.put("success", true);
            result.put("message", message);
            result.put("savedCount", savedCount);
            result.put("updatedCount", updatedCount);
            result.put("skippedCount", skippedCount);
            result.put("errorCount", errorCount);
            result.put("errors", errors);
            
            log.info("관리자 요청: 전략 설명 CSV 업로드 완료 - 신규: {}개, 업데이트: {}개, 오류: {}개", 
                    savedCount, updatedCount, errorCount);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 전략 설명 CSV 업로드 실패", e);
            
            result.put("success", false);
            result.put("message", "CSV 업로드 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * CSV 라인 파싱 (쉼표로 구분, 따옴표 처리)
     */
    private List<String> parseCsvLine(String line, String delimiter) {
        List<String> columns = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // 이스케이프된 따옴표 ("")
                    current.append('"');
                    i++; // 다음 따옴표 건너뛰기
                } else {
                    // 따옴표 시작/끝
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter.charAt(0) && !inQuotes) {
                // 구분자 (따옴표 밖에서만)
                columns.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        // 마지막 컬럼 추가
        columns.add(current.toString());
        
        return columns;
    }
    
    /**
     * 전략 설명 내용을 기반으로 해시 일련번호 생성 (SHA-256)
     * 내용이 변경되면 해시가 달라지므로 자동으로 업데이트 여부를 판단할 수 있음
     */
    private String generateContentHash(
            String title,
            String shortDescription,
            String description,
            String features,
            String algorithm,
            String scenarios,
            String notes) {
        try {
            // 모든 내용을 하나의 문자열로 결합
            String content = String.join("|",
                    title != null ? title : "",
                    shortDescription != null ? shortDescription : "",
                    description != null ? description : "",
                    features != null ? features : "",
                    algorithm != null ? algorithm : "",
                    scenarios != null ? scenarios : "",
                    notes != null ? notes : ""
            );
            
            // SHA-256 해시 생성
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // 16진수 문자열로 변환 (64자)
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("해시 생성 실패", e);
            // 해시 생성 실패 시 타임스탬프 기반 일련번호 생성
            return String.valueOf(System.currentTimeMillis());
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStrategyDescriptionRequest {
        private String title;
        private String shortDescription;
        private String description;
        private List<String> features;
        private List<String> algorithm;
        private List<String> scenarios;
        private List<String> notes;
    }
    
    /**
     * 운명의 번호 추천 경고 메시지 목록 조회
     * 
     * GET /api/v1/admin/destiny-limit-messages
     * 
     * @return 경고 메시지 목록
     */
    @GetMapping("/destiny-limit-messages")
    public ResponseEntity<Map<String, Object>> getDestinyLimitMessages() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<DestinyLimitMessageEntity> messages = destinyLimitMessageRepository.findAllByOrderByOrderIndexAsc();
            
            // a 문구와 b 문구 목록 추출 (중복 제거)
            Set<String> partASet = new LinkedHashSet<>();
            Set<String> partBSet = new LinkedHashSet<>();
            
            for (DestinyLimitMessageEntity msg : messages) {
                if (msg.getMessagePartA() != null && !msg.getMessagePartA().trim().isEmpty()) {
                    partASet.add(msg.getMessagePartA().trim());
                }
                if (msg.getMessagePartB() != null && !msg.getMessagePartB().trim().isEmpty()) {
                    partBSet.add(msg.getMessagePartB().trim());
                }
            }
            
            List<String> partAList = new ArrayList<>(partASet);
            List<String> partBList = new ArrayList<>(partBSet);
            
            // 정렬하여 일관된 해시 생성
            Collections.sort(partAList);
            Collections.sort(partBList);
            
            // 일련번호 생성 (모든 a/b 문구 내용 기반 해시) - 전략 설명과 동일한 방식
            String serialNumber = generateDestinyMessagesHash(partAList, partBList);
            
            // 기존 형식도 유지 (호환성)
            List<Map<String, Object>> messageList = messages.stream()
                    .map(msg -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", msg.getId());
                        map.put("message", msg.getMessage());
                        map.put("orderIndex", msg.getOrderIndex());
                        if (msg.getMessagePartA() != null) {
                            map.put("messagePartA", msg.getMessagePartA());
                        }
                        if (msg.getMessagePartB() != null) {
                            map.put("messagePartB", msg.getMessagePartB());
                        }
                        return map;
                    })
                    .collect(Collectors.toList());
            
            result.put("success", true);
            result.put("data", messageList);
            result.put("count", messageList.size());
            result.put("partAList", partAList);
            result.put("partBList", partBList);
            result.put("serialNumber", serialNumber);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 경고 메시지 조회 실패", e);
            
            result.put("success", false);
            result.put("message", "경고 메시지 조회 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 운명의 번호 추천 경고 메시지 CSV 다운로드
     * 
     * GET /api/v1/admin/destiny-limit-messages/export-csv
     * 
     * CSV 형식:
     * id,message,orderIndex
     * 1,"별들의 계시가 모두 전달되었습니다. 내일 다시 당신의 운명을 확인하세요.",1
     * 
     * @return CSV 파일 다운로드
     */
    @GetMapping("/destiny-limit-messages/export-csv")
    public ResponseEntity<byte[]> exportDestinyLimitMessagesCsv() {
        try {
            log.info("관리자 요청: 경고 메시지 CSV 다운로드 시작");
            
            List<DestinyLimitMessageEntity> messages = destinyLimitMessageRepository.findAllByOrderByOrderIndexAsc();
            
            log.info("CSV 다운로드: DB에서 조회된 경고 메시지 개수 = {}", messages.size());
            
            // CSV 생성
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8);
            
            // BOM 추가
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);
            
            // 헤더 작성
            writer.println("id,message,orderIndex");
            
            // 데이터 작성
            int dataRowCount = 0;
            for (DestinyLimitMessageEntity msg : messages) {
                if (msg == null) {
                    log.warn("CSV 다운로드: null 메시지 엔티티 발견, 건너뜀");
                    continue;
                }
                String message = msg.getMessage();
                if (message == null || message.trim().isEmpty()) {
                    log.warn("CSV 다운로드: 메시지가 null이거나 비어있음 (id={}), 건너뜀", msg.getId());
                    continue;
                }
                String escapedMessage = escapeCsvValue(message);
                writer.printf("%d,%s,%d%n", msg.getId(), escapedMessage, msg.getOrderIndex());
                dataRowCount++;
            }
            
            writer.flush();
            byte[] csvBytes = baos.toByteArray();
            writer.close();
            
            // 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", 
                    String.format("destiny_limit_messages_%s.csv", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))));
            headers.setContentLength(csvBytes.length);
            
            log.info("관리자 요청: 경고 메시지 CSV 다운로드 완료 - DB 조회: {}개, 실제 CSV 행: {}개, 파일 크기: {} bytes", 
                    messages.size(), dataRowCount, csvBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvBytes);
            
        } catch (Exception e) {
            log.error("관리자 요청: 경고 메시지 CSV 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 운명의 번호 추천 경고 메시지 CSV 업로드
     * 
     * POST /api/v1/admin/destiny-limit-messages/import-csv
     * 
     * CSV 형식:
     * id,message,orderIndex
     * 1,"별들의 계시가 모두 전달되었습니다. 내일 다시 당신의 운명을 확인하세요.",1
     * 
     * 주의:
     * - id가 있으면 업데이트, 없으면 신규 생성
     * - 기존 데이터는 모두 삭제 후 업로드된 데이터로 교체 (전체 교체 모드)
     * 
     * @param file CSV 파일
     * @return 업로드 결과
     */
    @PostMapping("/destiny-limit-messages/import-csv")
    @Transactional
    public ResponseEntity<Map<String, Object>> importDestinyLimitMessagesCsv(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 경고 메시지 CSV 업로드 시작 - 파일명: {}, 크기: {} bytes", 
                    file.getOriginalFilename(), file.getSize());
            
            // 1. 파일 검증
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "파일이 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                result.put("success", false);
                result.put("message", "CSV 파일만 업로드 가능합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            // 2. 기존 데이터 모두 삭제 (전체 교체 모드)
            destinyLimitMessageRepository.deleteAll();
            
            // 3. CSV 파싱 및 저장
            int savedCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();
            List<DestinyLimitMessageEntity> entitiesToSave = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                int lineNumber = 0;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    
                    // BOM 제거
                    if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    
                    // 빈 줄 건너뛰기
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // 헤더 건너뛰기
                    if (isFirstLine) {
                        isFirstLine = false;
                        if (line.toLowerCase().contains("id") && line.toLowerCase().contains("message")) {
                            continue;
                        }
                    }
                    
                    // CSV 파싱
                    List<String> columns = parseCsvLine(line, ",");
                    if (columns.size() < 2) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 컬럼 수가 부족합니다 (최소 2개 필요, 실제: %d개)", lineNumber, columns.size()));
                        continue;
                    }
                    
                    try {
                        String message = columns.get(1).trim();
                        if (message.isEmpty()) {
                            errorCount++;
                            errors.add(String.format("라인 %d: 메시지가 비어있습니다", lineNumber));
                            continue;
                        }
                        
                        int orderIndex = columns.size() >= 3 && !columns.get(2).trim().isEmpty() 
                                ? Integer.parseInt(columns.get(2).trim()) 
                                : savedCount + 1;
                        
                        // a/b 문구 분리 시도 (예: "별들의 계시가 모두 전달되었습니다. 내일 다시 당신의 운명을 확인하세요.")
                        String messagePartA = null;
                        String messagePartB = null;
                        String[] parts = message.split("[.。]\\s+", 2);
                        if (parts.length >= 2) {
                            messagePartA = parts[0].trim();
                            messagePartB = parts[1].trim();
                        } else if (parts.length == 1) {
                            // 마침표가 없는 경우 전체를 a 문구로
                            messagePartA = parts[0].trim();
                        }
                        
                        DestinyLimitMessageEntity entity = DestinyLimitMessageEntity.builder()
                                .message(message)
                                .messagePartA(messagePartA)
                                .messagePartB(messagePartB)
                                .orderIndex(orderIndex)
                                .build();
                        
                        entitiesToSave.add(entity);
                        savedCount++;
                        
                    } catch (Exception e) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 저장 실패 - %s", lineNumber, e.getMessage()));
                    }
                }
            }
            
            // 4. 모든 엔티티 저장
            for (DestinyLimitMessageEntity entity : entitiesToSave) {
                destinyLimitMessageRepository.save(entity);
            }
            
            // 일련번호는 API 응답에서 DB 내용을 기반으로 자동 계산되므로 여기서는 저장만 수행
            
            result.put("success", true);
            result.put("message", String.format("경고 메시지 %d개 저장 완료", savedCount));
            result.put("savedCount", savedCount);
            result.put("errorCount", errorCount);
            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }
            
            log.info("관리자 요청: 경고 메시지 CSV 업로드 완료 - 저장: {}개, 오류: {}개", savedCount, errorCount);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 경고 메시지 CSV 업로드 실패", e);
            
            result.put("success", false);
            result.put("message", "CSV 업로드 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 운명의 추천 메시지 일련번호 생성 (전략 설명의 contentHash와 동일한 방식)
     * 모든 a 문구와 b 문구를 정렬하여 결합한 후 SHA-256 해시 생성
     * 
     * @param partAList a 문구 목록
     * @param partBList b 문구 목록
     * @return SHA-256 해시값 (64자 16진수 문자열)
     */
    private String generateDestinyMessagesHash(List<String> partAList, List<String> partBList) {
        try {
            // 모든 a 문구와 b 문구를 정렬하여 결합
            String content = String.join("|",
                    partAList != null ? String.join(",", partAList) : "",
                    partBList != null ? String.join(",", partBList) : ""
            );
            
            // SHA-256 해시 생성 (전략 설명과 동일한 방식)
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            
            // 16진수 문자열로 변환 (64자)
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            log.error("일련번호 생성 실패", e);
            // 폴백: 간단한 해시
            String content = String.join("|",
                    partAList != null ? String.join(",", partAList) : "",
                    partBList != null ? String.join(",", partBList) : ""
            );
            return String.valueOf(content.hashCode());
        }
    }
    
    /**
     * AI 로딩 메시지 조회
     * 
     * GET /api/v1/admin/ai-loading-messages
     * 
     * @return AI 로딩 메시지 목록 (a/b 문구 분리 및 일련번호 포함)
     */
    @GetMapping("/ai-loading-messages")
    public ResponseEntity<Map<String, Object>> getAiLoadingMessages() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<AiLoadingMessageEntity> messages = aiLoadingMessageRepository.findAllByOrderByOrderIndexAsc();
            
            // a 문구와 b 문구 목록 추출 (중복 제거)
            Set<String> partASet = new LinkedHashSet<>();
            Set<String> partBSet = new LinkedHashSet<>();
            
            for (AiLoadingMessageEntity msg : messages) {
                if (msg.getMessagePartA() != null && !msg.getMessagePartA().trim().isEmpty()) {
                    partASet.add(msg.getMessagePartA().trim());
                }
                if (msg.getMessagePartB() != null && !msg.getMessagePartB().trim().isEmpty()) {
                    partBSet.add(msg.getMessagePartB().trim());
                }
            }
            
            List<String> partAList = new ArrayList<>(partASet);
            List<String> partBList = new ArrayList<>(partBSet);
            
            // 정렬하여 일관된 해시 생성
            Collections.sort(partAList);
            Collections.sort(partBList);
            
            // 일련번호 생성 (모든 a/b 문구 내용 기반 해시) - 전략 설명과 동일한 방식
            String serialNumber = generateAiLoadingMessagesHash(partAList, partBList);
            
            // 기존 형식도 유지 (호환성)
            List<Map<String, Object>> messageList = messages.stream()
                    .map(msg -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", msg.getId());
                        map.put("message", msg.getMessage());
                        map.put("orderIndex", msg.getOrderIndex());
                        if (msg.getMessagePartA() != null) {
                            map.put("messagePartA", msg.getMessagePartA());
                        }
                        if (msg.getMessagePartB() != null) {
                            map.put("messagePartB", msg.getMessagePartB());
                        }
                        return map;
                    })
                    .collect(Collectors.toList());
            
            result.put("success", true);
            result.put("data", messageList);
            result.put("count", messageList.size());
            result.put("partAList", partAList);
            result.put("partBList", partBList);
            result.put("serialNumber", serialNumber);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: AI 로딩 메시지 조회 실패", e);
            
            result.put("success", false);
            result.put("message", "AI 로딩 메시지 조회 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * AI 로딩 메시지 일련번호 생성 (전략 설명의 contentHash와 동일한 방식)
     * 모든 a 문구와 b 문구를 정렬하여 결합한 후 SHA-256 해시 생성
     * 
     * @param partAList a 문구 목록
     * @param partBList b 문구 목록
     * @return SHA-256 해시값 (64자 16진수 문자열)
     */
    private String generateAiLoadingMessagesHash(List<String> partAList, List<String> partBList) {
        try {
            // 모든 a 문구와 b 문구를 정렬하여 결합
            String content = String.join("|",
                    partAList != null ? String.join(",", partAList) : "",
                    partBList != null ? String.join(",", partBList) : ""
            );
            
            // SHA-256 해시 생성 (전략 설명과 동일한 방식)
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            
            // 16진수 문자열로 변환 (64자)
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            log.error("일련번호 생성 실패", e);
            // 폴백: 간단한 해시
            String content = String.join("|",
                    partAList != null ? String.join(",", partAList) : "",
                    partBList != null ? String.join(",", partBList) : ""
            );
            return String.valueOf(content.hashCode());
        }
    }
    
    /**
     * 운명의 번호 추천 미션 템플릿 목록 조회
     * 
     * GET /api/v1/admin/mission-templates
     * 
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 100)
     * @return 미션 템플릿 목록
     */
    @GetMapping("/mission-templates")
    public ResponseEntity<Map<String, Object>> getMissionTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 미션 템플릿 목록 조회 - page={}, size={}", page, size);
            
            // 전체 데이터 조회
            List<MissionTemplateEntity> allTemplates = missionTemplateRepository.findAll();
            
            // 페이징 처리
            int total = allTemplates.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<MissionTemplateEntity> pagedTemplates = start < total ? allTemplates.subList(start, end) : new ArrayList<>();
            
            // 응답 데이터 구성
            List<Map<String, Object>> templateList = pagedTemplates.stream().map(template -> {
                Map<String, Object> templateData = new HashMap<>();
                templateData.put("id", template.getId());
                templateData.put("category", template.getCategory());
                templateData.put("theme", template.getTheme());
                templateData.put("tone", template.getTone());
                templateData.put("placeHint", template.getPlaceHint());
                templateData.put("timeHint", template.getTimeHint());
                templateData.put("text", template.getText());
                templateData.put("weight", template.getWeight());
                templateData.put("createdAt", template.getCreatedAt() != null ? template.getCreatedAt().toString() : null);
                templateData.put("updatedAt", template.getUpdatedAt() != null ? template.getUpdatedAt().toString() : null);
                return templateData;
            }).collect(Collectors.toList());
            
            result.put("success", true);
            result.put("data", templateList);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (total + size - 1) / size);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 미션 템플릿 목록 조회 실패", e);
            
            result.put("success", false);
            result.put("message", "미션 템플릿 목록 조회 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 운명의 번호 추천 미션 템플릿 CSV 다운로드
     * 
     * GET /api/v1/admin/mission-templates/export-csv
     * 
     * CSV 형식:
     * id,category,theme,tone,placeHint,timeHint,text,weight
     * 
     * @return CSV 파일 다운로드
     */
    @GetMapping("/mission-templates/export-csv")
    public ResponseEntity<byte[]> exportMissionTemplatesCsv() {
        try {
            log.info("관리자 요청: 미션 템플릿 CSV 다운로드 시작");
            
            List<MissionTemplateEntity> templates = missionTemplateRepository.findAll();
            
            log.info("CSV 다운로드: 총 {}개 미션 템플릿", templates.size());
            
            // CSV 생성
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8);
            
            // BOM 추가 (Excel에서 UTF-8 인식)
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);
            
            // 헤더 작성
            writer.println("id,category,theme,tone,placeHint,timeHint,text,weight");
            
            // 데이터 작성
            for (MissionTemplateEntity template : templates) {
                writer.printf("%d,%s,%s,%s,%s,%s,%s,%d%n",
                    template.getId() != null ? template.getId() : "",
                    escapeCsvValue(template.getCategory()),
                    escapeCsvValue(template.getTheme()),
                    escapeCsvValue(template.getTone()),
                    template.getPlaceHint() != null ? escapeCsvValue(template.getPlaceHint()) : "",
                    template.getTimeHint() != null ? escapeCsvValue(template.getTimeHint()) : "",
                    escapeCsvValue(template.getText()),
                    template.getWeight() != null ? template.getWeight() : 1);
            }
            
            writer.flush();
            byte[] csvBytes = baos.toByteArray();
            writer.close();
            
            // 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", 
                    String.format("mission_templates_%s.csv", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))));
            headers.setContentLength(csvBytes.length);
            
            log.info("관리자 요청: 미션 템플릿 CSV 다운로드 완료 - {}개 템플릿, {} bytes", templates.size(), csvBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvBytes);
            
        } catch (Exception e) {
            log.error("관리자 요청: 미션 템플릿 CSV 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 운명의 번호 추천 미션 템플릿 CSV 업로드
     * 
     * POST /api/v1/admin/mission-templates/import-csv
     * 
     * CSV 형식:
     * id,category,theme,tone,placeHint,timeHint,text,weight
     * 
     * 주의:
     * - id가 있으면 업데이트, 없으면 신규 생성
     * - 기존 데이터는 모두 삭제 후 업로드된 데이터로 교체 (전체 교체 모드)
     * 
     * @param file CSV 파일
     * @return 업로드 결과
     */
    @PostMapping("/mission-templates/import-csv")
    @Transactional
    public ResponseEntity<Map<String, Object>> importMissionTemplatesCsv(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 미션 템플릿 CSV 업로드 시작 - 파일명: {}, 크기: {} bytes", 
                    file.getOriginalFilename(), file.getSize());
            
            // 1. 파일 검증
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "파일이 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                result.put("success", false);
                result.put("message", "CSV 파일만 업로드 가능합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            // 2. 기존 데이터 모두 삭제 (전체 교체 모드)
            missionTemplateRepository.deleteAll();
            
            // 3. CSV 파싱 및 저장
            int savedCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();
            List<MissionTemplateEntity> entitiesToSave = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                int lineNumber = 0;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    
                    // BOM 제거
                    if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    
                    // 빈 줄 건너뛰기
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // 헤더 건너뛰기
                    if (isFirstLine) {
                        isFirstLine = false;
                        if (line.toLowerCase().contains("id") && line.toLowerCase().contains("category")) {
                            continue;
                        }
                    }
                    
                    // CSV 파싱
                    List<String> columns = parseCsvLine(line, ",");
                    if (columns.size() < 4) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 컬럼 수가 부족합니다 (최소 4개 필요, 실제: %d개)", lineNumber, columns.size()));
                        continue;
                    }
                    
                    try {
                        String category = columns.size() > 1 ? columns.get(1).trim() : "";
                        String theme = columns.size() > 2 ? columns.get(2).trim() : "";
                        String tone = columns.size() > 3 ? columns.get(3).trim() : "";
                        String placeHint = columns.size() > 4 && !columns.get(4).trim().isEmpty() ? columns.get(4).trim() : null;
                        String timeHint = columns.size() > 5 && !columns.get(5).trim().isEmpty() ? columns.get(5).trim() : null;
                        String text = columns.size() > 6 ? columns.get(6).trim() : "";
                        Integer weight = columns.size() > 7 && !columns.get(7).trim().isEmpty() 
                                ? Integer.parseInt(columns.get(7).trim()) 
                                : 1;
                        
                        // 필수 필드 검증
                        if (category.isEmpty() || theme.isEmpty() || tone.isEmpty() || text.isEmpty()) {
                            errorCount++;
                            errors.add(String.format("라인 %d: 필수 필드가 비어있습니다", lineNumber));
                            continue;
                        }
                        
                        MissionTemplateEntity entity = MissionTemplateEntity.builder()
                                .category(category)
                                .theme(theme)
                                .tone(tone)
                                .placeHint(placeHint)
                                .timeHint(timeHint)
                                .text(text)
                                .weight(weight)
                                .build();
                        
                        entitiesToSave.add(entity);
                        savedCount++;
                        
                    } catch (Exception e) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 저장 실패 - %s", lineNumber, e.getMessage()));
                    }
                }
            }
            
            // 4. 모든 엔티티 저장
            for (MissionTemplateEntity entity : entitiesToSave) {
                missionTemplateRepository.save(entity);
            }
            
            result.put("success", true);
            result.put("message", String.format("미션 템플릿 %d개 저장 완료", savedCount));
            result.put("savedCount", savedCount);
            result.put("errorCount", errorCount);
            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }
            
            log.info("관리자 요청: 미션 템플릿 CSV 업로드 완료 - 저장: {}개, 오류: {}개", savedCount, errorCount);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 미션 템플릿 CSV 업로드 실패", e);
            
            result.put("success", false);
            result.put("message", "CSV 업로드 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 시스템 옵션 조회 (일련번호 포함)
     * 
     * GET /api/v1/admin/system-options
     * 
     * @return 시스템 옵션 목록 및 일련번호
     */
    @GetMapping("/system-options")
    public ResponseEntity<Map<String, Object>> getSystemOptions() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 시스템 옵션 조회");
            
            List<SystemOptionEntity> options = systemOptionRepository.findAll();
            
            // 옵션을 key-value 맵으로 변환 (정렬하여 일관된 해시 생성)
            Map<String, String> optionsMap = new TreeMap<>();
            for (SystemOptionEntity option : options) {
                optionsMap.put(option.getOptionKey(), option.getOptionValue() != null ? option.getOptionValue() : "");
            }
            
            // 일련번호 생성 (모든 옵션 내용 기반 해시) - 전략 설명과 동일한 방식
            String serialNumber = generateSystemOptionsHash(optionsMap);
            
            result.put("success", true);
            result.put("data", optionsMap);
            result.put("count", optionsMap.size());
            result.put("serialNumber", serialNumber);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 시스템 옵션 조회 실패", e);
            
            result.put("success", false);
            result.put("message", "시스템 옵션 조회 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 시스템 옵션 일련번호 생성 (전략 설명의 contentHash와 동일한 방식)
     * 모든 옵션의 key-value를 정렬하여 결합한 후 SHA-256 해시 생성
     * 
     * @param optionsMap 옵션 맵 (key-value)
     * @return SHA-256 해시값 (64자 16진수 문자열)
     */
    private String generateSystemOptionsHash(Map<String, String> optionsMap) {
        try {
            // 모든 옵션을 key 순서대로 정렬하여 결합
            StringBuilder content = new StringBuilder();
            for (Map.Entry<String, String> entry : optionsMap.entrySet()) {
                content.append(entry.getKey()).append("=").append(entry.getValue() != null ? entry.getValue() : "").append("|");
            }
            
            // SHA-256 해시 생성 (전략 설명과 동일한 방식)
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.toString().getBytes(StandardCharsets.UTF_8));
            
            // 16진수 문자열로 변환 (64자)
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            log.error("일련번호 생성 실패", e);
            // 폴백: 간단한 해시
            return String.valueOf(optionsMap.toString().hashCode());
        }
    }
    
    /**
     * 시스템 옵션 조회 (키별)
     * 
     * GET /api/v1/admin/system-options/{key}
     * 
     * @param key 옵션 키
     * @return 옵션 값
     */
    @GetMapping("/system-options/{key}")
    public ResponseEntity<Map<String, Object>> getSystemOption(@PathVariable String key) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 시스템 옵션 조회 - key: {}", key);
            
            Optional<SystemOptionEntity> optionOpt = systemOptionRepository.findByOptionKey(key);
            
            if (optionOpt.isPresent()) {
                result.put("success", true);
                result.put("key", key);
                result.put("value", optionOpt.get().getOptionValue());
                result.put("description", optionOpt.get().getDescription());
            } else {
                result.put("success", false);
                result.put("message", "옵션을 찾을 수 없습니다: " + key);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 시스템 옵션 조회 실패", e);
            
            result.put("success", false);
            result.put("message", "시스템 옵션 조회 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 시스템 옵션 수정
     * 
     * PUT /api/v1/admin/system-options/{key}
     * 
     * @param key 옵션 키
     * @param request 수정 요청
     * @return 수정 결과
     */
    @PutMapping("/system-options/{key}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateSystemOption(
            @PathVariable String key,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: 시스템 옵션 수정 - key: {}", key);
            
            String value = request.get("value") != null ? request.get("value").toString() : null;
            String description = request.get("description") != null ? request.get("description").toString() : null;
            
            if (value == null) {
                result.put("success", false);
                result.put("message", "옵션 값(value)은 필수입니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            Optional<SystemOptionEntity> existingOpt = systemOptionRepository.findByOptionKey(key);
            SystemOptionEntity entity;
            
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
                entity.setOptionValue(value);
                if (description != null) {
                    entity.setDescription(description);
                }
            } else {
                entity = SystemOptionEntity.builder()
                        .optionKey(key)
                        .optionValue(value)
                        .description(description)
                        .build();
            }
            
            systemOptionRepository.save(entity);
            
            result.put("success", true);
            result.put("message", String.format("시스템 옵션 %s 수정 완료", key));
            result.put("key", key);
            result.put("value", value);
            
            log.info("관리자 요청: 시스템 옵션 수정 완료 - key: {}", key);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: 시스템 옵션 수정 실패", e);
            
            result.put("success", false);
            result.put("message", "시스템 옵션 수정 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    // ==================== A/B/C 멘트 관리 API ====================
    
    /**
     * A 멘트 목록 조회
     * 
     * GET /api/v1/admin/mission-phrase-a
     * 
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 100)
     * @return A 멘트 목록
     */
    @GetMapping("/mission-phrase-a")
    public ResponseEntity<Map<String, Object>> getMissionPhraseA(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: A 멘트 목록 조회 - page={}, size={}", page, size);
            
            List<MissionPhraseAEntity> allPhrases = missionPhraseARepository.findAll();
            
            int total = allPhrases.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<MissionPhraseAEntity> pagedPhrases = start < total ? allPhrases.subList(start, end) : new ArrayList<>();
            
            List<Map<String, Object>> phraseList = pagedPhrases.stream().map(phrase -> {
                Map<String, Object> phraseData = new HashMap<>();
                phraseData.put("id", phrase.getId());
                phraseData.put("text", phrase.getText());
                phraseData.put("strategyTags", phrase.getStrategyTags());
                phraseData.put("comboTags", phrase.getComboTags());
                phraseData.put("zodiacTags", phrase.getZodiacTags());
                phraseData.put("toneTags", phrase.getToneTags());
                phraseData.put("weightBase", phrase.getWeightBase());
                phraseData.put("createdAt", phrase.getCreatedAt() != null ? phrase.getCreatedAt().toString() : null);
                phraseData.put("updatedAt", phrase.getUpdatedAt() != null ? phrase.getUpdatedAt().toString() : null);
                return phraseData;
            }).collect(Collectors.toList());
            
            result.put("success", true);
            result.put("data", phraseList);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (total + size - 1) / size);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: A 멘트 목록 조회 실패", e);
            
            result.put("success", false);
            result.put("message", "A 멘트 목록 조회 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * A 멘트 CSV 다운로드
     * 
     * GET /api/v1/admin/mission-phrase-a/export-csv
     * 
     * CSV 형식: id,text,strategy_tags,combo_tags,zodiac_tags,tone_tags,weight_base
     * 
     * @return CSV 파일 다운로드
     */
    @GetMapping("/mission-phrase-a/export-csv")
    public ResponseEntity<byte[]> exportMissionPhraseACsv() {
        try {
            log.info("관리자 요청: A 멘트 CSV 다운로드 시작");
            
            List<MissionPhraseAEntity> phrases = missionPhraseARepository.findAll();
            
            log.info("CSV 다운로드: 총 {}개 A 멘트", phrases.size());
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8);
            
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);
            
            writer.println("id,text,strategy_tags,combo_tags,zodiac_tags,tone_tags,weight_base");
            
            for (MissionPhraseAEntity phrase : phrases) {
                writer.printf("%d,%s,%s,%s,%s,%s,%d%n",
                    phrase.getId() != null ? phrase.getId() : "",
                    escapeCsvValue(phrase.getText()),
                    escapeCsvValue(phrase.getStrategyTags() != null ? phrase.getStrategyTags() : ""),
                    escapeCsvValue(phrase.getComboTags() != null ? phrase.getComboTags() : ""),
                    escapeCsvValue(phrase.getZodiacTags() != null ? phrase.getZodiacTags() : ""),
                    escapeCsvValue(phrase.getToneTags() != null ? phrase.getToneTags() : ""),
                    phrase.getWeightBase() != null ? phrase.getWeightBase() : 1);
            }
            
            writer.flush();
            byte[] csvBytes = baos.toByteArray();
            writer.close();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", 
                    String.format("mission_phrase_a_%s.csv", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))));
            headers.setContentLength(csvBytes.length);
            
            log.info("관리자 요청: A 멘트 CSV 다운로드 완료 - {}개, {} bytes", phrases.size(), csvBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvBytes);
            
        } catch (Exception e) {
            log.error("관리자 요청: A 멘트 CSV 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * A 멘트 CSV 업로드
     * 
     * POST /api/v1/admin/mission-phrase-a/import-csv
     * 
     * CSV 형식: id,text,strategy_tags,combo_tags,zodiac_tags,tone_tags,weight_base
     * 
     * 주의: 전체 교체 모드 (기존 데이터 모두 삭제 후 업로드된 데이터로 교체)
     * 
     * @param file CSV 파일
     * @return 업로드 결과
     */
    @PostMapping("/mission-phrase-a/import-csv")
    @Transactional
    public ResponseEntity<Map<String, Object>> importMissionPhraseACsv(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: A 멘트 CSV 업로드 시작 - 파일명: {}, 크기: {} bytes", 
                    file.getOriginalFilename(), file.getSize());
            
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "파일이 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                result.put("success", false);
                result.put("message", "CSV 파일만 업로드 가능합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            missionPhraseARepository.deleteAll();
            
            int savedCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();
            List<MissionPhraseAEntity> entitiesToSave = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                int lineNumber = 0;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    
                    if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    if (isFirstLine) {
                        isFirstLine = false;
                        if (line.toLowerCase().contains("id") && line.toLowerCase().contains("text")) {
                            continue;
                        }
                    }
                    
                    List<String> columns = parseCsvLine(line, ",");
                    if (columns.size() < 2) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 컬럼 수가 부족합니다 (최소 2개 필요, 실제: %d개)", lineNumber, columns.size()));
                        continue;
                    }
                    
                    try {
                        String text = columns.size() > 1 ? columns.get(1).trim() : "";
                        String strategyTags = columns.size() > 2 && !columns.get(2).trim().isEmpty() ? columns.get(2).trim() : null;
                        String comboTags = columns.size() > 3 && !columns.get(3).trim().isEmpty() ? columns.get(3).trim() : null;
                        String zodiacTags = columns.size() > 4 && !columns.get(4).trim().isEmpty() ? columns.get(4).trim() : null;
                        String toneTags = columns.size() > 5 && !columns.get(5).trim().isEmpty() ? columns.get(5).trim() : null;
                        Integer weightBase = 1;
                        if (columns.size() > 6 && !columns.get(6).trim().isEmpty()) {
                            String weightStr = columns.get(6).trim();
                            try {
                                // 소수점 값도 처리 (예: 0.8 -> 1, 1.0 -> 1)
                                if (weightStr.contains(".")) {
                                    double weightDouble = Double.parseDouble(weightStr);
                                    weightBase = (int) Math.round(weightDouble);
                                } else {
                                    weightBase = Integer.parseInt(weightStr);
                                }
                            } catch (NumberFormatException e) {
                                // 파싱 실패 시 기본값 1 사용
                                weightBase = 1;
                            }
                        }
                        
                        if (text.isEmpty()) {
                            errorCount++;
                            errors.add(String.format("라인 %d: text는 필수 필드입니다", lineNumber));
                            continue;
                        }
                        
                        MissionPhraseAEntity entity = MissionPhraseAEntity.builder()
                                .text(text)
                                .strategyTags(strategyTags)
                                .comboTags(comboTags)
                                .zodiacTags(zodiacTags)
                                .toneTags(toneTags)
                                .weightBase(weightBase)
                                .build();
                        
                        entitiesToSave.add(entity);
                        savedCount++;
                        
                    } catch (Exception e) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 저장 실패 - %s", lineNumber, e.getMessage()));
                    }
                }
            }
            
            for (MissionPhraseAEntity entity : entitiesToSave) {
                missionPhraseARepository.save(entity);
            }
            
            result.put("success", true);
            result.put("message", String.format("A 멘트 %d개 저장 완료", savedCount));
            result.put("savedCount", savedCount);
            result.put("errorCount", errorCount);
            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }
            
            log.info("관리자 요청: A 멘트 CSV 업로드 완료 - 저장: {}개, 오류: {}개", savedCount, errorCount);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: A 멘트 CSV 업로드 실패", e);
            
            result.put("success", false);
            result.put("message", "CSV 업로드 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * B 멘트 목록 조회
     * 
     * GET /api/v1/admin/mission-phrase-b
     * 
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 100)
     * @return B 멘트 목록
     */
    @GetMapping("/mission-phrase-b")
    public ResponseEntity<Map<String, Object>> getMissionPhraseB(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: B 멘트 목록 조회 - page={}, size={}", page, size);
            
            List<MissionPhraseBEntity> allPhrases = missionPhraseBRepository.findAll();
            
            int total = allPhrases.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<MissionPhraseBEntity> pagedPhrases = start < total ? allPhrases.subList(start, end) : new ArrayList<>();
            
            List<Map<String, Object>> phraseList = pagedPhrases.stream().map(phrase -> {
                Map<String, Object> phraseData = new HashMap<>();
                phraseData.put("id", phrase.getId());
                phraseData.put("text", phrase.getText());
                phraseData.put("placeHint", phrase.getPlaceHint());
                phraseData.put("colorHint", phrase.getColorHint());
                phraseData.put("alignTags", phrase.getAlignTags());
                phraseData.put("avoidTags", phrase.getAvoidTags());
                phraseData.put("weightBase", phrase.getWeightBase());
                phraseData.put("createdAt", phrase.getCreatedAt() != null ? phrase.getCreatedAt().toString() : null);
                phraseData.put("updatedAt", phrase.getUpdatedAt() != null ? phrase.getUpdatedAt().toString() : null);
                return phraseData;
            }).collect(Collectors.toList());
            
            result.put("success", true);
            result.put("data", phraseList);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (total + size - 1) / size);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: B 멘트 목록 조회 실패", e);
            
            result.put("success", false);
            result.put("message", "B 멘트 목록 조회 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * B 멘트 CSV 다운로드
     * 
     * GET /api/v1/admin/mission-phrase-b/export-csv
     * 
     * CSV 형식: id,text,place_hint,color_hint,align_tags,avoid_tags,weight_base
     * 
     * @return CSV 파일 다운로드
     */
    @GetMapping("/mission-phrase-b/export-csv")
    public ResponseEntity<byte[]> exportMissionPhraseBCsv() {
        try {
            log.info("관리자 요청: B 멘트 CSV 다운로드 시작");
            
            List<MissionPhraseBEntity> phrases = missionPhraseBRepository.findAll();
            
            log.info("CSV 다운로드: 총 {}개 B 멘트", phrases.size());
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8);
            
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);
            
            writer.println("id,text,place_hint,color_hint,align_tags,avoid_tags,weight_base");
            
            for (MissionPhraseBEntity phrase : phrases) {
                writer.printf("%d,%s,%s,%s,%s,%s,%d%n",
                    phrase.getId() != null ? phrase.getId() : "",
                    escapeCsvValue(phrase.getText()),
                    escapeCsvValue(phrase.getPlaceHint() != null ? phrase.getPlaceHint() : ""),
                    escapeCsvValue(phrase.getColorHint() != null ? phrase.getColorHint() : ""),
                    escapeCsvValue(phrase.getAlignTags() != null ? phrase.getAlignTags() : ""),
                    escapeCsvValue(phrase.getAvoidTags() != null ? phrase.getAvoidTags() : ""),
                    phrase.getWeightBase() != null ? phrase.getWeightBase() : 1);
            }
            
            writer.flush();
            byte[] csvBytes = baos.toByteArray();
            writer.close();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", 
                    String.format("mission_phrase_b_%s.csv", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))));
            headers.setContentLength(csvBytes.length);
            
            log.info("관리자 요청: B 멘트 CSV 다운로드 완료 - {}개, {} bytes", phrases.size(), csvBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvBytes);
            
        } catch (Exception e) {
            log.error("관리자 요청: B 멘트 CSV 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * B 멘트 CSV 업로드
     * 
     * POST /api/v1/admin/mission-phrase-b/import-csv
     * 
     * CSV 형식: id,text,place_hint,color_hint,align_tags,avoid_tags,weight_base
     * 
     * 주의: 전체 교체 모드 (기존 데이터 모두 삭제 후 업로드된 데이터로 교체)
     * 
     * @param file CSV 파일
     * @return 업로드 결과
     */
    @PostMapping("/mission-phrase-b/import-csv")
    @Transactional
    public ResponseEntity<Map<String, Object>> importMissionPhraseBCsv(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: B 멘트 CSV 업로드 시작 - 파일명: {}, 크기: {} bytes", 
                    file.getOriginalFilename(), file.getSize());
            
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "파일이 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                result.put("success", false);
                result.put("message", "CSV 파일만 업로드 가능합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            missionPhraseBRepository.deleteAll();
            
            int savedCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();
            List<MissionPhraseBEntity> entitiesToSave = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                int lineNumber = 0;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    
                    if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    if (isFirstLine) {
                        isFirstLine = false;
                        if (line.toLowerCase().contains("id") && line.toLowerCase().contains("text")) {
                            continue;
                        }
                    }
                    
                    List<String> columns = parseCsvLine(line, ",");
                    if (columns.size() < 2) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 컬럼 수가 부족합니다 (최소 2개 필요, 실제: %d개)", lineNumber, columns.size()));
                        continue;
                    }
                    
                    try {
                        String text = columns.size() > 1 ? columns.get(1).trim() : "";
                        String placeHint = columns.size() > 2 && !columns.get(2).trim().isEmpty() ? columns.get(2).trim() : null;
                        String colorHint = columns.size() > 3 && !columns.get(3).trim().isEmpty() ? columns.get(3).trim() : null;
                        String alignTags = columns.size() > 4 && !columns.get(4).trim().isEmpty() ? columns.get(4).trim() : null;
                        String avoidTags = columns.size() > 5 && !columns.get(5).trim().isEmpty() ? columns.get(5).trim() : null;
                        Integer weightBase = 1;
                        if (columns.size() > 6 && !columns.get(6).trim().isEmpty()) {
                            String weightStr = columns.get(6).trim();
                            try {
                                // 소수점 값도 처리 (예: 0.8 -> 1, 1.0 -> 1)
                                if (weightStr.contains(".")) {
                                    double weightDouble = Double.parseDouble(weightStr);
                                    weightBase = (int) Math.round(weightDouble);
                                } else {
                                    weightBase = Integer.parseInt(weightStr);
                                }
                            } catch (NumberFormatException e) {
                                // 파싱 실패 시 기본값 1 사용
                                weightBase = 1;
                            }
                        }
                        
                        if (text.isEmpty()) {
                            errorCount++;
                            errors.add(String.format("라인 %d: text는 필수 필드입니다", lineNumber));
                            continue;
                        }
                        
                        MissionPhraseBEntity entity = MissionPhraseBEntity.builder()
                                .text(text)
                                .placeHint(placeHint)
                                .colorHint(colorHint)
                                .alignTags(alignTags)
                                .avoidTags(avoidTags)
                                .weightBase(weightBase)
                                .build();
                        
                        entitiesToSave.add(entity);
                        savedCount++;
                        
                    } catch (Exception e) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 저장 실패 - %s", lineNumber, e.getMessage()));
                    }
                }
            }
            
            for (MissionPhraseBEntity entity : entitiesToSave) {
                missionPhraseBRepository.save(entity);
            }
            
            result.put("success", true);
            result.put("message", String.format("B 멘트 %d개 저장 완료", savedCount));
            result.put("savedCount", savedCount);
            result.put("errorCount", errorCount);
            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }
            
            log.info("관리자 요청: B 멘트 CSV 업로드 완료 - 저장: {}개, 오류: {}개", savedCount, errorCount);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: B 멘트 CSV 업로드 실패", e);
            
            result.put("success", false);
            result.put("message", "CSV 업로드 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * C 멘트 목록 조회
     * 
     * GET /api/v1/admin/mission-phrase-c
     * 
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 100)
     * @return C 멘트 목록
     */
    @GetMapping("/mission-phrase-c")
    public ResponseEntity<Map<String, Object>> getMissionPhraseC(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: C 멘트 목록 조회 - page={}, size={}", page, size);
            
            List<MissionPhraseCEntity> allPhrases = missionPhraseCRepository.findAll();
            
            int total = allPhrases.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<MissionPhraseCEntity> pagedPhrases = start < total ? allPhrases.subList(start, end) : new ArrayList<>();
            
            List<Map<String, Object>> phraseList = pagedPhrases.stream().map(phrase -> {
                Map<String, Object> phraseData = new HashMap<>();
                phraseData.put("id", phrase.getId());
                phraseData.put("text", phrase.getText());
                phraseData.put("toneTags", phrase.getToneTags());
                phraseData.put("weightBase", phrase.getWeightBase());
                phraseData.put("createdAt", phrase.getCreatedAt() != null ? phrase.getCreatedAt().toString() : null);
                phraseData.put("updatedAt", phrase.getUpdatedAt() != null ? phrase.getUpdatedAt().toString() : null);
                return phraseData;
            }).collect(Collectors.toList());
            
            result.put("success", true);
            result.put("data", phraseList);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (total + size - 1) / size);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: C 멘트 목록 조회 실패", e);
            
            result.put("success", false);
            result.put("message", "C 멘트 목록 조회 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * C 멘트 CSV 다운로드
     * 
     * GET /api/v1/admin/mission-phrase-c/export-csv
     * 
     * CSV 형식: id,text,tone_tags,weight_base
     * 
     * @return CSV 파일 다운로드
     */
    @GetMapping("/mission-phrase-c/export-csv")
    public ResponseEntity<byte[]> exportMissionPhraseCCsv() {
        try {
            log.info("관리자 요청: C 멘트 CSV 다운로드 시작");
            
            List<MissionPhraseCEntity> phrases = missionPhraseCRepository.findAll();
            
            log.info("CSV 다운로드: 총 {}개 C 멘트", phrases.size());
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8);
            
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);
            
            writer.println("id,text,tone_tags,weight_base");
            
            for (MissionPhraseCEntity phrase : phrases) {
                writer.printf("%d,%s,%s,%d%n",
                    phrase.getId() != null ? phrase.getId() : "",
                    escapeCsvValue(phrase.getText()),
                    escapeCsvValue(phrase.getToneTags() != null ? phrase.getToneTags() : ""),
                    phrase.getWeightBase() != null ? phrase.getWeightBase() : 1);
            }
            
            writer.flush();
            byte[] csvBytes = baos.toByteArray();
            writer.close();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", 
                    String.format("mission_phrase_c_%s.csv", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))));
            headers.setContentLength(csvBytes.length);
            
            log.info("관리자 요청: C 멘트 CSV 다운로드 완료 - {}개, {} bytes", phrases.size(), csvBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvBytes);
            
        } catch (Exception e) {
            log.error("관리자 요청: C 멘트 CSV 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * C 멘트 CSV 업로드
     * 
     * POST /api/v1/admin/mission-phrase-c/import-csv
     * 
     * CSV 형식: id,text,tone_tags,weight_base
     * 
     * 주의: 전체 교체 모드 (기존 데이터 모두 삭제 후 업로드된 데이터로 교체)
     * 
     * @param file CSV 파일
     * @return 업로드 결과
     */
    @PostMapping("/mission-phrase-c/import-csv")
    @Transactional
    public ResponseEntity<Map<String, Object>> importMissionPhraseCCsv(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("관리자 요청: C 멘트 CSV 업로드 시작 - 파일명: {}, 크기: {} bytes", 
                    file.getOriginalFilename(), file.getSize());
            
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "파일이 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                result.put("success", false);
                result.put("message", "CSV 파일만 업로드 가능합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            missionPhraseCRepository.deleteAll();
            
            int savedCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();
            List<MissionPhraseCEntity> entitiesToSave = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                int lineNumber = 0;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    
                    if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    if (isFirstLine) {
                        isFirstLine = false;
                        if (line.toLowerCase().contains("id") && line.toLowerCase().contains("text")) {
                            continue;
                        }
                    }
                    
                    List<String> columns = parseCsvLine(line, ",");
                    if (columns.size() < 2) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 컬럼 수가 부족합니다 (최소 2개 필요, 실제: %d개)", lineNumber, columns.size()));
                        continue;
                    }
                    
                    try {
                        String text = columns.size() > 1 ? columns.get(1).trim() : "";
                        String toneTags = columns.size() > 2 && !columns.get(2).trim().isEmpty() ? columns.get(2).trim() : null;
                        Integer weightBase = 1;
                        if (columns.size() > 3 && !columns.get(3).trim().isEmpty()) {
                            String weightStr = columns.get(3).trim();
                            try {
                                // 소수점 값도 처리 (예: 0.8 -> 1, 1.0 -> 1)
                                if (weightStr.contains(".")) {
                                    double weightDouble = Double.parseDouble(weightStr);
                                    weightBase = (int) Math.round(weightDouble);
                                } else {
                                    weightBase = Integer.parseInt(weightStr);
                                }
                            } catch (NumberFormatException e) {
                                // 파싱 실패 시 기본값 1 사용
                                weightBase = 1;
                            }
                        }
                        
                        if (text.isEmpty()) {
                            errorCount++;
                            errors.add(String.format("라인 %d: text는 필수 필드입니다", lineNumber));
                            continue;
                        }
                        
                        MissionPhraseCEntity entity = MissionPhraseCEntity.builder()
                                .text(text)
                                .toneTags(toneTags)
                                .weightBase(weightBase)
                                .build();
                        
                        entitiesToSave.add(entity);
                        savedCount++;
                        
                    } catch (Exception e) {
                        errorCount++;
                        errors.add(String.format("라인 %d: 저장 실패 - %s", lineNumber, e.getMessage()));
                    }
                }
            }
            
            for (MissionPhraseCEntity entity : entitiesToSave) {
                missionPhraseCRepository.save(entity);
            }
            
            result.put("success", true);
            result.put("message", String.format("C 멘트 %d개 저장 완료", savedCount));
            result.put("savedCount", savedCount);
            result.put("errorCount", errorCount);
            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }
            
            log.info("관리자 요청: C 멘트 CSV 업로드 완료 - 저장: {}개, 오류: {}개", savedCount, errorCount);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("관리자 요청: C 멘트 CSV 업로드 실패", e);
            
            result.put("success", false);
            result.put("message", "CSV 업로드 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
}
