package com.gradientgeeks.aegis.sfe.service;

import com.gradientgeeks.aegis.sfe.entity.*;
import com.gradientgeeks.aegis.sfe.repository.PolicyRepository;
import com.gradientgeeks.aegis.sfe.repository.PolicyViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class PolicyEngineService {
    
    private static final Logger logger = LoggerFactory.getLogger(PolicyEngineService.class);
    
    private final PolicyRepository policyRepository;
    private final PolicyViolationRepository violationRepository;
    
    @Autowired
    public PolicyEngineService(PolicyRepository policyRepository, 
                             PolicyViolationRepository violationRepository) {
        this.policyRepository = policyRepository;
        this.violationRepository = violationRepository;
    }
    
    /**
     * Evaluates all active policies for a given request context
     */
    public PolicyEvaluationResult evaluatePolicies(PolicyContext context) {
        logger.info("Evaluating policies for device: {} and client: {}", 
            context.getDeviceId(), context.getClientId());
        
        List<Policy> activePolicies = policyRepository.findActiveByClientId(context.getClientId());
        List<PolicyViolation> violations = new ArrayList<>();
        Policy.EnforcementLevel highestEnforcement = null;
        
        for (Policy policy : activePolicies) {
            PolicyEvaluationResult policyResult = evaluatePolicy(policy, context);
            
            if (!policyResult.isPassed()) {
                violations.addAll(policyResult.getViolations());
                
                if (highestEnforcement == null || 
                    policy.getEnforcementLevel().ordinal() < highestEnforcement.ordinal()) {
                    highestEnforcement = policy.getEnforcementLevel();
                }
            }
        }
        
        if (violations.isEmpty()) {
            logger.info("All policies passed for device: {}", context.getDeviceId());
            return PolicyEvaluationResult.success();
        }
        
        logger.warn("Policy violations detected for device: {}: {} violations", 
            context.getDeviceId(), violations.size());
        
        // Save violations for audit
        violations.forEach(violationRepository::save);
        
        return PolicyEvaluationResult.failure(violations, highestEnforcement);
    }
    
    /**
     * Evaluates a single policy against the context
     */
    private PolicyEvaluationResult evaluatePolicy(Policy policy, PolicyContext context) {
        List<PolicyViolation> violations = new ArrayList<>();
        
        for (PolicyRule rule : policy.getRules()) {
            if (!rule.getIsActive()) continue;
            
            boolean rulePassed = evaluateRule(rule, context);
            
            if (!rulePassed) {
                PolicyViolation violation = createViolation(policy, rule, context);
                violations.add(violation);
                
                // If enforcement is BLOCK, stop evaluation
                if (policy.getEnforcementLevel() == Policy.EnforcementLevel.BLOCK) {
                    break;
                }
            }
        }
        
        return violations.isEmpty() 
            ? PolicyEvaluationResult.success() 
            : PolicyEvaluationResult.failure(violations, policy.getEnforcementLevel());
    }
    
    /**
     * Evaluates a single rule against the context
     */
    private boolean evaluateRule(PolicyRule rule, PolicyContext context) {
        Object fieldValue = extractFieldValue(rule.getConditionField(), context);
        String conditionValue = rule.getConditionValue();
        
        try {
            switch (rule.getOperator()) {
                case EQUALS:
                    return Objects.equals(String.valueOf(fieldValue), conditionValue);
                    
                case NOT_EQUALS:
                    return !Objects.equals(String.valueOf(fieldValue), conditionValue);
                    
                case GREATER_THAN:
                    return compareNumeric(fieldValue, conditionValue) > 0;
                    
                case LESS_THAN:
                    return compareNumeric(fieldValue, conditionValue) < 0;
                    
                case GREATER_THAN_OR_EQUALS:
                    return compareNumeric(fieldValue, conditionValue) >= 0;
                    
                case LESS_THAN_OR_EQUALS:
                    return compareNumeric(fieldValue, conditionValue) <= 0;
                    
                case CONTAINS:
                    return String.valueOf(fieldValue).contains(conditionValue);
                    
                case NOT_CONTAINS:
                    return !String.valueOf(fieldValue).contains(conditionValue);
                    
                case STARTS_WITH:
                    return String.valueOf(fieldValue).startsWith(conditionValue);
                    
                case ENDS_WITH:
                    return String.valueOf(fieldValue).endsWith(conditionValue);
                    
                case IN:
                    return Arrays.asList(conditionValue.split(","))
                        .contains(String.valueOf(fieldValue));
                    
                case NOT_IN:
                    return !Arrays.asList(conditionValue.split(","))
                        .contains(String.valueOf(fieldValue));
                    
                case REGEX_MATCH:
                    return Pattern.compile(conditionValue)
                        .matcher(String.valueOf(fieldValue)).matches();
                    
                case BETWEEN:
                    String[] range = conditionValue.split(",");
                    if (range.length != 2) return false;
                    double value = Double.parseDouble(String.valueOf(fieldValue));
                    double min = Double.parseDouble(range[0]);
                    double max = Double.parseDouble(range[1]);
                    return value >= min && value <= max;
                    
                case IS_NULL:
                    return fieldValue == null;
                    
                case IS_NOT_NULL:
                    return fieldValue != null;
                    
                default:
                    logger.warn("Unknown operator: {}", rule.getOperator());
                    return true;
            }
        } catch (Exception e) {
            logger.error("Error evaluating rule: {}", rule.getRuleName(), e);
            return true; // Default to pass on error
        }
    }
    
    /**
     * Extracts field value from context based on field path
     */
    private Object extractFieldValue(String fieldPath, PolicyContext context) {
        // Support dot notation for nested fields
        String[] parts = fieldPath.split("\\.");
        
        switch (parts[0]) {
            case "device":
                return extractDeviceField(parts, context);
            case "transaction":
                return extractTransactionField(parts, context);
            case "request":
                return extractRequestField(parts, context);
            case "location":
                return extractLocationField(parts, context);
            default:
                return context.getAdditionalData().get(fieldPath);
        }
    }
    
    private Object extractDeviceField(String[] parts, PolicyContext context) {
        if (parts.length < 2) return null;
        
        switch (parts[1]) {
            case "id":
                return context.getDeviceId();
            case "platform":
                return context.getDevicePlatform();
            case "osVersion":
                return context.getDeviceOsVersion();
            case "appVersion":
                return context.getAppVersion();
            default:
                return null;
        }
    }
    
    private Object extractTransactionField(String[] parts, PolicyContext context) {
        if (parts.length < 2) return null;
        
        switch (parts[1]) {
            case "amount":
                return context.getTransactionAmount();
            case "type":
                return context.getTransactionType();
            case "currency":
                return context.getCurrency();
            default:
                return null;
        }
    }
    
    private Object extractRequestField(String[] parts, PolicyContext context) {
        if (parts.length < 2) return null;
        
        switch (parts[1]) {
            case "method":
                return context.getHttpMethod();
            case "uri":
                return context.getRequestUri();
            case "timestamp":
                return context.getTimestamp();
            case "ipAddress":
                return context.getIpAddress();
            case "userAgent":
                return context.getUserAgent();
            default:
                return null;
        }
    }
    
    private Object extractLocationField(String[] parts, PolicyContext context) {
        if (parts.length < 2) return null;
        
        switch (parts[1]) {
            case "country":
                return context.getCountry();
            case "city":
                return context.getCity();
            case "latitude":
                return context.getLatitude();
            case "longitude":
                return context.getLongitude();
            default:
                return null;
        }
    }
    
    private int compareNumeric(Object fieldValue, String conditionValue) {
        try {
            double field = Double.parseDouble(String.valueOf(fieldValue));
            double condition = Double.parseDouble(conditionValue);
            return Double.compare(field, condition);
        } catch (NumberFormatException e) {
            return String.valueOf(fieldValue).compareTo(conditionValue);
        }
    }
    
    private PolicyViolation createViolation(Policy policy, PolicyRule rule, PolicyContext context) {
        PolicyViolation violation = new PolicyViolation();
        violation.setDeviceId(context.getDeviceId());
        violation.setPolicy(policy);
        violation.setViolatedRule(rule);
        violation.setActionTaken(mapEnforcementToAction(policy.getEnforcementLevel()));
        violation.setRequestDetails(context.toString());
        violation.setViolationDetails(String.format(
            "Rule '%s' violated: %s %s %s",
            rule.getRuleName(),
            rule.getConditionField(),
            rule.getOperator(),
            rule.getConditionValue()
        ));
        violation.setIpAddress(context.getIpAddress());
        violation.setUserAgent(context.getUserAgent());
        
        return violation;
    }
    
    private PolicyViolation.ActionTaken mapEnforcementToAction(Policy.EnforcementLevel level) {
        switch (level) {
            case BLOCK:
                return PolicyViolation.ActionTaken.BLOCKED;
            case WARN:
                return PolicyViolation.ActionTaken.WARNED;
            case NOTIFY:
                return PolicyViolation.ActionTaken.NOTIFIED;
            case REQUIRE_MFA:
                return PolicyViolation.ActionTaken.MFA_REQUIRED;
            case MONITOR:
            default:
                return PolicyViolation.ActionTaken.MONITORED;
        }
    }
    
    /**
     * Creates a new policy
     */
    public Policy createPolicy(Policy policy) {
        logger.info("Creating new policy: {} for client: {}", 
            policy.getPolicyName(), policy.getClientId());
        return policyRepository.save(policy);
    }
    
    /**
     * Updates an existing policy
     */
    public Policy updatePolicy(Long policyId, Policy updatedPolicy) {
        Policy existing = policyRepository.findById(policyId)
            .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        
        existing.setPolicyName(updatedPolicy.getPolicyName());
        existing.setPolicyType(updatedPolicy.getPolicyType());
        existing.setEnforcementLevel(updatedPolicy.getEnforcementLevel());
        existing.setDescription(updatedPolicy.getDescription());
        existing.setIsActive(updatedPolicy.getIsActive());
        
        return policyRepository.save(existing);
    }
    
    /**
     * Retrieves policies by client ID
     */
    @Transactional(readOnly = true)
    public List<Policy> getPoliciesByClientId(String clientId) {
        return policyRepository.findByClientId(clientId);
    }
    
    /**
     * Retrieves violation history
     */
    @Transactional(readOnly = true)
    public List<PolicyViolation> getViolationHistory(String deviceId, LocalDateTime from, LocalDateTime to) {
        return violationRepository.findByDeviceIdAndCreatedAtBetween(deviceId, from, to);
    }
}