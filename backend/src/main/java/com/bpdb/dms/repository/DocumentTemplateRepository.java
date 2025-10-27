package com.bpdb.dms.repository;

import com.bpdb.dms.entity.DocumentTemplate;
import com.bpdb.dms.entity.TemplateStatus;
import com.bpdb.dms.entity.TemplateType;
import com.bpdb.dms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for DocumentTemplate entity
 */
@Repository
public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Long> {
    
    /**
     * Find templates by status
     */
    Page<DocumentTemplate> findByStatus(TemplateStatus status, Pageable pageable);
    
    /**
     * Find templates by type
     */
    Page<DocumentTemplate> findByTemplateType(TemplateType templateType, Pageable pageable);
    
    /**
     * Find templates by creator
     */
    Page<DocumentTemplate> findByCreatedBy(User createdBy, Pageable pageable);
    
    /**
     * Find public templates
     */
    Page<DocumentTemplate> findByIsPublicTrue(Pageable pageable);
    
    /**
     * Find templates by name containing
     */
    Page<DocumentTemplate> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    /**
     * Find most used templates
     */
    @Query("SELECT dt FROM DocumentTemplate dt WHERE dt.status = 'ACTIVE' ORDER BY dt.usageCount DESC")
    List<DocumentTemplate> findMostUsedTemplates(Pageable pageable);
    
    /**
     * Find recently used templates
     */
    @Query("SELECT dt FROM DocumentTemplate dt WHERE dt.lastUsedAt IS NOT NULL ORDER BY dt.lastUsedAt DESC")
    List<DocumentTemplate> findRecentlyUsedTemplates(Pageable pageable);
    
    /**
     * Find templates by MIME type
     */
    List<DocumentTemplate> findByMimeType(String mimeType);
    
    /**
     * Count templates by status
     */
    long countByStatus(TemplateStatus status);
    
    /**
     * Count public templates
     */
    long countByIsPublicTrue();
    
    /**
     * Find templates by multiple criteria
     */
    @Query("SELECT dt FROM DocumentTemplate dt WHERE " +
           "(:status IS NULL OR dt.status = :status) AND " +
           "(:templateType IS NULL OR dt.templateType = :templateType) AND " +
           "(:isPublic IS NULL OR dt.isPublic = :isPublic) AND " +
           "(:createdBy IS NULL OR dt.createdBy = :createdBy)")
    Page<DocumentTemplate> findByMultipleCriteria(@Param("status") TemplateStatus status,
                                                 @Param("templateType") TemplateType templateType,
                                                 @Param("isPublic") Boolean isPublic,
                                                 @Param("createdBy") User createdBy,
                                                 Pageable pageable);
}
