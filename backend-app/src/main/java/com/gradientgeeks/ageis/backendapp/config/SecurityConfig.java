package com.gradientgeeks.ageis.backendapp.config;

import com.gradientgeeks.ageis.backendapp.filter.AegisAuthenticationFilter;
import com.gradientgeeks.ageis.backendapp.security.AegisSecurityInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security configuration for the UCO Bank Backend.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {
    
    private final AegisSecurityInterceptor aegisSecurityInterceptor;
    private final AegisAuthenticationFilter aegisAuthenticationFilter;
    
    public SecurityConfig(AegisSecurityInterceptor aegisSecurityInterceptor,
                         AegisAuthenticationFilter aegisAuthenticationFilter) {
        this.aegisSecurityInterceptor = aegisSecurityInterceptor;
        this.aegisAuthenticationFilter = aegisAuthenticationFilter;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/health/**", "/actuator/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()  // Allow auth endpoints
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll()
            )
            .addFilterBefore(aegisAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(aegisSecurityInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/health/**", "/api/v1/auth/**");
    }
}