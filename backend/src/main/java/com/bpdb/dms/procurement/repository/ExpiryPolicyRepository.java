package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.ExpiryPolicy;

@Repository
public interface ExpiryPolicyRepository extends JpaRepository<ExpiryPolicy, Long> {

    Optional<ExpiryPolicy> findByEntityTypeAndIsActiveTrue(String entityType);

    List<ExpiryPolicy> findByIsActiveTrueOrderByEntityTypeAsc();
}
