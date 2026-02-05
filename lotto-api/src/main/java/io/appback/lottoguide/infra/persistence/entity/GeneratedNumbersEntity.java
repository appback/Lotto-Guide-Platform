package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 생성된 번호 엔티티 (Member 전용)
 * 테이블명: generated_numbers
 * GeneratedSetEntity와 1:N 관계
 */
@Entity
@Table(name = "generated_numbers")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedNumbersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "generated_set_id", nullable = false)
    private Long generatedSetId; // FK to GeneratedSetEntity

    @Column(name = "idx", nullable = false)
    private Integer idx; // 세트 내 인덱스 (0부터 시작)

    @Column(name = "n1", nullable = false)
    private Integer n1;

    @Column(name = "n2", nullable = false)
    private Integer n2;

    @Column(name = "n3", nullable = false)
    private Integer n3;

    @Column(name = "n4", nullable = false)
    private Integer n4;

    @Column(name = "n5", nullable = false)
    private Integer n5;

    @Column(name = "n6", nullable = false)
    private Integer n6;

    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson; // Explain Tags JSON
}
