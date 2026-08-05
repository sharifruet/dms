package com.bpdb.dms.procurement.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Warranty - procurement lifecycle entity mapped to warranty.
 */
@Entity
@Table(name = "warranty")
public class Warranty extends BaseProcurementEntity {

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "final_delivery_id")
    private Long finalDeliveryId;

    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "acceptance_certificate_doc_id")
    private Long acceptanceCertificateDocId;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }

    public Long getFinalDeliveryId() { return finalDeliveryId; }
    public void setFinalDeliveryId(Long finalDeliveryId) { this.finalDeliveryId = finalDeliveryId; }

    public LocalDate getWarrantyStartDate() { return warrantyStartDate; }
    public void setWarrantyStartDate(LocalDate warrantyStartDate) { this.warrantyStartDate = warrantyStartDate; }

    public LocalDate getWarrantyEndDate() { return warrantyEndDate; }
    public void setWarrantyEndDate(LocalDate warrantyEndDate) { this.warrantyEndDate = warrantyEndDate; }

    public Long getAcceptanceCertificateDocId() { return acceptanceCertificateDocId; }
    public void setAcceptanceCertificateDocId(Long acceptanceCertificateDocId) { this.acceptanceCertificateDocId = acceptanceCertificateDocId; }
}
