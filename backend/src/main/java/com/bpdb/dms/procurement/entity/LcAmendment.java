package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * LcAmendment - procurement lifecycle entity mapped to lc_amendment.
 */
@Entity
@Table(name = "lc_amendment")
public class LcAmendment extends BaseProcurementEntity {

    @Column(name = "lc_id")
    private Long lcId;

    @Column(name = "amendment_no")
    private Integer amendmentNo;

    @Column(name = "amendment_date")
    private LocalDate amendmentDate;

    @Column(name = "revised_amount")
    private BigDecimal revisedAmount;

    @Column(name = "revised_expiry_date")
    private LocalDate revisedExpiryDate;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    public Long getLcId() { return lcId; }
    public void setLcId(Long lcId) { this.lcId = lcId; }

    public Integer getAmendmentNo() { return amendmentNo; }
    public void setAmendmentNo(Integer amendmentNo) { this.amendmentNo = amendmentNo; }

    public LocalDate getAmendmentDate() { return amendmentDate; }
    public void setAmendmentDate(LocalDate amendmentDate) { this.amendmentDate = amendmentDate; }

    public BigDecimal getRevisedAmount() { return revisedAmount; }
    public void setRevisedAmount(BigDecimal revisedAmount) { this.revisedAmount = revisedAmount; }

    public LocalDate getRevisedExpiryDate() { return revisedExpiryDate; }
    public void setRevisedExpiryDate(LocalDate revisedExpiryDate) { this.revisedExpiryDate = revisedExpiryDate; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
