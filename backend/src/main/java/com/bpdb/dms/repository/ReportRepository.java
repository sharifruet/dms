package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Report;
import com.bpdb.dms.entity.ReportFormat;
import com.bpdb.dms.entity.ReportStatus;
import com.bpdb.dms.entity.ReportType;
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
 * Repository interface for Report entity
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    /**
     * Find reports by creator
     */
    Page<Report> findByCreatedBy(User createdBy, Pageable pageable);
    
    /**
     * Find reports by type
     */
    Page<Report> findByType(ReportType type, Pageable pageable);
    
    /**
     * Find reports by status
     */
    List<Report> findByStatus(ReportStatus status);
    
    /**
     * Find reports by format
     */
    Page<Report> findByFormat(ReportFormat format, Pageable pageable);
    
    /**
     * Find public reports
     */
    Page<Report> findByIsPublicTrue(Pageable pageable);
    
    /**
     * Find scheduled reports
     */
    List<Report> findByIsScheduledTrue();
    
    /**
     * Find reports scheduled for generation
     */
    @Query("SELECT r FROM Report r WHERE r.isScheduled = true AND r.nextGenerationAt <= :now")
    List<Report> findScheduledForGeneration(@Param("now") LocalDateTime now);
    
    /**
     * Find expired reports
     */
    @Query("SELECT r FROM Report r WHERE r.expiresAt IS NOT NULL AND r.expiresAt < :now")
    List<Report> findExpiredReports(@Param("now") LocalDateTime now);
    
    /**
     * Find reports created between dates
     */
    List<Report> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find reports by name containing
     */
    Page<Report> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    /**
     * Count reports by type
     */
    long countByType(ReportType type);
    
    /**
     * Count reports by status
     */
    long countByStatus(ReportStatus status);
    
    /**
     * Find most accessed reports
     */
    @Query("SELECT r FROM Report r ORDER BY r.accessCount DESC")
    List<Report> findMostAccessedReports(Pageable pageable);
    
    /**
     * Find recently generated reports
     */
    @Query("SELECT r FROM Report r WHERE r.generatedAt IS NOT NULL ORDER BY r.generatedAt DESC")
    List<Report> findRecentlyGeneratedReports(Pageable pageable);
    
    /**
     * Delete old reports
     */
    @Query("DELETE FROM Report r WHERE r.createdAt < :cutoffDate AND r.status IN ('COMPLETED', 'EXPIRED')")
    void deleteOldReports(@Param("cutoffDate") LocalDateTime cutoffDate);
}
