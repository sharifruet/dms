package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.BidSecurity;

@Repository
public interface BidSecurityRepository extends JpaRepository<BidSecurity, Long> {

    List<BidSecurity> findByOpeningId(Long openingId);
}
