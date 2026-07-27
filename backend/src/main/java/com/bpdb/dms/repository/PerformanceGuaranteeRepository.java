package com.bpdb.dms.repository;

import com.bpdb.dms.entity.PerformanceGuarantee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerformanceGuaranteeRepository extends JpaRepository<PerformanceGuarantee, Long> {
    Optional<PerformanceGuarantee> findByDocumentId(Long documentId);
    boolean existsByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
}
