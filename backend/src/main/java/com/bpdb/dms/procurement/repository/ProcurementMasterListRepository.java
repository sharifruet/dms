package com.bpdb.dms.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.ProcurementMasterList;

@Repository
public interface ProcurementMasterListRepository extends JpaRepository<ProcurementMasterList, Long> {

    List<ProcurementMasterList> findByListKeyAndIsActiveTrueOrderByDisplayOrderAsc(String listKey);

    List<ProcurementMasterList> findByIsActiveTrueOrderByListKeyAscDisplayOrderAsc();
}
