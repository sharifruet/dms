package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Workflow;
import com.bpdb.dms.entity.WorkflowStatus;
import com.bpdb.dms.entity.WorkflowType;
import com.bpdb.dms.entity.User;
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
 * Repository interface for Workflow entity
 */
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    
    /**
     * Find workflows by status
     */
    Page<Workflow> findByStatus(WorkflowStatus status, Pageable pageable);
    
    /**
     * Find workflows by type
     */
    Page<Workflow> findByType(WorkflowType type, Pageable pageable);
    
    /**
     * Find workflows by creator
     */
    Page<Workflow> findByCreatedBy(User createdBy, Pageable pageable);
    
    /**
     * Find public workflows
     */
    Page<Workflow> findByIsPublicTrue(Pageable pageable);
    
    /**
     * Find automatic workflows
     */
    List<Workflow> findByIsAutomaticTrueAndStatus(WorkflowStatus status);
    
    /**
     * Find workflows due for execution
     */
    @Query("SELECT w FROM Workflow w WHERE w.nextExecutionAt <= :currentTime AND w.isAutomatic = true AND w.status = 'ACTIVE'")
    List<Workflow> findWorkflowsDueForExecution(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find workflows by name containing
     */
    Page<Workflow> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    /**
     * Count workflows by status
     */
    long countByStatus(WorkflowStatus status);
    
    /**
     * Find workflows by multiple criteria
     */
    @Query("SELECT w FROM Workflow w WHERE " +
           "(:status IS NULL OR w.status = :status) AND " +
           "(:type IS NULL OR w.type = :type) AND " +
           "(:isPublic IS NULL OR w.isPublic = :isPublic) AND " +
           "(:isAutomatic IS NULL OR w.isAutomatic = :isAutomatic)")
    Page<Workflow> findByMultipleCriteria(@Param("status") WorkflowStatus status,
                                         @Param("type") WorkflowType type,
                                         @Param("isPublic") Boolean isPublic,
                                         @Param("isAutomatic") Boolean isAutomatic,
                                         Pageable pageable);
}
