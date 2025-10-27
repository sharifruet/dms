package com.bpdb.dms.repository;

import com.bpdb.dms.entity.SystemHealthCheck;
import com.bpdb.dms.entity.HealthCheckType;
import com.bpdb.dms.entity.HealthStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for SystemHealthCheck entity
 */
@Repository
public interface SystemHealthCheckRepository extends JpaRepository<SystemHealthCheck, Long> {
    
    /**
     * Find health checks by type
     */
    Page<SystemHealthCheck> findByCheckType(HealthCheckType checkType, Pageable pageable);
    
    /**
     * Find health checks by status
     */
    Page<SystemHealthCheck> findByStatus(HealthStatus status, Pageable pageable);
    
    /**
     * Find health checks by component
     */
    Page<SystemHealthCheck> findByComponent(String component, Pageable pageable);
    
    /**
     * Find health checks by service
     */
    Page<SystemHealthCheck> findByService(String service, Pageable pageable);
    
    /**
     * Find health checks by severity
     */
    Page<SystemHealthCheck> findBySeverity(String severity, Pageable pageable);
    
    /**
     * Find enabled health checks
     */
    List<SystemHealthCheck> findByIsEnabledTrue();
    
    /**
     * Find health checks due for execution
     */
    @Query("SELECT shc FROM SystemHealthCheck shc WHERE shc.nextCheckAt <= :currentTime AND shc.isEnabled = true")
    List<SystemHealthCheck> findHealthChecksDueForExecution(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find failed health checks
     */
    @Query("SELECT shc FROM SystemHealthCheck shc WHERE shc.status = 'FAILED' AND shc.retryCount < shc.maxRetries")
    List<SystemHealthCheck> findFailedHealthChecks();
    
    /**
     * Find health checks by time range
     */
    @Query("SELECT shc FROM SystemHealthCheck shc WHERE shc.executedAt BETWEEN :startTime AND :endTime")
    List<SystemHealthCheck> findByExecutedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                                    @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find latest health check for each component
     */
    @Query("SELECT shc FROM SystemHealthCheck shc WHERE shc.id IN " +
           "(SELECT MAX(shc2.id) FROM SystemHealthCheck shc2 GROUP BY shc2.component)")
    List<SystemHealthCheck> findLatestHealthChecksByComponent();
    
    /**
     * Count health checks by status
     */
    long countByStatus(HealthStatus status);
    
    /**
     * Count health checks by severity
     */
    long countBySeverity(String severity);
    
    /**
     * Find health checks by multiple criteria
     */
    @Query("SELECT shc FROM SystemHealthCheck shc WHERE " +
           "(:checkType IS NULL OR shc.checkType = :checkType) AND " +
           "(:status IS NULL OR shc.status = :status) AND " +
           "(:component IS NULL OR shc.component = :component) AND " +
           "(:service IS NULL OR shc.service = :service) AND " +
           "(:severity IS NULL OR shc.severity = :severity)")
    Page<SystemHealthCheck> findByMultipleCriteria(@Param("checkType") HealthCheckType checkType,
                                                   @Param("status") HealthStatus status,
                                                   @Param("component") String component,
                                                   @Param("service") String service,
                                                   @Param("severity") String severity,
                                                   Pageable pageable);
}
