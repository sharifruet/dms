package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Correspondence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CorrespondenceRepository extends JpaRepository<Correspondence, Long> {
    Optional<Correspondence> findByDocumentId(Long documentId);
    boolean existsByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
}
