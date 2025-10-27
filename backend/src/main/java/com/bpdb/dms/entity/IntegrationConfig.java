package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for managing enterprise system integrations
 */
@Entity
@Table(name = "integration_configs")
@EntityListeners(AuditingEntityListener.class)
public class IntegrationConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false)
    private IntegrationType integrationType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IntegrationStatus status = IntegrationStatus.INACTIVE;
    
    @Column(name = "endpoint_url", length = 500)
    private String endpointUrl;
    
    @Column(name = "api_key", length = 500)
    private String apiKey;
    
    @Column(name = "api_secret", length = 500)
    private String apiSecret;
    
    @Column(name = "auth_type", length = 50)
    private String authType;
    
    @Column(name = "config_data", length = 5000)
    private String configData; // JSON configuration
    
    @Column(name = "mapping_rules", length = 5000)
    private String mappingRules; // JSON mapping rules
    
    @Column(name = "sync_frequency_minutes")
    private Integer syncFrequencyMinutes = 60;
    
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
    
    @Column(name = "next_sync_at")
    private LocalDateTime nextSyncAt;
    
    @Column(name = "success_count")
    private Long successCount = 0L;
    
    @Column(name = "failure_count")
    private Long failureCount = 0L;
    
    @Column(name = "last_error", length = 1000)
    private String lastError;
    
    @Column(name = "is_enabled")
    private Boolean isEnabled = false;
    
    @Column(name = "retry_count")
    private Integer retryCount = 3;
    
    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 30;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by")
    private User lastModifiedBy;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public IntegrationConfig() {}
    
    public IntegrationConfig(String name, String description, IntegrationType integrationType, User createdBy) {
        this.name = name;
        this.description = description;
        this.integrationType = integrationType;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public IntegrationType getIntegrationType() { return integrationType; }
    public void setIntegrationType(IntegrationType integrationType) { this.integrationType = integrationType; }
    
    public IntegrationStatus getStatus() { return status; }
    public void setStatus(IntegrationStatus status) { this.status = status; }
    
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    
    public String getConfigData() { return configData; }
    public void setConfigData(String configData) { this.configData = configData; }
    
    public String getMappingRules() { return mappingRules; }
    public void setMappingRules(String mappingRules) { this.mappingRules = mappingRules; }
    
    public Integer getSyncFrequencyMinutes() { return syncFrequencyMinutes; }
    public void setSyncFrequencyMinutes(Integer syncFrequencyMinutes) { this.syncFrequencyMinutes = syncFrequencyMinutes; }
    
    public LocalDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(LocalDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    
    public LocalDateTime getNextSyncAt() { return nextSyncAt; }
    public void setNextSyncAt(LocalDateTime nextSyncAt) { this.nextSyncAt = nextSyncAt; }
    
    public Long getSuccessCount() { return successCount; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }
    
    public Long getFailureCount() { return failureCount; }
    public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }
    
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public User getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(User lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
