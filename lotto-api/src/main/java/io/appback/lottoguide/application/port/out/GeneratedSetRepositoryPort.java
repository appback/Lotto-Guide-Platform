package io.appback.lottoguide.application.port.out;

import io.appback.lottoguide.domain.generator.model.GeneratedSet;

import java.util.List;

/**
 * 생성된 번호 세트 저장/조회 Port (Outbound)
 * Infrastructure 레이어에서 구현
 * Member 전용
 */
public interface GeneratedSetRepositoryPort {
    
    /**
     * 생성된 세트 저장
     * @param userId 사용자 ID
     * @param generatedSets 생성된 세트 리스트
     * @return 저장된 세트 ID
     */
    Long save(Long userId, List<GeneratedSet> generatedSets, String strategyCode, String strategyParamsJson, String constraintsJson);
    
    /**
     * 사용자별 생성된 세트 조회 (최신순)
     */
    List<GeneratedSetInfo> findByUserId(Long userId);
    
    /**
     * 사용자별 생성된 세트 조회 (페이징)
     */
    PagedResult<GeneratedSetInfo> findByUserId(Long userId, int page, int size);
    
    /**
     * 생성된 세트 정보
     */
    record GeneratedSetInfo(
        Long setId,
        Long userId,
        String strategyCode,
        List<GeneratedSet> generatedSets,
        java.time.LocalDateTime createdAt
    ) {}
    
    /**
     * 페이징 결과
     */
    record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {}
}
