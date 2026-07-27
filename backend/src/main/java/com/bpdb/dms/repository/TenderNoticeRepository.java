package com.bpdb.dms.repository;

import com.bpdb.dms.entity.TenderNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenderNoticeRepository extends JpaRepository<TenderNotice, Long> {
    Optional<TenderNotice> findByDocumentId(Long documentId);
    boolean existsByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
}
