package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.InvoiceDeliveryLink;

@Repository
public interface InvoiceDeliveryLinkRepository extends JpaRepository<InvoiceDeliveryLink, Long> {

    List<InvoiceDeliveryLink> findByInvoiceId(Long invoiceId);

    List<InvoiceDeliveryLink> findByDeliveryId(Long deliveryId);
}
