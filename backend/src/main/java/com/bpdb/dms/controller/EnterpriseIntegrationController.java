package com.bpdb.dms.controller;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.EnterpriseIntegrationServiceSimple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Enterprise Integration
 */
@RestController
@RequestMapping("/api/integrations")
@CrossOrigin(origins = "*")
public class EnterpriseIntegrationController {
    
    @Autowired
    private EnterpriseIntegrationServiceSimple integrationService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Helper method to get User from Authentication
     */
    private User getUserFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    /**
     * Create integration
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IntegrationConfig> createIntegration(
            @RequestBody IntegrationConfig integrationConfig,
            Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            IntegrationConfig created = integrationService.createIntegrationConfig(
                integrationConfig.getName(),
                integrationConfig.getDescription(),
                integrationConfig.getIntegrationType(),
                integrationConfig.getEndpointUrl(),
                integrationConfig.getAuthType(),
                integrationConfig.getConfigData(),
                user
            );
            
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get integrations
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Page<IntegrationConfig>> getIntegrations(
            @RequestParam(required = false) IntegrationType integrationType,
            @RequestParam(required = false) IntegrationStatus status,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<IntegrationConfig> integrations = integrationService.getIntegrations(
            integrationType, status, searchQuery, pageable);
        
        return ResponseEntity.ok(integrations);
    }
    
    /**
     * Get integration by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<IntegrationConfig> getIntegrationById(@PathVariable Long id) {
        try {
            IntegrationConfig integration = integrationService.getIntegrationById(id);
            return ResponseEntity.ok(integration);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Update integration
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IntegrationConfig> updateIntegration(
            @PathVariable Long id,
            @RequestBody IntegrationConfig integrationConfig) {
        
        try {
            IntegrationConfig updated = integrationService.updateIntegration(id, integrationConfig);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Delete integration
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteIntegration(@PathVariable Long id) {
        try {
            integrationService.deleteIntegration(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Test integration
     */
    @PostMapping("/{id}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testIntegration(@PathVariable Long id) {
        try {
            Map<String, Object> result = integrationService.testIntegration(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Sync integration
     */
    @PostMapping("/{id}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> syncIntegration(@PathVariable Long id) {
        try {
            Map<String, Object> result = integrationService.syncIntegration(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get integration logs
     */
    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<List<Map<String, Object>>> getIntegrationLogs(@PathVariable Long id) {
        try {
            List<Map<String, Object>> logs = integrationService.getIntegrationLogs(id);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get integration statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getIntegrationStatistics() {
        Map<String, Object> stats = integrationService.getIntegrationStatistics();
        return ResponseEntity.ok(stats);
    }
}

