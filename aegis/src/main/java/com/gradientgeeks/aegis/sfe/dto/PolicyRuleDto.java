package com.gradientgeeks.aegis.sfe.dto;

import com.gradientgeeks.aegis.sfe.entity.PolicyRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class PolicyRuleDto {
    
    private Long id;
    
    @NotBlank(message = "Rule name is required")
    @Size(max = 255)
    private String ruleName;
    
    @NotBlank(message = "Condition field is required")
    @Size(max = 100)
    private String conditionField;
    
    @NotNull(message = "Operator is required")
    private PolicyRule.RuleOperator operator;
    
    @NotBlank(message = "Condition value is required")
    private String conditionValue;
    
    private String errorMessage;
    
    private Integer priority = 100;
    
    private Boolean isActive = true;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
    
    public String getConditionField() {
        return conditionField;
    }
    
    public void setConditionField(String conditionField) {
        this.conditionField = conditionField;
    }
    
    public PolicyRule.RuleOperator getOperator() {
        return operator;
    }
    
    public void setOperator(PolicyRule.RuleOperator operator) {
        this.operator = operator;
    }
    
    public String getConditionValue() {
        return conditionValue;
    }
    
    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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