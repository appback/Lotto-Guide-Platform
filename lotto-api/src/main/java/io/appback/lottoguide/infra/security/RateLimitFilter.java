package io.appback.lottoguide.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API 호출 제한 Filter
 * 
 * 프로토타입에서는 기본 구조만 구현
 * 실제 Rate Limiting은 Redis나 다른 메커니즘으로 구현 필요
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // TODO: 실제 구현 시 Rate Limiting 로직 추가
        // 1. 클라이언트 식별 (IP, User ID 등)
        // 2. 요청 횟수 체크
        // 3. 제한 초과 시 429 Too Many Requests 반환
        
        // 프로토타입에서는 모든 요청 허용
        filterChain.doFilter(request, response);
    }
}
