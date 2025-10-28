package com.bpdb.dms.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity for storing system analytics and metrics
 */
@Entity
@Table(name = "analytics")
@EntityListeners(AuditingEntityListener.class)
public class Analytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private MetricType metricType;
    
    @Column(name = "metric_name", nullable = false)
    private String metricName;
    
    @Column(name = "metric_value", nullable = false)
    private Double metricValue;
    
    @Column(name = "metric_unit")
    private String metricUnit;
    
    @Column(name = "dimension_key")
    private String dimensionKey; // e.g., department, user, document_type
    
    @Column(name = "dimension_value")
    private String dimensionValue; // e.g., "Finance", "admin", "CONTRACT"
    
    @Column(name = "period_start")
    private LocalDateTime periodStart;
    
    @Column(name = "period_end")
    private LocalDateTime periodEnd;
    
    @Column(name = "metadata", length = 2000)
    private String metadata; // JSON string for additional data
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public Analytics() {}
    
    public Analytics(MetricType metricType, String metricName, Double metricValue, 
                    String dimensionKey, String dimensionValue) {
        this.metricType = metricType;
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.dimensionKey = dimensionKey;
        this.dimensionValue = dimensionValue;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public MetricType getMetricType() { return metricType; }
    public void setMetricType(MetricType metricType) { this.metricType = metricType; }
    
    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }
    
    public Double getMetricValue() { return metricValue; }
    public void setMetricValue(Double metricValue) { this.metricValue = metricValue; }
    
    public String getMetricUnit() { return metricUnit; }
    public void setMetricUnit(String metricUnit) { this.metricUnit = metricUnit; }
    
    public String getDimensionKey() { return dimensionKey; }
    public void setDimensionKey(String dimensionKey) { this.dimensionKey = dimensionKey; }
    
    public String getDimensionValue() { return dimensionValue; }
    public void setDimensionValue(String dimensionValue) { this.dimensionValue = dimensionValue; }
    
    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }
    
    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
