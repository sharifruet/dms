package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * BudgetEntry - procurement lifecycle entity mapped to budget_entry.
 */
@Entity
@Table(name = "budget_entry")
public class BudgetEntry extends BaseProcurementEntity {

    @Column(name = "package_id")
    private Long packageId;

    /** The annual departmental budget this line draws down from (Q-13, REQ-B0). */
    @Column(name = "department_budget_id")
    private Long departmentBudgetId;

    @Column(name = "entry_type")
    private String entryType;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "created_by")
    private Long createdBy;

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public Long getDepartmentBudgetId() { return departmentBudgetId; }
    public void setDepartmentBudgetId(Long departmentBudgetId) { this.departmentBudgetId = departmentBudgetId; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
