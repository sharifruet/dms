package com.bpdb.dms.service;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.IntegrationConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class EnterpriseIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseIntegrationService.class);

    @Autowired
    private IntegrationConfigRepository integrationConfigRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

    private final RestTemplate restTemplate = new RestTemplate();

    public IntegrationConfig createIntegrationConfig(String name,
                                                     String description,
                                                     IntegrationType integrationType,
                                                     String endpointUrl,
                                                     String authType,
                                                     String configData,
                                                     User createdBy) {
        try {
            IntegrationConfig config = new IntegrationConfig(name, description, integrationType, createdBy);
            config.setEndpointUrl(endpointUrl);
            config.setAuthType(authType);
            config.setConfigData(configData);
            config.setStatus(IntegrationStatus.CONFIGURING);

            IntegrationConfig saved = integrationConfigRepository.save(config);
            auditService.logActivity(createdBy.getUsername(), "INTEGRATION_CONFIG_CREATED",
                "Integration configuration created: " + name, null);
            return saved;
        } catch (Exception e) {
            logger.error("Failed to create integration configuration: {}", e.getMessage());
            throw new RuntimeException("Failed to create integration configuration", e);
        }
    }

    public IntegrationConfig updateIntegration(Long configId, IntegrationConfig update) {
        try {
            IntegrationConfig existing = integrationConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Integration configuration not found"));

            existing.setName(update.getName());
            existing.setDescription(update.getDescription());
            existing.setEndpointUrl(update.getEndpointUrl());
            existing.setAuthType(update.getAuthType());
            existing.setApiKey(update.getApiKey());
            existing.setApiSecret(update.getApiSecret());
            existing.setConfigData(update.getConfigData());
            existing.setMappingRules(update.getMappingRules());
            existing.setSyncFrequencyMinutes(update.getSyncFrequencyMinutes());
            existing.setUpdatedAt(LocalDateTime.now());

            return integrationConfigRepository.save(existing);
        } catch (Exception e) {
            logger.error("Failed to update integration configuration: {}", e.getMessage());
            throw new RuntimeException("Failed to update integration configuration", e);
        }
    }

    public Page<IntegrationConfig> getIntegrations(IntegrationType integrationType,
                                                   IntegrationStatus status,
                                                   String searchQuery,
                                                   Pageable pageable) {
        Page<IntegrationConfig> page = integrationConfigRepository.findByMultipleCriteria(
            integrationType, status, null, null, pageable);
        if (searchQuery == null || searchQuery.isBlank()) {
            return page;
        }
        List<IntegrationConfig> filtered = page.stream()
            .filter(cfg -> cfg.getName() != null && cfg.getName().toLowerCase().contains(searchQuery.toLowerCase()))
            .collect(Collectors.toList());
        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    public IntegrationConfig getIntegrationById(Long id) {
        return integrationConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Integration configuration not found"));
    }

    public void deleteIntegration(Long id) {
        if (!integrationConfigRepository.existsById(id)) {
            throw new RuntimeException("Integration configuration not found");
        }
        integrationConfigRepository.deleteById(id);
    }

    public Map<String, Object> testIntegration(Long id) {
        IntegrationConfig config = getIntegrationById(id);
        boolean success = testIntegrationConnection(config);
        return Map.of(
            "integrationId", id,
            "success", success,
            "timestamp", LocalDateTime.now()
        );
    }

    @Async
    public CompletableFuture<Map<String, Object>> syncIntegration(Long id) {
        try {
            IntegrationConfig config = getIntegrationById(id);
            if (!Boolean.TRUE.equals(config.getIsEnabled())) {
                return CompletableFuture.completedFuture(Map.of(
                    "integrationId", id,
                    "status", "SKIPPED",
                    "message", "Integration is disabled"
                ));
            }

            boolean success = performDataSync(config);
            config.setLastSyncAt(LocalDateTime.now());
            config.setNextSyncAt(LocalDateTime.now().plusMinutes(
                config.getSyncFrequencyMinutes() != null ? config.getSyncFrequencyMinutes() : 60));
            integrationConfigRepository.save(config);

            if (!success) {
                notificationService.createNotification(
                    config.getCreatedBy(),
                    "Integration Sync Failed",
                    "Sync failed for integration: " + config.getName(),
                    NotificationType.SYSTEM_ALERT,
                    NotificationPriority.HIGH
                );
            }

            return CompletableFuture.completedFuture(Map.of(
                "integrationId", id,
                "status", success ? "SUCCESS" : "FAILED",
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public Map<String, Object> getIntegrationStatistics() {
        return Map.of(
            "totalIntegrations", integrationConfigRepository.count(),
            "activeIntegrations", integrationConfigRepository.countByStatus(IntegrationStatus.ACTIVE),
            "enabledIntegrations", integrationConfigRepository.countByIsEnabledTrue(),
            "inactiveIntegrations", integrationConfigRepository.countByStatus(IntegrationStatus.INACTIVE)
        );
    }

    private boolean testIntegrationConnection(IntegrationConfig config) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            if ("API_KEY".equalsIgnoreCase(config.getAuthType())) {
                headers.set("X-API-Key", config.getApiKey());
            } else if ("BEARER_TOKEN".equalsIgnoreCase(config.getAuthType())) {
                headers.set("Authorization", "Bearer " + config.getApiKey());
            }

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                config.getEndpointUrl() + "/health",
                HttpMethod.GET,
                request,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Integration connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean performDataSync(IntegrationConfig config) {
        try {
            switch (config.getIntegrationType()) {
                case ERP_SYSTEM:
                    return syncERPData(config);
                case CRM_SYSTEM:
                    return syncCRMData(config);
                case HR_SYSTEM:
                    return syncHRData(config);
                case EMAIL_SYSTEM:
                    return syncEmailData(config);
                default:
                    return syncGenericData(config);
            }
        } catch (Exception e) {
            logger.error("Data sync failed for integration: {}", config.getName());
            return false;
        }
    }

    private boolean syncERPData(IntegrationConfig config) {
        logger.info("Syncing ERP data for integration: {}", config.getName());
        return true;
    }

    private boolean syncCRMData(IntegrationConfig config) {
        logger.info("Syncing CRM data for integration: {}", config.getName());
        return true;
    }

    private boolean syncHRData(IntegrationConfig config) {
        logger.info("Syncing HR data for integration: {}", config.getName());
        return true;
    }

    private boolean syncEmailData(IntegrationConfig config) {
        logger.info("Syncing Email data for integration: {}", config.getName());
        return true;
    }

    private boolean syncGenericData(IntegrationConfig config) {
        logger.info("Syncing generic data for integration: {}", config.getName());
        return true;
    }

    public Page<IntegrationConfig> getIntegrationsByType(IntegrationType integrationType, Pageable pageable) {
        return integrationConfigRepository.findByIntegrationType(integrationType, pageable);
    }

    public Page<IntegrationConfig> getIntegrationsForUser(User user, Pageable pageable) {
        return integrationConfigRepository.findByCreatedBy(user, pageable);
    }

    public List<Map<String, Object>> getIntegrationLogs(Long id) {
        return List.of(Map.of(
            "integrationId", id,
            "timestamp", LocalDateTime.now(),
            "message", "No logs available"
        ));
    }

}

