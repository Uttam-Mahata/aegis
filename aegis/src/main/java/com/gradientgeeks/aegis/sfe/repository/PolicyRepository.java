package com.gradientgeeks.aegis.sfe.repository;

import com.gradientgeeks.aegis.sfe.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    
    @Query("SELECT p FROM Policy p WHERE p.clientId = :clientId AND p.isActive = true")
    List<Policy> findActiveByClientId(@Param("clientId") String clientId);
    
    List<Policy> findByClientId(String clientId);
    
    List<Policy> findByPolicyType(Policy.PolicyType policyType);
    
    @Query("SELECT p FROM Policy p WHERE p.clientId = :clientId AND p.policyType = :policyType AND p.isActive = true")
    List<Policy> findActiveByClientIdAndType(@Param("clientId") String clientId, 
                                           @Param("policyType") Policy.PolicyType policyType);
    
    Optional<Policy> findByClientIdAndPolicyName(String clientId, String policyName);
    
    @Query("SELECT COUNT(p) FROM Policy p WHERE p.clientId = :clientId AND p.isActive = true")
    long countActiveByClientId(@Param("clientId") String clientId);
}