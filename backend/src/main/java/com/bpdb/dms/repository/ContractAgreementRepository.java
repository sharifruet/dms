package com.bpdb.dms.repository;

import com.bpdb.dms.entity.ContractAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractAgreementRepository extends JpaRepository<ContractAgreement, Long> {
    Optional<ContractAgreement> findByDocumentId(Long documentId);
    boolean existsByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
}
