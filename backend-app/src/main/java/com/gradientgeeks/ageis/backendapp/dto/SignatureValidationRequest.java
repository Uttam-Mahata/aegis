package com.gradientgeeks.ageis.backendapp.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for signature validation request to Aegis API.
 */
public class SignatureValidationRequest {
    
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    @NotBlank(message = "Signature is required")
    private String signature;
    
    @NotBlank(message = "String to sign is required")
    private String stringToSign;
    
    // Constructors
    public SignatureValidationRequest() {
    }
    
    public SignatureValidationRequest(String deviceId, String signature, String stringToSign) {
        this.deviceId = deviceId;
        this.signature = signature;
        this.stringToSign = stringToSign;
    }
    
    // Getters and Setters
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public String getStringToSign() {
        return stringToSign;
    }
    
    public void setStringToSign(String stringToSign) {
        this.stringToSign = stringToSign;
    }
}