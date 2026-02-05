package io.appback.lottoguide.domain.mission.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 미션 템플릿 Domain 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionTemplate {
    private String text;
    private String category;
    private String theme;
    private String tone;
    private String placeHint;
    private String timeHint;
}
