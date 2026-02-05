package io.appback.lottoguide.infra.persistence.mapper;

import io.appback.lottoguide.application.port.out.DrawRepositoryPort;
import io.appback.lottoguide.application.port.out.MetricsRepositoryPort;
import io.appback.lottoguide.domain.generator.model.ExplainTag;
import io.appback.lottoguide.domain.generator.model.GeneratedSet;
import io.appback.lottoguide.infra.persistence.entity.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entity ↔ Domain 모델 변환 Mapper
 */
@Component
@RequiredArgsConstructor
public class EntityMapper {
    
    private final ObjectMapper objectMapper;
    
    /**
     * DrawEntity를 DrawInfo로 변환
     */
    public DrawRepositoryPort.DrawInfo toDrawInfo(DrawEntity entity) {
        List<Integer> numbers = Arrays.asList(
            entity.getN1(), entity.getN2(), entity.getN3(),
            entity.getN4(), entity.getN5(), entity.getN6()
        );
        
        return new DrawRepositoryPort.DrawInfo(
            entity.getDrawNo(),
            entity.getDrawDate(),
            numbers,
            entity.getBonus()
        );
    }
    
    /**
     * LottoNumberMetricsEntity를 NumberMetrics로 변환
     */
    public MetricsRepositoryPort.NumberMetrics toNumberMetrics(LottoNumberMetricsEntity entity) {
        return new MetricsRepositoryPort.NumberMetrics(
            entity.getNumber(),
            entity.getFreq(),
            entity.getOverdue(),
            entity.getLastSeenDrawNo()
        );
    }
    
    /**
     * GeneratedSetEntity와 GeneratedNumbersEntity 리스트를 GeneratedSet 리스트로 변환
     */
    public List<GeneratedSet> toGeneratedSets(GeneratedSetEntity setEntity, List<GeneratedNumbersEntity> numbersEntities) {
        List<GeneratedSet> sets = new ArrayList<>();
        
        for (GeneratedNumbersEntity numbersEntity : numbersEntities) {
            List<Integer> numbers = Arrays.asList(
                numbersEntity.getN1(), numbersEntity.getN2(), numbersEntity.getN3(),
                numbersEntity.getN4(), numbersEntity.getN5(), numbersEntity.getN6()
            );
            
            List<ExplainTag> tags = parseTagsJson(numbersEntity.getTagsJson());
            
            GeneratedSet set = GeneratedSet.builder()
                .index(numbersEntity.getIdx())
                .numbers(numbers)
                .tags(tags)
                .createdAt(setEntity.getCreatedAt())
                .build();
            
            sets.add(set);
        }
        
        return sets;
    }
    
    /**
     * GeneratedSet 리스트를 Entity로 변환하여 저장
     */
    public GeneratedSetEntity toGeneratedSetEntity(Long userId, List<GeneratedSet> generatedSets, 
                                                    String strategyCode, String strategyParamsJson, String constraintsJson) {
        return GeneratedSetEntity.builder()
            .userId(userId)
            .strategyCode(strategyCode)
            .strategyParamsJson(strategyParamsJson)
            .constraintsJson(constraintsJson)
            .generatedCount(generatedSets.size())
            .build();
    }
    
    /**
     * GeneratedSet 리스트를 GeneratedNumbersEntity 리스트로 변환
     */
    public List<GeneratedNumbersEntity> toGeneratedNumbersEntities(Long generatedSetId, List<GeneratedSet> generatedSets) {
        return generatedSets.stream()
            .map(set -> {
                String tagsJson = set.getTags() != null ? toTagsJson(set.getTags()) : null;
                
                return GeneratedNumbersEntity.builder()
                    .generatedSetId(generatedSetId)
                    .idx(set.getIndex())
                    .n1(set.getNumbers().get(0))
                    .n2(set.getNumbers().get(1))
                    .n3(set.getNumbers().get(2))
                    .n4(set.getNumbers().get(3))
                    .n5(set.getNumbers().get(4))
                    .n6(set.getNumbers().get(5))
                    .tagsJson(tagsJson)
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Tags JSON을 ExplainTag 리스트로 파싱
     */
    private List<ExplainTag> parseTagsJson(String tagsJson) {
        if (tagsJson == null || tagsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            List<String> tagStrings = objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
            return tagStrings.stream()
                .map(tag -> {
                    try {
                        return ExplainTag.valueOf(tag);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(tag -> tag != null)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * ExplainTag 리스트를 JSON으로 변환
     */
    private String toTagsJson(List<ExplainTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        
        try {
            List<String> tagStrings = tags.stream()
                .map(ExplainTag::name)
                .collect(Collectors.toList());
            return objectMapper.writeValueAsString(tagStrings);
        } catch (Exception e) {
            return null;
        }
    }
}
