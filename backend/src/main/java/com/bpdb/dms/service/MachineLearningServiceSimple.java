package com.bpdb.dms.service;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.MLModelRepository;
import com.bpdb.dms.repository.MLPredictionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Simplified Machine Learning Service
 */
@Service
@Transactional
public class MachineLearningServiceSimple {
    
    @Autowired
    private MLModelRepository mlModelRepository;
    
    @Autowired
    private MLPredictionRepository mlPredictionRepository;
    
    /**
     * Create ML model
     */
    public MLModel createMLModel(MLModel model, User createdBy) {
        model.setCreatedBy(createdBy);
        model.setCreatedAt(LocalDateTime.now());
        if (model.getStatus() == null) {
            model.setStatus(ModelStatus.TRAINING);
        }
        return mlModelRepository.save(model);
    }
    
    /**
     * Get ML models
     */
    public Page<MLModel> getMLModels(
            ModelType modelType,
            ModelStatus status,
            String searchQuery,
            Pageable pageable) {
        
        if (modelType != null && status != null) {
            return mlModelRepository.findByModelTypeAndStatus(modelType, status, pageable);
        } else if (modelType != null) {
            return mlModelRepository.findByModelType(modelType, pageable);
        } else if (status != null) {
            return mlModelRepository.findByStatus(status, pageable);
        } else {
            return mlModelRepository.findAll(pageable);
        }
    }
    
