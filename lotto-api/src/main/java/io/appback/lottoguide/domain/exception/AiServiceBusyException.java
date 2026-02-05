package io.appback.lottoguide.domain.exception;

/**
 * AI 서비스가 현재 사용량이 많아 처리할 수 없을 때 발생하는 예외
 */
public class AiServiceBusyException extends RuntimeException {
    
    public AiServiceBusyException(String message) {
        super(message);
    }
    
    public AiServiceBusyException(String message, Throwable cause) {
        super(message, cause);
    }
}
