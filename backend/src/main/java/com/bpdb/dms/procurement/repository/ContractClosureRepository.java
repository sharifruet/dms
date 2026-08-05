package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.ContractClosure;

@Repository
public interface ContractClosureRepository extends JpaRepository<ContractClosure, Long> {

    Optional<ContractClosure> findByContractId(Long contractId);
}
