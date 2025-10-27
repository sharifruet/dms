package com.bpdb.dms.repository;

import com.bpdb.dms.entity.DocumentVersion;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.entity.VersionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DocumentVersion entity
 */
@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    
    /**
     * Find document versions by document
     */
    List<DocumentVersion> findByDocumentOrderByCreatedAtDesc(Document document);
    
    /**
     * Find document versions by document and version number
     */
    Optional<DocumentVersion> findByDocumentAndVersionNumber(Document document, String versionNumber);
    
    /**
     * Find current version of document
     */
    Optional<DocumentVersion> findByDocumentAndIsCurrentTrue(Document document);
    
    /**
     * Find document versions by creator
     */
    Page<DocumentVersion> findByCreatedBy(User createdBy, Pageable pageable);
    
    /**
     * Find document versions by version type
     */
    Page<DocumentVersion> findByVersionType(VersionType versionType, Pageable pageable);
    
    /**
     * Find archived document versions
     */
    Page<DocumentVersion> findByIsArchivedTrue(Pageable pageable);
    
    /**
     * Find document versions created after date
     */
    List<DocumentVersion> findByDocumentAndCreatedAtAfter(Document document, LocalDateTime date);
    
    /**
     * Find document versions by file hash
     */
    List<DocumentVersion> findByFileHash(String fileHash);
    
    /**
     * Count versions for document
     */
    long countByDocument(Document document);
    
    /**
     * Find latest version for document
     */
    @Query("SELECT dv FROM DocumentVersion dv WHERE dv.document = :document ORDER BY dv.createdAt DESC")
    Optional<DocumentVersion> findLatestVersion(@Param("document") Document document);
    
    /**
     * Find document versions by multiple criteria
     */
    @Query("SELECT dv FROM DocumentVersion dv WHERE " +
           "(:document IS NULL OR dv.document = :document) AND " +
           "(:createdBy IS NULL OR dv.createdBy = :createdBy) AND " +
           "(:versionType IS NULL OR dv.versionType = :versionType) AND " +
           "(:isArchived IS NULL OR dv.isArchived = :isArchived)")
    Page<DocumentVersion> findByMultipleCriteria(@Param("document") Document document,
                                                @Param("createdBy") User createdBy,
                                                @Param("versionType") VersionType versionType,
                                                @Param("isArchived") Boolean isArchived,
                                                Pageable pageable);
}
