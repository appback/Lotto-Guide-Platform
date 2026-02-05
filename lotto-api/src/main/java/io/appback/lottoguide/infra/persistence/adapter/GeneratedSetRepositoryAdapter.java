package io.appback.lottoguide.infra.persistence.adapter;

import io.appback.lottoguide.application.port.out.GeneratedSetRepositoryPort;
import io.appback.lottoguide.domain.generator.model.GeneratedSet;
import io.appback.lottoguide.infra.persistence.entity.GeneratedNumbersEntity;
import io.appback.lottoguide.infra.persistence.entity.GeneratedSetEntity;
import io.appback.lottoguide.infra.persistence.mapper.EntityMapper;
import io.appback.lottoguide.infra.persistence.repository.GeneratedNumbersRepository;
import io.appback.lottoguide.infra.persistence.repository.GeneratedSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GeneratedSetRepositoryPort 구현체
 */
@Component
@RequiredArgsConstructor
public class GeneratedSetRepositoryAdapter implements GeneratedSetRepositoryPort {
    
    private final GeneratedSetRepository generatedSetRepository;
    private final GeneratedNumbersRepository generatedNumbersRepository;
    private final EntityMapper entityMapper;
    
    @Override
    @Transactional
    public Long save(Long userId, List<GeneratedSet> generatedSets, String strategyCode, 
                     String strategyParamsJson, String constraintsJson) {
        // GeneratedSetEntity 저장
        GeneratedSetEntity setEntity = entityMapper.toGeneratedSetEntity(
            userId, generatedSets, strategyCode, strategyParamsJson, constraintsJson
        );
        GeneratedSetEntity savedEntity = generatedSetRepository.save(setEntity);
        
        // GeneratedNumbersEntity 리스트 저장
        List<GeneratedNumbersEntity> numbersEntities = entityMapper.toGeneratedNumbersEntities(
            savedEntity.getId(), generatedSets
        );
        generatedNumbersRepository.saveAll(numbersEntities);
        
        return savedEntity.getId();
    }
    
    @Override
    public List<GeneratedSetInfo> findByUserId(Long userId) {
        List<GeneratedSetEntity> setEntities = generatedSetRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        return setEntities.stream()
            .map(this::toGeneratedSetInfo)
            .collect(Collectors.toList());
    }
    
    @Override
    public PagedResult<GeneratedSetInfo> findByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GeneratedSetEntity> pageResult = generatedSetRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        List<GeneratedSetInfo> content = pageResult.getContent().stream()
            .map(this::toGeneratedSetInfo)
            .collect(Collectors.toList());
        
        return new PagedResult<>(
            content,
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages()
        );
    }
    
    /**
     * GeneratedSetEntity를 GeneratedSetInfo로 변환
     */
    private GeneratedSetInfo toGeneratedSetInfo(GeneratedSetEntity setEntity) {
        List<GeneratedNumbersEntity> numbersEntities = 
            generatedNumbersRepository.findByGeneratedSetIdOrderByIdxAsc(setEntity.getId());
        
        List<GeneratedSet> generatedSets = entityMapper.toGeneratedSets(setEntity, numbersEntities);
        
        return new GeneratedSetInfo(
            setEntity.getId(),
            setEntity.getUserId(),
            setEntity.getStrategyCode(),
            generatedSets,
            setEntity.getCreatedAt()
        );
    }
}