    /**
     * Get ML model by ID
     */
    public MLModel getMLModelById(Long id) {
        return mlModelRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("ML Model not found: " + id));
    }
    
    /**
     * Update ML model
     */
    public MLModel updateMLModel(Long id, MLModel updatedModel) {
        MLModel existing = getMLModelById(id);
        
        if (updatedModel.getName() != null) existing.setName(updatedModel.getName());
        if (updatedModel.getDescription() != null) existing.setDescription(updatedModel.getDescription());
        if (updatedModel.getModelType() != null) existing.setModelType(updatedModel.getModelType());
        if (updatedModel.getStatus() != null) existing.setStatus(updatedModel.getStatus());
        if (updatedModel.getVersion() != null) existing.setVersion(updatedModel.getVersion());
        if (updatedModel.getModelPath() != null) existing.setModelPath(updatedModel.getModelPath());
        if (updatedModel.getModelConfig() != null) existing.setModelConfig(updatedModel.getModelConfig());
        
        existing.setUpdatedAt(LocalDateTime.now());
        
        return mlModelRepository.save(existing);
    }
    
    /**
     * Delete ML model
     */
    public void deleteMLModel(Long id) {
        MLModel model = getMLModelById(id);
        mlModelRepository.delete(model);
    }
    
    /**
     * Train ML model
     */
    public MLModel trainMLModel(Long id, String trainingDataPath) {
        MLModel model = getMLModelById(id);
        
        model.setStatus(ModelStatus.TRAINING);
        model.setTrainingDataPath(trainingDataPath);
        model.setTrainingStartedAt(LocalDateTime.now());
        
        // Simulate training completion
        model.setTrainingCompletedAt(LocalDateTime.now());
        model.setStatus(ModelStatus.TRAINED);
        model.setAccuracyScore(0.85 + Math.random() * 0.15); // Mock accuracy 85-100%
        
        return mlModelRepository.save(model);
    }
    
    /**
     * Deploy ML model
     */
    public MLModel deployMLModel(Long id, User deployedBy) {
        MLModel model = getMLModelById(id);
        
        if (model.getStatus() != ModelStatus.TRAINED) {
            throw new RuntimeException("Model must be in TRAINED status to deploy");
        }
        
        model.setStatus(ModelStatus.DEPLOYED);
        model.setIsActive(true);
        model.setUpdatedAt(LocalDateTime.now());
        
        return mlModelRepository.save(model);
    }
    
    /**
     * Make predictions with ML model
     */
    public Map<String, Object> getMLModelPredictions(Long id, Map<String, Object> inputData) {
        MLModel model = getMLModelById(id);
        
        if (!model.getIsActive()) {
            throw new RuntimeException("Model is not active");
        }
        
        // Create prediction record
        MLPrediction prediction = new MLPrediction();
        prediction.setModel(model);
        prediction.setInputData(inputData.toString());
        prediction.setCreatedAt(LocalDateTime.now());
        
        // Mock prediction result
        Map<String, Object> result = new HashMap<>();
        result.put("prediction", "Class_" + (int)(Math.random() * 5));
        result.put("confidence", 0.7 + Math.random() * 0.3);
        result.put("timestamp", LocalDateTime.now());
        
        prediction.setPredictionResult(result.toString());
        prediction.setConfidenceScore(0.7 + Math.random() * 0.3);
        mlPredictionRepository.save(prediction);
        
        // Update model stats
        model.setLastPredictionAt(LocalDateTime.now());
        model.setPredictionCount(model.getPredictionCount() + 1);
        mlModelRepository.save(model);
        
        return result;
    }
    
    /**
     * Get ML model performance metrics
     */
    public Map<String, Object> getMLModelPerformance(Long id) {
        MLModel model = getMLModelById(id);
        
        Map<String, Object> performance = new HashMap<>();
        performance.put("modelId", model.getId());
        performance.put("modelName", model.getName());
        performance.put("accuracy", model.getAccuracyScore());
        performance.put("precision", model.getPrecisionScore());
        performance.put("recall", model.getRecallScore());
        performance.put("f1Score", model.getF1Score());
        performance.put("predictionCount", model.getPredictionCount());
        performance.put("lastPredictionAt", model.getLastPredictionAt());
        performance.put("status", model.getStatus());
        
        return performance;
    }
    
    /**
     * Get ML model prediction logs
     */
    public List<Map<String, Object>> getMLModelLogs(Long id) {
        MLModel model = getMLModelById(id);
        List<MLPrediction> predictions = mlPredictionRepository.findByModelOrderByCreatedAtDesc(model);
        
        List<Map<String, Object>> logs = new ArrayList<>();
        for (MLPrediction prediction : predictions) {
            Map<String, Object> log = new HashMap<>();
            log.put("id", prediction.getId());
            log.put("inputData", prediction.getInputData());
            log.put("result", prediction.getPredictionResult());
            log.put("confidence", prediction.getConfidenceScore());
            log.put("timestamp", prediction.getCreatedAt());
            logs.add(log);
        }
        
        return logs;
    }
    
    /**
     * Get ML insights
     */
    public Map<String, Object> getMLInsights() {
        Map<String, Object> insights = new HashMap<>();
        
        long totalModels = mlModelRepository.count();
        long activeModels = mlModelRepository.countByIsActiveTrue();
        long trainingModels = mlModelRepository.countByStatus(ModelStatus.TRAINING);
        long deployedModels = mlModelRepository.countByStatus(ModelStatus.DEPLOYED);
        
        insights.put("totalModels", totalModels);
        insights.put("activeModels", activeModels);
        insights.put("trainingModels", trainingModels);
        insights.put("deployedModels", deployedModels);
        
        // Get predictions count
        long totalPredictions = mlPredictionRepository.count();
        insights.put("totalPredictions", totalPredictions);
        
        return insights;
    }
    
    /**
     * Get ML recommendations
     */
    public List<Map<String, Object>> getMLRecommendations(String entityType, String context) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        // Mock recommendations
        for (int i = 0; i < 5; i++) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("id", i + 1);
            rec.put("title", "Recommendation " + (i + 1));
            rec.put("confidence", 0.7 + Math.random() * 0.3);
            rec.put("reason", "Based on " + entityType + " analysis");
            recommendations.add(rec);
        }
        
        return recommendations;
    }
}

