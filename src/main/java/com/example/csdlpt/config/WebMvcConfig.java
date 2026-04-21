package com.example.csdlpt.config;

import com.example.csdlpt.interceptor.SiteInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SiteInterceptor siteInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply the interceptor to all API endpoints, but you can exclude some if
        // needed (e.g., public endpoints).
        registry.addInterceptor(siteInterceptor)
                .addPathPatterns("/api/**");
    }
}
