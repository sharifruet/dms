package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for ML model predictions
 */
@Entity
@Table(name = "ml_predictions")
@EntityListeners(AuditingEntityListener.class)
public class MLPrediction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "model_id")
    private MLModel model;
    
    @Column(name = "input_data", length = 5000)
    private String inputData;
    
    @Column(name = "prediction_result", length = 5000)
    private String predictionResult;
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public MLModel getModel() {
        return model;
    }
    
    public void setModel(MLModel model) {
        this.model = model;
    }
    
    public String getInputData() {
        return inputData;
    }
    
    public void setInputData(String inputData) {
        this.inputData = inputData;
    }
    
    public String getPredictionResult() {
        return predictionResult;
    }
    
    public void setPredictionResult(String predictionResult) {
        this.predictionResult = predictionResult;
    }
    
    public Double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

