package com.bpdb.dms.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.entity.Analytics;
import com.bpdb.dms.entity.MetricType;

/**
 * Repository interface for Analytics entity
 */
@Repository
public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {
    
    /**
     * Find analytics by metric type
     */
    List<Analytics> findByMetricType(MetricType metricType);
    
    /**
     * Find analytics by metric type (pageable)
     */
    Page<Analytics> findByMetricType(MetricType metricType, Pageable pageable);
    
    /**
     * Find analytics by metric name
     */
    List<Analytics> findByMetricName(String metricName);
    
    /**
     * Find analytics by metric name and created at between
     */
    List<Analytics> findByMetricNameAndCreatedAtBetween(String metricName, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find analytics by metric name and created at between (pageable)
     */
    Page<Analytics> findByMetricNameAndCreatedAtBetween(String metricName, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find analytics by metric type and created at between
     */
    List<Analytics> findByMetricTypeAndCreatedAtBetween(MetricType metricType, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find analytics by metric type and created at between (pageable)
     */
    Page<Analytics> findByMetricTypeAndCreatedAtBetween(MetricType metricType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find analytics by metric type and metric name and created at between
     */
    Page<Analytics> findByMetricTypeAndMetricNameAndCreatedAtBetween(MetricType metricType, String metricName, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find analytics by dimension
     */
    List<Analytics> findByDimensionKeyAndDimensionValue(String dimensionKey, String dimensionValue);
    
    /**
     * Find analytics within time period
     */
    @Query("SELECT a FROM Analytics a WHERE a.periodStart >= :startDate AND a.periodEnd <= :endDate")
    List<Analytics> findByPeriodBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find latest analytics by metric type
     */
    @Query("SELECT a FROM Analytics a WHERE a.metricType = :metricType ORDER BY a.createdAt DESC")
    List<Analytics> findLatestByMetricType(@Param("metricType") MetricType metricType, Pageable pageable);
    
    /**
     * Find analytics by metric type and dimension
     */
    @Query("SELECT a FROM Analytics a WHERE a.metricType = :metricType AND a.dimensionKey = :dimensionKey ORDER BY a.metricValue DESC")
    List<Analytics> findByMetricTypeAndDimension(@Param("metricType") MetricType metricType, @Param("dimensionKey") String dimensionKey);
    
    /**
     * Get aggregated metrics by dimension
     */
    @Query("SELECT a.dimensionValue, SUM(a.metricValue) FROM Analytics a WHERE a.metricType = :metricType AND a.dimensionKey = :dimensionKey GROUP BY a.dimensionValue")
    List<Object[]> getAggregatedMetricsByDimension(@Param("metricType") MetricType metricType, @Param("dimensionKey") String dimensionKey);
    
    /**
     * Get time series data
     */
    @Query("SELECT a FROM Analytics a WHERE a.metricType = :metricType AND a.periodStart >= :startDate ORDER BY a.periodStart ASC")
    List<Analytics> getTimeSeriesData(@Param("metricType") MetricType metricType, @Param("startDate") LocalDateTime startDate);
    
    /**
     * Get top metrics by value
     */
    @Query("SELECT a FROM Analytics a WHERE a.metricType = :metricType ORDER BY a.metricValue DESC")
    List<Analytics> getTopMetricsByValue(@Param("metricType") MetricType metricType, Pageable pageable);
    
    /**
     * Count analytics by metric type
     */
    long countByMetricType(MetricType metricType);
    
    /**
     * Get average metric value by type
     */
    @Query("SELECT AVG(a.metricValue) FROM Analytics a WHERE a.metricType = :metricType")
    Double getAverageMetricValue(@Param("metricType") MetricType metricType);
    
    /**
     * Get sum of metric values by type
     */
    @Query("SELECT SUM(a.metricValue) FROM Analytics a WHERE a.metricType = :metricType")
    Double getSumOfMetricValues(@Param("metricType") MetricType metricType);
    
    /**
     * Delete old analytics data
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Analytics a WHERE a.createdAt < :cutoffDate")
    void deleteOldAnalytics(@Param("cutoffDate") LocalDateTime cutoffDate);
}
