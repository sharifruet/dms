package com.bpdb.dms.repository;

import com.bpdb.dms.entity.StationeryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationeryRecordRepository extends JpaRepository<StationeryRecord, Long> {
    Optional<StationeryRecord> findByDocumentId(Long documentId);
    boolean existsByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
    List<StationeryRecord> findByEmployeeId(Long employeeId);
}
