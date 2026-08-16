package com.bpdb.dms.procurement.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.entity.ExpiryStatus;
import com.bpdb.dms.entity.ExpiryTracking;
import com.bpdb.dms.entity.NotificationPriority;
import com.bpdb.dms.entity.NotificationType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.procurement.entity.ExpiryPolicy;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.ExpiryPolicyRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.repository.ExpiryTrackingRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.NotificationService;

/**
 * Telling people that something is about to expire (REQ-E2, REQ-E3).
 *
 * <p>Recording an expiry date is not the same as anyone knowing about it. Tender validity,
 * a performance guarantee, an LC, a warranty — each has a date, and the point of tracking
 * them is that somebody acts before it passes. Until now the dates were stored and nobody
 * was told.
 *
 * <p>The intervals come from {@code expiry_policy} rather than from code, because Q-15
 * asked for defaults that stay configurable: 90/60/30/15/7 days is what is seeded, and an
 * administrator can change it per document type. Each threshold fires once — a warning
 * repeated hourly is a warning nobody reads.
 */
@Service
public class ProcurementExpiryAlertService {

    private static final Logger log = LoggerFactory.getLogger(ProcurementExpiryAlertService.class);

    /** Hourly. Warnings are measured in days, so anything finer is wasted work. */
    private static final long EVERY_HOUR = 3_600_000L;

    /** Recorded against sent_warnings when the date itself has passed. */
    private static final String EXPIRED_MARKER = "expired";

