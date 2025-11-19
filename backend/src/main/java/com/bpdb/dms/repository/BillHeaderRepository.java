package com.bpdb.dms.repository;

import com.bpdb.dms.entity.BillHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillHeaderRepository extends JpaRepository<BillHeader, Long> {
}


