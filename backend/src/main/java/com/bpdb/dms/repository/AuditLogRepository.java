package com.bpdb.dms.repository;

import com.bpdb.dms.entity.AuditLog;
import com.bpdb.dms.entity.AuditStatus;
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
 * Repository interface for AuditLog entity
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    /**
     * Find audit logs by user
     */
    Page<AuditLog> findByUser(User user, Pageable pageable);
    
    /**
     * Find audit logs by action
     */
    Page<AuditLog> findByAction(String action, Pageable pageable);
    
    /**
     * Find audit logs by status
     */
    Page<AuditLog> findByStatus(AuditStatus status, Pageable pageable);
    
    /**
     * Find audit logs by resource type
     */
    Page<AuditLog> findByResourceType(String resourceType, Pageable pageable);
    
    /**
     * Find audit logs by date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate, 
                                  Pageable pageable);
    
    /**
     * Find audit logs by user and date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.user = :user AND a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByUserAndDateRange(@Param("user") User user,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         Pageable pageable);
    
    /**
     * Find recent audit logs
     */
    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC")
    Page<AuditLog> findRecentAuditLogs(Pageable pageable);
    
    /**
     * Count audit logs by action
     */
    long countByAction(String action);
    
    /**
     * Count audit logs by status
     */
    long countByStatus(AuditStatus status);
    
    /**
     * Count audit logs created after date and by action
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt > :date AND a.action = :action")
    long countByCreatedAtAfterAndAction(@Param("date") LocalDateTime date, @Param("action") String action);
    
    /**
     * Count audit logs created after date
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt > :date")
    long countByCreatedAtAfter(@Param("date") LocalDateTime date);
    
    /**
     * Count audit logs by username and date range
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt > :date AND a.user.username = :username")
    long countByCreatedAtAfterAndUsername(@Param("date") LocalDateTime date, @Param("username") String username);
    
    /**
     * Count audit logs by description containing and date range
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt > :date AND a.description LIKE %:description%")
    long countByCreatedAtAfterAndDescriptionContaining(@Param("date") LocalDateTime date, @Param("description") String description);
}
