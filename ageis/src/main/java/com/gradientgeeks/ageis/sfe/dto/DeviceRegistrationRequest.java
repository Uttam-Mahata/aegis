package com.gradientgeeks.ageis.sfe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DeviceRegistrationRequest {
    
    @NotBlank(message = "Client ID is required")
    @Size(max = 100, message = "Client ID must not exceed 100 characters")
    private String clientId;
    
    @NotBlank(message = "Registration key is required")
    @Size(max = 512, message = "Registration key must not exceed 512 characters")
    private String registrationKey;
    
    private String integrityToken;
    
    public DeviceRegistrationRequest() {}
    
    public DeviceRegistrationRequest(String clientId, String registrationKey, String integrityToken) {
        this.clientId = clientId;
        this.registrationKey = registrationKey;
        this.integrityToken = integrityToken;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getRegistrationKey() {
        return registrationKey;
    }
    
    public void setRegistrationKey(String registrationKey) {
        this.registrationKey = registrationKey;
    }
    
    public String getIntegrityToken() {
        return integrityToken;
    }
    
    public void setIntegrityToken(String integrityToken) {
        this.integrityToken = integrityToken;
    }
    
    @Override
    public String toString() {
        return "DeviceRegistrationRequest{" +
                "clientId='" + clientId + '\'' +
                ", registrationKey='[REDACTED]'" +
                ", integrityToken='" + (integrityToken != null ? "[PRESENT]" : "null") + '\'' +
                '}';
    }
}