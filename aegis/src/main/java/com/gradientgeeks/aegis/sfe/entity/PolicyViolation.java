package com.gradientgeeks.aegis.sfe.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_violations", indexes = {
    @Index(name = "idx_violation_device_id", columnList = "deviceId"),
    @Index(name = "idx_violation_policy_id", columnList = "policy_id"),
    @Index(name = "idx_violation_created_at", columnList = "createdAt")
})
public class PolicyViolation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "device_id", nullable = false)
    private String deviceId;
    
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rule_id")
    private PolicyRule violatedRule;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", nullable = false)
    private ActionTaken actionTaken;
    
    @Column(name = "request_details", columnDefinition = "TEXT")
    private String requestDetails;
    
    @Column(name = "violation_details", columnDefinition = "TEXT")
    private String violationDetails;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum ActionTaken {
        BLOCKED,
        WARNED,
        NOTIFIED,
        MFA_REQUIRED,
        MONITORED
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public Policy getPolicy() {
        return policy;
    }
    
    public void setPolicy(Policy policy) {
        this.policy = policy;
    }
    
    public PolicyRule getViolatedRule() {
        return violatedRule;
    }
    
    public void setViolatedRule(PolicyRule violatedRule) {
        this.violatedRule = violatedRule;
    }
    
    public ActionTaken getActionTaken() {
        return actionTaken;
    }
    
    public void setActionTaken(ActionTaken actionTaken) {
        this.actionTaken = actionTaken;
    }
    
    public String getRequestDetails() {
        return requestDetails;
    }
    
    public void setRequestDetails(String requestDetails) {
        this.requestDetails = requestDetails;
    }
    
    public String getViolationDetails() {
        return violationDetails;
    }
    
    public void setViolationDetails(String violationDetails) {
        this.violationDetails = violationDetails;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}