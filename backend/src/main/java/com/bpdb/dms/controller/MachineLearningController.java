package com.bpdb.dms.controller;

import com.bpdb.dms.entity.MLModel;
import com.bpdb.dms.entity.ModelStatus;
import com.bpdb.dms.entity.ModelType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.MachineLearningServiceSimple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Machine Learning
 */
@RestController
@RequestMapping("/api/ml")
@CrossOrigin(origins = "*")
public class MachineLearningController {
    
    @Autowired
    private MachineLearningServiceSimple mlService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Helper method to get User from Authentication
     */
    private User getUserFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    /**
     * Create ML model
     */
    @PostMapping("/models")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<MLModel> createMLModel(
            @RequestBody MLModel model,
            Authentication authentication) {
        
        try {
            User user = getUserFromAuthentication(authentication);
            MLModel created = mlService.createMLModel(model, user);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get ML models
     */
    @GetMapping("/models")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Page<MLModel>> getMLModels(
            @RequestParam(required = false) ModelType modelType,
            @RequestParam(required = false) ModelStatus status,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MLModel> models = mlService.getMLModels(modelType, status, searchQuery, pageable);
        return ResponseEntity.ok(models);
    }
    
    /**
     * Get ML model by ID
     */
    @GetMapping("/models/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<MLModel> getMLModelById(@PathVariable Long id) {
        try {
            MLModel model = mlService.getMLModelById(id);
            return ResponseEntity.ok(model);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Update ML model
     */
    @PutMapping("/models/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<MLModel> updateMLModel(
            @PathVariable Long id,
            @RequestBody MLModel model) {
        
        try {
            MLModel updated = mlService.updateMLModel(id, model);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Delete ML model
     */
    @DeleteMapping("/models/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMLModel(@PathVariable Long id) {
        try {
            mlService.deleteMLModel(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Train ML model
     */
    @PostMapping("/models/{id}/train")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<MLModel> trainMLModel(
            @PathVariable Long id,
            @RequestParam String trainingDataPath) {
        
        try {
            MLModel trained = mlService.trainMLModel(id, trainingDataPath);
            return ResponseEntity.ok(trained);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Deploy ML model
     */
    @PostMapping("/models/{id}/deploy")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<MLModel> deployMLModel(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            User user = getUserFromAuthentication(authentication);
            MLModel deployed = mlService.deployMLModel(id, user);
            return ResponseEntity.ok(deployed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Make predictions
     */
    @PostMapping("/models/{id}/predict")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> predict(
            @PathVariable Long id,
            @RequestBody Map<String, Object> inputData) {
        
        try {
            Map<String, Object> result = mlService.getMLModelPredictions(id, inputData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get model performance metrics
     */
    @GetMapping("/models/{id}/performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getMLModelPerformance(@PathVariable Long id) {
        try {
            Map<String, Object> performance = mlService.getMLModelPerformance(id);
            return ResponseEntity.ok(performance);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get model prediction logs
     */
    @GetMapping("/models/{id}/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<List<Map<String, Object>>> getMLModelLogs(@PathVariable Long id) {
        try {
            List<Map<String, Object>> logs = mlService.getMLModelLogs(id);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get ML insights
     */
    @GetMapping("/insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getMLInsights() {
        Map<String, Object> insights = mlService.getMLInsights();
        return ResponseEntity.ok(insights);
    }
    
    /**
     * Get ML recommendations
     */
    @GetMapping("/recommendations")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<List<Map<String, Object>>> getMLRecommendations(
            @RequestParam String entityType,
            @RequestParam String context) {
        
        List<Map<String, Object>> recommendations = mlService.getMLRecommendations(entityType, context);
        return ResponseEntity.ok(recommendations);
    }
}

