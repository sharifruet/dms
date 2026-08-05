package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.OcrResult;

@Repository
public interface OcrResultRepository extends JpaRepository<OcrResult, Long> {

    Optional<OcrResult> findByDocumentIdAndIsCurrentTrue(Long documentId);

    List<OcrResult> findByDocumentIdOrderByVersionDesc(Long documentId);
}
