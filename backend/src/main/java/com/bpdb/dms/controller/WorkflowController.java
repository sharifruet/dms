package com.bpdb.dms.controller;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.WorkflowService;
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
import java.util.Optional;

/**
 * Controller for workflow management
 */
@RestController
@RequestMapping("/api/workflows")
@CrossOrigin(origins = "*")
public class WorkflowController {
    
    @Autowired
    private WorkflowService workflowService;
    
    @Autowired
    private com.bpdb.dms.service.TenderWorkflowService tenderWorkflowService;
    
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
     * Create a new workflow
     */
    @PostMapping
    public ResponseEntity<Workflow> createWorkflow(@RequestBody CreateWorkflowRequest request,
                                                  Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            Workflow workflow = workflowService.createWorkflow(
                request.getName(),
                request.getDescription(),
                request.getType(),
                request.getDefinition(),
                user
            );
            
            return ResponseEntity.ok(workflow);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Start a workflow instance
     */
    @PostMapping("/{workflowId}/start")
    public ResponseEntity<WorkflowInstance> startWorkflow(@PathVariable Long workflowId,
                                                        @RequestBody StartWorkflowRequest request,
                                                        Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            WorkflowInstance instance = workflowService.startWorkflow(
                workflowId,
                request.getDocumentId(),
                user
            );
            
            return ResponseEntity.ok(instance);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Complete a workflow step
     */
    @PostMapping("/steps/{stepId}/complete")
    public ResponseEntity<Void> completeWorkflowStep(@PathVariable Long stepId,
                                                    @RequestBody CompleteStepRequest request,
                                                    Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            workflowService.completeWorkflowStep(
                stepId,
                request.getActionTaken(),
                request.getComments(),
                user
            );
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Reject a workflow step
     */
    @PostMapping("/steps/{stepId}/reject")
    public ResponseEntity<Void> rejectWorkflowStep(@PathVariable Long stepId,
                                                 @RequestBody RejectStepRequest request,
                                                 Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            workflowService.rejectWorkflowStep(
                stepId,
                request.getReason(),
                user
            );
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get workflow instances for user
     */
    @GetMapping("/instances")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Page<WorkflowInstance>> getWorkflowInstances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<WorkflowInstance> instances = workflowService.getWorkflowInstancesForUser(user, pageable);
            
            return ResponseEntity.ok(instances);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get workflow steps assigned to user
     */
    @GetMapping("/steps")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Page<WorkflowStep>> getWorkflowSteps(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<WorkflowStep> steps = workflowService.getWorkflowStepsForUser(user, pageable);
            
            return ResponseEntity.ok(steps);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get overdue workflow instances
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<WorkflowInstance>> getOverdueWorkflowInstances() {
        try {
            List<WorkflowInstance> overdueInstances = workflowService.getOverdueWorkflowInstances();
            
            return ResponseEntity.ok(overdueInstances);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get workflow statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getWorkflowStatistics() {
        try {
            Map<String, Object> statistics = workflowService.getWorkflowStatistics();
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get active tender workflow instances (for document upload)
     */
    @GetMapping("/instances/tender/active")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<WorkflowInstance>> getActiveTenderWorkflowInstances() {
        try {
            List<WorkflowInstance> instances = workflowService.getActiveTenderWorkflowInstances();
            return ResponseEntity.ok(instances);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Link a workflow to an APP entry (budget entry)
     */
    @PostMapping("/{workflowId}/link-app-entry")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW_MANAGE')")
    public ResponseEntity<Map<String, Object>> linkWorkflowToAppEntry(
            @PathVariable Long workflowId,
            @RequestBody LinkAppEntryRequest request) {
        try {
            Workflow workflow = tenderWorkflowService.linkWorkflowToAppEntry(workflowId, request.getAppEntryId());
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("workflow", workflow);
            response.put("message", "Workflow linked to APP entry successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to link workflow to APP entry: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get APP entry for a workflow
     */
    @GetMapping("/{workflowId}/app-entry")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Map<String, Object>> getAppEntryForWorkflow(@PathVariable Long workflowId) {
        try {
            Optional<AppHeader> appEntry = tenderWorkflowService.getAppEntryForWorkflow(workflowId);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("appEntry", appEntry.orElse(null));
            response.put("hasAppEntry", appEntry.isPresent());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get APP entry for workflow: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Unlink workflow from APP entry
     */
    @DeleteMapping("/{workflowId}/link-app-entry")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW_MANAGE')")
    public ResponseEntity<Map<String, Object>> unlinkWorkflowFromAppEntry(@PathVariable Long workflowId) {
        try {
            Workflow workflow = tenderWorkflowService.unlinkWorkflowFromAppEntry(workflowId);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("workflow", workflow);
            response.put("message", "Workflow unlinked from APP entry successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to unlink workflow from APP entry: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Request DTOs
    public static class CreateWorkflowRequest {
        private String name;
        private String description;
        private WorkflowType type;
        private String definition;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public WorkflowType getType() { return type; }
        public void setType(WorkflowType type) { this.type = type; }
        
        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }
    }
    
    public static class StartWorkflowRequest {
        private Long documentId;
        
        // Getters and Setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
    }
    
    public static class CompleteStepRequest {
        private String actionTaken;
        private String comments;
        
        // Getters and Setters
        public String getActionTaken() { return actionTaken; }
        public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
        
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }
    
    public static class RejectStepRequest {
        private String reason;
        
        // Getters and Setters
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    public static class LinkAppEntryRequest {
        private Long appEntryId;
        
        // Getters and Setters
        public Long getAppEntryId() { return appEntryId; }
        public void setAppEntryId(Long appEntryId) { this.appEntryId = appEntryId; }
    }
}
