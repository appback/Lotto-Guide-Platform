package io.appback.lottoguide.application.usecase;

import io.appback.lottoguide.application.port.out.GeneratedSetRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 히스토리 조회 UseCase
 * Member 전용
 */
@Service
@RequiredArgsConstructor
public class HistoryUseCase {
    
    private final GeneratedSetRepositoryPort generatedSetRepositoryPort;
    
    /**
     * 사용자 히스토리 조회
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 히스토리 결과
     */
    public GeneratedSetRepositoryPort.PagedResult<GeneratedSetRepositoryPort.GeneratedSetInfo> execute(
            Long userId, int page, int size) {
        
        return generatedSetRepositoryPort.findByUserId(userId, page, size);
    }
    
    /**
     * 사용자 히스토리 전체 조회 (최신순)
     */
    public List<GeneratedSetRepositoryPort.GeneratedSetInfo> executeAll(Long userId) {
        return generatedSetRepositoryPort.findByUserId(userId);
    }
}
