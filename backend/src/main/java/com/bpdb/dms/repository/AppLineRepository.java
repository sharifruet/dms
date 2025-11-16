package com.bpdb.dms.repository;

import com.bpdb.dms.entity.AppLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppLineRepository extends JpaRepository<AppLine, Long> {
}


