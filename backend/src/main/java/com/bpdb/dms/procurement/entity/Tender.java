package com.bpdb.dms.procurement.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Tender - procurement lifecycle entity mapped to tender.
 */
@Entity
@Table(name = "tender")
public class Tender extends BaseProcurementEntity {

    @Column(name = "package_id")
    private Long packageId;

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
