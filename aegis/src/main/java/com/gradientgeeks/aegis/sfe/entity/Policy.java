package com.gradientgeeks.aegis.sfe.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "policies", indexes = {
    @Index(name = "idx_policy_client_id", columnList = "clientId"),
    @Index(name = "idx_policy_type", columnList = "policyType"),
    @Index(name = "idx_policy_active", columnList = "isActive")
})
public class Policy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "client_id", nullable = false)
    private String clientId;
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "policy_name", nullable = false)
    private String policyName;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false)
    private PolicyType policyType;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "enforcement_level", nullable = false)
    private EnforcementLevel enforcementLevel;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<PolicyRule> rules = new HashSet<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    public enum PolicyType {
        DEVICE_SECURITY,
        TRANSACTION_LIMIT,
        GEOGRAPHIC_RESTRICTION,
        TIME_RESTRICTION,
        DEVICE_BINDING,
        RISK_ASSESSMENT,
        API_RATE_LIMIT,
        AUTHENTICATION_REQUIREMENT
    }
    
    public enum EnforcementLevel {
        BLOCK,    // Block the request completely
        WARN,     // Allow but log warning
        NOTIFY,   // Allow but notify admin
        REQUIRE_MFA, // Require additional authentication
        MONITOR   // Monitor only, don't enforce
    }
    
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
    
    public PolicyType getPolicyType() {
        return policyType;
    }
    
    public void setPolicyType(PolicyType policyType) {
        this.policyType = policyType;
    }
    
    public EnforcementLevel getEnforcementLevel() {
        return enforcementLevel;
    }
    
    public void setEnforcementLevel(EnforcementLevel enforcementLevel) {
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
    
    public Set<PolicyRule> getRules() {
        return rules;
    }
    
    public void setRules(Set<PolicyRule> rules) {
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
    
    public User getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
}