package com.gradientgeeks.aegis.sfe.dto;

import com.gradientgeeks.aegis.sfe.entity.Policy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class PolicyDto {
    
    private Long id;
    
    @NotBlank(message = "Client ID is required")
    @Size(max = 100)
    private String clientId;
    
    @NotBlank(message = "Policy name is required")
    @Size(max = 255)
    private String policyName;
    
    @NotNull(message = "Policy type is required")
    private Policy.PolicyType policyType;
    
    @NotNull(message = "Enforcement level is required")
    private Policy.EnforcementLevel enforcementLevel;
    
    private String description;
    
    private Boolean isActive = true;
    
    private List<PolicyRuleDto> rules;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Getters and Setters
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
    
    public String getPolicyName() {
        return policyName;
    }
    
    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }
    
    public Policy.PolicyType getPolicyType() {
        return policyType;
    }
    
    public void setPolicyType(Policy.PolicyType policyType) {
        this.policyType = policyType;
    }
    
    public Policy.EnforcementLevel getEnforcementLevel() {
        return enforcementLevel;
    }
    
    public void setEnforcementLevel(Policy.EnforcementLevel enforcementLevel) {
        this.enforcementLevel = enforcementLevel;
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
    
    public List<PolicyRuleDto> getRules() {
        return rules;
    }
    
    public void setRules(List<PolicyRuleDto> rules) {
        this.rules = rules;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}