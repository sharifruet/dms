package com.bpdb.dms.repository;

import com.bpdb.dms.entity.MLModel;
import com.bpdb.dms.entity.MLPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for MLPrediction entity
 */
@Repository
public interface MLPredictionRepository extends JpaRepository<MLPrediction, Long> {
    
    /**
     * Find predictions by model
     */
    List<MLPrediction> findByModelOrderByCreatedAtDesc(MLModel model);
    
    /**
     * Find predictions by model and date range
     */
    List<MLPrediction> findByModelAndCreatedAtBetween(MLModel model, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Count predictions by model
     */
    long countByModel(MLModel model);
}

