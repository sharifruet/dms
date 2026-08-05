package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.Noa;

@Repository
public interface NoaRepository extends JpaRepository<Noa, Long> {

    Optional<Noa> findByApprovalId(Long approvalId);
}
