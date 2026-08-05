package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.PerformanceSecurity;

@Repository
public interface PerformanceSecurityRepository extends JpaRepository<PerformanceSecurity, Long> {

    List<PerformanceSecurity> findByNoaId(Long noaId);
}
