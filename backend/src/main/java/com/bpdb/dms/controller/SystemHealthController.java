package com.bpdb.dms.controller;

import com.bpdb.dms.service.SystemHealthMonitoringServiceSimple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for System Health Monitoring
 */
@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class SystemHealthController {
    
    @Autowired
    private SystemHealthMonitoringServiceSimple healthService;
    
    /**
     * Get system health overview
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getSystemHealthOverview() {
        Map<String, Object> health = healthService.getSystemHealthOverview();
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get system metrics
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<List<Map<String, Object>>> getSystemMetrics(
            @RequestParam String metricType,
            @RequestParam(defaultValue = "24") int hours) {
        
        List<Map<String, Object>> metrics = healthService.getSystemMetrics(metricType, hours);
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get database health
     */
    @GetMapping("/database")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<Map<String, Object>> getDatabaseHealth() {
        Map<String, Object> dbHealth = healthService.getDatabaseHealth();
        return ResponseEntity.ok(dbHealth);
    }
    
    /**
     * Get application health
     */
    @GetMapping("/application")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getApplicationHealth() {
        Map<String, Object> appHealth = healthService.getApplicationHealth();
        return ResponseEntity.ok(appHealth);
    }
    
    /**
     * Get infrastructure health
     */
    @GetMapping("/infrastructure")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getInfrastructureHealth() {
        Map<String, Object> infraHealth = healthService.getInfrastructureHealth();
        return ResponseEntity.ok(infraHealth);
    }
    
    /**
     * Get performance metrics
     */
    @GetMapping("/performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> performance = healthService.getPerformanceMetrics();
        return ResponseEntity.ok(performance);
    }
    
    /**
     * Get health alerts
     */
    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<List<Map<String, Object>>> getHealthAlerts() {
        List<Map<String, Object>> alerts = healthService.getHealthAlerts();
        return ResponseEntity.ok(alerts);
    }
    
    /**
     * Acknowledge health alert
     */
    @PostMapping("/alerts/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<Void> acknowledgeHealthAlert(@PathVariable Long id) {
        try {
            healthService.acknowledgeHealthAlert(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

