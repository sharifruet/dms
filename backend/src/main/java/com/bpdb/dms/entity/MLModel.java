package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for managing machine learning models
 */
@Entity
@Table(name = "ml_models")
@EntityListeners(AuditingEntityListener.class)
public class MLModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false)
    private ModelType modelType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ModelStatus status = ModelStatus.TRAINING;
    
    @Column(name = "version", nullable = false)
    private String version;
    
    @Column(name = "model_path", length = 500)
    private String modelPath;
    
    @Column(name = "training_data_path", length = 500)
    private String trainingDataPath;
    
    @Column(name = "model_config", length = 5000)
    private String modelConfig; // JSON configuration
    
    @Column(name = "performance_metrics", length = 2000)
    private String performanceMetrics; // JSON metrics
    
    @Column(name = "accuracy_score")
    private Double accuracyScore;
    
    @Column(name = "precision_score")
    private Double precisionScore;
    
    @Column(name = "recall_score")
    private Double recallScore;
    
    @Column(name = "f1_score")
    private Double f1Score;
    
    @Column(name = "training_started_at")
    private LocalDateTime trainingStartedAt;
    
    @Column(name = "training_completed_at")
    private LocalDateTime trainingCompletedAt;
    
    @Column(name = "last_prediction_at")
    private LocalDateTime lastPredictionAt;
    
    @Column(name = "prediction_count")
    private Long predictionCount = 0L;
    
    @Column(name = "is_active")
    private Boolean isActive = false;
    
    @Column(name = "auto_retrain")
    private Boolean autoRetrain = false;
    
    @Column(name = "retrain_frequency_days")
    private Integer retrainFrequencyDays = 30;
    
    @Column(name = "next_retrain_at")
    private LocalDateTime nextRetrainAt;
    
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
    public MLModel() {}
    
    public MLModel(String name, String description, ModelType modelType, String version, User createdBy) {
        this.name = name;
        this.description = description;
        this.modelType = modelType;
        this.version = version;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public ModelType getModelType() { return modelType; }
    public void setModelType(ModelType modelType) { this.modelType = modelType; }
    
    public ModelStatus getStatus() { return status; }
    public void setStatus(ModelStatus status) { this.status = status; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }
    
    public String getTrainingDataPath() { return trainingDataPath; }
    public void setTrainingDataPath(String trainingDataPath) { this.trainingDataPath = trainingDataPath; }
    
    public String getModelConfig() { return modelConfig; }
    public void setModelConfig(String modelConfig) { this.modelConfig = modelConfig; }
    
    public String getPerformanceMetrics() { return performanceMetrics; }
    public void setPerformanceMetrics(String performanceMetrics) { this.performanceMetrics = performanceMetrics; }
    
    public Double getAccuracyScore() { return accuracyScore; }
    public void setAccuracyScore(Double accuracyScore) { this.accuracyScore = accuracyScore; }
    
    public Double getPrecisionScore() { return precisionScore; }
    public void setPrecisionScore(Double precisionScore) { this.precisionScore = precisionScore; }
    
    public Double getRecallScore() { return recallScore; }
    public void setRecallScore(Double recallScore) { this.recallScore = recallScore; }
    
    public Double getF1Score() { return f1Score; }
    public void setF1Score(Double f1Score) { this.f1Score = f1Score; }
    
    public LocalDateTime getTrainingStartedAt() { return trainingStartedAt; }
    public void setTrainingStartedAt(LocalDateTime trainingStartedAt) { this.trainingStartedAt = trainingStartedAt; }
    
    public LocalDateTime getTrainingCompletedAt() { return trainingCompletedAt; }
    public void setTrainingCompletedAt(LocalDateTime trainingCompletedAt) { this.trainingCompletedAt = trainingCompletedAt; }
    
    public LocalDateTime getLastPredictionAt() { return lastPredictionAt; }
    public void setLastPredictionAt(LocalDateTime lastPredictionAt) { this.lastPredictionAt = lastPredictionAt; }
    
    public Long getPredictionCount() { return predictionCount; }
    public void setPredictionCount(Long predictionCount) { this.predictionCount = predictionCount; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Boolean getAutoRetrain() { return autoRetrain; }
    public void setAutoRetrain(Boolean autoRetrain) { this.autoRetrain = autoRetrain; }
    
    public Integer getRetrainFrequencyDays() { return retrainFrequencyDays; }
    public void setRetrainFrequencyDays(Integer retrainFrequencyDays) { this.retrainFrequencyDays = retrainFrequencyDays; }
    
    public LocalDateTime getNextRetrainAt() { return nextRetrainAt; }
    public void setNextRetrainAt(LocalDateTime nextRetrainAt) { this.nextRetrainAt = nextRetrainAt; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public User getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(User lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
