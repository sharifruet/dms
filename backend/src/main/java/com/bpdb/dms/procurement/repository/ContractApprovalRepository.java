package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.ContractApproval;

@Repository
public interface ContractApprovalRepository extends JpaRepository<ContractApproval, Long> {

    Optional<ContractApproval> findByPackageId(Long packageId);
}
