package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for managing webhooks
 */
@Entity
@Table(name = "webhooks")
@EntityListeners(AuditingEntityListener.class)
public class Webhook {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "url", nullable = false, length = 500)
    private String url;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private WebhookEventType eventType;
    
    @Column(name = "secret_key", length = 100)
    private String secretKey;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WebhookStatus status = WebhookStatus.ACTIVE;
    
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;
    
    @Column(name = "retry_count")
    private Integer retryCount = 3;
    
    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 30;
    
    @Column(name = "headers", length = 2000)
    private String headers; // JSON string for custom headers
    
    @Column(name = "payload_template", length = 5000)
    private String payloadTemplate; // JSON template for payload
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;
    
    @Column(name = "success_count")
    private Long successCount = 0L;
    
    @Column(name = "failure_count")
    private Long failureCount = 0L;
    
    @Column(name = "last_error", length = 1000)
    private String lastError;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Webhook() {}
    
    public Webhook(String name, String description, String url, WebhookEventType eventType, User createdBy) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.eventType = eventType;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public WebhookEventType getEventType() { return eventType; }
    public void setEventType(WebhookEventType eventType) { this.eventType = eventType; }
    
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    
    public WebhookStatus getStatus() { return status; }
    public void setStatus(WebhookStatus status) { this.status = status; }
    
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    
    public String getHeaders() { return headers; }
    public void setHeaders(String headers) { this.headers = headers; }
    
    public String getPayloadTemplate() { return payloadTemplate; }
    public void setPayloadTemplate(String payloadTemplate) { this.payloadTemplate = payloadTemplate; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getLastTriggeredAt() { return lastTriggeredAt; }
    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }
    
    public Long getSuccessCount() { return successCount; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }
    
    public Long getFailureCount() { return failureCount; }
    public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }
    
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

