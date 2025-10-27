package com.bpdb.dms.repository;

import com.bpdb.dms.entity.AnalyticsData;
import com.bpdb.dms.entity.AnalyticsType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for AnalyticsData entity
 */
@Repository
public interface AnalyticsDataRepository extends JpaRepository<AnalyticsData, Long> {
    
    /**
     * Find analytics data by type
     */
    Page<AnalyticsData> findByAnalyticsType(AnalyticsType analyticsType, Pageable pageable);
    
    /**
     * Find analytics data by metric name
     */
    Page<AnalyticsData> findByMetricName(String metricName, Pageable pageable);
    
    /**
     * Find analytics data by user
     */
    Page<AnalyticsData> findByUserId(Long userId, Pageable pageable);
    
    /**
     * Find analytics data by document
     */
    Page<AnalyticsData> findByDocumentId(Long documentId, Pageable pageable);
    
    /**
     * Find analytics data by department
     */
    Page<AnalyticsData> findByDepartment(String department, Pageable pageable);
    
    /**
     * Find analytics data by time range
     */
    @Query("SELECT ad FROM AnalyticsData ad WHERE ad.timestamp BETWEEN :startTime AND :endTime")
    List<AnalyticsData> findByTimestampBetween(@Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find predicted analytics data
     */
    Page<AnalyticsData> findByIsPredictedTrue(Pageable pageable);
    
    /**
     * Find analytics data by aggregation level
     */
    List<AnalyticsData> findByAggregationLevel(String aggregationLevel);
    
    /**
     * Find analytics data by source system
     */
    Page<AnalyticsData> findBySourceSystem(String sourceSystem, Pageable pageable);
    
    /**
     * Count analytics data by type
     */
    long countByAnalyticsType(AnalyticsType analyticsType);
    
    /**
     * Count predicted analytics data
     */
    long countByIsPredictedTrue();
    
    /**
     * Find analytics data by multiple criteria
     */
    @Query("SELECT ad FROM AnalyticsData ad WHERE " +
           "(:analyticsType IS NULL OR ad.analyticsType = :analyticsType) AND " +
           "(:metricName IS NULL OR ad.metricName = :metricName) AND " +
           "(:userId IS NULL OR ad.userId = :userId) AND " +
           "(:documentId IS NULL OR ad.documentId = :documentId) AND " +
           "(:department IS NULL OR ad.department = :department) AND " +
           "(:isPredicted IS NULL OR ad.isPredicted = :isPredicted)")
    Page<AnalyticsData> findByMultipleCriteria(@Param("analyticsType") AnalyticsType analyticsType,
                                               @Param("metricName") String metricName,
                                               @Param("userId") Long userId,
                                               @Param("documentId") Long documentId,
                                               @Param("department") String department,
                                               @Param("isPredicted") Boolean isPredicted,
                                               Pageable pageable);
    
    /**
     * Get aggregated metrics by time range
     */
    @Query("SELECT ad.metricName, AVG(ad.metricValue), MIN(ad.metricValue), MAX(ad.metricValue), COUNT(ad) " +
           "FROM AnalyticsData ad WHERE ad.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY ad.metricName")
    List<Object[]> getAggregatedMetricsByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                                   @Param("endTime") LocalDateTime endTime);
}
