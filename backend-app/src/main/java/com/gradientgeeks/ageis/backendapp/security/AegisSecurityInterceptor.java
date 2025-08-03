package com.gradientgeeks.ageis.backendapp.security;

import com.gradientgeeks.ageis.backendapp.dto.SignatureValidationResponse;
import com.gradientgeeks.ageis.backendapp.exception.UnauthorizedException;
import com.gradientgeeks.ageis.backendapp.service.AegisIntegrationService;
import com.gradientgeeks.ageis.backendapp.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Security interceptor that validates request signatures using the Aegis Security API.
 */
@Component
public class AegisSecurityInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AegisSecurityInterceptor.class);
    
    private final AegisIntegrationService aegisIntegrationService;
    
    @Autowired
    private UserContextService userContextService;
    
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
        
        // Extract user metadata for policy enforcement
        Map<String, Object> userMetadata = null;
        
        // Try to get username from session if available
        HttpSession session = request.getSession(false);
        if (session != null) {
            String username = (String) session.getAttribute("username");
            if (username != null) {
                // Determine transaction context based on URI
                String transactionType = determineTransactionType(request.getRequestURI());
                String beneficiaryType = extractBeneficiaryType(request);
                
                // Extract transaction amount if this is a transfer
                Object transactionAmount = null;
                if ("TRANSFER".equals(transactionType) && request.getContentLength() > 0) {
                    transactionAmount = extractTransactionAmount(request);
                }
                
                // Extract user metadata for policy enforcement
                userMetadata = userContextService.extractUserMetadata(
                        username, 
                        deviceId, 
                        transactionType,
                        transactionAmount,
                        beneficiaryType
                );
                
                logger.debug("Extracted user metadata for policy enforcement: {}", 
                           userMetadata != null ? userMetadata.keySet() : "none");
                if (transactionAmount != null) {
                    logger.debug("Transaction amount: {}", transactionAmount);
                }
            }
        }
        
        // Validate signature with Aegis API including user metadata
        SignatureValidationResponse validationResponse = aegisIntegrationService.validateSignatureWithMetadata(
                deviceId,
                signature,
                request.getMethod(),
                request.getRequestURI(),
                timestamp,
                nonce,
                bodyHash,
                userMetadata
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
    
    /**
     * Determines transaction type based on request URI
     */
    private String determineTransactionType(String uri) {
        if (uri == null) {
            return null;
        }
        
        // Map URIs to transaction types
        if (uri.contains("/transfer")) {
            return "TRANSFER";
        } else if (uri.contains("/payment")) {
            return "PAYMENT";
        } else if (uri.contains("/withdrawal")) {
            return "WITHDRAWAL";
        } else if (uri.contains("/deposit")) {
            return "DEPOSIT";
        } else if (uri.contains("/balance")) {
            return "BALANCE_CHECK";
        } else if (uri.contains("/account")) {
            return "ACCOUNT_ACCESS";
        } else if (uri.contains("/login")) {
            return "LOGIN";
        } else if (uri.contains("/auth")) {
            return "AUTHENTICATION";
        }
        
        return "GENERAL";
    }
    
    /**
     * Extracts beneficiary type from request if applicable
     */
    private String extractBeneficiaryType(HttpServletRequest request) {
        // Check request parameters or headers for beneficiary information
        String beneficiaryId = request.getParameter("beneficiaryId");
        if (beneficiaryId != null) {
            // In a real implementation, you would check if this is a new or existing beneficiary
            // For now, we'll use a simple heuristic
            return beneficiaryId.startsWith("NEW_") ? "NEW" : "EXISTING";
        }
        
        // Check custom header if present
        String beneficiaryType = request.getHeader("X-Beneficiary-Type");
        if (beneficiaryType != null) {
            return beneficiaryType;
        }
        
        return "UNKNOWN";
    }
    
    /**
     * Extracts transaction amount from request body if available
     */
    private Object extractTransactionAmount(HttpServletRequest request) {
        try {
            // Read the body - our RepeatableRequestBodyFilter ensures this can be read multiple times
            String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            if (!body.isEmpty()) {
                // Parse JSON to extract amount
                // This is a simple implementation - in production use proper JSON parsing
                if (body.contains("\"amount\"")) {
                    int amountIndex = body.indexOf("\"amount\"");
                    int colonIndex = body.indexOf(":", amountIndex);
                    int commaIndex = body.indexOf(",", colonIndex);
                    int endIndex = commaIndex > 0 ? commaIndex : body.indexOf("}", colonIndex);
                    
                    if (colonIndex > 0 && endIndex > colonIndex) {
                        String amountStr = body.substring(colonIndex + 1, endIndex).trim();
                        // Remove quotes if present
                        amountStr = amountStr.replace("\"", "");
                        
                        try {
                            return Double.parseDouble(amountStr);
                        } catch (NumberFormatException e) {
                            logger.debug("Could not parse amount: {}", amountStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract transaction amount from request", e);
        }
        return null;
    }
}