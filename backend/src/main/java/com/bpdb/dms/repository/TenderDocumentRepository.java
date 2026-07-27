package com.bpdb.dms.repository;

import com.bpdb.dms.entity.TenderDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenderDocumentRepository extends JpaRepository<TenderDocument, Long> {
    Optional<TenderDocument> findByDocumentId(Long documentId);
    boolean existsByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
}
