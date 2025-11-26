package com.bpdb.dms.repository;

import com.bpdb.dms.entity.AppHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppHeaderRepository extends JpaRepository<AppHeader, Long> {
    Optional<AppHeader> findByFiscalYear(Integer fiscalYear);
    
    Optional<AppHeader> findByFiscalYearAndReleaseInstallmentNo(Integer fiscalYear, Integer releaseInstallmentNo);
    
    List<AppHeader> findByFiscalYearOrderByReleaseInstallmentNoAsc(Integer fiscalYear);
    
    @Query("SELECT MAX(a.releaseInstallmentNo) FROM AppHeader a WHERE a.fiscalYear = :fiscalYear")
    Optional<Integer> findMaxInstallmentNoByFiscalYear(@Param("fiscalYear") Integer fiscalYear);
    
    @Query("SELECT DISTINCT a.fiscalYear FROM AppHeader a ORDER BY a.fiscalYear DESC")
    List<Integer> findDistinctFiscalYears();
}


