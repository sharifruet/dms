package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for managing workflow steps
 */
@Entity
@Table(name = "workflow_steps")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowStep {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstance workflowInstance;
    
    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;
    
    @Column(name = "step_name", nullable = false)
    private String stepName;
    
    @Column(name = "step_description", length = 1000)
    private String stepDescription;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false)
    private WorkflowStepType stepType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkflowStepStatus status = WorkflowStepStatus.PENDING;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "action_taken", length = 1000)
    private String actionTaken;
    
    @Column(name = "comments", length = 2000)
    private String comments;
    
    @Column(name = "step_data", length = 5000)
    private String stepData; // JSON string for step-specific data
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public WorkflowStep() {}
    
    public WorkflowStep(WorkflowInstance workflowInstance, Integer stepNumber, String stepName, 
                       WorkflowStepType stepType, User assignedTo) {
        this.workflowInstance = workflowInstance;
        this.stepNumber = stepNumber;
        this.stepName = stepName;
        this.stepType = stepType;
        this.assignedTo = assignedTo;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public WorkflowInstance getWorkflowInstance() { return workflowInstance; }
    public void setWorkflowInstance(WorkflowInstance workflowInstance) { this.workflowInstance = workflowInstance; }
    
    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }
    
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    
    public String getStepDescription() { return stepDescription; }
    public void setStepDescription(String stepDescription) { this.stepDescription = stepDescription; }
    
    public WorkflowStepType getStepType() { return stepType; }
    public void setStepType(WorkflowStepType stepType) { this.stepType = stepType; }
    
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    
    public WorkflowStepStatus getStatus() { return status; }
    public void setStatus(WorkflowStepStatus status) { this.status = status; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    
    public String getStepData() { return stepData; }
    public void setStepData(String stepData) { this.stepData = stepData; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

