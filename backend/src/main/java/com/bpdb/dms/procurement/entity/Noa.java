package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Noa - procurement lifecycle entity mapped to noa.
 */
@Entity
@Table(name = "noa")
public class Noa extends BaseProcurementEntity {

    @Column(name = "approval_id")
    private Long approvalId;

    @Column(name = "noa_date")
    private LocalDate noaDate;

    @Column(name = "ps_amount")
    private BigDecimal psAmount;

    @Column(name = "ps_currency")
    private String psCurrency;

    @Column(name = "ps_percentage")
    private BigDecimal psPercentage;

    @Column(name = "pg_submission_last_date")
    private LocalDate pgSubmissionLastDate;

    @Column(name = "contract_signing_last_date")
    private LocalDate contractSigningLastDate;

    public Long getApprovalId() { return approvalId; }
    public void setApprovalId(Long approvalId) { this.approvalId = approvalId; }

    public LocalDate getNoaDate() { return noaDate; }
    public void setNoaDate(LocalDate noaDate) { this.noaDate = noaDate; }

    public BigDecimal getPsAmount() { return psAmount; }
    public void setPsAmount(BigDecimal psAmount) { this.psAmount = psAmount; }

    public String getPsCurrency() { return psCurrency; }
    public void setPsCurrency(String psCurrency) { this.psCurrency = psCurrency; }

    public BigDecimal getPsPercentage() { return psPercentage; }
    public void setPsPercentage(BigDecimal psPercentage) { this.psPercentage = psPercentage; }

    public LocalDate getPgSubmissionLastDate() { return pgSubmissionLastDate; }
    public void setPgSubmissionLastDate(LocalDate pgSubmissionLastDate) { this.pgSubmissionLastDate = pgSubmissionLastDate; }

    public LocalDate getContractSigningLastDate() { return contractSigningLastDate; }
    public void setContractSigningLastDate(LocalDate contractSigningLastDate) { this.contractSigningLastDate = contractSigningLastDate; }
}
