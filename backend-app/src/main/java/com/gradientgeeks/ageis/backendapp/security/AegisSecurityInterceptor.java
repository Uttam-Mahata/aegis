package com.gradientgeeks.ageis.backendapp.security;

import com.gradientgeeks.ageis.backendapp.dto.SignatureValidationResponse;
import com.gradientgeeks.ageis.backendapp.exception.UnauthorizedException;
import com.gradientgeeks.ageis.backendapp.service.AegisIntegrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Security interceptor that validates request signatures using the Aegis Security API.
 */
@Component
public class AegisSecurityInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AegisSecurityInterceptor.class);
    
    private final AegisIntegrationService aegisIntegrationService;
    
    @Value("${security.request.signature.header}")
    private String signatureHeader;
    
    @Value("${security.request.device-id.header}")
    private String deviceIdHeader;
    
    @Value("${security.request.timestamp.header}")
    private String timestampHeader;
    
    @Value("${security.request.nonce.header}")
    private String nonceHeader;
    
    @Value("${security.request.timestamp.tolerance}")
    private long timestampTolerance;
    
    public AegisSecurityInterceptor(AegisIntegrationService aegisIntegrationService) {
        this.aegisIntegrationService = aegisIntegrationService;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.debug("Validating request: {} {}", request.getMethod(), request.getRequestURI());
        
        // Extract security headers
        String signature = request.getHeader(signatureHeader);
        String deviceId = request.getHeader(deviceIdHeader);
        String timestamp = request.getHeader(timestampHeader);
        String nonce = request.getHeader(nonceHeader);
        
        // Validate required headers
        if (signature == null || deviceId == null || timestamp == null || nonce == null) {
            logger.warn("Missing required security headers");
            throw new UnauthorizedException("Missing required security headers");
        }
        
        // Validate timestamp to prevent replay attacks
        validateTimestamp(timestamp);
        
        // Get request body hash if present
        String bodyHash = null;
        if (request.getContentLength() > 0) {
            bodyHash = computeBodyHash(request);
        }
        
        // Validate signature with Aegis API
        SignatureValidationResponse validationResponse = aegisIntegrationService.validateSignature(
                deviceId,
                signature,
                request.getMethod(),
                request.getRequestURI(),
                timestamp,
                nonce,
                bodyHash
        );
        
        if (!validationResponse.isValid()) {
            logger.warn("Invalid signature for device: {} - {}", deviceId, validationResponse.getMessage());
            throw new UnauthorizedException("Invalid request signature");
        }
        
        // Store device ID in request attributes for use in controllers
        request.setAttribute("deviceId", deviceId);
        request.setAttribute("signatureTimestamp", parseTimestamp(timestamp));
        request.setAttribute("nonce", nonce);
        request.setAttribute("signature", signature);
        
        logger.debug("Request validated successfully for device: {}", deviceId);
        return true;
    }
    
    /**
     * Validates the timestamp to prevent replay attacks.
     */
    private void validateTimestamp(String timestampStr) {
        try {
            long timestamp = Long.parseLong(timestampStr);
            long currentTime = System.currentTimeMillis();
            long difference = Math.abs(currentTime - timestamp);
            
            if (difference > timestampTolerance) {
                throw new UnauthorizedException("Request timestamp is too old or too far in the future");
            }
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("Invalid timestamp format");
        }
    }
    
    /**
     * Parses timestamp string to LocalDateTime.
     */
    private LocalDateTime parseTimestamp(String timestampStr) {
        try {
            long timestamp = Long.parseLong(timestampStr);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    /**
     * Computes SHA-256 hash of request body.
     */
    private String computeBodyHash(HttpServletRequest request) throws Exception {
        // For POST/PUT requests, we need to read the body
        if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
            // Read the body - our RepeatableRequestBodyFilter ensures this can be read multiple times
            String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            if (!body.isEmpty()) {
                return aegisIntegrationService.computeSha256Hash(body);
            }
        }
        return null;
    }
}