package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for managing system reports
 */
@Entity
@Table(name = "reports")
@EntityListeners(AuditingEntityListener.class)
public class Report {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ReportType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private ReportFormat format;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.PENDING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(name = "parameters", length = 2000)
    private String parameters; // JSON string for report parameters
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "is_scheduled")
    private Boolean isScheduled = false;
    
    @Column(name = "schedule_cron")
    private String scheduleCron;
    
    @Column(name = "last_generated_at")
    private LocalDateTime lastGeneratedAt;
    
    @Column(name = "next_generation_at")
    private LocalDateTime nextGenerationAt;
    
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    @Column(name = "access_count")
    private Long accessCount = 0L;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Report() {}
    
    public Report(String name, String description, ReportType type, ReportFormat format, User createdBy) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.format = format;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public ReportType getType() { return type; }
    public void setType(ReportType type) { this.type = type; }
    
    public ReportFormat getFormat() { return format; }
    public void setFormat(ReportFormat format) { this.format = format; }
    
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public Boolean getIsScheduled() { return isScheduled; }
    public void setIsScheduled(Boolean isScheduled) { this.isScheduled = isScheduled; }
    
    public String getScheduleCron() { return scheduleCron; }
    public void setScheduleCron(String scheduleCron) { this.scheduleCron = scheduleCron; }
    
    public LocalDateTime getLastGeneratedAt() { return lastGeneratedAt; }
    public void setLastGeneratedAt(LocalDateTime lastGeneratedAt) { this.lastGeneratedAt = lastGeneratedAt; }
    
    public LocalDateTime getNextGenerationAt() { return nextGenerationAt; }
    public void setNextGenerationAt(LocalDateTime nextGenerationAt) { this.nextGenerationAt = nextGenerationAt; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public Long getAccessCount() { return accessCount; }
    public void setAccessCount(Long accessCount) { this.accessCount = accessCount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
