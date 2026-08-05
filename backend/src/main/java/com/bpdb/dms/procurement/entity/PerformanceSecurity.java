package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * PerformanceSecurity - procurement lifecycle entity mapped to performance_security.
 */
@Entity
@Table(name = "performance_security")
public class PerformanceSecurity extends BaseProcurementEntity {

    @Column(name = "noa_id")
    private Long noaId;

    @Column(name = "instrument_type")
    private String instrumentType;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "issuing_bank")
    private String issuingBank;

    @Column(name = "validity_date")
    private LocalDate validityDate;

    public Long getNoaId() { return noaId; }
    public void setNoaId(Long noaId) { this.noaId = noaId; }

    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getIssuingBank() { return issuingBank; }
    public void setIssuingBank(String issuingBank) { this.issuingBank = issuingBank; }

    public LocalDate getValidityDate() { return validityDate; }
    public void setValidityDate(LocalDate validityDate) { this.validityDate = validityDate; }
}
