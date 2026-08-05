package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.Invoice;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByContractId(Long contractId);

    Optional<Invoice> findByContractIdAndSupplierNameAndInvoiceNumber(Long contractId, String supplierName, String invoiceNumber);
}
