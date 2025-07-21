package com.gradientgeeks.ageis.backendapp.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter that creates Spring Security authentication based on Aegis headers.
 * This allows the AegisSecurityInterceptor to handle actual validation while
 * satisfying Spring Security's authentication requirements.
 */
@Component
public class AegisAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String DEVICE_ID_HEADER = "X-Device-Id";
    private static final String SIGNATURE_HEADER = "X-Signature";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String deviceId = request.getHeader(DEVICE_ID_HEADER);
        String signature = request.getHeader(SIGNATURE_HEADER);
        
        // If Aegis headers are present, create authentication token
        if (deviceId != null && signature != null) {
            // Create a simple authentication token
            // The actual validation will be done by AegisSecurityInterceptor
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    deviceId, 
                    signature, 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_DEVICE"))
                );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
}