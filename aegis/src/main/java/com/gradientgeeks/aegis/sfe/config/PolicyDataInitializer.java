package com.gradientgeeks.aegis.sfe.config;

import com.gradientgeeks.aegis.sfe.entity.Policy;
import com.gradientgeeks.aegis.sfe.entity.PolicyRule;
import com.gradientgeeks.aegis.sfe.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class PolicyDataInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(PolicyDataInitializer.class);
    
    @Autowired
    private PolicyRepository policyRepository;
    
    @Bean
    @Order(2)
    public CommandLineRunner initializePolicies() {
        return args -> {
            if (policyRepository.count() == 0) {
                logger.info("Initializing example bank policies...");
                
                // Example policies for UCO Bank
                createTransactionLimitPolicy();
                createDeviceSecurityPolicy();
                createGeographicRestrictionPolicy();
                createTimeRestrictionPolicy();
                createRiskAssessmentPolicy();
                
                logger.info("Example policies initialized successfully");
            }
        };
    }
    
    private void createTransactionLimitPolicy() {
        Policy policy = new Policy();
        policy.setClientId("uco-bank");
        policy.setPolicyName("Daily Transaction Limit Policy");
        policy.setPolicyType(Policy.PolicyType.TRANSACTION_LIMIT);
        policy.setEnforcementLevel(Policy.EnforcementLevel.BLOCK);
        policy.setDescription("Enforces daily transaction limits for mobile banking");
        policy.setIsActive(true);
        
        Set<PolicyRule> rules = new HashSet<>();
        
        // Rule 1: Single transaction limit
        PolicyRule singleTxnRule = new PolicyRule();
        singleTxnRule.setPolicy(policy);
        singleTxnRule.setRuleName("Single Transaction Limit");
        singleTxnRule.setConditionField("transaction.amount");
        singleTxnRule.setOperator(PolicyRule.RuleOperator.GREATER_THAN);
        singleTxnRule.setConditionValue("500000"); // 5 lakh INR
        singleTxnRule.setErrorMessage("Transaction amount exceeds single transaction limit of ₹5,00,000");
        singleTxnRule.setPriority(100);
        singleTxnRule.setIsActive(true);
        rules.add(singleTxnRule);
        
        // Rule 2: Daily cumulative limit
        PolicyRule dailyLimitRule = new PolicyRule();
        dailyLimitRule.setPolicy(policy);
        dailyLimitRule.setRuleName("Daily Cumulative Limit");
        dailyLimitRule.setConditionField("dailyTotal");
        dailyLimitRule.setOperator(PolicyRule.RuleOperator.GREATER_THAN);
        dailyLimitRule.setConditionValue("1000000"); // 10 lakh INR
        dailyLimitRule.setErrorMessage("Daily transaction limit of ₹10,00,000 exceeded");
        dailyLimitRule.setPriority(90);
        dailyLimitRule.setIsActive(true);
        rules.add(dailyLimitRule);
        
        policy.setRules(rules);
        policyRepository.save(policy);
    }
    
    private void createDeviceSecurityPolicy() {
        Policy policy = new Policy();
        policy.setClientId("uco-bank");
        policy.setPolicyName("Device Security Policy");
        policy.setPolicyType(Policy.PolicyType.DEVICE_SECURITY);
        policy.setEnforcementLevel(Policy.EnforcementLevel.BLOCK);
        policy.setDescription("Ensures device meets minimum security requirements");
        policy.setIsActive(true);
        
        Set<PolicyRule> rules = new HashSet<>();
        
        // Rule 1: No rooted devices
        PolicyRule rootedRule = new PolicyRule();
        rootedRule.setPolicy(policy);
        rootedRule.setRuleName("No Rooted Devices");
        rootedRule.setConditionField("device.isRooted");
        rootedRule.setOperator(PolicyRule.RuleOperator.EQUALS);
        rootedRule.setConditionValue("true");
        rootedRule.setErrorMessage("Banking operations not allowed on rooted/jailbroken devices");
        rootedRule.setPriority(100);
        rootedRule.setIsActive(true);
        rules.add(rootedRule);
        
        // Rule 2: Minimum OS version
        PolicyRule osVersionRule = new PolicyRule();
        osVersionRule.setPolicy(policy);
        osVersionRule.setRuleName("Minimum Android Version");
        osVersionRule.setConditionField("device.osVersion");
        osVersionRule.setOperator(PolicyRule.RuleOperator.LESS_THAN);
        osVersionRule.setConditionValue("10.0");
        osVersionRule.setErrorMessage("Android version 10 or higher required for banking");
        osVersionRule.setPriority(90);
        osVersionRule.setIsActive(true);
        rules.add(osVersionRule);
        
        policy.setRules(rules);
        policyRepository.save(policy);
    }
    
    private void createGeographicRestrictionPolicy() {
        Policy policy = new Policy();
        policy.setClientId("uco-bank");
        policy.setPolicyName("Geographic Restriction Policy");
        policy.setPolicyType(Policy.PolicyType.GEOGRAPHIC_RESTRICTION);
        policy.setEnforcementLevel(Policy.EnforcementLevel.REQUIRE_MFA);
        policy.setDescription("Requires additional authentication for international transactions");
        policy.setIsActive(true);
        
        Set<PolicyRule> rules = new HashSet<>();
        
        // Rule: International transactions require MFA
        PolicyRule intlRule = new PolicyRule();
        intlRule.setPolicy(policy);
        intlRule.setRuleName("International Transaction MFA");
        intlRule.setConditionField("location.country");
        intlRule.setOperator(PolicyRule.RuleOperator.NOT_EQUALS);
        intlRule.setConditionValue("IN");
        intlRule.setErrorMessage("International transactions require additional authentication");
        intlRule.setPriority(100);
        intlRule.setIsActive(true);
        rules.add(intlRule);
        
        // Rule: Block high-risk countries
        PolicyRule blockedCountriesRule = new PolicyRule();
        blockedCountriesRule.setPolicy(policy);
        blockedCountriesRule.setRuleName("Block High-Risk Countries");
        blockedCountriesRule.setConditionField("location.country");
        blockedCountriesRule.setOperator(PolicyRule.RuleOperator.IN);
        blockedCountriesRule.setConditionValue("KP,IR,SY"); // Example blocked countries
        blockedCountriesRule.setErrorMessage("Transactions not allowed from this location");
        blockedCountriesRule.setPriority(110);
        blockedCountriesRule.setIsActive(true);
        rules.add(blockedCountriesRule);
        
        policy.setRules(rules);
        policyRepository.save(policy);
    }
    
    private void createTimeRestrictionPolicy() {
        Policy policy = new Policy();
        policy.setClientId("uco-bank");
        policy.setPolicyName("Time-Based Transaction Policy");
        policy.setPolicyType(Policy.PolicyType.TIME_RESTRICTION);
        policy.setEnforcementLevel(Policy.EnforcementLevel.WARN);
        policy.setDescription("Monitors unusual transaction timing patterns");
        policy.setIsActive(true);
        
        Set<PolicyRule> rules = new HashSet<>();
        
        // Rule: Late night high-value transactions
        PolicyRule nightTxnRule = new PolicyRule();
        nightTxnRule.setPolicy(policy);
        nightTxnRule.setRuleName("Late Night High Value");
        nightTxnRule.setConditionField("hourOfDay");
        nightTxnRule.setOperator(PolicyRule.RuleOperator.BETWEEN);
        nightTxnRule.setConditionValue("0,6"); // 12 AM to 6 AM
        nightTxnRule.setErrorMessage("High-value transaction detected during unusual hours");
        nightTxnRule.setPriority(80);
        nightTxnRule.setIsActive(true);
        rules.add(nightTxnRule);
        
        policy.setRules(rules);
        policyRepository.save(policy);
    }
    
    private void createRiskAssessmentPolicy() {
        Policy policy = new Policy();
        policy.setClientId("uco-bank");
        policy.setPolicyName("Risk Assessment Policy");
        policy.setPolicyType(Policy.PolicyType.RISK_ASSESSMENT);
        policy.setEnforcementLevel(Policy.EnforcementLevel.NOTIFY);
        policy.setDescription("Monitors and flags suspicious transaction patterns");
        policy.setIsActive(true);
        
        Set<PolicyRule> rules = new HashSet<>();
        
        // Rule 1: Multiple failed attempts
        PolicyRule failedAttemptsRule = new PolicyRule();
        failedAttemptsRule.setPolicy(policy);
        failedAttemptsRule.setRuleName("Multiple Failed Attempts");
        failedAttemptsRule.setConditionField("failedAttempts");
        failedAttemptsRule.setOperator(PolicyRule.RuleOperator.GREATER_THAN);
        failedAttemptsRule.setConditionValue("3");
        failedAttemptsRule.setErrorMessage("Multiple failed authentication attempts detected");
        failedAttemptsRule.setPriority(100);
        failedAttemptsRule.setIsActive(true);
        rules.add(failedAttemptsRule);
        
        // Rule 2: Rapid transaction velocity
        PolicyRule velocityRule = new PolicyRule();
        velocityRule.setPolicy(policy);
        velocityRule.setRuleName("Transaction Velocity Check");
        velocityRule.setConditionField("transactionsPerHour");
        velocityRule.setOperator(PolicyRule.RuleOperator.GREATER_THAN);
        velocityRule.setConditionValue("10");
        velocityRule.setErrorMessage("Unusual transaction velocity detected");
        velocityRule.setPriority(90);
        velocityRule.setIsActive(true);
        rules.add(velocityRule);
        
        policy.setRules(rules);
        policyRepository.save(policy);
    }
}