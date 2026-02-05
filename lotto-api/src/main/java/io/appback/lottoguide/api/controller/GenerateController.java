package io.appback.lottoguide.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appback.lottoguide.api.dto.GenerateRequest;
import io.appback.lottoguide.api.dto.GenerateResponse;
import io.appback.lottoguide.application.usecase.GenerateUseCase;
import io.appback.lottoguide.domain.generator.model.Constraints;
import io.appback.lottoguide.domain.generator.model.ExplainTag;
import io.appback.lottoguide.domain.generator.model.GeneratedSet;
import io.appback.lottoguide.domain.generator.model.Strategy;
import io.appback.lottoguide.infra.persistence.entity.StrategyDescriptionEntity;
import io.appback.lottoguide.infra.persistence.repository.StrategyDescriptionRepository;
import io.appback.lottoguide.infra.persistence.repository.SystemOptionRepository;
import io.appback.lottoguide.infra.persistence.entity.SystemOptionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 번호 생성 API Controller
 */
@RestController
@RequestMapping("/api/v1/generate")
@RequiredArgsConstructor
public class GenerateController {
    
    private final GenerateUseCase generateUseCase;
    private final StrategyDescriptionRepository strategyDescriptionRepository;
    private final SystemOptionRepository systemOptionRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 번호 생성
     * POST /api/v1/generate
     * 
     * @param request 생성 요청
     * @param userId 사용자 ID (인증된 경우, null이면 Guest)
     * @return 생성된 번호 세트
     */
    @PostMapping
    public ResponseEntity<GenerateResponse> generate(
            @RequestBody GenerateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        // 기본값 설정
        Strategy strategy = request.getStrategy() != null ? request.getStrategy() : Strategy.BALANCED;
        Constraints constraints = request.getConstraints() != null ? request.getConstraints() : Constraints.builder().build();
        int count = request.getCount() != null && request.getCount() > 0 ? request.getCount() : 1;
        
        // Wheeling System의 경우 기본값이 14이지만, 사용자가 요청한 count 값 사용
        // (5등 보장을 위해서는 14게임 권장)
        
        Integer windowSize = request.getWindowSize() != null ? request.getWindowSize() : 50;
        
        // UseCase 실행
        GenerateUseCase.GenerateResult result = generateUseCase.execute(
            strategy, constraints, count, windowSize, userId
        );
        
        // Domain 모델을 DTO로 변환
        List<GenerateResponse.GeneratedSetDto> setDtos = result.generatedSets().stream()
            .map(this::toSetDto)
            .collect(Collectors.toList());
        
        GenerateResponse response = GenerateResponse.builder()
            .generatedSets(setDtos)
            .setId(result.setId())
            .build();
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /**
     * 전략 설명 조회 (공개 API)
     * 
     * GET /api/v1/generate/strategy-descriptions
     * 
     * 모든 전략 설명을 조회합니다.
     * 
     * @return 전략 설명 목록
     */
    @GetMapping("/strategy-descriptions")
    public ResponseEntity<Map<String, Object>> getStrategyDescriptions() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<StrategyDescriptionEntity> entities = strategyDescriptionRepository.findAll();
            
            Map<String, Map<String, Object>> descriptions = entities.stream()
                    .collect(Collectors.toMap(
                            StrategyDescriptionEntity::getStrategyCode,
                            entity -> {
                                Map<String, Object> desc = new HashMap<>();
                                desc.put("title", entity.getTitle());
                                desc.put("shortDescription", entity.getShortDescription());
                                desc.put("description", entity.getDescription());
                                try {
                                    desc.put("features", objectMapper.readValue(entity.getFeatures(), new TypeReference<List<String>>() {}));
                                    desc.put("algorithm", objectMapper.readValue(entity.getAlgorithm(), new TypeReference<List<String>>() {}));
                                    desc.put("scenarios", objectMapper.readValue(entity.getScenarios(), new TypeReference<List<String>>() {}));
                                    if (entity.getNotes() != null && !entity.getNotes().isEmpty()) {
                                        desc.put("notes", objectMapper.readValue(entity.getNotes(), new TypeReference<List<String>>() {}));
                                    }
                                } catch (Exception e) {
                                    // JSON 파싱 실패 시 빈 리스트 반환
                                    desc.put("features", List.of());
                                    desc.put("algorithm", List.of());
                                    desc.put("scenarios", List.of());
                                }
                                return desc;
                            }
                    ));
            
            result.put("success", true);
            result.put("data", descriptions);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "전략 설명 조회 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 공개 시스템 옵션 조회 (인증 불필요, 일련번호 포함)
     * 
     * GET /api/v1/generate/system-options
     * 
     * @return 모든 시스템 옵션 및 일련번호
     */
    @GetMapping("/system-options")
    public ResponseEntity<Map<String, Object>> getPublicSystemOptions() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<SystemOptionEntity> options = systemOptionRepository.findAll();
            
            // 옵션을 key-value 맵으로 변환 (정렬하여 일관된 해시 생성)
            Map<String, String> optionsMap = new java.util.TreeMap<>();
            for (SystemOptionEntity option : options) {
                optionsMap.put(option.getOptionKey(), option.getOptionValue() != null ? option.getOptionValue() : "");
            }
            
            // 일련번호 생성 (모든 옵션 내용 기반 해시)
            String serialNumber = generateSystemOptionsHash(optionsMap);
            
            result.put("success", true);
            result.put("data", optionsMap);
            result.put("count", optionsMap.size());
            result.put("serialNumber", serialNumber);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "시스템 옵션 조회 실패: " + e.getMessage());
            
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
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
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
            
        } catch (java.security.NoSuchAlgorithmException e) {
            // 폴백: 간단한 해시
            return String.valueOf(optionsMap.toString().hashCode());
        }
    }
    
    /**
     * GeneratedSet을 DTO로 변환
     */
    private GenerateResponse.GeneratedSetDto toSetDto(GeneratedSet set) {
        List<String> tagStrings = set.getTags() != null
            ? set.getTags().stream()
                .map(ExplainTag::name)
                .collect(Collectors.toList())
            : List.of();
        
        return GenerateResponse.GeneratedSetDto.builder()
            .index(set.getIndex())
            .numbers(set.getNumbers())
            .tags(tagStrings)
            .build();
    }
}
