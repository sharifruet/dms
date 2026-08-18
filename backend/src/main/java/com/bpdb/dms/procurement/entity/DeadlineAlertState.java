package com.bpdb.dms.procurement.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * What has already been said about one deadline on one package (REQ-X8).
 *
 * <p>Exists so a warning goes out once. A deadline that has been met stops being talked
 * about at all — the row is kept rather than deleted, because "this was met, and here is
 * when we stopped chasing it" is worth more than an absent row.
 */
@Entity
@Table(name = "deadline_alert_state")
public class DeadlineAlertState {

    /** The moment a deadline has passed, recorded alongside the day thresholds. */
    public static final String OVERDUE = "overdue";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "deadline_key", nullable = false, length = 40)
    private String deadlineKey;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "sent_warnings", length = 100)
    private String sentWarnings;

    @Column(name = "satisfied")
    private Boolean satisfied = Boolean.FALSE;

    @Column(name = "satisfied_at")
    private LocalDateTime satisfiedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** Markers already sent: day thresholds as numbers, plus {@link #OVERDUE}. */
    public Set<String> sentMarkers() {
        Set<String> markers = new LinkedHashSet<>();
        if (sentWarnings == null || sentWarnings.isBlank()) {
            return markers;
        }
        for (String part : sentWarnings.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                markers.add(trimmed);
            }
        }
        return markers;
    }

    public void setSentMarkers(Set<String> markers) {
        List<String> ordered = new ArrayList<>(markers);
        this.sentWarnings = String.join(",", ordered);
    }

    /**
     * A deadline that moved is a different deadline — an extension resets the warnings so
     * the new date is warned about on its own terms, rather than being treated as already
     * announced.
     */
    public boolean dateChanged(LocalDate newDate) {
        return deadlineDate != null && newDate != null && !deadlineDate.equals(newDate);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public String getDeadlineKey() { return deadlineKey; }
    public void setDeadlineKey(String deadlineKey) { this.deadlineKey = deadlineKey; }
    public LocalDate getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(LocalDate deadlineDate) { this.deadlineDate = deadlineDate; }
    public String getSentWarnings() { return sentWarnings; }
    public void setSentWarnings(String sentWarnings) { this.sentWarnings = sentWarnings; }
    public Boolean getSatisfied() { return satisfied; }
    public void setSatisfied(Boolean satisfied) { this.satisfied = satisfied; }
    public LocalDateTime getSatisfiedAt() { return satisfiedAt; }
    public void setSatisfiedAt(LocalDateTime satisfiedAt) { this.satisfiedAt = satisfiedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
