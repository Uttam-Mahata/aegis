package com.gradientgeeks.aegis.sfe.repository;

import com.gradientgeeks.aegis.sfe.entity.PolicyViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PolicyViolationRepository extends JpaRepository<PolicyViolation, Long> {
    
    List<PolicyViolation> findByDeviceId(String deviceId);
    
    List<PolicyViolation> findByDeviceIdAndCreatedAtBetween(String deviceId, 
                                                           LocalDateTime startDate, 
                                                           LocalDateTime endDate);
    
    @Query("SELECT v FROM PolicyViolation v WHERE v.policy.clientId = :clientId " +
           "AND v.createdAt BETWEEN :startDate AND :endDate")
    List<PolicyViolation> findByClientIdAndDateRange(@Param("clientId") String clientId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(v) FROM PolicyViolation v WHERE v.deviceId = :deviceId " +
           "AND v.createdAt >= :since")
    long countRecentViolations(@Param("deviceId") String deviceId, 
                              @Param("since") LocalDateTime since);
    
    @Query("SELECT v FROM PolicyViolation v WHERE v.policy.id = :policyId " +
           "ORDER BY v.createdAt DESC")
    List<PolicyViolation> findByPolicyId(@Param("policyId") Long policyId);
}