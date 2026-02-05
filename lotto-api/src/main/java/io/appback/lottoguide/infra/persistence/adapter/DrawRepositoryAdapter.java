package io.appback.lottoguide.infra.persistence.adapter;

import io.appback.lottoguide.application.port.out.DrawRepositoryPort;
import io.appback.lottoguide.infra.persistence.entity.DrawEntity;
import io.appback.lottoguide.infra.persistence.mapper.EntityMapper;
import io.appback.lottoguide.infra.persistence.repository.DrawRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DrawRepositoryPort 구현체
 */
@Component
@RequiredArgsConstructor
public class DrawRepositoryAdapter implements DrawRepositoryPort {
    
    private final DrawRepository drawRepository;
    private final EntityMapper entityMapper;
    
    @Override
    public Optional<DrawInfo> findLatestDraw() {
        return drawRepository.findFirstByOrderByDrawNoDesc()
            .map(entityMapper::toDrawInfo);
    }
    
    @Override
    public List<DrawInfo> findByDrawDateAfter(LocalDate date) {
        return drawRepository.findByDrawDateAfterOrderByDrawNoAsc(date).stream()
            .map(entityMapper::toDrawInfo)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<DrawInfo> findByDrawNo(Integer drawNo) {
        return drawRepository.findByDrawNo(drawNo)
            .map(entityMapper::toDrawInfo);
    }
    
    @Override
    public List<DrawInfo> findRecentDraws(int limit) {
        return drawRepository.findRecentDraws(limit).stream()
            .map(entityMapper::toDrawInfo)
            .collect(Collectors.toList());
    }
}
