package io.appback.lottoguide.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 설정 - SPA 라우팅 지원
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 정적 리소스만 처리 (API 경로는 제외)
        // Context path가 /lotto이므로 /assets/**만 지정하면 Spring이 자동으로 /lotto/assets/**로 매핑
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(3600);
        
        // Context path가 /lotto이므로 /build-info.json, /vite.svg만 지정하면 Spring이 자동으로 /lotto/build-info.json, /lotto/vite.svg로 매핑
        registry.addResourceHandler("/build-info.json", "/vite.svg")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);
        
        // index.html 명시적 서빙 (API 경로 제외)
        // Context path가 /lotto이므로 /index.html만 지정하면 Spring이 자동으로 /lotto/index.html로 매핑
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/index.html")
                .setCachePeriod(0);
        
        // 나머지 SPA 라우팅(/)은 SpaRedirectController가 처리
    }
}
