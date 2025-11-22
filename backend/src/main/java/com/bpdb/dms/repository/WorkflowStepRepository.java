package com.bpdb.dms.repository;

import com.bpdb.dms.entity.WorkflowStep;
import com.bpdb.dms.entity.WorkflowStepStatus;
import com.bpdb.dms.entity.WorkflowInstance;
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
 * Repository interface for WorkflowStep entity
 */
@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    
    /**
     * Find workflow steps by workflow instance
     */
    List<WorkflowStep> findByWorkflowInstanceOrderByStepNumber(WorkflowInstance workflowInstance);
    
    /**
     * Find workflow steps by assigned user
     */
    @Query("SELECT ws FROM WorkflowStep ws " +
           "LEFT JOIN FETCH ws.workflowInstance wi " +
           "LEFT JOIN FETCH wi.workflow " +
           "LEFT JOIN FETCH wi.document " +
           "WHERE ws.assignedTo = :assignedTo")
    Page<WorkflowStep> findByAssignedTo(@Param("assignedTo") User assignedTo, Pageable pageable);
    
    /**
     * Find workflow steps by status
     */
    Page<WorkflowStep> findByStatus(WorkflowStepStatus status, Pageable pageable);
    
    /**
     * Find current step for workflow instance
     */
    @Query("SELECT ws FROM WorkflowStep ws WHERE ws.workflowInstance = :workflowInstance AND ws.status = 'IN_PROGRESS' ORDER BY ws.stepNumber")
    List<WorkflowStep> findCurrentSteps(@Param("workflowInstance") WorkflowInstance workflowInstance);
    
    /**
     * Find next step for workflow instance
     */
    @Query("SELECT ws FROM WorkflowStep ws WHERE ws.workflowInstance = :workflowInstance AND ws.status = 'PENDING' ORDER BY ws.stepNumber")
    List<WorkflowStep> findNextSteps(@Param("workflowInstance") WorkflowInstance workflowInstance);
    
    /**
     * Find overdue workflow steps
     */
    @Query("SELECT ws FROM WorkflowStep ws WHERE ws.dueDate < :currentTime AND ws.status IN ('PENDING', 'IN_PROGRESS')")
    List<WorkflowStep> findOverdueSteps(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find workflow steps due soon
     */
    @Query("SELECT ws FROM WorkflowStep ws WHERE ws.dueDate BETWEEN :startTime AND :endTime AND ws.status IN ('PENDING', 'IN_PROGRESS')")
    List<WorkflowStep> findStepsDueSoon(@Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * Count workflow steps by status
     */
    long countByStatus(WorkflowStepStatus status);
    
    /**
     * Count workflow steps by assigned user and status
     */
    long countByAssignedToAndStatus(User assignedTo, WorkflowStepStatus status);
    
    /**
     * Find workflow steps by multiple criteria
     */
    @Query("SELECT ws FROM WorkflowStep ws WHERE " +
           "(:workflowInstance IS NULL OR ws.workflowInstance = :workflowInstance) AND " +
           "(:assignedTo IS NULL OR ws.assignedTo = :assignedTo) AND " +
           "(:status IS NULL OR ws.status = :status)")
    Page<WorkflowStep> findByMultipleCriteria(@Param("workflowInstance") WorkflowInstance workflowInstance,
                                            @Param("assignedTo") User assignedTo,
                                            @Param("status") WorkflowStepStatus status,
                                            Pageable pageable);
}
