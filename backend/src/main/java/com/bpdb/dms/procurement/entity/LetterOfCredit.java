package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * LetterOfCredit - procurement lifecycle entity mapped to letter_of_credit.
 */
@Entity
@Table(name = "letter_of_credit")
public class LetterOfCredit extends BaseProcurementEntity {

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "lc_number")
    private String lcNumber;

    @Column(name = "lc_amount")
    private BigDecimal lcAmount;

    @Column(name = "lc_currency")
    private String lcCurrency;

    @Column(name = "lc_opening_date")
    private LocalDate lcOpeningDate;

    @Column(name = "lc_expiry_date")
    private LocalDate lcExpiryDate;

    @Column(name = "issuing_bank")
    private String issuingBank;

    @Column(name = "advising_bank")
    private String advisingBank;

    @Column(name = "is_current")
    private Boolean isCurrent;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }

    public String getLcNumber() { return lcNumber; }
    public void setLcNumber(String lcNumber) { this.lcNumber = lcNumber; }

    public BigDecimal getLcAmount() { return lcAmount; }
    public void setLcAmount(BigDecimal lcAmount) { this.lcAmount = lcAmount; }

    public String getLcCurrency() { return lcCurrency; }
    public void setLcCurrency(String lcCurrency) { this.lcCurrency = lcCurrency; }

    public LocalDate getLcOpeningDate() { return lcOpeningDate; }
    public void setLcOpeningDate(LocalDate lcOpeningDate) { this.lcOpeningDate = lcOpeningDate; }

    public LocalDate getLcExpiryDate() { return lcExpiryDate; }
    public void setLcExpiryDate(LocalDate lcExpiryDate) { this.lcExpiryDate = lcExpiryDate; }

    public String getIssuingBank() { return issuingBank; }
    public void setIssuingBank(String issuingBank) { this.issuingBank = issuingBank; }

    public String getAdvisingBank() { return advisingBank; }
    public void setAdvisingBank(String advisingBank) { this.advisingBank = advisingBank; }

    public Boolean getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }
}
