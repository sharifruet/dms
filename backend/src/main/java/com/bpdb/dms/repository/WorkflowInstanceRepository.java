package com.bpdb.dms.repository;

import com.bpdb.dms.entity.WorkflowInstance;
import com.bpdb.dms.entity.WorkflowInstanceStatus;
import com.bpdb.dms.entity.Workflow;
import com.bpdb.dms.entity.Document;
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
 * Repository interface for WorkflowInstance entity
 */
@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {
    
    /**
     * Find workflow instances by workflow
     */
    Page<WorkflowInstance> findByWorkflow(Workflow workflow, Pageable pageable);
    
    /**
     * Find workflow instances by document
     */
    Page<WorkflowInstance> findByDocument(Document document, Pageable pageable);
    
    /**
     * Find workflow instances by initiator
     */
    Page<WorkflowInstance> findByInitiatedBy(User initiatedBy, Pageable pageable);
    
    /**
     * Find workflow instances by status
     */
    Page<WorkflowInstance> findByStatus(WorkflowInstanceStatus status, Pageable pageable);
    
    /**
     * Find workflow instances by priority
     */
    Page<WorkflowInstance> findByPriority(Integer priority, Pageable pageable);
    
    /**
     * Find overdue workflow instances
     */
    @Query("SELECT wi FROM WorkflowInstance wi WHERE wi.dueDate < :currentTime AND wi.status IN ('PENDING', 'IN_PROGRESS')")
    List<WorkflowInstance> findOverdueInstances(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find workflow instances due soon
     */
    @Query("SELECT wi FROM WorkflowInstance wi WHERE wi.dueDate BETWEEN :startTime AND :endTime AND wi.status IN ('PENDING', 'IN_PROGRESS')")
    List<WorkflowInstance> findInstancesDueSoon(@Param("startTime") LocalDateTime startTime, 
                                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * Count workflow instances by status
     */
    long countByStatus(WorkflowInstanceStatus status);
    
    /**
     * Find workflow instances by multiple criteria
     */
    @Query("SELECT wi FROM WorkflowInstance wi WHERE " +
           "(:workflow IS NULL OR wi.workflow = :workflow) AND " +
           "(:document IS NULL OR wi.document = :document) AND " +
           "(:initiatedBy IS NULL OR wi.initiatedBy = :initiatedBy) AND " +
           "(:status IS NULL OR wi.status = :status)")
    Page<WorkflowInstance> findByMultipleCriteria(@Param("workflow") Workflow workflow,
                                                 @Param("document") Document document,
                                                 @Param("initiatedBy") User initiatedBy,
                                                 @Param("status") WorkflowInstanceStatus status,
                                                 Pageable pageable);
}
