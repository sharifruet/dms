package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Many-to-many invoice/delivery link carrying the apportioned amount so partial billing reconciles (REQ-L7).
 */
@Entity
@Table(name = "invoice_delivery_link")
public class InvoiceDeliveryLink extends BaseProcurementEntity {

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "delivery_id", nullable = false)
    private Long deliveryId;

    @Column(name = "apportioned_amount")
    private BigDecimal apportionedAmount;

    @Column(name = "apportioned_quantity")
    private BigDecimal apportionedQuantity;

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public Long getDeliveryId() { return deliveryId; }
    public void setDeliveryId(Long deliveryId) { this.deliveryId = deliveryId; }

    public BigDecimal getApportionedAmount() { return apportionedAmount; }
    public void setApportionedAmount(BigDecimal apportionedAmount) { this.apportionedAmount = apportionedAmount; }

    public BigDecimal getApportionedQuantity() { return apportionedQuantity; }
    public void setApportionedQuantity(BigDecimal apportionedQuantity) { this.apportionedQuantity = apportionedQuantity; }
}
