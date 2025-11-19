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
     * Find documents by folder
     */
    Page<Document> findByFolderId(Long folderId, Pageable pageable);
    
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
    
    /**
     * Count documents by type
     */
    long countByDocumentType(DocumentType documentType);
    
    /**
     * Count documents by department
     */
    long countByDepartment(String department);
    
    /**
     * Count documents by uploaded user
     */
    long countByUploadedBy(User user);
    
    /**
     * Count active documents
     */
    long countByIsActiveTrue();
    
    /**
     * Count documents uploaded by user and active
     */
    long countByUploadedByAndIsActiveTrue(User user);
    
    /**
     * Count documents by department and active
     */
    long countByDepartmentAndIsActiveTrue(String department);
    
    /**
     * Count documents created after date
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.createdAt > :date")
    long countByCreatedAtAfter(@Param("date") java.time.LocalDateTime date);
    
    /**
     * Get document count by department
     */
    @Query("SELECT d.department, COUNT(d) FROM Document d GROUP BY d.department")
    java.util.List<Object[]> getDocumentCountByDepartment();
}
