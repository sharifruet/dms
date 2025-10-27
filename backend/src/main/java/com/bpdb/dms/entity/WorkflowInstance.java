package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for managing workflow instances
 */
@Entity
@Table(name = "workflow_instances")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowInstance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkflowInstanceStatus status = WorkflowInstanceStatus.PENDING;
    
    @Column(name = "current_step")
    private Integer currentStep = 0;
    
    @Column(name = "total_steps")
    private Integer totalSteps = 0;
    
    @Column(name = "context_data", length = 5000)
    private String contextData; // JSON string for workflow context
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "priority")
    private Integer priority = 0;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public WorkflowInstance() {}
    
    public WorkflowInstance(Workflow workflow, Document document, User initiatedBy) {
        this.workflow = workflow;
        this.document = document;
        this.initiatedBy = initiatedBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Workflow getWorkflow() { return workflow; }
    public void setWorkflow(Workflow workflow) { this.workflow = workflow; }
    
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    
    public User getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(User initiatedBy) { this.initiatedBy = initiatedBy; }
    
    public WorkflowInstanceStatus getStatus() { return status; }
    public void setStatus(WorkflowInstanceStatus status) { this.status = status; }
    
    public Integer getCurrentStep() { return currentStep; }
    public void setCurrentStep(Integer currentStep) { this.currentStep = currentStep; }
    
    public Integer getTotalSteps() { return totalSteps; }
    public void setTotalSteps(Integer totalSteps) { this.totalSteps = totalSteps; }
    
    public String getContextData() { return contextData; }
    public void setContextData(String contextData) { this.contextData = contextData; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

