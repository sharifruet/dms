package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.LetterOfCredit;

@Repository
public interface LetterOfCreditRepository extends JpaRepository<LetterOfCredit, Long> {

    List<LetterOfCredit> findByContractId(Long contractId);
}
