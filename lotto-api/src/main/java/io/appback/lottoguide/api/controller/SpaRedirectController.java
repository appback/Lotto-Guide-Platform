package io.appback.lottoguide.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.time.ZonedDateTime;

/**
 * SPA(Single Page Application) 라우팅을 위한 Fallback Controller
 * 
 * React/Vue 등 SPA에서 클라이언트 사이드 라우팅을 사용할 때,
 * 직접 URL 접근 시 404 에러가 발생하는 것을 방지하기 위해
 * 모든 경로를 index.html로 포워딩합니다.
 * 
 * Context Path: /lotto
 * 
 * 주의: API 경로(/api/**)는 제외되어야 하며, RestController가 처리합니다.
 */
@Controller
public class SpaRedirectController {
    
    private static final Logger log = LoggerFactory.getLogger(SpaRedirectController.class);
    
    /**
     * SPA 라우팅을 위한 fallback 핸들러
     * 
     * API 경로(/api/**)는 제외하고, SPA 페이지 경로만 처리합니다.
     * 와일드카드 패턴을 제거하여 API 경로가 매칭되지 않도록 합니다.
     */
    @RequestMapping({
        "/",
        "/generate",
        "/deep-generate",
        "/history",
        "/admin"
    })
    public ResponseEntity<Resource> redirect(jakarta.servlet.http.HttpServletRequest request) {
        log.info("✅ [SpaRedirectController] SPA 라우팅 요청 처리 시작 - URI: {}, Method: {}", request.getRequestURI(), request.getMethod());
        
        try {
            Resource resource = new ClassPathResource("static/index.html");
            if (!resource.exists()) {
                log.error("❌ [SpaRedirectController] index.html 파일을 찾을 수 없음: static/index.html");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            // 브라우저 캐시 완전 차단
            headers.setCacheControl("no-cache, no-store, must-revalidate, max-age=0");
            headers.setPragma("no-cache");
            headers.setExpires(ZonedDateTime.now().minusYears(1)); // 과거 시간으로 설정하여 캐시 무효화
            
            long contentLength = resource.contentLength();
            if (contentLength > 0) {
                headers.setContentLength(contentLength);
            }
            
            log.info("✅ [SpaRedirectController] index.html 반환 - 크기: {} bytes", contentLength);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (IOException e) {
            log.error("❌ [SpaRedirectController] index.html 읽기 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("❌ [SpaRedirectController] 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
