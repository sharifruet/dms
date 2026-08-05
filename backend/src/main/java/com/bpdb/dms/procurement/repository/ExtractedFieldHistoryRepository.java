package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.ExtractedFieldHistory;

@Repository
public interface ExtractedFieldHistoryRepository extends JpaRepository<ExtractedFieldHistory, Long> {

    List<ExtractedFieldHistory> findByExtractedFieldIdOrderByVersionDesc(Long extractedFieldId);

    long countByExtractedFieldId(Long extractedFieldId);
}
