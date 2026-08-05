package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * ProgressReport - procurement lifecycle entity mapped to progress_report.
 */
@Entity
@Table(name = "progress_report")
public class ProgressReport extends BaseProcurementEntity {

    @Column(name = "production_schedule_id")
    private Long productionScheduleId;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "progress_pct")
    private BigDecimal progressPct;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    public Long getProductionScheduleId() { return productionScheduleId; }
    public void setProductionScheduleId(Long productionScheduleId) { this.productionScheduleId = productionScheduleId; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public BigDecimal getProgressPct() { return progressPct; }
    public void setProgressPct(BigDecimal progressPct) { this.progressPct = progressPct; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
