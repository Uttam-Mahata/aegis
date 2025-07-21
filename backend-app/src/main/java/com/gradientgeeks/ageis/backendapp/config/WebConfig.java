package com.gradientgeeks.ageis.backendapp.config;

import com.gradientgeeks.ageis.backendapp.filter.RepeatableRequestBodyFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for the application.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
    
    @Bean
    public FilterRegistrationBean<RepeatableRequestBodyFilter> repeatableRequestBodyFilter() {
        FilterRegistrationBean<RepeatableRequestBodyFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RepeatableRequestBodyFilter());
        registrationBean.addUrlPatterns("/api/v1/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}