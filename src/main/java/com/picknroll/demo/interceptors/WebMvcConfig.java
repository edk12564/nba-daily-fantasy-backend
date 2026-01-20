package com.picknroll.demo.interceptors;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    public WebMvcConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SignatureVerificationInterceptor())
                .addPathPatterns("/api/interactions/**");

        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/activity/**")
                .excludePathPatterns("/api/activity/token");
    }
}
