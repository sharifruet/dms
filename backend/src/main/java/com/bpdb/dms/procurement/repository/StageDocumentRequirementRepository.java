package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.StageDocumentRequirement;

@Repository
public interface StageDocumentRequirementRepository extends JpaRepository<StageDocumentRequirement, Long> {

    List<StageDocumentRequirement> findByStageCodeAndIsActiveTrueOrderByDisplayOrderAsc(Short stageCode);

    List<StageDocumentRequirement> findByIsActiveTrueOrderByStageCodeAscDisplayOrderAsc();
}
