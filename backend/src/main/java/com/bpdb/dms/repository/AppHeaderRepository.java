package com.bpdb.dms.repository;

import com.bpdb.dms.entity.AppHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppHeaderRepository extends JpaRepository<AppHeader, Long> {
    Optional<AppHeader> findByFiscalYear(Integer fiscalYear);
}


