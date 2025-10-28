package com.bpdb.dms.repository;

import com.bpdb.dms.entity.SystemMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for SystemMetric entity
 */
@Repository
public interface SystemMetricRepository extends JpaRepository<SystemMetric, Long> {
    
    /**
     * Find metrics by name
     */
    List<SystemMetric> findByMetricName(String metricName);
    
    /**
     * Find metrics by name and time range
     */
    List<SystemMetric> findByMetricNameAndCollectedAtBetween(
        String metricName, 
        LocalDateTime startTime, 
        LocalDateTime endTime
    );
    
    /**
     * Find metrics by status
     */
    List<SystemMetric> findByStatus(String status);
    
    /**
     * Find latest metrics
     */
    List<SystemMetric> findTop100ByOrderByCollectedAtDesc();
}

