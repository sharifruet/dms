package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.ExtractedField;

@Repository
public interface ExtractedFieldRepository extends JpaRepository<ExtractedField, Long> {

    List<ExtractedField> findByPackageIdAndStageCode(Long packageId, Short stageCode);

    List<ExtractedField> findByPackageId(Long packageId);

    List<ExtractedField> findByEntityTypeAndEntityId(String entityType, Long entityId);

    Optional<ExtractedField> findByEntityTypeAndEntityIdAndFieldKey(String entityType, Long entityId, String fieldKey);

    List<ExtractedField> findByDocumentId(Long documentId);

    /**
     * Mandatory fields for a stage that no one has confirmed yet. A non-empty result
     * is what blocks stage completion (WF-03).
     */
    @Query("SELECT f FROM ExtractedField f WHERE f.packageId = :packageId "
         + "AND f.stageCode = :stageCode AND f.isMandatory = true "
         + "AND f.status NOT IN ('VERIFIED', 'MANUAL_OVERRIDE')")
    List<ExtractedField> findUnconfirmedMandatory(@Param("packageId") Long packageId,
                                                  @Param("stageCode") Short stageCode);

    @Query("SELECT COUNT(f) FROM ExtractedField f WHERE f.packageId = :packageId "
         + "AND f.stageCode = :stageCode AND f.status = 'OCR_SUGGESTED'")
    long countPendingVerification(@Param("packageId") Long packageId, @Param("stageCode") Short stageCode);

    @Query("SELECT f FROM ExtractedField f WHERE f.validationState = 'NEEDS_REVIEW' "
         + "OR f.validationState = 'INVALID'")
    List<ExtractedField> findNeedingReview();
}
