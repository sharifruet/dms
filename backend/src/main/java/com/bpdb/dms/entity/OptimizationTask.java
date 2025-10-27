package com.bpdb.dms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for optimization tasks
 */
@Entity
@Table(name = "optimization_tasks")
public class OptimizationTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "optimization_type", nullable = false)
    private OptimizationType optimizationType;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OptimizationStatus status;
    
    @Column(name = "results", columnDefinition = "TEXT")
    private String results;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    // Constructors
    public OptimizationTask() {
        this.createdAt = LocalDateTime.now();
    }
    
    public OptimizationTask(OptimizationType optimizationType, String description) {
        this();
        this.optimizationType = optimizationType;
        this.description = description;
        this.status = OptimizationStatus.PENDING;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public OptimizationType getOptimizationType() {
        return optimizationType;
    }
    
    public void setOptimizationType(OptimizationType optimizationType) {
        this.optimizationType = optimizationType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getParameters() {
        return parameters;
    }
    
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    
    public OptimizationStatus getStatus() {
        return status;
    }
    
    public void setStatus(OptimizationStatus status) {
        this.status = status;
    }
    
    public String getResults() {
        return results;
    }
    
    public void setResults(String results) {
        this.results = results;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
