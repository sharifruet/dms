package com.bpdb.dms.repository;

import com.bpdb.dms.entity.TenderNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenderNoticeRepository extends JpaRepository<TenderNotice, Long> {
    Optional<TenderNotice> findByDocumentId(Long documentId);
    List<TenderNotice> findAllByProcurementPackageNoOrderByCreatedAtDesc(String procurementPackageNo);
    boolean existsByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);

    @Query("SELECT COUNT(t) FROM TenderNotice t WHERE t.tenderDate <= :today AND t.closingDate >= :today")
    long countLiveTenders(@Param("today") LocalDate today);
}
