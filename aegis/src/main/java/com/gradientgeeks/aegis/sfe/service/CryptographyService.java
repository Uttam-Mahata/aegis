package com.gradientgeeks.aegis.sfe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptographyService {
    
    private static final Logger logger = LoggerFactory.getLogger(CryptographyService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    public String generateSecretKey() {
        return new BigInteger(256, SECURE_RANDOM).toString(32);
    }
    
    public String generateRegistrationKey() {
        return new BigInteger(256, SECURE_RANDOM).toString(32);
    }
    
    public String generateDeviceId() {
        return "dev_" + new BigInteger(128, SECURE_RANDOM).toString(32);
    }
    
    public String computeHmacSha256(String secretKey, String data) {
        try {
            logger.debug("Computing HMAC-SHA256 with data length: {} characters", data.length());
            logger.trace("Data to sign: {}", data);
            
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hashBytes);
            
            logger.debug("Computed HMAC-SHA256 signature: {}", signature);
            return signature;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Failed to compute HMAC-SHA256", e);
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }
    
    public boolean verifyHmacSha256(String secretKey, String data, String expectedSignature) {
        try {
            logger.debug("Verifying HMAC-SHA256 signature");
            String computedSignature = computeHmacSha256(secretKey, data);
            
            boolean isEqual = MessageDigest.isEqual(
                computedSignature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
            
            logger.debug("Signature verification result: {}", isEqual);
            if (!isEqual) {
                logger.debug("Computed: {} vs Expected: {}", computedSignature, expectedSignature);
            }
            
            return isEqual;
        } catch (Exception e) {
            logger.error("Error during HMAC-SHA256 verification", e);
            return false;
        }
    }
    
    public String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute SHA-256 hash", e);
        }
    }
    
    public boolean isValidBase64(String input) {
        try {
            Base64.getDecoder().decode(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}