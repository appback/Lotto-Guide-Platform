package io.appback.lottoguide.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 인증 설정
 * 
 * 프로토타입에서는 최소한의 보안 설정
 * - Guest/Member 구분은 X-User-Id 헤더로 처리
 * - 실제 인증은 후반 작업으로 예정
 */
@Configuration
@EnableWebSecurity
public class AuthConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // 프로토타입에서는 CSRF 비활성화
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // API 경로
                .requestMatchers("/api/v1/**").permitAll() // 프로토타입에서는 모든 API 허용
                .requestMatchers("/actuator/**").permitAll() // Actuator 엔드포인트 허용
                
                // 정적 리소스
                .requestMatchers("/lotto/assets/**", "/lotto/vite.svg", "/lotto/build-info.json").permitAll()
                
                // SPA 라우팅 경로 (프론트엔드에서 인증 처리)
                .requestMatchers("/lotto", "/lotto/", "/lotto/index.html").permitAll()
                .requestMatchers("/lotto/generate", "/lotto/deep-generate", "/lotto/history").permitAll()
                
                // 나머지는 허용 (SPA fallback)
                .anyRequest().permitAll()
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
