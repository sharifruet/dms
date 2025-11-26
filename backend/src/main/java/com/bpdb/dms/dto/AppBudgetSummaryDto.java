package com.bpdb.dms.dto;

import java.math.BigDecimal;

/**
 * DTO for per-APP (AppHeader) budget vs billed summary.
 */
public class AppBudgetSummaryDto {

    private Long appId;
    private Integer fiscalYear;
    private Integer releaseInstallmentNo;
    private String allocationType;
    private BigDecimal allocationAmount;

    private BigDecimal totalBilled;
    private BigDecimal remaining;
    private BigDecimal utilizationPct;

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public Integer getReleaseInstallmentNo() {
        return releaseInstallmentNo;
    }

    public void setReleaseInstallmentNo(Integer releaseInstallmentNo) {
        this.releaseInstallmentNo = releaseInstallmentNo;
    }

    public String getAllocationType() {
        return allocationType;
    }

    public void setAllocationType(String allocationType) {
        this.allocationType = allocationType;
    }

    public BigDecimal getAllocationAmount() {
        return allocationAmount;
    }

    public void setAllocationAmount(BigDecimal allocationAmount) {
        this.allocationAmount = allocationAmount;
    }

    public BigDecimal getTotalBilled() {
        return totalBilled;
    }

    public void setTotalBilled(BigDecimal totalBilled) {
        this.totalBilled = totalBilled;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public void setRemaining(BigDecimal remaining) {
        this.remaining = remaining;
    }

    public BigDecimal getUtilizationPct() {
        return utilizationPct;
    }

    public void setUtilizationPct(BigDecimal utilizationPct) {
        this.utilizationPct = utilizationPct;
    }
}


