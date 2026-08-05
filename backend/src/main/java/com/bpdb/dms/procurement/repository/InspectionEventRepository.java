package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.InspectionEvent;

@Repository
public interface InspectionEventRepository extends JpaRepository<InspectionEvent, Long> {

    List<InspectionEvent> findByContractId(Long contractId);
}
