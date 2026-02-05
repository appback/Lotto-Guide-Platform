package io.appback.lottoguide.infra.persistence.repository;

import io.appback.lottoguide.infra.persistence.entity.DrawEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 로또 추첨 결과 Repository
 */
@Repository
public interface DrawRepository extends JpaRepository<DrawEntity, Integer> {

    /**
     * 최신 추첨 결과 조회
     */
    Optional<DrawEntity> findFirstByOrderByDrawNoDesc();

    /**
     * 특정 날짜 이후의 추첨 결과 조회
     */
    List<DrawEntity> findByDrawDateAfterOrderByDrawNoAsc(LocalDate date);

    /**
     * 최근 N개 추첨 결과 조회 (최신 순)
     */
    @Query(value = "SELECT * FROM lotto_draw ORDER BY draw_no DESC LIMIT :limit", nativeQuery = true)
    List<DrawEntity> findRecentDraws(int limit);
    
    /**
     * 특정 회차 범위의 추첨 결과 조회 (최신 순)
     */
    @Query("SELECT d FROM DrawEntity d WHERE d.drawNo >= :startDrawNo AND d.drawNo <= :endDrawNo ORDER BY d.drawNo DESC")
    List<DrawEntity> findByDrawNoRange(Integer startDrawNo, Integer endDrawNo);

    /**
     * 추첨 번호로 조회
     */
    Optional<DrawEntity> findByDrawNo(Integer drawNo);
    
    /**
     * 추첨 번호 존재 여부 확인
     */
    boolean existsByDrawNo(Integer drawNo);
    
    /**
     * 회차 번호 오름차순으로 모든 추첨 결과 조회
     */
    @Query("SELECT d FROM DrawEntity d ORDER BY d.drawNo ASC")
    List<DrawEntity> findAllByOrderByDrawNoAsc();
    
    /**
     * 최소 회차 번호 조회
     */
    @Query("SELECT MIN(d.drawNo) FROM DrawEntity d")
    Optional<Integer> findMinDrawNo();
    
    /**
     * 최대 회차 번호 조회
     */
    @Query("SELECT MAX(d.drawNo) FROM DrawEntity d")
    Optional<Integer> findMaxDrawNo();
    
    /**
     * 첫 번째 누락된 회차 번호 찾기 (SQL로 효율적으로)
     * 
     * 예: DB에 1, 2, 3, 5, 6이 있으면 -> 4 반환
     * 예: DB에 1, 2, 3이 있으면 -> null 반환 (모든 회차가 연속)
     * 예: DB가 비어있으면 -> null 반환
     */
    @Query(value = """
        SELECT MIN(d1.draw_no + 1) as missing_draw_no
        FROM lotto_draw d1
        WHERE NOT EXISTS (
            SELECT 1 FROM lotto_draw d2 WHERE d2.draw_no = d1.draw_no + 1
        )
        AND EXISTS (SELECT 1 FROM lotto_draw d3 WHERE d3.draw_no > d1.draw_no)
        """, nativeQuery = true)
    Optional<Integer> findFirstMissingDrawNo();
}
