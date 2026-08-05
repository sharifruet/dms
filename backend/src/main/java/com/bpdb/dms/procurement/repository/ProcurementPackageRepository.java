package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.ProcurementPackage;

@Repository
public interface ProcurementPackageRepository extends JpaRepository<ProcurementPackage, Long> {

    Optional<ProcurementPackage> findByPackageNumber(String packageNumber);

    boolean existsByPackageNumber(String packageNumber);

    List<ProcurementPackage> findByAppLineId(Long appLineId);

    List<ProcurementPackage> findByStatus(String status);

    @Query("SELECT p FROM ProcurementPackage p WHERE "
         + "(:search IS NULL OR LOWER(p.packageNumber) LIKE LOWER(CONCAT('%', :search, '%')) "
         + "   OR LOWER(p.packageDescription) LIKE LOWER(CONCAT('%', :search, '%'))) "
         + "AND (:stage IS NULL OR p.currentStage = :stage) "
         + "AND (:status IS NULL OR p.status = :status) "
         + "AND (:department IS NULL OR p.department = :department)")
    Page<ProcurementPackage> search(@Param("search") String search,
                                    @Param("stage") Short stage,
                                    @Param("status") String status,
                                    @Param("department") String department,
                                    Pageable pageable);

    @Query("SELECT p.currentStage, COUNT(p) FROM ProcurementPackage p "
         + "WHERE p.status = 'ACTIVE' GROUP BY p.currentStage ORDER BY p.currentStage")
    List<Object[]> countByStage();
}
