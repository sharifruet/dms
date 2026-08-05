package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.BudgetEntry;

@Repository
public interface BudgetEntryRepository extends JpaRepository<BudgetEntry, Long> {

    List<BudgetEntry> findByPackageId(Long packageId);

    List<BudgetEntry> findByPackageIdAndEntryType(Long packageId, String entryType);
}
