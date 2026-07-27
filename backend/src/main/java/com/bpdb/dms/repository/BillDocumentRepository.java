package com.bpdb.dms.repository;

import com.bpdb.dms.entity.BillDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillDocumentRepository extends JpaRepository<BillDocument, Long> {
    Optional<BillDocument> findByDocumentId(Long documentId);
    boolean existsByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
}
