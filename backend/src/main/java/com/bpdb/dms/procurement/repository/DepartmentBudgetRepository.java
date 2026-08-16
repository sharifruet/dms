package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.DepartmentBudget;

@Repository
public interface DepartmentBudgetRepository extends JpaRepository<DepartmentBudget, Long> {

    Optional<DepartmentBudget> findByFiscalYearAndDepartment(Integer fiscalYear, String department);

    List<DepartmentBudget> findByFiscalYearOrderByDepartmentAsc(Integer fiscalYear);
}
