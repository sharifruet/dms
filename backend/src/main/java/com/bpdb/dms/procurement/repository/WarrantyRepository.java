package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.Warranty;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Long> {

    Optional<Warranty> findByContractId(Long contractId);
}
