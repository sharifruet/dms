package com.bpdb.dms.procurement.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * How far ahead to warn about an expiring instrument, and who to tell (Q-15, REQ-E2/E3).
 *
 * <p>The client asked for sensible defaults kept configurable, so the intervals live in a
 * row rather than in code: 90/60/30/15/7 days is what is seeded, and an administrator can
 * change it per document type without a release.
 */
@Entity
@Table(name = "expiry_policy")
public class ExpiryPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "doc_label")
    private String docLabel;

    @Column(name = "expiry_field_label")
    private String expiryFieldLabel;

    /** Comma-separated days before expiry, e.g. "90,60,30,15,7". */
    @Column(name = "warning_days")
    private String warningDays;

    @Column(name = "notify_responsible_officer")
    private Boolean notifyResponsibleOfficer = Boolean.TRUE;

    /** Comma-separated role names that also receive the warning. */
    @Column(name = "notify_roles")
    private String notifyRoles;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    /** The configured thresholds, furthest out first, ignoring anything unparseable. */
    public List<Integer> warningThresholds() {
        List<Integer> days = new ArrayList<>();
        if (warningDays == null || warningDays.isBlank()) {
            return days;
        }
        for (String part : warningDays.split(",")) {
            try {
                days.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // a malformed entry should not stop the rest of the warnings going out
            }
        }
        days.sort(Comparator.reverseOrder());
        return days;
    }

    public List<String> notifyRoleNames() {
        List<String> roles = new ArrayList<>();
        if (notifyRoles == null || notifyRoles.isBlank()) {
            return roles;
        }
        for (String part : notifyRoles.split(",")) {
            if (!part.isBlank()) {
                roles.add(part.trim().toUpperCase());
            }
        }
        return roles;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getDocLabel() { return docLabel; }
    public void setDocLabel(String docLabel) { this.docLabel = docLabel; }
    public String getExpiryFieldLabel() { return expiryFieldLabel; }
    public void setExpiryFieldLabel(String expiryFieldLabel) { this.expiryFieldLabel = expiryFieldLabel; }
    public String getWarningDays() { return warningDays; }
    public void setWarningDays(String warningDays) { this.warningDays = warningDays; }
    public Boolean getNotifyResponsibleOfficer() { return notifyResponsibleOfficer; }
    public void setNotifyResponsibleOfficer(Boolean v) { this.notifyResponsibleOfficer = v; }
    public String getNotifyRoles() { return notifyRoles; }
    public void setNotifyRoles(String notifyRoles) { this.notifyRoles = notifyRoles; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
