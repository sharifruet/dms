package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Payment - procurement lifecycle entity mapped to payment.
 */
@Entity
@Table(name = "payment")
public class Payment extends BaseProcurementEntity {

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "voucher_number")
    private String voucherNumber;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_amount")
    private BigDecimal paymentAmount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "bank_advice_ref")
    private String bankAdviceRef;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }

    public String getVoucherNumber() { return voucherNumber; }
    public void setVoucherNumber(String voucherNumber) { this.voucherNumber = voucherNumber; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getBankAdviceRef() { return bankAdviceRef; }
    public void setBankAdviceRef(String bankAdviceRef) { this.bankAdviceRef = bankAdviceRef; }
}
