package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for managing document versions
 */
@Entity
@Table(name = "document_versions")
@EntityListeners(AuditingEntityListener.class)
public class DocumentVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @Column(name = "version_number", nullable = false)
    private String versionNumber;
    
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_hash")
    private String fileHash;
    
    @Column(name = "mime_type")
    private String mimeType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(name = "change_description", length = 1000)
    private String changeDescription;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "version_type", nullable = false)
    private VersionType versionType = VersionType.MINOR;
    
    @Column(name = "is_current")
    private Boolean isCurrent = false;
    
    @Column(name = "is_archived")
    private Boolean isArchived = false;
    
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
    
    @Column(name = "metadata", length = 5000)
    private String metadata; // JSON string for version-specific metadata
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public DocumentVersion() {}
    
    public DocumentVersion(Document document, String versionNumber, String filePath, 
                          User createdBy, String changeDescription) {
        this.document = document;
        this.versionNumber = versionNumber;
        this.filePath = filePath;
        this.createdBy = createdBy;
        this.changeDescription = changeDescription;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    
    public String getVersionNumber() { return versionNumber; }
    public void setVersionNumber(String versionNumber) { this.versionNumber = versionNumber; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public String getChangeDescription() { return changeDescription; }
    public void setChangeDescription(String changeDescription) { this.changeDescription = changeDescription; }
    
    public VersionType getVersionType() { return versionType; }
    public void setVersionType(VersionType versionType) { this.versionType = versionType; }
    
    public Boolean getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }
    
    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }
    
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

