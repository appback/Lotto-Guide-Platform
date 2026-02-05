package io.appback.lottoguide.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 시스템 옵션 엔티티
 * 
 * 웹사이트 타이틀 등 시스템 설정값을 저장합니다.
 */
@Entity
@Table(name = "system_options", uniqueConstraints = {
    @UniqueConstraint(columnNames = "option_key")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemOptionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "option_key", nullable = false, unique = true, length = 100)
    private String optionKey;
    
    @Column(name = "option_value", columnDefinition = "TEXT")
    private String optionValue;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
