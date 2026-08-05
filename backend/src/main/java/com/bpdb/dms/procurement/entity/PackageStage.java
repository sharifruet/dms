package com.bpdb.dms.procurement.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One row per stage per package. This is the stage gate record: the StageEngine is
 * the only component that writes status (WF-01..WF-10).
 */
@Entity
@Table(name = "package_stage")
public class PackageStage extends BaseProcurementEntity {

    public static final String NOT_STARTED = "NOT_STARTED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String PENDING_VERIFICATION = "PENDING_VERIFICATION";
    public static final String COMPLETED = "COMPLETED";
    public static final String REWORK = "REWORK";

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "stage_code", nullable = false)
    private Short stageCode;

    @Column(name = "status", nullable = false)
    private String status = NOT_STARTED;

    /** False when the stage is declared Not Applicable, e.g. a non-LC contract (REQ-9.5). */
    @Column(name = "is_applicable", nullable = false)
    private Boolean isApplicable = Boolean.TRUE;

    @Column(name = "not_applicable_reason", columnDefinition = "TEXT")
    private String notApplicableReason;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "rework_reason", columnDefinition = "TEXT")
    private String reworkReason;

    public boolean isCompleted() {
        return COMPLETED.equals(status);
    }

    /** A Not Applicable stage satisfies the gate for the stage that follows it. */
    public boolean isSatisfied() {
        return isCompleted() || Boolean.FALSE.equals(isApplicable);
    }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public Short getStageCode() { return stageCode; }
    public void setStageCode(Short stageCode) { this.stageCode = stageCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getIsApplicable() { return isApplicable; }
    public void setIsApplicable(Boolean isApplicable) { this.isApplicable = isApplicable; }
    public String getNotApplicableReason() { return notApplicableReason; }
    public void setNotApplicableReason(String notApplicableReason) { this.notApplicableReason = notApplicableReason; }
    public LocalDateTime getEnteredAt() { return enteredAt; }
    public void setEnteredAt(LocalDateTime enteredAt) { this.enteredAt = enteredAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Long getCompletedBy() { return completedBy; }
    public void setCompletedBy(Long completedBy) { this.completedBy = completedBy; }
    public String getReworkReason() { return reworkReason; }
    public void setReworkReason(String reworkReason) { this.reworkReason = reworkReason; }
}
