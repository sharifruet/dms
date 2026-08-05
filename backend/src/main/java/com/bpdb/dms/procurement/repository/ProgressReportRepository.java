package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.ProgressReport;

@Repository
public interface ProgressReportRepository extends JpaRepository<ProgressReport, Long> {

    List<ProgressReport> findByProductionScheduleId(Long productionScheduleId);
}