    private final ExpiryTrackingRepository expiryRepository;
    private final ExpiryPolicyRepository policyRepository;
    private final ProcurementPackageRepository packageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ProcurementExpiryAlertService(ExpiryTrackingRepository expiryRepository,
                                         ExpiryPolicyRepository policyRepository,
                                         ProcurementPackageRepository packageRepository,
                                         UserRepository userRepository,
                                         NotificationService notificationService) {
        this.expiryRepository = expiryRepository;
        this.policyRepository = policyRepository;
        this.packageRepository = packageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = EVERY_HOUR)
    public void sendDueWarnings() {
        try {
            int sent = run(LocalDate.now());
            if (sent > 0) {
                log.info("Procurement expiry warnings sent: {}", sent);
            }
        } catch (Exception e) {
            // A failure here must not kill the scheduler thread - the next run should try
            // again rather than the alerts going quiet for the life of the process
            log.error("Procurement expiry warning run failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Send whatever is due as of {@code today}.
     *
     * @return how many notifications were raised
     */
    @Transactional
    public int run(LocalDate today) {
        int sent = 0;
        for (ExpiryTracking tracker : expiryRepository.findAll()) {
            if (tracker.getStatus() != ExpiryStatus.ACTIVE || tracker.getExpiryDate() == null) {
                continue;
            }
            Optional<ExpiryPolicy> policy = policyFor(tracker);
            if (policy.isEmpty()) {
                continue; // not a procurement instrument, or nobody configured it
            }
            sent += process(tracker, policy.get(), today);
        }
        return sent;
    }

    private int process(ExpiryTracking tracker, ExpiryPolicy policy, LocalDate today) {
        LocalDate expiry = tracker.getExpiryDate().toLocalDate();
        long daysRemaining = ChronoUnit.DAYS.between(today, expiry);
        Set<String> alreadySent = sentMarkers(tracker);

        if (daysRemaining < 0) {
            if (alreadySent.add(EXPIRED_MARKER)) {
                notify(tracker, policy, daysRemaining, true);
                persist(tracker, alreadySent);
                return 1;
            }
            return 0;
        }

        // The nearest threshold we have reached and not yet warned about. Only one goes
        // out per run: crossing several at once (a tracker created late, say) should not
        // produce five notifications about the same instrument.
        Integer due = null;
        for (Integer threshold : policy.warningThresholds()) {
            if (daysRemaining <= threshold && !alreadySent.contains(String.valueOf(threshold))) {
                due = threshold;
            }
        }
        if (due == null) {
            return 0;
        }
        // Mark every threshold at or above the one being sent, so the wider ones that were
        // skipped do not fire later
        for (Integer threshold : policy.warningThresholds()) {
            if (threshold >= due) {
                alreadySent.add(String.valueOf(threshold));
            }
        }
        notify(tracker, policy, daysRemaining, false);
        persist(tracker, alreadySent);
        return 1;
    }

    private void notify(ExpiryTracking tracker, ExpiryPolicy policy, long daysRemaining,
                        boolean expired) {
        String what = policy.getDocLabel() == null ? tracker.getEntityType() : policy.getDocLabel();
        String where = packageContext(tracker);

        String title = expired
                ? what + " has expired"
                : what + " expires in " + daysRemaining + (daysRemaining == 1 ? " day" : " days");
        String message = (policy.getExpiryFieldLabel() == null ? "Expiry" : policy.getExpiryFieldLabel())
                + " is " + tracker.getExpiryDate().toLocalDate()
                + (where == null ? "" : " for package " + where)
                + (expired ? ". This instrument is no longer valid." : ".");

        NotificationPriority priority = expired || daysRemaining <= 7
                ? NotificationPriority.HIGH : NotificationPriority.MEDIUM;

        for (User recipient : recipients(tracker, policy)) {
            notificationService.createNotification(recipient, title, message,
                    typeFor(tracker.getEntityType()), priority);
        }
    }

    /**
     * Who hears about it: the officer responsible for the package, plus anyone holding a
     * role the policy names — Checker by default (Q-15).
     */
    private List<User> recipients(ExpiryTracking tracker, ExpiryPolicy policy) {
        List<User> recipients = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();

        if (Boolean.TRUE.equals(policy.getNotifyResponsibleOfficer())) {
            responsibleOfficer(tracker).ifPresent(user -> {
                if (seen.add(user.getId())) {
                    recipients.add(user);
                }
            });
        }
        for (String roleName : policy.notifyRoleNames()) {
            for (User user : userRepository.findAll()) {
                if (user.getRole() != null && user.getRole().getName() != null
                        && roleName.equalsIgnoreCase(user.getRole().getName().name())
                        && Boolean.TRUE.equals(user.getIsActive())
                        && seen.add(user.getId())) {
                    recipients.add(user);
                }
            }
        }
        if (recipients.isEmpty()) {
            log.warn("Expiry warning for {}#{} has no recipient - nobody will hear about it",
                    tracker.getEntityType(), tracker.getEntityId());
        }
        return recipients;
    }

    private Optional<User> responsibleOfficer(ExpiryTracking tracker) {
        if (tracker.getPackageId() == null) {
            return Optional.empty();
        }
        return packageRepository.findById(tracker.getPackageId())
                .map(ProcurementPackage::getCreatedBy)
                .flatMap(userRepository::findById);
    }

    private String packageContext(ExpiryTracking tracker) {
        if (tracker.getPackageId() == null) {
            return null;
        }
        return packageRepository.findById(tracker.getPackageId())
                .map(ProcurementPackage::getPackageNumber)
                .orElse(null);
    }

    private Optional<ExpiryPolicy> policyFor(ExpiryTracking tracker) {
        return tracker.getEntityType() == null
                ? Optional.empty()
                : policyRepository.findByEntityTypeAndIsActiveTrue(tracker.getEntityType());
    }

    private static NotificationType typeFor(String entityType) {
        if (entityType == null) {
            return NotificationType.DOCUMENT_EXPIRY;
        }
        return switch (entityType) {
            case "LETTER_OF_CREDIT" -> NotificationType.LC_EXPIRY;
            case "PERFORMANCE_SECURITY" -> NotificationType.PS_EXPIRY;
            case "CONTRACT" -> NotificationType.CONTRACT_EXPIRY;
            default -> NotificationType.DOCUMENT_EXPIRY;
        };
    }

    private static Set<String> sentMarkers(ExpiryTracking tracker) {
        Set<String> markers = new LinkedHashSet<>();
        String raw = tracker.getSentWarnings();
        if (raw != null && !raw.isBlank()) {
            for (String part : raw.split(",")) {
                if (!part.isBlank()) {
                    markers.add(part.trim());
                }
            }
        }
        return markers;
    }

    private void persist(ExpiryTracking tracker, Set<String> markers) {
        tracker.setSentWarnings(String.join(",", markers));
        expiryRepository.save(tracker);
    }
}
