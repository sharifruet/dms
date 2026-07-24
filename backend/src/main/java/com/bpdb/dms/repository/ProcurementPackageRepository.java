package com.bpdb.dms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.entity.ProcurementPackage;

@Repository
public interface ProcurementPackageRepository extends JpaRepository<ProcurementPackage, Long> {

}
