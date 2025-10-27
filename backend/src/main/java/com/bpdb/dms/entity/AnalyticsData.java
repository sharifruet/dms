package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for storing analytics data
 */
@Entity
@Table(name = "analytics_data")
@EntityListeners(AuditingEntityListener.class)
public class AnalyticsData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "analytics_type", nullable = false)
    private AnalyticsType analyticsType;
    
    @Column(name = "metric_name", nullable = false)
    private String metricName;
    
    @Column(name = "metric_value")
    private Double metricValue;
    
    @Column(name = "metric_data", length = 5000)
    private String metricData; // JSON data
    
    @Column(name = "dimensions", length = 2000)
    private String dimensions; // JSON dimensions
    
    @Column(name = "tags", length = 1000)
    private String tags;
    
    @Column(name = "source_system", length = 100)
    private String sourceSystem;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "document_id")
    private Long documentId;
    
    @Column(name = "department")
    private String department;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "aggregation_level")
    private String aggregationLevel; // minute, hour, day, week, month
    
    @Column(name = "is_predicted")
    private Boolean isPredicted = false;
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @Column(name = "model_version")
    private String modelVersion;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public AnalyticsData() {}
    
    public AnalyticsData(AnalyticsType analyticsType, String metricName, Double metricValue, LocalDateTime timestamp) {
        this.analyticsType = analyticsType;
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public AnalyticsType getAnalyticsType() { return analyticsType; }
    public void setAnalyticsType(AnalyticsType analyticsType) { this.analyticsType = analyticsType; }
    
    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }
    
    public Double getMetricValue() { return metricValue; }
    public void setMetricValue(Double metricValue) { this.metricValue = metricValue; }
    
    public String getMetricData() { return metricData; }
    public void setMetricData(String metricData) { this.metricData = metricData; }
    
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getAggregationLevel() { return aggregationLevel; }
    public void setAggregationLevel(String aggregationLevel) { this.aggregationLevel = aggregationLevel; }
    
    public Boolean getIsPredicted() { return isPredicted; }
    public void setIsPredicted(Boolean isPredicted) { this.isPredicted = isPredicted; }
    
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
