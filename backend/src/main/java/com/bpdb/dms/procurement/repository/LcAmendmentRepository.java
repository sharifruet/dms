package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.LcAmendment;

@Repository
public interface LcAmendmentRepository extends JpaRepository<LcAmendment, Long> {

    List<LcAmendment> findByLcIdOrderByAmendmentNoAsc(Long lcId);
}
