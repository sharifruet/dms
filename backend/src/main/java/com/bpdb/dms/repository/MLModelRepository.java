package com.bpdb.dms.repository;

import com.bpdb.dms.entity.MLModel;
import com.bpdb.dms.entity.ModelStatus;
import com.bpdb.dms.entity.ModelType;
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
 * Repository interface for MLModel entity
 */
@Repository
public interface MLModelRepository extends JpaRepository<MLModel, Long> {
    
    /**
     * Find ML models by type
     */
    Page<MLModel> findByModelType(ModelType modelType, Pageable pageable);
    
    /**
     * Find ML models by status
     */
    Page<MLModel> findByStatus(ModelStatus status, Pageable pageable);
    
    /**
     * Find ML models by type and status
     */
    Page<MLModel> findByModelTypeAndStatus(ModelType modelType, ModelStatus status, Pageable pageable);
    
    /**
     * Find active ML models
     */
    List<MLModel> findByIsActiveTrue();
    
    /**
     * Find ML models by creator
     */
    Page<MLModel> findByCreatedBy(User createdBy, Pageable pageable);
    
    /**
     * Find ML models by version
     */
    List<MLModel> findByVersion(String version);
    
    /**
     * Find ML models due for retraining
     */
    @Query("SELECT ml FROM MLModel ml WHERE ml.nextRetrainAt <= :currentTime AND ml.autoRetrain = true AND ml.isActive = true")
    List<MLModel> findMLModelsDueForRetraining(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find ML models by accuracy range
     */
    @Query("SELECT ml FROM MLModel ml WHERE ml.accuracyScore BETWEEN :minAccuracy AND :maxAccuracy")
    List<MLModel> findByAccuracyScoreBetween(@Param("minAccuracy") Double minAccuracy, 
                                            @Param("maxAccuracy") Double maxAccuracy);
    
    /**
     * Find best performing ML models
     */
    @Query("SELECT ml FROM MLModel ml WHERE ml.isActive = true ORDER BY ml.accuracyScore DESC")
    List<MLModel> findBestPerformingModels(Pageable pageable);
    
    /**
     * Count ML models by status
     */
    long countByStatus(ModelStatus status);
    
    /**
     * Count active ML models
     */
    long countByIsActiveTrue();
    
    /**
     * Find ML models by multiple criteria
     */
    @Query("SELECT ml FROM MLModel ml WHERE " +
           "(:modelType IS NULL OR ml.modelType = :modelType) AND " +
           "(:status IS NULL OR ml.status = :status) AND " +
           "(:isActive IS NULL OR ml.isActive = :isActive) AND " +
           "(:createdBy IS NULL OR ml.createdBy = :createdBy)")
    Page<MLModel> findByMultipleCriteria(@Param("modelType") ModelType modelType,
                                         @Param("status") ModelStatus status,
                                         @Param("isActive") Boolean isActive,
                                         @Param("createdBy") User createdBy,
                                         Pageable pageable);
}
