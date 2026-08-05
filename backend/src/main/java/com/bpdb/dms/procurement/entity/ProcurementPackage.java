package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The root aggregate of the procurement lifecycle. Every document, metadata value,
 * budget entry and expiry tracker resolves to a package (REQ-L1, REQ-L2).
 *
 * One APP line may produce several packages when a procurement is tendered in lots
 * (client answer Q-1), so app_line_id is not unique.
 */
@Entity
@Table(name = "procurement_package")
public class ProcurementPackage extends BaseProcurementEntity {

    @Column(name = "app_line_id")
    private Long appLineId;

    @Column(name = "package_number", nullable = false, unique = true)
    private String packageNumber;

    @Column(name = "lot_number")
    private String lotNumber;

    @Column(name = "lot_description")
    private String lotDescription;

    @Column(name = "package_description", columnDefinition = "TEXT")
    private String packageDescription;

    @Column(name = "approving_authority")
    private String approvingAuthority;

    @Column(name = "price_lac_bdt")
    private BigDecimal priceLacBdt;

    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    @Column(name = "department")
    private String department;

    /** Highest stage the package has reached, 1..16. */
    @Column(name = "current_stage", nullable = false)
    private Short currentStage = 1;

    /** ACTIVE | CLOSED | CANCELLED */
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    public Long getAppLineId() { return appLineId; }
    public void setAppLineId(Long appLineId) { this.appLineId = appLineId; }
    public String getPackageNumber() { return packageNumber; }
    public void setPackageNumber(String packageNumber) { this.packageNumber = packageNumber; }
    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }
    public String getLotDescription() { return lotDescription; }
    public void setLotDescription(String lotDescription) { this.lotDescription = lotDescription; }
    public String getPackageDescription() { return packageDescription; }
    public void setPackageDescription(String packageDescription) { this.packageDescription = packageDescription; }
    public String getApprovingAuthority() { return approvingAuthority; }
    public void setApprovingAuthority(String approvingAuthority) { this.approvingAuthority = approvingAuthority; }
    public BigDecimal getPriceLacBdt() { return priceLacBdt; }
    public void setPriceLacBdt(BigDecimal priceLacBdt) { this.priceLacBdt = priceLacBdt; }
    public Integer getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(Integer fiscalYear) { this.fiscalYear = fiscalYear; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Short getCurrentStage() { return currentStage; }
    public void setCurrentStage(Short currentStage) { this.currentStage = currentStage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
