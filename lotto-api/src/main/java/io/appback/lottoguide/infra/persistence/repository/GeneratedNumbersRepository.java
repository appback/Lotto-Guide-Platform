package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.GeneratedNumbersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 생성된 번호 Repository (Member 전용)
 */
@Repository
public interface GeneratedNumbersRepository extends JpaRepository<GeneratedNumbersEntity, Long> {

    /**
     * 세트 ID로 생성된 번호 조회 (인덱스 순)
     */
    List<GeneratedNumbersEntity> findByGeneratedSetIdOrderByIdxAsc(Long generatedSetId);

    /**
     * 세트 ID로 생성된 번호 개수 조회
     */
    long countByGeneratedSetId(Long generatedSetId);

    /**
     * 세트 ID로 모든 생성된 번호 삭제
     */
    void deleteByGeneratedSetId(Long generatedSetId);
}
