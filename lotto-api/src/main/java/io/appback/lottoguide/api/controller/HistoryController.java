package io.appback.lottoguide.api.controller;

import io.appback.lottoguide.api.dto.GenerateResponse;
import io.appback.lottoguide.api.dto.HistoryResponse;
import io.appback.lottoguide.application.port.out.GeneratedSetRepositoryPort;
import io.appback.lottoguide.application.usecase.HistoryUseCase;
import io.appback.lottoguide.domain.generator.model.ExplainTag;
import io.appback.lottoguide.domain.generator.model.GeneratedSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 히스토리 조회 API Controller
 * Member 전용
 */
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class HistoryController {
    
    private final HistoryUseCase historyUseCase;
    
    /**
     * 히스토리 조회 (페이징)
     * GET /api/v1/history?page=0&size=10
     * 
     * @param userId 사용자 ID (필수)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return 페이징된 히스토리
     */
    @GetMapping
    public ResponseEntity<HistoryResponse> getHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // UseCase 실행
        GeneratedSetRepositoryPort.PagedResult<GeneratedSetRepositoryPort.GeneratedSetInfo> result =
            historyUseCase.execute(userId, page, size);
        
        // Domain 모델을 DTO로 변환
        List<HistoryResponse.HistoryItemDto> items = result.content().stream()
            .map(this::toHistoryItemDto)
            .collect(Collectors.toList());
        
        HistoryResponse response = HistoryResponse.builder()
            .content(items)
            .page(result.page())
            .size(result.size())
            .totalElements(result.totalElements())
            .totalPages(result.totalPages())
            .build();
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /**
     * GeneratedSetInfo를 HistoryItemDto로 변환
     */
    private HistoryResponse.HistoryItemDto toHistoryItemDto(
            GeneratedSetRepositoryPort.GeneratedSetInfo info) {
        
        List<GenerateResponse.GeneratedSetDto> setDtos = info.generatedSets().stream()
            .map(this::toSetDto)
            .collect(Collectors.toList());
        
        return HistoryResponse.HistoryItemDto.builder()
            .setId(info.setId())
            .strategyCode(info.strategyCode())
            .generatedSets(setDtos)
            .createdAt(info.createdAt())
            .build();
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
