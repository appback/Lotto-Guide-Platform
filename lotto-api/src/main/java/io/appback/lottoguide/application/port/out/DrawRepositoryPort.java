package io.appback.lottoguide.application.port.out;

import io.appback.lottoguide.domain.generator.model.GeneratedSet;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 추첨 결과 조회 Port (Outbound)
 * Infrastructure 레이어에서 구현
 */
public interface DrawRepositoryPort {
    
    /**
     * 최신 추첨 결과 조회
     */
    Optional<DrawInfo> findLatestDraw();
    
    /**
     * 특정 날짜 이후의 추첨 결과 조회
     */
    List<DrawInfo> findByDrawDateAfter(LocalDate date);
    
    /**
     * 추첨 번호로 조회
     */
    Optional<DrawInfo> findByDrawNo(Integer drawNo);
    
    /**
     * 최근 N개 추첨 결과 조회 (최신 순)
     */
    List<DrawInfo> findRecentDraws(int limit);
    
    /**
     * 추첨 정보
     */
    record DrawInfo(
        Integer drawNo,
        LocalDate drawDate,
        List<Integer> numbers, // n1..n6
        Integer bonus
    ) {}
}
