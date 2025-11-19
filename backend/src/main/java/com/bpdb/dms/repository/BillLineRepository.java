package com.bpdb.dms.repository;

import com.bpdb.dms.entity.BillLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillLineRepository extends JpaRepository<BillLine, Long> {
}


