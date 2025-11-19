package com.bpdb.dms.controller;

import com.bpdb.dms.entity.HealthCheckType;
import com.bpdb.dms.entity.HealthStatus;
import com.bpdb.dms.entity.SystemHealthCheck;
import com.bpdb.dms.service.SystemHealthMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for System Health Monitoring
 */
@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class SystemHealthController {

    @Autowired
    private SystemHealthMonitoringService healthMonitoringService;

    @PostMapping("/checks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemHealthCheck> createHealthCheck(@RequestBody SystemHealthCheck healthCheck) {
        try {
            SystemHealthCheck created = healthMonitoringService.createHealthCheck(
                healthCheck.getCheckName(),
                healthCheck.getCheckType(),
                healthCheck.getComponent(),
                healthCheck.getService(),
                healthCheck.getEnvironment(),
                healthCheck.getThresholdValue(),
                healthCheck.getCheckIntervalSeconds()
            );

            return ResponseEntity.ok(created);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/checks")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<Page<SystemHealthCheck>> getAllHealthChecks(
            @RequestParam(required = false) HealthCheckType checkType,
            @RequestParam(required = false) HealthStatus status,
            @RequestParam(required = false) String component,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String severity,
            Pageable pageable) {

        Page<SystemHealthCheck> checks = healthMonitoringService.getHealthChecks(
            checkType, status, component, service, severity, pageable);

        return ResponseEntity.ok(checks);
    }

    @GetMapping("/checks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<SystemHealthCheck> getHealthCheckById(@PathVariable Long id) {
        try {
            SystemHealthCheck check = healthMonitoringService.getHealthCheckById(id);
            return ResponseEntity.ok(check);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/checks/{id}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<Map<String, Object>> executeHealthCheck(@PathVariable Long id) {
        try {
            CompletableFuture<SystemHealthCheck> future = healthMonitoringService.executeHealthCheck(id);
            SystemHealthCheck result = future.get();

            Map<String, Object> response = Map.of(
                "status", result.getStatus(),
                "responseTime", result.getResponseTimeMs(),
                "actualValue", result.getActualValue(),
                "errorMessage", result.getErrorMessage(),
                "severity", result.getSeverity()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getSystemHealthOverview() {
        Map<String, Object> overview = healthMonitoringService.getSystemHealthOverview();
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getHealthCheckStatistics() {
        Map<String, Object> statistics = healthMonitoringService.getHealthCheckStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/checks/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<List<SystemHealthCheck>> getHealthCheckHistory(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(7);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        List<SystemHealthCheck> history = healthMonitoringService.getHealthCheckHistory(id, startDate, endDate);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/components/{component}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getComponentHealth(@PathVariable String component) {
        try {
            Map<String, Object> componentHealth = healthMonitoringService.getComponentHealth(component);
            return ResponseEntity.ok(componentHealth);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/services/{service}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getServiceHealth(@PathVariable String service) {
        try {
            Map<String, Object> serviceHealth = healthMonitoringService.getServiceHealth(service);
            return ResponseEntity.ok(serviceHealth);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<List<SystemHealthCheck>> getCriticalHealthAlerts() {
        try {
            List<SystemHealthCheck> alerts = healthMonitoringService.getCriticalHealthAlerts();
            return ResponseEntity.ok(alerts);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/alerts/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<Void> acknowledgeHealthAlert(@PathVariable Long id) {
        try {
            healthMonitoringService.acknowledgeHealthAlert(id);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
