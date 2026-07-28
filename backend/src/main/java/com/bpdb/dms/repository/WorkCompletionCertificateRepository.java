package com.bpdb.dms.repository;

import com.bpdb.dms.entity.WorkCompletionCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkCompletionCertificateRepository extends JpaRepository<WorkCompletionCertificate, Long> {
    Optional<WorkCompletionCertificate> findByDocumentId(Long documentId);
    boolean existsByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
}
