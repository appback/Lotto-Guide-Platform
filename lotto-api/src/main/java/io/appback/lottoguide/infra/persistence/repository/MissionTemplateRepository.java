package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.MissionTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 미션 템플릿 리포지토리
 */
@Repository
public interface MissionTemplateRepository extends JpaRepository<MissionTemplateEntity, Long> {
    
    /**
     * 카테고리로 조회
     */
    List<MissionTemplateEntity> findByCategory(String category);
    
    /**
     * 테마로 조회
     */
    List<MissionTemplateEntity> findByTheme(String theme);
    
    /**
     * 별자리 카테고리로 조회 (ZODIAC_*)
     */
    @Query("SELECT m FROM MissionTemplateEntity m WHERE m.category LIKE 'ZODIAC_%'")
    List<MissionTemplateEntity> findZodiacTemplates();
    
    /**
     * 원소 카테고리로 조회 (ELEMENT_*)
     */
    @Query("SELECT m FROM MissionTemplateEntity m WHERE m.category LIKE 'ELEMENT_%'")
    List<MissionTemplateEntity> findElementTemplates();
    
    /**
     * 숫자 패턴 카테고리로 조회 (NUM_PATTERN_*)
     */
    @Query("SELECT m FROM MissionTemplateEntity m WHERE m.category LIKE 'NUM_PATTERN_%'")
    List<MissionTemplateEntity> findNumberPatternTemplates();
    
    /**
     * 특정 별자리 템플릿 조회
     */
    @Query("SELECT m FROM MissionTemplateEntity m WHERE m.category = :category ORDER BY m.weight DESC")
    List<MissionTemplateEntity> findByCategoryOrderByWeightDesc(@Param("category") String category);
    
    /**
     * 랜덤 템플릿 조회 (테마별)
     */
    @Query(value = "SELECT * FROM mission_templates WHERE theme = :theme ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<MissionTemplateEntity> findRandomByTheme(@Param("theme") String theme);
}
