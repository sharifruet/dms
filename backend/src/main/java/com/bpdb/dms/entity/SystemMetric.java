package com.bpdb.dms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for system metrics
 */
@Entity
@Table(name = "system_metrics")
public class SystemMetric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "metric_name", nullable = false)
    private String metricName;
    
    @Column(name = "metric_value")
    private Double metricValue;
    
    @Column(name = "metric_unit")
    private String metricUnit;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "threshold_warning")
    private Double thresholdWarning;
    
    @Column(name = "threshold_critical")
    private Double thresholdCritical;
    
    @Column(name = "collected_at")
    private LocalDateTime collectedAt;
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMetricName() {
        return metricName;
    }
    
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }
    
    public Double getMetricValue() {
        return metricValue;
    }
    
    public void setMetricValue(Double metricValue) {
        this.metricValue = metricValue;
    }
    
    public String getMetricUnit() {
        return metricUnit;
    }
    
    public void setMetricUnit(String metricUnit) {
        this.metricUnit = metricUnit;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Double getThresholdWarning() {
        return thresholdWarning;
    }
    
    public void setThresholdWarning(Double thresholdWarning) {
        this.thresholdWarning = thresholdWarning;
    }
    
    public Double getThresholdCritical() {
        return thresholdCritical;
    }
    
    public void setThresholdCritical(Double thresholdCritical) {
        this.thresholdCritical = thresholdCritical;
    }
    
    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }
    
    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
}

