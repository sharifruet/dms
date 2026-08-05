package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.TenderOpening;

@Repository
public interface TenderOpeningRepository extends JpaRepository<TenderOpening, Long> {

    Optional<TenderOpening> findByTenderId(Long tenderId);
}
