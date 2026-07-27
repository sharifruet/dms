package com.bpdb.dms.repository;

import com.bpdb.dms.entity.PerformanceSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerformanceSecurityRepository extends JpaRepository<PerformanceSecurity, Long> {
    Optional<PerformanceSecurity> findByDocumentId(Long documentId);
    boolean existsByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
}
