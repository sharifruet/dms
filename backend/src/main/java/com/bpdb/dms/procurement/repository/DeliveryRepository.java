package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.Delivery;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByContractId(Long contractId);

    Optional<Delivery> findByContractIdAndDeliveryReferenceNumber(Long contractId, String deliveryReferenceNumber);
}
