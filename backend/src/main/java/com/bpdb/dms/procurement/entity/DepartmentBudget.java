package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The annual budget a department holds, from which package allocations are drawn down
 * (client answer Q-13, REQ-B0).
 *
 * One row per (fiscal year, department). Package-level budget lines sit below this in
 * budget_entry and point back at the row they draw from.
 */
@Entity
@Table(name = "department_budget")
public class DepartmentBudget extends BaseProcurementEntity {

    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    @Column(name = "department")
    private String department;

    @Column(name = "allocated_amount")
    private BigDecimal allocatedAmount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Any user holding the approver permission - not a routed workflow (Q-13). */
    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_by")
    private Long createdBy;

    public Integer getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(Integer fiscalYear) { this.fiscalYear = fiscalYear; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public BigDecimal getAllocatedAmount() { return allocatedAmount; }
    public void setAllocatedAmount(BigDecimal allocatedAmount) { this.allocatedAmount = allocatedAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
