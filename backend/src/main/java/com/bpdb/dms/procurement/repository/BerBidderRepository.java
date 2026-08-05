package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.BerBidder;

@Repository
public interface BerBidderRepository extends JpaRepository<BerBidder, Long> {

    List<BerBidder> findByEvaluationIdOrderByBidRankAsc(Long evaluationId);

    Optional<BerBidder> findByEvaluationIdAndIsAwardedTrue(Long evaluationId);

    long countByEvaluationId(Long evaluationId);
}
