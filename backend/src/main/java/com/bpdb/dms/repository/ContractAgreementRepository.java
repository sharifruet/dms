package com.bpdb.dms.repository;

import com.bpdb.dms.entity.ContractAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ContractAgreementRepository extends JpaRepository<ContractAgreement, Long> {
    Optional<ContractAgreement> findByDocumentId(Long documentId);
    boolean existsByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);

    @Query("SELECT COUNT(c) FROM ContractAgreement c WHERE c.contractDate <= :today "
        + "AND (c.expiryDate IS NULL OR c.expiryDate >= :today)")
    long countRunningContracts(@Param("today") LocalDate today);
}
