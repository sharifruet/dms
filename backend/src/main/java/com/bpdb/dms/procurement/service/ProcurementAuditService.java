package com.bpdb.dms.procurement.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.AuditService;

/**
 * The action-level audit trail for the procurement lifecycle (REQ-X6).
 *
 * <p>"All actions described in this document shall be recorded immutably (who, what, when,
 * before/after)." Field values already carry their own history in
 * {@code extracted_field_history}; this covers the decisions — completing a stage, sending
 * it back, declaring it not applicable, re-tendering, moving budget, closing a package.
 * Those are the moments a procurement audit actually asks about, and until now they existed
 * only as application log lines, which are neither queryable nor durable.
 *
 * <p>A thin layer over {@link AuditService} rather than a parallel mechanism: the DMS
 * already has one audit table and one place that writes it. What this adds is a vocabulary
 * for procurement actions and, where the requirement calls for it, the before and after
 * state in the description — an audit entry saying a stage changed is far less use than one
 * saying it went from IN_PROGRESS to COMPLETED.
 */
@Service
public class ProcurementAuditService {

    private static final Logger log = LoggerFactory.getLogger(ProcurementAuditService.class);

    /** Resource types, so procurement entries can be filtered out of the wider log. */
    public static final String PACKAGE = "PROCUREMENT_PACKAGE";
    public static final String STAGE = "PROCUREMENT_STAGE";
    public static final String TENDER = "PROCUREMENT_TENDER";
    public static final String BUDGET = "PROCUREMENT_BUDGET";
    public static final String DELIVERY = "PROCUREMENT_DELIVERY";
    public static final String INVOICE = "PROCUREMENT_INVOICE";
    public static final String PAYMENT = "PROCUREMENT_PAYMENT";
    public static final String RETENTION = "PROCUREMENT_RETENTION";

    private final AuditService auditService;
    private final UserRepository userRepository;

    public ProcurementAuditService(AuditService auditService, UserRepository userRepository) {
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /**
     * Record an action. Never throws: an audit failure must not take down the operation it
     * is describing, but it is logged loudly because a silent gap in an audit trail is
     * worse than a noisy one.
     */
    public void record(Long userId, String action, String resourceType, Long resourceId,
                       String description) {
        try {
            User user = resolve(userId);
            auditService.logUserAction(user, action, resourceType, resourceId, description, null);
        } catch (Exception e) {
            log.error("AUDIT GAP: could not record {} on {}#{} by user {}: {}",
                    action, resourceType, resourceId, userId, e.getMessage(), e);
        }
    }

    /** Record a change, naming what it was before and what it is now (REQ-X6). */
    public void recordChange(Long userId, String action, String resourceType, Long resourceId,
                             String what, String before, String after, String reason) {
        StringBuilder description = new StringBuilder(what);
        if (before != null || after != null) {
            description.append(": ").append(before == null ? "(none)" : before)
                    .append(" -> ").append(after == null ? "(none)" : after);
        }
        if (reason != null && !reason.isBlank()) {
            description.append(". Reason: ").append(reason.trim());
        }
        record(userId, action, resourceType, resourceId, description.toString());
    }

    // ------------------------------------------------------- the lifecycle decisions

    public void stageCompleted(Long userId, Long packageId, short stageCode, String previousStatus,
                               String overrideReason) {
        recordChange(userId, "PROCUREMENT_STAGE_COMPLETED", STAGE, packageId,
                "Stage " + stageCode + " completed", previousStatus, "COMPLETED",
                overrideReason == null ? null : "Completed with override: " + overrideReason);
    }

    public void stageMarkedNotApplicable(Long userId, Long packageId, short stageCode,
                                         String reason) {
        recordChange(userId, "PROCUREMENT_STAGE_NOT_APPLICABLE", STAGE, packageId,
                "Stage " + stageCode + " marked not applicable", "APPLICABLE", "NOT_APPLICABLE",
                reason);
    }

    public void stageReworked(Long userId, Long packageId, short stageCode, String previousStatus,
                              String reason) {
        recordChange(userId, "PROCUREMENT_STAGE_REWORK", STAGE, packageId,
                "Stage " + stageCode + " sent back for rework", previousStatus, "REWORK", reason);
    }

    public void reTendered(Long userId, Long packageId, int failedAttempt, int newAttempt,
                           String reason) {
        recordChange(userId, "PROCUREMENT_RETENDER", TENDER, packageId,
                "Tender re-opened", "attempt " + failedAttempt, "attempt " + newAttempt, reason);
    }

    public void budgetEntryAdded(Long userId, Long packageId, String entryType, Object amount,
                                 String reason) {
        record(userId, "PROCUREMENT_BUDGET_ENTRY", BUDGET, packageId,
                entryType + " of " + amount + " recorded against the package"
                        + (reason == null || reason.isBlank() ? "" : ". Reason: " + reason));
    }

    public void departmentBudgetSet(Long userId, Long budgetId, String department, int fiscalYear,
                                    Object previousAmount, Object newAmount) {
        recordChange(userId, "PROCUREMENT_DEPARTMENT_BUDGET", BUDGET, budgetId,
                "Annual budget for " + department + " FY" + fiscalYear,
                previousAmount == null ? null : String.valueOf(previousAmount),
                String.valueOf(newAmount), null);
    }

    public void packageCreated(Long userId, Long packageId, String packageNumber, String origin) {
        record(userId, "PROCUREMENT_PACKAGE_CREATED", PACKAGE, packageId,
                "Package " + packageNumber + " created" + (origin == null ? "" : " (" + origin + ")"));
    }

    public void deliveryDeclaredFinal(Long userId, Long deliveryId, String reference) {
        record(userId, "PROCUREMENT_DELIVERY_FINAL", DELIVERY, deliveryId,
                "Delivery " + reference + " declared final, closing the delivery set");
    }

    public void invoiceRecorded(Long userId, Long invoiceId, String invoiceNumber, Object amount) {
        record(userId, "PROCUREMENT_INVOICE_RECORDED", INVOICE, invoiceId,
                "Invoice " + invoiceNumber + " for " + amount + " recorded");
    }

    public void paymentRecorded(Long userId, Long paymentId, String voucher, Object amount) {
        record(userId, "PROCUREMENT_PAYMENT_RECORDED", PAYMENT, paymentId,
                "Payment " + voucher + " of " + amount + " recorded");
    }

    /**
     * A refusal is worth recording too. Someone attempting to bill beyond the contract
     * value is exactly the kind of thing an auditor wants to see, whether or not it
     * succeeded (Q-12).
     */
    public void refused(Long userId, String resourceType, Long resourceId, String what,
                        String why) {
        try {
            User user = resolve(userId);
            auditService.logFailedAction(user, "PROCUREMENT_REFUSED", resourceType, resourceId,
                    what, why, null);
        } catch (Exception e) {
            log.error("AUDIT GAP: could not record refusal of {} on {}#{}: {}",
                    what, resourceType, resourceId, e.getMessage(), e);
        }
    }

    public void retentionPurge(Long userId, String description) {
        record(userId, "PROCUREMENT_RETENTION_PURGE", RETENTION, null, description);
    }

    private User resolve(Long userId) {
        if (userId == null) {
            return null;
        }
        Optional<User> user = userRepository.findById(userId);
        return user.orElse(null);
    }
}
