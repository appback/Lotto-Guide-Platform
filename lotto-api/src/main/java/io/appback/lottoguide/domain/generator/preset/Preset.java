package io.appback.lottoguide.domain.generator.preset;

import io.appback.lottoguide.domain.generator.model.Constraints;

import java.util.List;

/**
 * 번호 생성 Preset 인터페이스
 */
public interface Preset {
    
    /**
     * 번호 생성
     * @param constraints 제약 조건
     * @param windowSize 윈도우 크기 (20, 50, 100)
     * @param metricsList 메트릭 데이터 리스트 (번호별 빈도, 과거 데이터)
     *                    비어있으면 랜덤 생성, 있으면 메트릭 기반 생성
     * @return 생성된 번호 6개
     */
    List<Integer> generate(Constraints constraints, Integer windowSize, List<NumberMetrics> metricsList);
    
    /**
     * 번호 메트릭 데이터 (Domain 레이어에서 사용)
     */
    record NumberMetrics(
        Integer number,
        Integer frequency,
        Integer overdue,
        Integer lastSeenDrawNo
    ) {}
}
