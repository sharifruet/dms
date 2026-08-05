package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * BudgetConsumption - procurement lifecycle entity mapped to budget_consumption.
 */
@Entity
@Table(name = "budget_consumption")
public class BudgetConsumption extends BaseProcurementEntity {

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "consumed_amount")
    private BigDecimal consumedAmount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public BigDecimal getConsumedAmount() { return consumedAmount; }
    public void setConsumedAmount(BigDecimal consumedAmount) { this.consumedAmount = consumedAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
}
