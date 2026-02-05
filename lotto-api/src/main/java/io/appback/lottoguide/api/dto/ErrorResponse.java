package io.appback.lottoguide.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 에러 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * 에러 메시지
     */
    private String message;
    
    /**
     * 에러 코드
     */
    private String errorCode;
    
    /**
     * 발생 시간
     */
    private LocalDateTime timestamp;
    
    /**
     * 상세 정보 (선택적)
     */
    private String details;
}
