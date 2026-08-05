package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Delivery - procurement lifecycle entity mapped to delivery.
 */
@Entity
@Table(name = "delivery")
public class Delivery extends BaseProcurementEntity {

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "inspection_event_id")
    private Long inspectionEventId;

    @Column(name = "delivery_reference_number")
    private String deliveryReferenceNumber;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "delivered_quantity")
    private BigDecimal deliveredQuantity;

    @Column(name = "is_final")
    private Boolean isFinal;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }

    public Long getInspectionEventId() { return inspectionEventId; }
    public void setInspectionEventId(Long inspectionEventId) { this.inspectionEventId = inspectionEventId; }

    public String getDeliveryReferenceNumber() { return deliveryReferenceNumber; }
    public void setDeliveryReferenceNumber(String deliveryReferenceNumber) { this.deliveryReferenceNumber = deliveryReferenceNumber; }

    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }

    public BigDecimal getDeliveredQuantity() { return deliveredQuantity; }
    public void setDeliveredQuantity(BigDecimal deliveredQuantity) { this.deliveredQuantity = deliveredQuantity; }

    public Boolean getIsFinal() { return isFinal; }
    public void setIsFinal(Boolean isFinal) { this.isFinal = isFinal; }
}
