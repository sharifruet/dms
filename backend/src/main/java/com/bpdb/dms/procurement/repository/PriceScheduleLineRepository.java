package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.PriceScheduleLine;

@Repository
public interface PriceScheduleLineRepository extends JpaRepository<PriceScheduleLine, Long> {

    List<PriceScheduleLine> findByScheduleIdOrderByLineNoAsc(Long scheduleId);
}
