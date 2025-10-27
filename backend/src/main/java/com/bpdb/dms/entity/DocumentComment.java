package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for managing document comments and annotations
 */
@Entity
@Table(name = "document_comments")
@EntityListeners(AuditingEntityListener.class)
public class DocumentComment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(name = "content", length = 2000, nullable = false)
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", nullable = false)
    private CommentType commentType = CommentType.GENERAL;
    
    @Column(name = "page_number")
    private Integer pageNumber;
    
    @Column(name = "x_coordinate")
    private Double xCoordinate;
    
    @Column(name = "y_coordinate")
    private Double yCoordinate;
    
    @Column(name = "width")
    private Double width;
    
    @Column(name = "height")
    private Double height;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private DocumentComment parentComment;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CommentStatus status = CommentStatus.ACTIVE;
    
    @Column(name = "is_resolved")
    private Boolean isResolved = false;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;
    
    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;
    
    @Column(name = "metadata", length = 2000)
    private String metadata; // JSON string for comment-specific metadata
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public DocumentComment() {}
    
    public DocumentComment(Document document, User createdBy, String content, CommentType commentType) {
        this.document = document;
        this.createdBy = createdBy;
        this.content = content;
        this.commentType = commentType;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public CommentType getCommentType() { return commentType; }
    public void setCommentType(CommentType commentType) { this.commentType = commentType; }
    
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    
    public Double getXCoordinate() { return xCoordinate; }
    public void setXCoordinate(Double xCoordinate) { this.xCoordinate = xCoordinate; }
    
    public Double getYCoordinate() { return yCoordinate; }
    public void setYCoordinate(Double yCoordinate) { this.yCoordinate = yCoordinate; }
    
    public Double getWidth() { return width; }
    public void setWidth(Double width) { this.width = width; }
    
    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }
    
    public DocumentComment getParentComment() { return parentComment; }
    public void setParentComment(DocumentComment parentComment) { this.parentComment = parentComment; }
    
    public CommentStatus getStatus() { return status; }
    public void setStatus(CommentStatus status) { this.status = status; }
    
    public Boolean getIsResolved() { return isResolved; }
    public void setIsResolved(Boolean isResolved) { this.isResolved = isResolved; }
    
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    
    public User getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(User resolvedBy) { this.resolvedBy = resolvedBy; }
    
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

