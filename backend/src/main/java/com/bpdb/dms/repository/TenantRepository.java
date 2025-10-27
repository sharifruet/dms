package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Tenant;
import com.bpdb.dms.entity.TenantPlan;
import com.bpdb.dms.entity.TenantStatus;
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
 * Repository for Tenant entity
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    
    /**
     * Find tenant by tenant code
     */
    Optional<Tenant> findByTenantCode(String tenantCode);
    
    /**
     * Find tenant by domain
     */
    Optional<Tenant> findByDomain(String domain);
    
    /**
     * Find tenant by subdomain
     */
    Optional<Tenant> findBySubdomain(String subdomain);
    
    /**
     * Find tenants by status
     */
    List<Tenant> findByStatus(TenantStatus status);
    
    /**
     * Find tenants by plan
     */
    List<Tenant> findByPlan(TenantPlan plan);
    
    /**
     * Find active tenants
     */
    List<Tenant> findByIsActiveTrue();
    
    /**
     * Count tenants by status
     */
    long countByStatus(TenantStatus status);
    
    /**
     * Count tenants by plan
     */
    long countByPlan(TenantPlan plan);
    
    /**
     * Sum current users across all tenants
     */
    @Query("SELECT COALESCE(SUM(t.currentUsers), 0) FROM Tenant t WHERE t.isActive = true")
    Long sumCurrentUsers();
    
    /**
     * Sum current storage bytes across all tenants
     */
    @Query("SELECT COALESCE(SUM(t.currentStorageBytes), 0) FROM Tenant t WHERE t.isActive = true")
    Long sumCurrentStorageBytes();
    
    /**
     * Find tenants by multiple criteria
     */
    @Query("SELECT t FROM Tenant t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:plan IS NULL OR t.plan = :plan) AND " +
           "t.isActive = true")
    Page<Tenant> findByMultipleCriteria(
        @Param("status") TenantStatus status,
        @Param("plan") TenantPlan plan,
        Pageable pageable
    );
    
    /**
     * Find expired tenants
     */
    @Query("SELECT t FROM Tenant t WHERE t.subscriptionEndDate < :currentDate AND t.status != 'EXPIRED'")
    List<Tenant> findExpiredTenants(@Param("currentDate") LocalDateTime currentDate);
    
    /**
     * Find tenants expiring soon
     */
    @Query("SELECT t FROM Tenant t WHERE t.subscriptionEndDate BETWEEN :startDate AND :endDate")
    List<Tenant> findTenantsExpiringSoon(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find tenants by created date range
     */
    List<Tenant> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find tenants by contact email
     */
    List<Tenant> findByContactEmailContainingIgnoreCase(String email);
    
    /**
     * Find tenants by country
     */
    List<Tenant> findByCountryOrderByTenantNameAsc(String country);
    
    /**
     * Find tenants with usage approaching limits
     */
    @Query("SELECT t FROM Tenant t WHERE " +
           "(t.currentUsers >= t.maxUsers * 0.9) OR " +
           "(t.currentStorageBytes >= t.maxStorageGb * 1024 * 1024 * 1024 * 0.9)")
    List<Tenant> findTenantsApproachingLimits();
    
    /**
     * Find tenants by timezone
     */
    List<Tenant> findByTimezoneOrderByTenantNameAsc(String timezone);
    
    /**
     * Find tenants by locale
     */
    List<Tenant> findByLocaleOrderByTenantNameAsc(String locale);
    
    /**
     * Find recent tenants
     */
    @Query("SELECT t FROM Tenant t ORDER BY t.createdAt DESC")
    List<Tenant> findRecentTenants(Pageable pageable);
    
    /**
     * Find tenants created by user
     */
    List<Tenant> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    
    /**
     * Find tenants updated by user
     */
    List<Tenant> findByUpdatedByOrderByUpdatedAtDesc(String updatedBy);
    
    /**
     * Find tenants with trial status
     */
    List<Tenant> findByStatusOrderByCreatedAtDesc(TenantStatus status);
    
    /**
     * Find tenants by billing email
     */
    List<Tenant> findByBillingEmailContainingIgnoreCase(String billingEmail);
    
    /**
     * Find tenants by contact name
     */
    List<Tenant> findByContactNameContainingIgnoreCase(String contactName);
}
