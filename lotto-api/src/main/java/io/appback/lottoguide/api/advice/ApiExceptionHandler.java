package io.appback.lottoguide.api.advice;

import io.appback.lottoguide.api.dto.ErrorResponse;
import io.appback.lottoguide.domain.exception.AiServiceBusyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

/**
 * 전역 예외 처리 Handler
 */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {
    
    /**
     * 잘못된 요청 파라미터 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation error: {}", e.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .message("요청 파라미터가 유효하지 않습니다.")
            .errorCode("VALIDATION_ERROR")
            .timestamp(LocalDateTime.now())
            .details(e.getMessage())
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .message(e.getMessage())
            .errorCode("ILLEGAL_ARGUMENT")
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * AI 서비스 사용량 초과 처리
     */
    @ExceptionHandler(AiServiceBusyException.class)
    public ResponseEntity<ErrorResponse> handleAiServiceBusyException(AiServiceBusyException e) {
        log.warn("AI 서비스 사용량 초과: {}", e.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .message(e.getMessage())
            .errorCode("AI_SERVICE_BUSY")
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }
    
    /**
     * 정적 리소스 없음 처리 (무시)
     * favicon.ico, .well-known 등 브라우저 자동 요청
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Void> handleResourceNotFoundException(Exception e) {
        // 정적 리소스 요청은 무시 (로그만 남기지 않음)
        return ResponseEntity.notFound().build();
    }
    
    /**
     * 일반 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error", e);
        
        ErrorResponse error = ErrorResponse.builder()
            .message("서버 내부 오류가 발생했습니다.")
            .errorCode("INTERNAL_ERROR")
            .timestamp(LocalDateTime.now())
            .details(e.getMessage())
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
