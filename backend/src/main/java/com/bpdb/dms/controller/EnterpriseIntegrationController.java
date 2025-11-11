package com.bpdb.dms.controller;

import com.bpdb.dms.entity.IntegrationConfig;
import com.bpdb.dms.entity.IntegrationStatus;
import com.bpdb.dms.entity.IntegrationType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.EnterpriseIntegrationService;
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

@RestController
@RequestMapping("/api/integrations")
@CrossOrigin(origins = "*")
public class EnterpriseIntegrationController {

    @Autowired
    private EnterpriseIntegrationService integrationService;

    @Autowired
    private UserRepository userRepository;

    private User getUser(Authentication authentication) {
        UserDetails details = (UserDetails) authentication.getPrincipal();
        return userRepository.findByUsername(details.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IntegrationConfig> createIntegration(@RequestBody IntegrationConfig integrationConfig,
                                                               Authentication authentication) {
        try {
            User user = getUser(authentication);
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Page<IntegrationConfig>> getIntegrations(
            @RequestParam(required = false) IntegrationType integrationType,
            @RequestParam(required = false) IntegrationStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<IntegrationConfig> configs = integrationService.getIntegrations(integrationType, status, search, pageable);
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<IntegrationConfig> getIntegrationById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(integrationService.getIntegrationById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IntegrationConfig> updateIntegration(@PathVariable Long id,
                                                               @RequestBody IntegrationConfig integrationConfig) {
        try {
            return ResponseEntity.ok(integrationService.updateIntegration(id, integrationConfig));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

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

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<Map<String, Object>> testIntegration(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(integrationService.testIntegration(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/sync")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<Map<String, Object>> syncIntegration(@PathVariable Long id) {
        try {
            Map<String, Object> result = integrationService.syncIntegration(id).join();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<List<Map<String, Object>>> getIntegrationLogs(@PathVariable Long id) {
        List<Map<String, Object>> logs = integrationService.getIntegrationLogs(id);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getIntegrationStatistics() {
        return ResponseEntity.ok(integrationService.getIntegrationStatistics());
    }
}

