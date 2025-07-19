package com.gradientgeeks.ageis.sfe.repository;

import com.gradientgeeks.ageis.sfe.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    
    Optional<Device> findByDeviceId(String deviceId);
    
    @Query("SELECT d FROM Device d WHERE d.deviceId = :deviceId AND d.isActive = true")
    Optional<Device> findActiveByDeviceId(@Param("deviceId") String deviceId);
    
    boolean existsByDeviceId(String deviceId);
    
    @Modifying
    @Query("UPDATE Device d SET d.lastSeen = :lastSeen WHERE d.deviceId = :deviceId")
    void updateLastSeen(@Param("deviceId") String deviceId, @Param("lastSeen") LocalDateTime lastSeen);
    
    @Modifying
    @Query("UPDATE Device d SET d.isActive = false WHERE d.deviceId = :deviceId")
    void deactivateDevice(@Param("deviceId") String deviceId);
    
    @Query("SELECT COUNT(d) FROM Device d WHERE d.clientId = :clientId AND d.isActive = true")
    long countActiveDevicesByClientId(@Param("clientId") String clientId);
}