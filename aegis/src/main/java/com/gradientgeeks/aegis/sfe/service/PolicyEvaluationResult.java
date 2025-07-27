package com.gradientgeeks.aegis.sfe.service;

import com.gradientgeeks.aegis.sfe.entity.Policy;
import com.gradientgeeks.aegis.sfe.entity.PolicyViolation;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of policy evaluation
 */
public class PolicyEvaluationResult {
    private boolean passed;
    private List<PolicyViolation> violations;
    private Policy.EnforcementLevel enforcementLevel;
    private String message;
    private String action;
    
    private PolicyEvaluationResult(boolean passed) {
        this.passed = passed;
        this.violations = new ArrayList<>();
    }
    
    public static PolicyEvaluationResult success() {
        PolicyEvaluationResult result = new PolicyEvaluationResult(true);
        result.message = "All policies passed";
        return result;
    }
    
    public static PolicyEvaluationResult failure(List<PolicyViolation> violations, 
                                                Policy.EnforcementLevel enforcementLevel) {
        PolicyEvaluationResult result = new PolicyEvaluationResult(false);
        result.violations = violations;
        result.enforcementLevel = enforcementLevel;
        result.message = String.format("%d policy violations detected", violations.size());
        return result;
    }
    
    public boolean isPassed() {
        return passed;
    }
    
    public List<PolicyViolation> getViolations() {
        return violations;
    }
    
    public Policy.EnforcementLevel getEnforcementLevel() {
        return enforcementLevel;
    }
    
    public String getMessage() {
        return message;
    }
    
    public boolean shouldBlock() {
        return !passed && enforcementLevel == Policy.EnforcementLevel.BLOCK;
    }
    
    public boolean requiresMfa() {
        return !passed && enforcementLevel == Policy.EnforcementLevel.REQUIRE_MFA;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    /**
     * Builder for PolicyEvaluationResult
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean passed = false;
        private List<PolicyViolation> violations = new ArrayList<>();
        private Policy.EnforcementLevel enforcementLevel;
        private String message;
        private String action;
        
        public Builder passed(boolean passed) {
            this.passed = passed;
            return this;
        }
        
        public Builder violations(List<PolicyViolation> violations) {
            this.violations = violations;
            return this;
        }
        
        public Builder enforcementLevel(Policy.EnforcementLevel enforcementLevel) {
            this.enforcementLevel = enforcementLevel;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder action(String action) {
            this.action = action;
            return this;
        }
        
        public PolicyEvaluationResult build() {
            PolicyEvaluationResult result = new PolicyEvaluationResult(passed);
            result.violations = this.violations;
            result.enforcementLevel = this.enforcementLevel;
            result.message = this.message;
            result.action = this.action;
            return result;
        }
    }
}