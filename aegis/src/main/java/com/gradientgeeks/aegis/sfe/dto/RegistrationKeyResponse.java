package com.gradientgeeks.aegis.sfe.dto;

import java.time.LocalDateTime;

public class RegistrationKeyResponse {
    
    private Long id;
    private String clientId;
    private String registrationKey;
    private String description;
    private Boolean isActive;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private String status;
    private String message;
    
    public RegistrationKeyResponse() {}
    
    public RegistrationKeyResponse(Long id, String clientId, String registrationKey, 
                                   String description, Boolean isActive, LocalDateTime expiresAt, 
                                   LocalDateTime createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.registrationKey = registrationKey;
        this.description = description;
        this.isActive = isActive;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.status = "success";
    }
    
    public RegistrationKeyResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "RegistrationKeyResponse{" +
                "id=" + id +
                ", clientId='" + clientId + '\'' +
                ", registrationKey='[REDACTED]'" +
                ", description='" + description + '\'' +
                ", isActive=" + isActive +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}