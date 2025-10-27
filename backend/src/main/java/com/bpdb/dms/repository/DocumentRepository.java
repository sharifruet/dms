package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentType;
import com.bpdb.dms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Document entity
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    /**
     * Find documents by uploaded user
     */
    Page<Document> findByUploadedBy(User user, Pageable pageable);
    
    /**
     * Find documents by document type
     */
    Page<Document> findByDocumentType(DocumentType documentType, Pageable pageable);
    
    /**
     * Find documents by department
     */
    Page<Document> findByDepartment(String department, Pageable pageable);
    
    /**
     * Search documents by filename
     */
    @Query("SELECT d FROM Document d WHERE d.fileName LIKE %:filename% OR d.originalName LIKE %:filename%")
    Page<Document> findByFileNameContaining(@Param("filename") String filename, Pageable pageable);
    
    /**
     * Find active documents
     */
    Page<Document> findByIsActiveTrue(Pageable pageable);
    
    /**
     * Find documents by tags
     */
    @Query("SELECT d FROM Document d WHERE d.tags LIKE %:tag%")
    List<Document> findByTagsContaining(@Param("tag") String tag);
}
