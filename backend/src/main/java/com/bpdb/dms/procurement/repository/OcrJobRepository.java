package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.OcrJob;

@Repository
public interface OcrJobRepository extends JpaRepository<OcrJob, Long> {

    List<OcrJob> findByDocumentIdOrderByIdDesc(Long documentId);

    List<OcrJob> findByStatus(String status);
}
