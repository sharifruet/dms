package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for managing document workflows
 */
@Entity
@Table(name = "workflows")
@EntityListeners(AuditingEntityListener.class)
public class Workflow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private WorkflowType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkflowStatus status = WorkflowStatus.ACTIVE;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(name = "definition", length = 5000)
    private String definition; // JSON string for workflow definition
    
    @Column(name = "trigger_conditions", length = 2000)
    private String triggerConditions; // JSON string for trigger conditions
    
    @Column(name = "is_automatic")
    private Boolean isAutomatic = false;
    
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    @Column(name = "execution_count")
    private Long executionCount = 0L;
    
    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;
    
    @Column(name = "next_execution_at")
    private LocalDateTime nextExecutionAt;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Workflow() {}
    
    public Workflow(String name, String description, WorkflowType type, User createdBy) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public WorkflowType getType() { return type; }
    public void setType(WorkflowType type) { this.type = type; }
    
    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }
    
    public String getTriggerConditions() { return triggerConditions; }
    public void setTriggerConditions(String triggerConditions) { this.triggerConditions = triggerConditions; }
    
    public Boolean getIsAutomatic() { return isAutomatic; }
    public void setIsAutomatic(Boolean isAutomatic) { this.isAutomatic = isAutomatic; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public Long getExecutionCount() { return executionCount; }
    public void setExecutionCount(Long executionCount) { this.executionCount = executionCount; }
    
    public LocalDateTime getLastExecutedAt() { return lastExecutedAt; }
    public void setLastExecutedAt(LocalDateTime lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; }
    
    public LocalDateTime getNextExecutionAt() { return nextExecutionAt; }
    public void setNextExecutionAt(LocalDateTime nextExecutionAt) { this.nextExecutionAt = nextExecutionAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

