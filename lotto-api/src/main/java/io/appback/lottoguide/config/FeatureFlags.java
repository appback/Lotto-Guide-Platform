package io.appback.lottoguide.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Feature Flags 설정
 * 프로토타입 → MVP 전환 시 설정만으로 제어 가능
 */
@Configuration
@ConfigurationProperties(prefix = "app.features")
@Getter
public class FeatureFlags {
    
    /**
     * Rate Limiting 활성화 여부
     * 프로토타입: false
     * MVP: true
     */
    private boolean rateLimitEnabled = false;
    
    /**
     * LLM 캐싱 활성화 여부
     * 프로토타입: false
     * MVP: true (Redis 또는 DB 기반 캐싱)
     */
    private boolean llmCacheEnabled = false;
    
    /**
     * 광고 플래그 활성화 여부
     * 프로토타입: false
     * MVP: true (프론트엔드 광고 배치 준비)
     */
    private boolean adEnabled = false;
    
    /**
     * LLM Budget Cap 활성화 여부
     * 프로토타입: false
     * MVP: true (일일/월간 예산 제한)
     */
    private boolean llmBudgetCapEnabled = false;
    
    /**
     * Mission Cache 활성화 여부
     * 프로토타입: false
     * MVP: true
     */
    private boolean missionCacheEnabled = false;
    
    /**
     * Monitoring 활성화 여부
     * 프로토타입: false
     * MVP: true
     */
    private boolean monitoringEnabled = false;
    
    // Setters for @ConfigurationProperties
    public void setRateLimitEnabled(boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }
    
    public void setLlmCacheEnabled(boolean llmCacheEnabled) {
        this.llmCacheEnabled = llmCacheEnabled;
    }
    
    public void setAdEnabled(boolean adEnabled) {
        this.adEnabled = adEnabled;
    }
    
    public void setLlmBudgetCapEnabled(boolean llmBudgetCapEnabled) {
        this.llmBudgetCapEnabled = llmBudgetCapEnabled;
    }
    
    public void setMissionCacheEnabled(boolean missionCacheEnabled) {
        this.missionCacheEnabled = missionCacheEnabled;
    }
    
    public void setMonitoringEnabled(boolean monitoringEnabled) {
        this.monitoringEnabled = monitoringEnabled;
    }
}
