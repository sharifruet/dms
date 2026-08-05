package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.PackageStage;

@Repository
public interface PackageStageRepository extends JpaRepository<PackageStage, Long> {

    List<PackageStage> findByPackageIdOrderByStageCodeAsc(Long packageId);

    Optional<PackageStage> findByPackageIdAndStageCode(Long packageId, Short stageCode);
}
