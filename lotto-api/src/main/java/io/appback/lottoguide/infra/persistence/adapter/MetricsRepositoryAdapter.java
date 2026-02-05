package io.appback.lottoguide.infra.persistence.adapter;

import io.appback.lottoguide.application.port.out.MetricsRepositoryPort;
import io.appback.lottoguide.infra.persistence.entity.LottoNumberMetricsEntity;
import io.appback.lottoguide.infra.persistence.mapper.EntityMapper;
import io.appback.lottoguide.infra.persistence.repository.LottoNumberMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MetricsRepositoryPort 구현체
 */
@Component
@RequiredArgsConstructor
public class MetricsRepositoryAdapter implements MetricsRepositoryPort {
    
    private final LottoNumberMetricsRepository metricsRepository;
    private final EntityMapper entityMapper;
    
    @Override
    public Optional<NumberMetrics> findByWindowSizeAndNumber(Integer windowSize, Integer number) {
        return metricsRepository.findByWindowSizeAndNumber(windowSize, number)
            .map(entityMapper::toNumberMetrics);
    }
    
    @Override
    public List<NumberMetrics> findByWindowSize(Integer windowSize) {
        return metricsRepository.findByWindowSizeOrderByNumberAsc(windowSize).stream()
            .map(entityMapper::toNumberMetrics)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<NumberMetrics> findByWindowSizeOrderByFreqDesc(Integer windowSize) {
        return metricsRepository.findByWindowSizeOrderByFreqDesc(windowSize).stream()
            .map(entityMapper::toNumberMetrics)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<NumberMetrics> findByWindowSizeOrderByOverdueDesc(Integer windowSize) {
        return metricsRepository.findByWindowSizeOrderByOverdueDesc(windowSize).stream()
            .map(entityMapper::toNumberMetrics)
            .collect(Collectors.toList());
    }
}
