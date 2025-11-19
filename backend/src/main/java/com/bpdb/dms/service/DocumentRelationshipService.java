package com.bpdb.dms.service;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentRelationship;
import com.bpdb.dms.entity.DocumentRelationshipType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRelationshipRepository;
import com.bpdb.dms.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing document relationships
 */
@Service
public class DocumentRelationshipService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentRelationshipService.class);
    
    @Autowired
    private DocumentRelationshipRepository relationshipRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Create a relationship between two documents
     */
    @Transactional
    public DocumentRelationship createRelationship(Long sourceDocumentId, Long targetDocumentId, 
                                                   DocumentRelationshipType relationshipType, 
                                                   String description, User createdBy) {
        try {
            // Validate documents exist
            Document sourceDocument = documentRepository.findById(sourceDocumentId)
                .orElseThrow(() -> new IllegalArgumentException("Source document not found: " + sourceDocumentId));
            
            Document targetDocument = documentRepository.findById(targetDocumentId)
                .orElseThrow(() -> new IllegalArgumentException("Target document not found: " + targetDocumentId));
            
            // Check if relationship already exists
            if (relationshipRepository.existsBySourceAndTargetAndType(
                    sourceDocumentId, targetDocumentId, relationshipType)) {
                throw new IllegalArgumentException(
                    "Relationship already exists between these documents with type: " + relationshipType);
            }
            
            // Prevent self-linking
            if (sourceDocumentId.equals(targetDocumentId)) {
                throw new IllegalArgumentException("Cannot create relationship between a document and itself");
            }
            
            // Create relationship
            DocumentRelationship relationship = new DocumentRelationship(
                sourceDocument, targetDocument, relationshipType, createdBy
            );
            relationship.setDescription(description);
            
            DocumentRelationship saved = relationshipRepository.save(relationship);
            
            // Log the action
            auditService.logUserAction(createdBy, "CREATE_RELATIONSHIP", "DOCUMENT", 
                sourceDocumentId, "Linked document " + sourceDocumentId + " to document " + targetDocumentId, null);
            
            logger.info("Created relationship {} between documents {} and {}", 
                relationshipType, sourceDocumentId, targetDocumentId);
            
            return saved;
            
        } catch (Exception e) {
            logger.error("Failed to create relationship: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get all relationships for a document
     */
    public List<DocumentRelationship> getDocumentRelationships(Long documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        return relationshipRepository.findAllRelationshipsForDocument(document);
    }
    
    /**
     * Get relationships where document is source
     */
    public List<DocumentRelationship> getSourceRelationships(Long documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        return relationshipRepository.findBySourceDocument(document);
    }
    
    /**
     * Get relationships where document is target
     */
    public List<DocumentRelationship> getTargetRelationships(Long documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        return relationshipRepository.findByTargetDocument(document);
    }
    
    /**
     * Delete a relationship
     */
    @Transactional
    public void deleteRelationship(Long relationshipId, User deletedBy) {
        try {
            DocumentRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new IllegalArgumentException("Relationship not found: " + relationshipId));
            
            Long sourceId = relationship.getSourceDocument().getId();
            Long targetId = relationship.getTargetDocument().getId();
            
            relationshipRepository.delete(relationship);
            
            // Log the action
            auditService.logUserAction(deletedBy, "DELETE_RELATIONSHIP", "DOCUMENT", 
                sourceId, "Unlinked document " + sourceId + " from document " + targetId, null);
            
            logger.info("Deleted relationship {} between documents {} and {}", 
                relationship.getRelationshipType(), sourceId, targetId);
            
        } catch (Exception e) {
            logger.error("Failed to delete relationship: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Delete all relationships for a document (when document is deleted)
     */
    @Transactional
    public void deleteAllRelationshipsForDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        relationshipRepository.deleteBySourceDocument(document);
        relationshipRepository.deleteByTargetDocument(document);
        
        logger.info("Deleted all relationships for document {}", documentId);
    }
    
    /**
     * Get related documents for a document (returns list of related document IDs)
     */
    public List<Long> getRelatedDocumentIds(Long documentId) {
        List<DocumentRelationship> relationships = getDocumentRelationships(documentId);
        
        return relationships.stream()
            .map(rel -> {
                if (rel.getSourceDocument().getId().equals(documentId)) {
                    return rel.getTargetDocument().getId();
                } else {
                    return rel.getSourceDocument().getId();
                }
            })
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Get relationships by type
     */
    public List<DocumentRelationship> getRelationshipsByType(DocumentRelationshipType relationshipType) {
        return relationshipRepository.findByRelationshipType(relationshipType);
    }
}

