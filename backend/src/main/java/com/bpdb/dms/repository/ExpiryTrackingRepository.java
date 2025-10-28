package com.bpdb.dms.repository;

import com.bpdb.dms.entity.ExpiryTracking;
import com.bpdb.dms.entity.ExpiryStatus;
import com.bpdb.dms.entity.ExpiryType;
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
 * Repository interface for ExpiryTracking entity
 */
@Repository
public interface ExpiryTrackingRepository extends JpaRepository<ExpiryTracking, Long> {
    
    /**
     * Find expiry tracking by document
     */
    List<ExpiryTracking> findByDocumentId(Long documentId);
    
    /**
     * Find expiry tracking by status
     */
    Page<ExpiryTracking> findByStatus(ExpiryStatus status, Pageable pageable);
    
    /**
     * Count expiry tracking by status
     */
    long countByStatus(ExpiryStatus status);
    
    /**
     * Find expiry tracking by type
     */
    Page<ExpiryTracking> findByExpiryType(ExpiryType expiryType, Pageable pageable);
    
    /**
     * Find expiry tracking by assigned user
     */
    Page<ExpiryTracking> findByAssignedTo(User user, Pageable pageable);
    
    /**
     * Find expiry tracking by department
     */
    Page<ExpiryTracking> findByDepartment(String department, Pageable pageable);
    
    /**
     * Find expiring documents within specified days
     */
    @Query("SELECT et FROM ExpiryTracking et WHERE et.expiryDate BETWEEN :startDate AND :endDate AND et.status = 'ACTIVE'")
    List<ExpiryTracking> findExpiringBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find expired documents
     */
    @Query("SELECT et FROM ExpiryTracking et WHERE et.expiryDate < :now AND et.status = 'ACTIVE'")
    List<ExpiryTracking> findExpired(@Param("now") LocalDateTime now);
    
    /**
     * Find documents expiring in 30 days
     */
    @Query("SELECT et FROM ExpiryTracking et WHERE et.expiryDate BETWEEN :now AND :thirtyDaysFromNow AND et.status = 'ACTIVE' AND et.alert30Days = false")
    List<ExpiryTracking> findExpiringIn30Days(@Param("now") LocalDateTime now, @Param("thirtyDaysFromNow") LocalDateTime thirtyDaysFromNow);
    
    /**
     * Find documents expiring in 15 days
     */
    @Query("SELECT et FROM ExpiryTracking et WHERE et.expiryDate BETWEEN :now AND :fifteenDaysFromNow AND et.status = 'ACTIVE' AND et.alert15Days = false")
    List<ExpiryTracking> findExpiringIn15Days(@Param("now") LocalDateTime now, @Param("fifteenDaysFromNow") LocalDateTime fifteenDaysFromNow);
    
    /**
     * Find documents expiring in 7 days
     */
    @Query("SELECT et FROM ExpiryTracking et WHERE et.expiryDate BETWEEN :now AND :sevenDaysFromNow AND et.status = 'ACTIVE' AND et.alert7Days = false")
    List<ExpiryTracking> findExpiringIn7Days(@Param("now") LocalDateTime now, @Param("sevenDaysFromNow") LocalDateTime sevenDaysFromNow);
    
    /**
     * Find recently expired documents
     */
    @Query("SELECT et FROM ExpiryTracking et WHERE et.expiryDate < :now AND et.status = 'ACTIVE' AND et.alertExpired = false")
    List<ExpiryTracking> findRecentlyExpired(@Param("now") LocalDateTime now);
    
    /**
     * Count active expiry tracking by type
     */
    long countByExpiryTypeAndStatus(ExpiryType expiryType, ExpiryStatus status);
    
    /**
     * Count active expiry tracking by department
     */
    long countByDepartmentAndStatus(String department, ExpiryStatus status);
    
    /**
     * Count active expiry tracking by assigned user
     */
    long countByAssignedToAndStatus(User user, ExpiryStatus status);
    
    /**
     * Find expiry tracking by vendor
     */
    List<ExpiryTracking> findByVendorNameContainingIgnoreCase(String vendorName);
    
    /**
     * Find expiry tracking by contract value range
     */
    @Query("SELECT et FROM ExpiryTracking et WHERE et.contractValue BETWEEN :minValue AND :maxValue AND et.status = 'ACTIVE'")
    List<ExpiryTracking> findByContractValueRange(@Param("minValue") Double minValue, @Param("maxValue") Double maxValue);
    
    /**
     * Find renewal tracking
     */
    @Query("SELECT et FROM ExpiryTracking et WHERE et.renewalDate IS NOT NULL AND et.renewalDate BETWEEN :startDate AND :endDate")
    List<ExpiryTracking> findRenewalsBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
