package com.bpdb.dms.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Entity representing relationships between documents
 */
@Entity
@Table(name = "document_relationships", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"source_document_id", "target_document_id", "relationship_type"}))
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DocumentRelationship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_document_id", nullable = false)
    @JsonIgnoreProperties({"uploadedBy", "hibernateLazyInitializer", "handler"})
    private Document sourceDocument;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_document_id", nullable = false)
    @JsonIgnoreProperties({"uploadedBy", "hibernateLazyInitializer", "handler"})
    private Document targetDocument;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false)
    private DocumentRelationshipType relationshipType;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({"role", "hibernateLazyInitializer", "handler"})
    private User createdBy;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public DocumentRelationship() {}
    
    public DocumentRelationship(Document sourceDocument, Document targetDocument, 
                               DocumentRelationshipType relationshipType, User createdBy) {
        this.sourceDocument = sourceDocument;
        this.targetDocument = targetDocument;
        this.relationshipType = relationshipType;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Document getSourceDocument() {
        return sourceDocument;
    }
    
    public void setSourceDocument(Document sourceDocument) {
        this.sourceDocument = sourceDocument;
    }
    
    public Document getTargetDocument() {
        return targetDocument;
    }
    
    public void setTargetDocument(Document targetDocument) {
        this.targetDocument = targetDocument;
    }
    
    public DocumentRelationshipType getRelationshipType() {
        return relationshipType;
    }
    
    public void setRelationshipType(DocumentRelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public User getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

