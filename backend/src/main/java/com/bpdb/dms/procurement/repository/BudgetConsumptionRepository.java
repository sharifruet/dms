package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.BudgetConsumption;

@Repository
public interface BudgetConsumptionRepository extends JpaRepository<BudgetConsumption, Long> {

    List<BudgetConsumption> findByPackageId(Long packageId);

    List<BudgetConsumption> findByInvoiceId(Long invoiceId);
}
