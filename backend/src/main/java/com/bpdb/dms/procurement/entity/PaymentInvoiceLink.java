package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Many-to-many payment/invoice link carrying the apportioned amount so part payments reconcile (REQ-L7).
 */
@Entity
@Table(name = "payment_invoice_link")
public class PaymentInvoiceLink extends BaseProcurementEntity {

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "apportioned_amount")
    private BigDecimal apportionedAmount;

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public BigDecimal getApportionedAmount() { return apportionedAmount; }
    public void setApportionedAmount(BigDecimal apportionedAmount) { this.apportionedAmount = apportionedAmount; }
}
