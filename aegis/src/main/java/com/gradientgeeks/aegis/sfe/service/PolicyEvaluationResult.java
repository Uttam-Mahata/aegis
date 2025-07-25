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
}