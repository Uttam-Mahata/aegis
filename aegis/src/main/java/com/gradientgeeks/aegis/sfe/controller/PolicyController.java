package com.gradientgeeks.aegis.sfe.controller;

import com.gradientgeeks.aegis.sfe.dto.PolicyDto;
import com.gradientgeeks.aegis.sfe.dto.PolicyRuleDto;
import com.gradientgeeks.aegis.sfe.entity.Policy;
import com.gradientgeeks.aegis.sfe.entity.PolicyRule;
import com.gradientgeeks.aegis.sfe.entity.PolicyViolation;
import com.gradientgeeks.aegis.sfe.service.PolicyEngineService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/policies")
public class PolicyController {
    
    private static final Logger logger = LoggerFactory.getLogger(PolicyController.class);
    
    private final PolicyEngineService policyEngineService;
    
    @Autowired
    public PolicyController(PolicyEngineService policyEngineService) {
        this.policyEngineService = policyEngineService;
    }
    
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PolicyDto> createPolicy(@Valid @RequestBody PolicyDto policyDto) {
        logger.info("Creating new policy: {} for client: {}", 
            policyDto.getPolicyName(), policyDto.getClientId());
        
        Policy policy = convertToEntity(policyDto);
        Policy savedPolicy = policyEngineService.createPolicy(policy);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(convertToDto(savedPolicy));
    }
    
    @PutMapping("/{policyId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PolicyDto> updatePolicy(
            @PathVariable Long policyId, 
            @Valid @RequestBody PolicyDto policyDto) {
        logger.info("Updating policy: {}", policyId);
        
        Policy policy = convertToEntity(policyDto);
        Policy updatedPolicy = policyEngineService.updatePolicy(policyId, policy);
        
        return ResponseEntity.ok(convertToDto(updatedPolicy));
    }
    
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<PolicyDto>> getPoliciesByClientId(@PathVariable String clientId) {
        logger.info("Fetching policies for client: {}", clientId);
        
        List<Policy> policies = policyEngineService.getPoliciesByClientId(clientId);
        List<PolicyDto> policyDtos = policies.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(policyDtos);
    }
    
    @GetMapping("/violations/{deviceId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<PolicyViolation>> getViolationHistory(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        logger.info("Fetching violation history for device: {} from {} to {}", 
            deviceId, from, to);
        
        List<PolicyViolation> violations = policyEngineService.getViolationHistory(deviceId, from, to);
        return ResponseEntity.ok(violations);
    }
    
    private Policy convertToEntity(PolicyDto dto) {
        Policy policy = new Policy();
        policy.setClientId(dto.getClientId());
        policy.setPolicyName(dto.getPolicyName());
        policy.setPolicyType(dto.getPolicyType());
        policy.setEnforcementLevel(dto.getEnforcementLevel());
        policy.setDescription(dto.getDescription());
        policy.setIsActive(dto.getIsActive());
        
        if (dto.getRules() != null) {
            policy.setRules(dto.getRules().stream()
                .map(ruleDto -> convertRuleToEntity(ruleDto, policy))
                .collect(Collectors.toSet()));
        }
        
        return policy;
    }
    
    private PolicyRule convertRuleToEntity(PolicyRuleDto dto, Policy policy) {
        PolicyRule rule = new PolicyRule();
        rule.setPolicy(policy);
        rule.setRuleName(dto.getRuleName());
        rule.setConditionField(dto.getConditionField());
        rule.setOperator(dto.getOperator());
        rule.setConditionValue(dto.getConditionValue());
        rule.setErrorMessage(dto.getErrorMessage());
        rule.setPriority(dto.getPriority());
        rule.setIsActive(dto.getIsActive());
        return rule;
    }
    
    private PolicyDto convertToDto(Policy policy) {
        PolicyDto dto = new PolicyDto();
        dto.setId(policy.getId());
        dto.setClientId(policy.getClientId());
        dto.setPolicyName(policy.getPolicyName());
        dto.setPolicyType(policy.getPolicyType());
        dto.setEnforcementLevel(policy.getEnforcementLevel());
        dto.setDescription(policy.getDescription());
        dto.setIsActive(policy.getIsActive());
        dto.setCreatedAt(policy.getCreatedAt());
        dto.setUpdatedAt(policy.getUpdatedAt());
        
        if (policy.getRules() != null) {
            dto.setRules(policy.getRules().stream()
                .map(this::convertRuleToDto)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private PolicyRuleDto convertRuleToDto(PolicyRule rule) {
        PolicyRuleDto dto = new PolicyRuleDto();
        dto.setId(rule.getId());
        dto.setRuleName(rule.getRuleName());
        dto.setConditionField(rule.getConditionField());
        dto.setOperator(rule.getOperator());
        dto.setConditionValue(rule.getConditionValue());
        dto.setErrorMessage(rule.getErrorMessage());
        dto.setPriority(rule.getPriority());
        dto.setIsActive(rule.getIsActive());
        dto.setCreatedAt(rule.getCreatedAt());
        dto.setUpdatedAt(rule.getUpdatedAt());
        return dto;
    }
}