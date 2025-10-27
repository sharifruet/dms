package com.bpdb.dms.repository;

import com.bpdb.dms.entity.BackupRecord;
import com.bpdb.dms.entity.BackupStatus;
import com.bpdb.dms.entity.BackupType;
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
 * Repository for BackupRecord entity
 */
@Repository
public interface BackupRecordRepository extends JpaRepository<BackupRecord, Long> {
    
    /**
     * Find backups by status
     */
    List<BackupRecord> findByStatus(BackupStatus status);
    
    /**
     * Find backups by type
     */
    List<BackupRecord> findByBackupType(BackupType backupType);
    
    /**
     * Find backups by status ordered by completion date descending
     */
    Optional<BackupRecord> findTopByStatusOrderByCompletedAtDesc(BackupStatus status);
    
    /**
     * Find backups by type and status ordered by completion date descending
     */
    Optional<BackupRecord> findTopByBackupTypeAndStatusOrderByCompletedAtDesc(BackupType backupType, BackupStatus status);
    
    /**
     * Find backups that should be cleaned up
     */
    List<BackupRecord> findByRetentionUntilBefore(LocalDateTime cutoffDate);
    
    /**
     * Count backups by status
     */
    long countByStatus(BackupStatus status);
    
    /**
     * Count backups by type
     */
    long countByBackupType(BackupType backupType);
    
    /**
     * Sum total size of all backups
     */
    @Query("SELECT COALESCE(SUM(b.sizeBytes), 0) FROM BackupRecord b WHERE b.status = :status")
    Long sumSizeBytesByStatus(@Param("status") BackupStatus status);
    
    /**
     * Sum total size of all backups
     */
    @Query("SELECT COALESCE(SUM(b.sizeBytes), 0) FROM BackupRecord b")
    Long sumSizeBytes();
    
    /**
     * Find backups by multiple criteria
     */
    @Query("SELECT b FROM BackupRecord b WHERE " +
           "(:backupType IS NULL OR b.backupType = :backupType) AND " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:startDate IS NULL OR b.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR b.createdAt <= :endDate)")
    Page<BackupRecord> findByMultipleCriteria(
        @Param("backupType") BackupType backupType,
        @Param("status") BackupStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * Find recent backups
     */
    @Query("SELECT b FROM BackupRecord b ORDER BY b.createdAt DESC")
    List<BackupRecord> findRecentBackups(Pageable pageable);
    
    /**
     * Find failed backups
     */
    List<BackupRecord> findByStatusOrderByCreatedAtDesc(BackupStatus status);
    
    /**
     * Find backups created by user
     */
    List<BackupRecord> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    
    /**
     * Find backups within date range
     */
    List<BackupRecord> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
}
