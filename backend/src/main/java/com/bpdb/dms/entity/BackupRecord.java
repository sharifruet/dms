package com.bpdb.dms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for backup records
 */
@Entity
@Table(name = "backup_records")
public class BackupRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "backup_type", nullable = false)
    private BackupType backupType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BackupStatus status;
    
    @Column(name = "backup_path", nullable = false)
    private String backupPath;
    
    @Column(name = "size_bytes")
    private Long sizeBytes;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    // Constructors
    public BackupRecord() {
        this.createdAt = LocalDateTime.now();
    }
    
    public BackupRecord(BackupType backupType, BackupStatus status, String backupPath) {
        this();
        this.backupType = backupType;
        this.status = status;
        this.backupPath = backupPath;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public BackupType getBackupType() {
        return backupType;
    }
    
    public void setBackupType(BackupType backupType) {
        this.backupType = backupType;
    }
    
    public BackupStatus getStatus() {
        return status;
    }
    
    public void setStatus(BackupStatus status) {
        this.status = status;
    }
    
    public String getBackupPath() {
        return backupPath;
    }
    
    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }
    
    public Long getSizeBytes() {
        return sizeBytes;
    }
    
    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public LocalDateTime getRetentionUntil() {
        return retentionUntil;
    }
    
    public void setRetentionUntil(LocalDateTime retentionUntil) {
        this.retentionUntil = retentionUntil;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
