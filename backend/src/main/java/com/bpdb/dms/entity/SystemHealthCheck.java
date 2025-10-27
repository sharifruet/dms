package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for system health checks
 */
@Entity
@Table(name = "system_health_checks")
@EntityListeners(AuditingEntityListener.class)
public class SystemHealthCheck {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "check_name", nullable = false)
    private String checkName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false)
    private HealthCheckType checkType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private HealthStatus status;
    
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "check_data", length = 2000)
    private String checkData; // JSON data
    
    @Column(name = "threshold_value")
    private Double thresholdValue;
    
    @Column(name = "actual_value")
    private Double actualValue;
    
    @Column(name = "severity", length = 20)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(name = "component", length = 100)
    private String component;
    
    @Column(name = "service", length = 100)
    private String service;
    
    @Column(name = "environment", length = 50)
    private String environment;
    
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
    
    @Column(name = "next_check_at")
    private LocalDateTime nextCheckAt;
    
    @Column(name = "check_interval_seconds")
    private Integer checkIntervalSeconds = 300; // 5 minutes default
    
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public SystemHealthCheck() {}
    
    public SystemHealthCheck(String checkName, HealthCheckType checkType, HealthStatus status, LocalDateTime executedAt) {
        this.checkName = checkName;
        this.checkType = checkType;
        this.status = status;
        this.executedAt = executedAt;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCheckName() { return checkName; }
    public void setCheckName(String checkName) { this.checkName = checkName; }
    
    public HealthCheckType getCheckType() { return checkType; }
    public void setCheckType(HealthCheckType checkType) { this.checkType = checkType; }
    
    public HealthStatus getStatus() { return status; }
    public void setStatus(HealthStatus status) { this.status = status; }
    
    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getCheckData() { return checkData; }
    public void setCheckData(String checkData) { this.checkData = checkData; }
    
    public Double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Double thresholdValue) { this.thresholdValue = thresholdValue; }
    
    public Double getActualValue() { return actualValue; }
    public void setActualValue(Double actualValue) { this.actualValue = actualValue; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
    
    public LocalDateTime getNextCheckAt() { return nextCheckAt; }
    public void setNextCheckAt(LocalDateTime nextCheckAt) { this.nextCheckAt = nextCheckAt; }
    
    public Integer getCheckIntervalSeconds() { return checkIntervalSeconds; }
    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) { this.checkIntervalSeconds = checkIntervalSeconds; }
    
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
