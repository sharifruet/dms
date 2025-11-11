package com.bpdb.dms.controller;

import com.bpdb.dms.entity.MLModel;
import com.bpdb.dms.entity.ModelStatus;
import com.bpdb.dms.entity.ModelType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.MachineLearningService;
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

@RestController
@RequestMapping("/api/ml")
@CrossOrigin(origins = "*")
public class MachineLearningController {

    @Autowired
    private MachineLearningService mlService;

    @Autowired
    private UserRepository userRepository;

    private User getUser(Authentication authentication) {
        UserDetails details = (UserDetails) authentication.getPrincipal();
        return userRepository.findByUsername(details.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/models")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<MLModel> createModel(@RequestBody MLModel model,
                                               Authentication authentication) {
        try {
            User user = getUser(authentication);
            MLModel created = mlService.createMLModel(model, user);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/models")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Page<MLModel>> listModels(
            @RequestParam(required = false) ModelType modelType,
            @RequestParam(required = false) ModelStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MLModel> models = mlService.getMLModels(modelType, status, search, pageable);
        return ResponseEntity.ok(models);
    }

    @GetMapping("/models/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<MLModel> getModel(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(mlService.getMLModelById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/models/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<MLModel> updateModel(@PathVariable Long id,
                                               @RequestBody MLModel update) {
        try {
            return ResponseEntity.ok(mlService.updateMLModel(id, update));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/models/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteModel(@PathVariable Long id) {
        try {
            mlService.deleteMLModel(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/models/{id}/train")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<MLModel> trainModel(@PathVariable Long id,
                                              @RequestParam(defaultValue = "default") String trainingDataPath) {
        try {
            MLModel trained = mlService.trainMLModel(id, trainingDataPath).join();
            return ResponseEntity.ok(trained);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/models/{id}/deploy")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<MLModel> deployModel(@PathVariable Long id,
                                               Authentication authentication) {
        try {
            User user = getUser(authentication);
            MLModel deployed = mlService.deployMLModel(id, user);
            return ResponseEntity.ok(deployed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/models/{id}/predict")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> predict(@PathVariable Long id,
                                                       @RequestBody Map<String, Object> inputData) {
        try {
            Map<String, Object> prediction = mlService.getMLModelPredictions(id, inputData);
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/models/{id}/performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> performance(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(mlService.getMLModelPerformance(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/models/{id}/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<List<Map<String, Object>>> logs(@PathVariable Long id) {
        return ResponseEntity.ok(mlService.getMLModelLogs(id));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> statistics() {
        return ResponseEntity.ok(mlService.getMLModelStatistics());
    }
}

