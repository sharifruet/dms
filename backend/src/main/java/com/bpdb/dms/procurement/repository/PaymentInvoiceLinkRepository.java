package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.PaymentInvoiceLink;

@Repository
public interface PaymentInvoiceLinkRepository extends JpaRepository<PaymentInvoiceLink, Long> {

    List<PaymentInvoiceLink> findByPaymentId(Long paymentId);

    List<PaymentInvoiceLink> findByInvoiceId(Long invoiceId);
}
