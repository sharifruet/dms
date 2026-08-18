package com.bpdb.dms.procurement.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bpdb.dms.entity.NotificationPriority;
import com.bpdb.dms.entity.NotificationType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.NotificationService;

/**
 * Telling somebody when the money is running out (REQ-B8).
 *
 * <p>{@code BudgetSummary} has carried a {@code lowBudget} flag for a while, but a flag on
 * a screen nobody has open is not an alert. This raises one at the moment the position
 * changes — when a budget line is entered or an invoice posts consumption — rather than
 * from a scheduled sweep. That is deliberate: the position only moves when somebody does
 * something, so alerting on the action tells the right person at the moment they can still
 * act, and cannot degenerate into a daily repeat of the same bad news.
 *
 * <p>The threshold is configurable per REQ-B8 ("below a configurable threshold") through
 * {@code app.budget.low-threshold-ratio}.
 */
@Service
public class BudgetAlertService {

    private static final Logger log = LoggerFactory.getLogger(BudgetAlertService.class);

    /** Who is told. The Checker approves budget entries, so the Checker hears about them. */
    private static final String CHECKER = "CHECKER";

    private final ProcurementPackageRepository packageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.budget.low-threshold-ratio:0.10}")
    private BigDecimal lowThresholdRatio;

    public BudgetAlertService(ProcurementPackageRepository packageRepository,
                              UserRepository userRepository,
                              NotificationService notificationService) {
        this.packageRepository = packageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public BigDecimal lowThresholdRatio() {
        return lowThresholdRatio == null ? new BigDecimal("0.10") : lowThresholdRatio;
    }

    /**
     * Look at a package's position after it changed, and raise an alert if it now warrants
     * one.
     *
     * <p>Two cases, and they are not the same message. Remaining below the threshold is a
     * warning about the future. Remaining below zero means the package has already
     * consumed more than it has, which is a statement about the present and goes out at
     * high priority.
     */
    public void positionChanged(Long packageId, BudgetService.BudgetSummary summary) {
        if (summary == null || summary.totalAvailable == null
                || summary.totalAvailable.signum() <= 0) {
            return; // no budget loaded yet - nothing to be below
        }
        BigDecimal threshold = summary.totalAvailable.multiply(lowThresholdRatio());

        if (summary.remaining.signum() < 0) {
            raise(packageId, NotificationPriority.HIGH,
                    "Budget exceeded",
                    "consumption of " + summary.totalConsumption.toPlainString()
                    + " has passed the available budget of "
                    + summary.totalAvailable.toPlainString()
                    + ", leaving " + summary.remaining.toPlainString()
                    + ". No further invoice can be accepted against the contract value"
                    + " without a budget revision (REQ-B8).");
        } else if (summary.remaining.compareTo(threshold) < 0) {
            raise(packageId, NotificationPriority.MEDIUM,
                    "Budget running low",
                    "only " + summary.remaining.toPlainString() + " of "
                    + summary.totalAvailable.toPlainString() + " remains"
                    + " - below the " + percentLabel() + " warning threshold (REQ-B8).");
        }
    }

    private String percentLabel() {
        return lowThresholdRatio().multiply(BigDecimal.valueOf(100)).stripTrailingZeros()
                .toPlainString() + "%";
    }

    private void raise(Long packageId, NotificationPriority priority, String title, String detail) {
        ProcurementPackage pkg = packageRepository.findById(packageId).orElse(null);
        String where = pkg == null || pkg.getPackageNumber() == null
                ? "package " + packageId : pkg.getPackageNumber();
        String message = "Package " + where + ": " + detail;

        List<User> recipients = recipients(pkg);
        if (recipients.isEmpty()) {
            log.warn("Budget alert for package {} has no recipient: {}", packageId, message);
            return;
        }
        for (User recipient : recipients) {
            notificationService.createNotification(recipient, title, message,
                    NotificationType.SYSTEM_ALERT, priority);
        }
        log.info("Budget alert raised for package {}: {}", packageId, title);
    }

    private List<User> recipients(ProcurementPackage pkg) {
        List<User> recipients = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        // The package has no "responsible officer" column; whoever created it is the
        // closest thing the data holds to one, and they are the person working it
        if (pkg != null && pkg.getCreatedBy() != null) {
            userRepository.findById(pkg.getCreatedBy())
                    .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                    .filter(u -> seen.add(u.getId()))
                    .ifPresent(recipients::add);
        }
        for (User user : userRepository.findAll()) {
            if (user.getRole() != null && user.getRole().getName() != null
                    && CHECKER.equalsIgnoreCase(user.getRole().getName().name())
                    && Boolean.TRUE.equals(user.getIsActive())
                    && seen.add(user.getId())) {
                recipients.add(user);
            }
        }
        return recipients;
    }
}
