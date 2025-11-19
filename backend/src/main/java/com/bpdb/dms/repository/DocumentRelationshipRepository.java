package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentRelationship;
import com.bpdb.dms.entity.DocumentRelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DocumentRelationship entity
 */
@Repository
public interface DocumentRelationshipRepository extends JpaRepository<DocumentRelationship, Long> {
    
    /**
     * Find all relationships where the given document is the source
     */
    List<DocumentRelationship> findBySourceDocument(Document document);
    
    /**
     * Find all relationships where the given document is the target
     */
    List<DocumentRelationship> findByTargetDocument(Document document);
    
    /**
     * Find all relationships for a document (as source or target)
     */
    @Query("SELECT dr FROM DocumentRelationship dr WHERE dr.sourceDocument = :document OR dr.targetDocument = :document")
    List<DocumentRelationship> findAllRelationshipsForDocument(@Param("document") Document document);
    
    /**
     * Find relationship between two specific documents
     */
    @Query("SELECT dr FROM DocumentRelationship dr WHERE " +
           "(dr.sourceDocument = :doc1 AND dr.targetDocument = :doc2) OR " +
           "(dr.sourceDocument = :doc2 AND dr.targetDocument = :doc1)")
    List<DocumentRelationship> findRelationshipBetweenDocuments(
        @Param("doc1") Document doc1, 
        @Param("doc2") Document doc2
    );
    
    /**
     * Find relationships by type
     */
    List<DocumentRelationship> findByRelationshipType(DocumentRelationshipType relationshipType);
    
    /**
     * Check if relationship exists between two documents with specific type
     */
    @Query("SELECT COUNT(dr) > 0 FROM DocumentRelationship dr WHERE " +
           "dr.sourceDocument.id = :sourceId AND dr.targetDocument.id = :targetId AND " +
           "dr.relationshipType = :relationshipType")
    boolean existsBySourceAndTargetAndType(
        @Param("sourceId") Long sourceId,
        @Param("targetId") Long targetId,
        @Param("relationshipType") DocumentRelationshipType relationshipType
    );
    
    /**
     * Delete relationships for a document (when document is deleted)
     */
    void deleteBySourceDocument(Document document);
    void deleteByTargetDocument(Document document);
}

