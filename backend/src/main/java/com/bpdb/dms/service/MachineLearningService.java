package com.bpdb.dms.service;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.MLModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class MachineLearningService {

    @Autowired
    private MLModelRepository mlModelRepository;

    @Autowired
    private AuditService auditService;

    public MLModel createMLModel(MLModel model, User createdBy) {
        model.setCreatedAt(LocalDateTime.now());
        model.setStatus(ModelStatus.TRAINING);
        model.setCreatedBy(createdBy);
        MLModel saved = mlModelRepository.save(model);
        auditService.logActivity(createdBy.getUsername(), "ML_MODEL_CREATED",
            "ML model created: " + model.getName(), null);
        return saved;
    }

    public Page<MLModel> getMLModels(ModelType modelType,
                                     ModelStatus status,
                                     String searchQuery,
                                     Pageable pageable) {
        Page<MLModel> page = mlModelRepository.findByMultipleCriteria(modelType, status, null, null, pageable);
        if (searchQuery == null || searchQuery.isBlank()) {
            return page;
        }
        List<MLModel> filtered = page.stream()
            .filter(model -> model.getName() != null && model.getName().toLowerCase().contains(searchQuery.toLowerCase()))
            .collect(Collectors.toList());
        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    public MLModel getMLModelById(Long id) {
        return mlModelRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("ML model not found"));
    }

    public MLModel updateMLModel(Long id, MLModel update) {
        MLModel existing = getMLModelById(id);
        existing.setName(update.getName());
        existing.setDescription(update.getDescription());
        existing.setModelType(update.getModelType());
        existing.setVersion(update.getVersion());
        existing.setModelConfig(update.getModelConfig());
        return mlModelRepository.save(existing);
    }

    public void deleteMLModel(Long id) {
        if (!mlModelRepository.existsById(id)) {
            throw new RuntimeException("ML model not found");
        }
        mlModelRepository.deleteById(id);
    }

    @Async
    public CompletableFuture<MLModel> trainMLModel(Long modelId, String trainingDataPath) {
        try {
            MLModel model = getMLModelById(modelId);
            model.setStatus(ModelStatus.TRAINING);
            model.setTrainingStartedAt(LocalDateTime.now());
            model.setTrainingDataPath(trainingDataPath);
            mlModelRepository.save(model);

            boolean success = simulateTraining(model, trainingDataPath);
            if (success) {
                model.setStatus(ModelStatus.TRAINED);
                model.setTrainingCompletedAt(LocalDateTime.now());
                model.setAccuracyScore(0.85);
                mlModelRepository.save(model);
                return CompletableFuture.completedFuture(model);
            } else {
                model.setStatus(ModelStatus.FAILED);
                mlModelRepository.save(model);
                return CompletableFuture.failedFuture(new RuntimeException("Training failed"));
            }
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public MLModel deployMLModel(Long modelId, User deployedBy) {
        MLModel model = getMLModelById(modelId);
        if (model.getStatus() != ModelStatus.TRAINED) {
            throw new RuntimeException("Model must be trained before deployment");
        }
        model.setStatus(ModelStatus.DEPLOYED);
        model.setIsActive(true);
        model.setLastModifiedBy(deployedBy);
        return mlModelRepository.save(model);
    }

    public Map<String, Object> getMLModelPerformance(Long modelId) {
        MLModel model = getMLModelById(modelId);
        return Map.of(
            "accuracy", model.getAccuracyScore(),
            "precision", model.getPrecisionScore(),
            "recall", model.getRecallScore(),
            "f1Score", model.getF1Score()
        );
    }

    public List<Map<String, Object>> getMLModelLogs(Long modelId) {
        return List.of(Map.of(
            "modelId", modelId,
            "timestamp", LocalDateTime.now(),
            "message", "No logs available"
        ));
    }

    public Page<MLModel> getBestPerformingModels(Pageable pageable) {
        List<MLModel> models = mlModelRepository.findBestPerformingModels(pageable);
        return new PageImpl<>(models, pageable, models.size());
    }

    public Map<String, Object> getMLModelStatistics() {
        return Map.of(
            "totalModels", mlModelRepository.count(),
            "activeModels", mlModelRepository.countByIsActiveTrue(),
            "trainedModels", mlModelRepository.countByStatus(ModelStatus.TRAINED),
            "deployedModels", mlModelRepository.countByStatus(ModelStatus.DEPLOYED)
        );
    }

    public Map<String, Object> getMLModelPredictions(Long modelId, Map<String, Object> inputData) {
        MLModel model = getMLModelById(modelId);
        if (!Boolean.TRUE.equals(model.getIsActive())) {
            throw new RuntimeException("Model is not active");
        }
        Map<String, Object> prediction = Map.of(
            "predictedClass", "Contract",
            "confidence", 0.92
        );
        model.setLastPredictionAt(LocalDateTime.now());
        model.setPredictionCount(model.getPredictionCount() + 1);
        mlModelRepository.save(model);
        return prediction;
    }

    private boolean simulateTraining(MLModel model, String trainingDataPath) {
        try {
            Thread.sleep(1000);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}

