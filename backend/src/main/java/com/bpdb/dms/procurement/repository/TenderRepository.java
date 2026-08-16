package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.Tender;

@Repository
public interface TenderRepository extends JpaRepository<Tender, Long> {

    /**
     * The live tender for a package. Everything downstream of Stage 2 resolves through
     * this - superseded attempts stay readable but never drive a stage gate (REQ-L15).
     */
    Optional<Tender> findByPackageIdAndIsCurrentTrue(Long packageId);

    /** Every attempt, newest first - the re-tender history shown on the Stage 2 panel. */
    List<Tender> findByPackageIdOrderByAttemptNoDesc(Long packageId);

    Optional<Tender> findByPackageIdAndAttemptNo(Long packageId, Integer attemptNo);
}
