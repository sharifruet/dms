package com.bpdb.dms.service;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.WorkflowRepository;
import com.bpdb.dms.repository.WorkflowInstanceRepository;
import com.bpdb.dms.repository.WorkflowStepRepository;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing workflows and workflow automation
 */
@Service
@Transactional
public class WorkflowService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);
    
    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;
    
    @Autowired
    private WorkflowStepRepository workflowStepRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AuditService auditService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Create a new workflow
     */
    public Workflow createWorkflow(String name, String description, WorkflowType type, 
                                 String definition, User createdBy) {
        try {
            Workflow workflow = new Workflow(name, description, type, createdBy);
            workflow.setDefinition(definition);
            
            Workflow savedWorkflow = workflowRepository.save(workflow);
            
            auditService.logActivity(createdBy.getUsername(), "WORKFLOW_CREATED", 
                "Workflow created: " + name, null);
            
            logger.info("Workflow created: {} by user: {}", name, createdBy.getUsername());
            
            return savedWorkflow;
            
        } catch (Exception e) {
            logger.error("Failed to create workflow: {}", e.getMessage());
            throw new RuntimeException("Failed to create workflow", e);
        }
    }
    
    /**
     * Start a workflow instance
     */
    public WorkflowInstance startWorkflow(Long workflowId, Long documentId, User initiatedBy) {
        try {
            Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
            
            Document document = null;
            if (documentId != null) {
                document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            }
            
            WorkflowInstance instance = new WorkflowInstance(workflow, document, initiatedBy);
            instance.setStatus(WorkflowInstanceStatus.PENDING);
            instance.setStartedAt(LocalDateTime.now());
            
            WorkflowInstance savedInstance = workflowInstanceRepository.save(instance);
            
            // Parse workflow definition and create steps
            createWorkflowSteps(savedInstance, workflow.getDefinition());
            
            // Update workflow execution count
            workflow.setExecutionCount(workflow.getExecutionCount() + 1);
            workflow.setLastExecutedAt(LocalDateTime.now());
            workflowRepository.save(workflow);
            
            // Start the first step
            startNextStep(savedInstance);
            
            auditService.logActivity(initiatedBy.getUsername(), "WORKFLOW_STARTED", 
                "Workflow started: " + workflow.getName(), null);
            
            logger.info("Workflow instance started: {} for document: {}", 
                workflow.getName(), documentId);
            
            return savedInstance;
            
        } catch (Exception e) {
            logger.error("Failed to start workflow: {}", e.getMessage());
            throw new RuntimeException("Failed to start workflow", e);
        }
    }
    
    /**
     * Complete a workflow step
     */
    public void completeWorkflowStep(Long stepId, String actionTaken, String comments, User completedBy) {
        try {
            WorkflowStep step = workflowStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Workflow step not found"));
            
            step.setStatus(WorkflowStepStatus.COMPLETED);
            step.setCompletedAt(LocalDateTime.now());
            step.setActionTaken(actionTaken);
            step.setComments(comments);
            
            workflowStepRepository.save(step);
            
            // Check if workflow instance is complete
            WorkflowInstance instance = step.getWorkflowInstance();
            if (isWorkflowInstanceComplete(instance)) {
                completeWorkflowInstance(instance);
            } else {
                // Start next step
                startNextStep(instance);
            }
            
            auditService.logActivity(completedBy.getUsername(), "WORKFLOW_STEP_COMPLETED", 
                "Workflow step completed: " + step.getStepName(), null);
            
            logger.info("Workflow step completed: {} by user: {}", 
                step.getStepName(), completedBy.getUsername());
            
        } catch (Exception e) {
            logger.error("Failed to complete workflow step: {}", e.getMessage());
            throw new RuntimeException("Failed to complete workflow step", e);
        }
    }
    
    /**
     * Reject a workflow step
     */
    public void rejectWorkflowStep(Long stepId, String reason, User rejectedBy) {
        try {
            WorkflowStep step = workflowStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Workflow step not found"));
            
            step.setStatus(WorkflowStepStatus.REJECTED);
            step.setCompletedAt(LocalDateTime.now());
            step.setActionTaken("REJECTED");
            step.setComments(reason);
            
            workflowStepRepository.save(step);
            
            // Reject the entire workflow instance
            WorkflowInstance instance = step.getWorkflowInstance();
            instance.setStatus(WorkflowInstanceStatus.REJECTED);
            instance.setCompletedAt(LocalDateTime.now());
            workflowInstanceRepository.save(instance);
            
            auditService.logActivity(rejectedBy.getUsername(), "WORKFLOW_STEP_REJECTED", 
                "Workflow step rejected: " + step.getStepName(), null);
            
            logger.info("Workflow step rejected: {} by user: {}", 
                step.getStepName(), rejectedBy.getUsername());
            
        } catch (Exception e) {
            logger.error("Failed to reject workflow step: {}", e.getMessage());
            throw new RuntimeException("Failed to reject workflow step", e);
        }
    }
    
    /**
     * Get workflow instances for a user
     */
    public Page<WorkflowInstance> getWorkflowInstancesForUser(User user, Pageable pageable) {
        return workflowInstanceRepository.findByInitiatedBy(user, pageable);
    }
    
    /**
     * Get workflow steps assigned to a user
     */
    public Page<WorkflowStep> getWorkflowStepsForUser(User user, Pageable pageable) {
        return workflowStepRepository.findByAssignedTo(user, pageable);
    }
    
    /**
     * Get overdue workflow instances
     */
    public List<WorkflowInstance> getOverdueWorkflowInstances() {
        return workflowInstanceRepository.findOverdueInstances(LocalDateTime.now());
    }
    
    /**
     * Get workflow statistics
     */
    public Map<String, Object> getWorkflowStatistics() {
        try {
            Map<String, Object> stats = Map.of(
                "totalWorkflows", workflowRepository.count(),
                "activeWorkflows", workflowRepository.countByStatus(WorkflowStatus.ACTIVE),
                "totalInstances", workflowInstanceRepository.count(),
                "pendingInstances", workflowInstanceRepository.countByStatus(WorkflowInstanceStatus.PENDING),
                "inProgressInstances", workflowInstanceRepository.countByStatus(WorkflowInstanceStatus.IN_PROGRESS),
                "completedInstances", workflowInstanceRepository.countByStatus(WorkflowInstanceStatus.COMPLETED),
                "rejectedInstances", workflowInstanceRepository.countByStatus(WorkflowInstanceStatus.REJECTED)
            );
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Failed to get workflow statistics: {}", e.getMessage());
            throw new RuntimeException("Failed to get workflow statistics", e);
        }
    }
    
    /**
     * Get active tender workflow instances (for document upload)
     */
    public List<WorkflowInstance> getActiveTenderWorkflowInstances() {
        return workflowInstanceRepository.findActiveTenderWorkflowInstances();
    }
    
    /**
     * Create workflow steps from definition
     */
    private void createWorkflowSteps(WorkflowInstance instance, String definition) {
        try {
            // Parse workflow definition (simplified implementation)
            // In production, use a proper workflow engine like Camunda or Activiti
            
            // For now, create a simple approval workflow
            WorkflowStep step1 = new WorkflowStep(instance, 1, "Initial Review", 
                WorkflowStepType.REVIEW, instance.getInitiatedBy());
            step1.setStatus(WorkflowStepStatus.PENDING);
            step1.setDueDate(LocalDateTime.now().plusDays(1));
            
            WorkflowStep step2 = new WorkflowStep(instance, 2, "Final Approval", 
                WorkflowStepType.APPROVAL, instance.getInitiatedBy());
            step2.setStatus(WorkflowStepStatus.PENDING);
            step2.setDueDate(LocalDateTime.now().plusDays(2));
            
            workflowStepRepository.save(step1);
            workflowStepRepository.save(step2);
            
            instance.setTotalSteps(2);
            workflowInstanceRepository.save(instance);
            
        } catch (Exception e) {
            logger.error("Failed to create workflow steps: {}", e.getMessage());
            throw new RuntimeException("Failed to create workflow steps", e);
        }
    }
    
    /**
     * Start the next step in workflow
     */
    private void startNextStep(WorkflowInstance instance) {
        try {
            List<WorkflowStep> nextSteps = workflowStepRepository.findNextSteps(instance);
            
            if (!nextSteps.isEmpty()) {
                WorkflowStep nextStep = nextSteps.get(0);
                nextStep.setStatus(WorkflowStepStatus.IN_PROGRESS);
                nextStep.setStartedAt(LocalDateTime.now());
                workflowStepRepository.save(nextStep);
                
                instance.setCurrentStep(nextStep.getStepNumber());
                instance.setStatus(WorkflowInstanceStatus.IN_PROGRESS);
                workflowInstanceRepository.save(instance);
                
                // Send notification to assigned user
                if (nextStep.getAssignedTo() != null) {
                    notificationService.createNotification(
                        nextStep.getAssignedTo(),
                        "Workflow Step Assigned",
                        "You have been assigned a workflow step: " + nextStep.getStepName(),
                        NotificationType.SYSTEM_ALERT,
                        NotificationPriority.MEDIUM
                    );
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to start next workflow step: {}", e.getMessage());
        }
    }
    
    /**
     * Check if workflow instance is complete
     */
    private boolean isWorkflowInstanceComplete(WorkflowInstance instance) {
        List<WorkflowStep> steps = workflowStepRepository.findByWorkflowInstanceOrderByStepNumber(instance);
        
        for (WorkflowStep step : steps) {
            if (step.getStatus() == WorkflowStepStatus.PENDING || 
                step.getStatus() == WorkflowStepStatus.IN_PROGRESS) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Complete workflow instance
     */
    private void completeWorkflowInstance(WorkflowInstance instance) {
        instance.setStatus(WorkflowInstanceStatus.COMPLETED);
        instance.setCompletedAt(LocalDateTime.now());
        workflowInstanceRepository.save(instance);
        
        // Send completion notification
        notificationService.createNotification(
            instance.getInitiatedBy(),
            "Workflow Completed",
            "Workflow has been completed: " + instance.getWorkflow().getName(),
            NotificationType.SYSTEM_ALERT,
            NotificationPriority.LOW
        );
        
        logger.info("Workflow instance completed: {}", instance.getId());
    }
    
    /**
     * Process automatic workflows
     */
    @Async
    public void processAutomaticWorkflows() {
        try {
            List<Workflow> automaticWorkflows = workflowRepository.findByIsAutomaticTrueAndStatus(WorkflowStatus.ACTIVE);
            
            for (Workflow workflow : automaticWorkflows) {
                if (workflow.getNextExecutionAt() != null && 
                    workflow.getNextExecutionAt().isBefore(LocalDateTime.now())) {
                    
                    // Execute automatic workflow
                    executeAutomaticWorkflow(workflow);
                    
                    // Update next execution time
                    workflow.setNextExecutionAt(LocalDateTime.now().plusHours(1));
                    workflowRepository.save(workflow);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to process automatic workflows: {}", e.getMessage());
        }
    }
    
    /**
     * Execute automatic workflow
     */
    private void executeAutomaticWorkflow(Workflow workflow) {
        try {
            logger.info("Executing automatic workflow: {}", workflow.getName());
            
            // Implementation depends on workflow type
            // For now, just log the execution
            
            auditService.logActivity("SYSTEM", "AUTOMATIC_WORKFLOW_EXECUTED", 
                "Automatic workflow executed: " + workflow.getName(), null);
            
        } catch (Exception e) {
            logger.error("Failed to execute automatic workflow: {}", e.getMessage());
        }
    }
}
