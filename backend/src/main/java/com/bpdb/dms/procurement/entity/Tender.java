package com.bpdb.dms.procurement.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Tender - procurement lifecycle entity mapped to tender.
 *
 * A package may hold several tenders: a failed tender is re-tendered as a new attempt
 * under the same package, with the previous attempt kept as history (Q-2, REQ-L14).
 * Exactly one attempt is current at a time, and Stages 3-7 always resolve to it.
 */
@Entity
@Table(name = "tender")
public class Tender extends BaseProcurementEntity {

    /** Procurement Type values that make a Letter of Credit applicable (Q-5, REQ-2.5). */
    public static final String TYPE_ICT = "ICT";

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "attempt_no")
    private Integer attemptNo = 1;

    @Column(name = "is_current")
    private Boolean isCurrent = Boolean.TRUE;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "procurement_type")
    private String procurementType;

    @Column(name = "procurement_method")
    private String procurementMethod;

    @Column(name = "procurement_nature")
    private String procurementNature;

    @Column(name = "opening_date")
    private LocalDate openingDate;

    @Column(name = "closing_date")
    private LocalDate closingDate;

    @Column(name = "tender_validity_days")
    private Integer tenderValidityDays;

    @Column(name = "tender_validity_date")
    private LocalDate tenderValidityDate;

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public Integer getAttemptNo() { return attemptNo; }
    public void setAttemptNo(Integer attemptNo) { this.attemptNo = attemptNo; }

    public Boolean getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    /** True when this tender is an international competitive tender, which may need an LC. */
    public boolean isInternational() {
        return TYPE_ICT.equalsIgnoreCase(procurementType == null ? null : procurementType.trim());
    }

    public String getProcurementType() { return procurementType; }
    public void setProcurementType(String procurementType) { this.procurementType = procurementType; }

    public String getProcurementMethod() { return procurementMethod; }
    public void setProcurementMethod(String procurementMethod) { this.procurementMethod = procurementMethod; }

    public String getProcurementNature() { return procurementNature; }
    public void setProcurementNature(String procurementNature) { this.procurementNature = procurementNature; }

    public LocalDate getOpeningDate() { return openingDate; }
    public void setOpeningDate(LocalDate openingDate) { this.openingDate = openingDate; }

    public LocalDate getClosingDate() { return closingDate; }
    public void setClosingDate(LocalDate closingDate) { this.closingDate = closingDate; }

    public Integer getTenderValidityDays() { return tenderValidityDays; }
    public void setTenderValidityDays(Integer tenderValidityDays) { this.tenderValidityDays = tenderValidityDays; }

    public LocalDate getTenderValidityDate() { return tenderValidityDate; }
    public void setTenderValidityDate(LocalDate tenderValidityDate) { this.tenderValidityDate = tenderValidityDate; }
}
