package com.bpdb.dms.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.entity.OptimizationStatus;
import com.bpdb.dms.entity.OptimizationTask;
import com.bpdb.dms.entity.OptimizationType;

/**
 * Repository for OptimizationTask entity
 */
@Repository
public interface OptimizationTaskRepository extends JpaRepository<OptimizationTask, Long> {
    
    /**
     * Find tasks by status
     */
    List<OptimizationTask> findByStatus(OptimizationStatus status);
    
    /**
     * Find tasks by optimization type
     */
    List<OptimizationTask> findByOptimizationType(OptimizationType optimizationType);
    
    /**
     * Find tasks by status ordered by completion date descending
     */
    Optional<OptimizationTask> findTopByStatusOrderByCompletedAtDesc(OptimizationStatus status);
    
    /**
     * Find tasks by type and status ordered by completion date descending
     */
    Optional<OptimizationTask> findTopByOptimizationTypeAndStatusOrderByCompletedAtDesc(
        OptimizationType optimizationType, OptimizationStatus status);
    
    /**
     * Count tasks by status
     */
    long countByStatus(OptimizationStatus status);
    
    /**
     * Count tasks by optimization type
     */
    long countByOptimizationType(OptimizationType optimizationType);
    
    /**
     * Find tasks by multiple criteria
     */
    @Query("SELECT o FROM OptimizationTask o WHERE " +
           "(:optimizationType IS NULL OR o.optimizationType = :optimizationType) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR o.createdAt <= :endDate)")
    Page<OptimizationTask> findByMultipleCriteria(
        @Param("optimizationType") OptimizationType optimizationType,
        @Param("status") OptimizationStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * Find recent tasks
     */
    @Query("SELECT o FROM OptimizationTask o ORDER BY o.createdAt DESC")
    List<OptimizationTask> findRecentTasks(Pageable pageable);
    
    /**
     * Find tasks created by user
     */
    List<OptimizationTask> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    
    /**
     * Find tasks within date range
     */
    List<OptimizationTask> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find completed tasks
     */
    List<OptimizationTask> findByStatusOrderByCompletedAtDesc(OptimizationStatus status);
    
    /**
     * Find failed tasks
     */
    List<OptimizationTask> findByStatusOrderByCreatedAtDesc(OptimizationStatus status);
    
    /**
     * Find tasks by execution time range
     */
    @Query("SELECT o FROM OptimizationTask o WHERE o.executionTimeMs BETWEEN :minTime AND :maxTime")
    List<OptimizationTask> findByExecutionTimeBetween(@Param("minTime") Long minTime, @Param("maxTime") Long maxTime);
    
    /**
     * Find tasks with longest execution time
     */
    @Query("SELECT o FROM OptimizationTask o WHERE o.status = 'COMPLETED' ORDER BY o.executionTimeMs DESC")
    List<OptimizationTask> findLongestRunningTasks(Pageable pageable);
    
    /**
     * Find tasks with shortest execution time
     */
    @Query("SELECT o FROM OptimizationTask o WHERE o.status = 'COMPLETED' ORDER BY o.executionTimeMs ASC")
    List<OptimizationTask> findShortestRunningTasks(Pageable pageable);
    
    /**
     * Calculate average execution time
     */
    @Query("SELECT AVG(o.executionTimeMs) FROM OptimizationTask o WHERE o.status = 'COMPLETED'")
    Double calculateAverageExecutionTime();
    
    /**
     * Find tasks by description containing text
     */
    List<OptimizationTask> findByDescriptionContainingIgnoreCase(String description);
    
    /**
     * Find tasks created today
     */
    @Query("SELECT o FROM OptimizationTask o WHERE o.createdAt >= :startOfDay AND o.createdAt < :endOfDay")
    List<OptimizationTask> findTasksCreatedToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    /**
     * Find tasks completed today
     */
    @Query("SELECT o FROM OptimizationTask o WHERE o.completedAt >= :startOfDay AND o.completedAt < :endOfDay")
    List<OptimizationTask> findTasksCompletedToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}
