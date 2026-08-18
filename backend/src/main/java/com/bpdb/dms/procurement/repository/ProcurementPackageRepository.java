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

    // CAST(:search AS string) is load-bearing on PostgreSQL, not decoration. An unfilterd
    // listing passes null for every filter, and PostgreSQL will not infer the type of a
    // null parameter: it settles on bytea, then fails with "function lower(bytea) does not
    // exist" and takes the whole listing down with it. The cast tells it the parameter is
    // text. H2 never needed telling, which is why the unit tests were happy.
    @Query("SELECT p FROM ProcurementPackage p WHERE "
         + "(CAST(:search AS string) IS NULL "
         + "   OR LOWER(p.packageNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
         + "   OR LOWER(p.packageDescription) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) "
         + "AND (CAST(:stage AS short) IS NULL OR p.currentStage = :stage) "
         + "AND (CAST(:status AS string) IS NULL OR p.status = :status) "
         + "AND (CAST(:department AS string) IS NULL OR p.department = :department)")
    Page<ProcurementPackage> search(@Param("search") String search,
                                    @Param("stage") Short stage,
                                    @Param("status") String status,
                                    @Param("department") String department,
                                    Pageable pageable);

    @Query("SELECT p.currentStage, COUNT(p) FROM ProcurementPackage p "
         + "WHERE p.status = 'ACTIVE' GROUP BY p.currentStage ORDER BY p.currentStage")
    List<Object[]> countByStage();
}
