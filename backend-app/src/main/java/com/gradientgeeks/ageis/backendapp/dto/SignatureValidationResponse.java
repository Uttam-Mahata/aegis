package com.gradientgeeks.ageis.backendapp.dto;

/**
 * DTO for signature validation response from Aegis API.
 */
public class SignatureValidationResponse {
    
    private boolean isValid;
    private String message;
    private String deviceId;
    
    // Constructors
    public SignatureValidationResponse() {
    }
    
    public SignatureValidationResponse(boolean isValid, String message, String deviceId) {
        this.isValid = isValid;
        this.message = message;
        this.deviceId = deviceId;
    }
    
    // Getters and Setters
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}