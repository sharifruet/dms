package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.Folder;
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
    Page<Document> findByDocumentType(String documentType, Pageable pageable);
    
    /**
     * Find documents by department
     */
    Page<Document> findByDepartment(String department, Pageable pageable);
    
    /**
     * Find documents by folder (active, not deleted)
     */
    @Query("SELECT d FROM Document d WHERE d.folder.id = :folderId AND d.isActive = true AND (d.deletedAt IS NULL)")
    Page<Document> findByFolderId(@Param("folderId") Long folderId, Pageable pageable);
    
    /**
     * Find documents by folder and document type (active, not deleted)
     */
    @Query("SELECT d FROM Document d WHERE d.folder.id = :folderId AND d.documentType = :documentType AND d.isActive = true AND (d.deletedAt IS NULL)")
    Page<Document> findByFolderIdAndDocumentType(@Param("folderId") Long folderId, @Param("documentType") String documentType, Pageable pageable);
    
    /**
     * Find documents by document type (active, not deleted)
     */
    @Query("SELECT d FROM Document d WHERE d.documentType = :documentType AND d.isActive = true AND (d.deletedAt IS NULL)")
    Page<Document> findByDocumentTypeAndIsActiveTrueAndDeletedAtIsNull(@Param("documentType") String documentType, Pageable pageable);
    
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
    long countByDocumentType(String documentType);
    
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
     * Count documents by folder
     */
    long countByFolder(Folder folder);
    
    /**
     * Count documents by folder and active
     */
    long countByFolderAndIsActiveTrue(Folder folder);
    
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
    
    /**
     * Find documents by file hash (for duplicate detection)
     */
    List<Document> findByFileHashAndIsActiveTrue(String fileHash);
    
    /**
     * Find a single document by file hash (for duplicate detection)
     */
    @Query("SELECT d FROM Document d WHERE d.fileHash = :fileHash AND d.isActive = true ORDER BY d.createdAt DESC")
    java.util.Optional<Document> findFirstByFileHashAndIsActiveTrue(@Param("fileHash") String fileHash);
    
    /**
     * Find archived documents
     */
    @Query("SELECT d FROM Document d WHERE d.isArchived = true AND d.isActive = true AND d.deletedAt IS NULL")
    Page<Document> findArchivedDocuments(Pageable pageable);
    
    /**
     * Find deleted documents (soft deleted)
     */
    @Query("SELECT d FROM Document d WHERE d.deletedAt IS NOT NULL")
    Page<Document> findDeletedDocuments(Pageable pageable);
    
    /**
     * Find active, non-archived documents
     */
    @Query("SELECT d FROM Document d WHERE d.isActive = true AND (d.isArchived = false OR d.isArchived IS NULL) AND d.deletedAt IS NULL")
    Page<Document> findActiveNonArchivedDocuments(Pageable pageable);
    
    /**
     * Count archived documents
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.isArchived = true AND d.isActive = true AND d.deletedAt IS NULL")
    long countArchivedDocuments();
    
    /**
     * Count deleted documents
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.deletedAt IS NOT NULL")
    long countDeletedDocuments();
    
    /**
     * Find stationery records assigned to an employee
     */
    @Query("SELECT d FROM Document d WHERE d.documentType = 'STATIONERY_RECORD' AND d.assignedEmployee.id = :employeeId AND d.isActive = true AND d.deletedAt IS NULL")
    List<Document> findStationeryRecordsByEmployee(@Param("employeeId") Long employeeId);
    
    /**
     * Find all stationery records
     */
    @Query("SELECT d FROM Document d WHERE d.documentType = 'STATIONERY_RECORD' AND d.isActive = true AND d.deletedAt IS NULL")
    Page<Document> findStationeryRecords(Pageable pageable);
    
    /**
     * Count stationery records per employee
     */
    @Query("SELECT d.assignedEmployee.id, COUNT(d) FROM Document d WHERE d.documentType = 'STATIONERY_RECORD' AND d.isActive = true AND d.deletedAt IS NULL GROUP BY d.assignedEmployee.id")
    List<Object[]> countStationeryRecordsPerEmployee();

    /**
     * Find all BILL documents in a given folder (active, not deleted)
     */
    @Query("SELECT d FROM Document d WHERE d.folder = :folder AND d.documentType = 'BILL' AND d.isActive = true AND d.deletedAt IS NULL")
    List<Document> findBillDocumentsByFolder(@Param("folder") Folder folder);
}
