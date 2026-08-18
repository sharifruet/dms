package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.DeadlineAlertState;

@Repository
public interface DeadlineAlertStateRepository extends JpaRepository<DeadlineAlertState, Long> {

    Optional<DeadlineAlertState> findByPackageIdAndDeadlineKey(Long packageId, String deadlineKey);

    List<DeadlineAlertState> findByPackageId(Long packageId);

    List<DeadlineAlertState> findBySatisfiedFalse();
}
