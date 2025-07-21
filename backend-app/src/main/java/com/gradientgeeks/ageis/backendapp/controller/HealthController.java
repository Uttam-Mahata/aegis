package com.gradientgeeks.ageis.backendapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for monitoring the application status.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    
    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "UCO Bank Backend");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "1.0.0");
        return health;
    }
    
    @GetMapping("/ready")
    public Map<String, Object> ready() {
        Map<String, Object> ready = new HashMap<>();
        ready.put("ready", true);
        ready.put("timestamp", LocalDateTime.now());
        return ready;
    }
}