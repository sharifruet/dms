package com.bpdb.dms.repository;

import com.bpdb.dms.entity.IntegrationConfig;
import com.bpdb.dms.entity.IntegrationStatus;
import com.bpdb.dms.entity.IntegrationType;
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
 * Repository interface for IntegrationConfig entity
 */
@Repository
public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, Long> {
    
    /**
     * Find integration configs by type
     */
    Page<IntegrationConfig> findByIntegrationType(IntegrationType integrationType, Pageable pageable);
    
    /**
     * Find integration configs by status
     */
    Page<IntegrationConfig> findByStatus(IntegrationStatus status, Pageable pageable);
    
    /**
     * Find integration configs by type and status
     */
    Page<IntegrationConfig> findByIntegrationTypeAndStatus(IntegrationType integrationType, IntegrationStatus status, Pageable pageable);
    
    /**
     * Find enabled integration configs
     */
    List<IntegrationConfig> findByIsEnabledTrue();
    
    /**
     * Find integration configs by creator
     */
    Page<IntegrationConfig> findByCreatedBy(User createdBy, Pageable pageable);
    
    /**
     * Find integration configs due for sync
     */
    @Query("SELECT ic FROM IntegrationConfig ic WHERE ic.nextSyncAt <= :currentTime AND ic.isEnabled = true")
    List<IntegrationConfig> findIntegrationConfigsDueForSync(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find integration configs with high failure rate
     */
    @Query("SELECT ic FROM IntegrationConfig ic WHERE ic.failureCount > :threshold AND ic.isEnabled = true")
    List<IntegrationConfig> findIntegrationConfigsWithHighFailureRate(@Param("threshold") Long threshold);
    
    /**
     * Count integration configs by status
     */
    long countByStatus(IntegrationStatus status);
    
    /**
     * Count enabled integration configs
     */
    long countByIsEnabledTrue();
    
    /**
     * Find integration configs by multiple criteria
     */
    @Query("SELECT ic FROM IntegrationConfig ic WHERE " +
           "(:integrationType IS NULL OR ic.integrationType = :integrationType) AND " +
           "(:status IS NULL OR ic.status = :status) AND " +
           "(:isEnabled IS NULL OR ic.isEnabled = :isEnabled) AND " +
           "(:createdBy IS NULL OR ic.createdBy = :createdBy)")
    Page<IntegrationConfig> findByMultipleCriteria(@Param("integrationType") IntegrationType integrationType,
                                                   @Param("status") IntegrationStatus status,
                                                   @Param("isEnabled") Boolean isEnabled,
                                                   @Param("createdBy") User createdBy,
                                                   Pageable pageable);
}
