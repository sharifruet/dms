package com.bpdb.dms.procurement.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.entity.NotificationPriority;
import com.bpdb.dms.entity.NotificationType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.procurement.entity.Contract;
import com.bpdb.dms.procurement.entity.DeadlineAlertState;
import com.bpdb.dms.procurement.entity.Noa;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.ContractApprovalRepository;
import com.bpdb.dms.procurement.repository.DeadlineAlertStateRepository;
import com.bpdb.dms.procurement.repository.DeliveryRepository;
import com.bpdb.dms.procurement.repository.NoaRepository;
import com.bpdb.dms.procurement.repository.PerformanceSecurityRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.NotificationService;

/**
 * The other half of REQ-X8: the dates somebody has to act <em>before</em>.
 *
 * <p>Expiry dates already drive alerts. Deadlines did not, though the requirement names
 * them in the same breath — PG Submission Last Date, Contract Signing Last Date, and the
 * contractual delivery window. The difference matters: an expiry is a property of an
 * instrument, while a deadline is an obligation that is either met or missed, and a
 * missed one has consequences that are nobody's idea of a surprise worth having.
 *
 * <p>Three rules shape it:
 *
 * <ol>
 *   <li><b>A met deadline goes quiet.</b> Once the performance security is in, nobody
 *       needs another reminder to submit it. The obligation, not the date, decides.</li>
 *   <li><b>A moved deadline is a new deadline.</b> An extension resets the warnings, so
 *       the new date is announced on its own terms.</li>
 *   <li><b>Overdue is said once, and loudly.</b> Repeating it daily trains people to
 *       ignore it.</li>
 * </ol>
 */
@Service
public class DeadlineAlertService {

    private static final Logger log = LoggerFactory.getLogger(DeadlineAlertService.class);

    public static final String PG_SUBMISSION = "PG_SUBMISSION";
    public static final String CONTRACT_SIGNING = "CONTRACT_SIGNING";
    public static final String DELIVERY_WINDOW = "DELIVERY_WINDOW";

    /** Daily at 07:00. Deadlines are counted in days; anything finer is noise. */
    private static final String DAILY_EARLY = "0 0 7 * * *";

    private static final String CHECKER = "CHECKER";

    private final ProcurementPackageRepository packageRepository;
    private final ContractApprovalRepository approvalRepository;
    private final NoaRepository noaRepository;
    private final PerformanceSecurityRepository psRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeadlineAlertStateRepository stateRepository;
    private final StageDataService stageDataService;
    private final ValidationService validationService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /** Days before a deadline at which to warn. Configurable per REQ-E2's spirit. */
    @Value("${app.deadline.warning-days:30,14,7,3,1}")
    private String warningDaysConfig;

    public DeadlineAlertService(ProcurementPackageRepository packageRepository,
                                ContractApprovalRepository approvalRepository,
                                NoaRepository noaRepository,
                                PerformanceSecurityRepository psRepository,
                                DeliveryRepository deliveryRepository,
                                DeadlineAlertStateRepository stateRepository,
                                StageDataService stageDataService,
                                ValidationService validationService,
                                UserRepository userRepository,
                                NotificationService notificationService) {
        this.packageRepository = packageRepository;
        this.approvalRepository = approvalRepository;
        this.noaRepository = noaRepository;
        this.psRepository = psRepository;
        this.deliveryRepository = deliveryRepository;
        this.stateRepository = stateRepository;
        this.stageDataService = stageDataService;
        this.validationService = validationService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = DAILY_EARLY)
    public void sendDueDeadlineWarnings() {
        try {
            int sent = run(LocalDate.now());
            if (sent > 0) {
                log.info("Procurement deadline warnings sent: {}", sent);
            }
        } catch (Exception e) {
            // Same reasoning as the expiry sweep: one bad run must not silence the next
            log.error("Deadline warning run failed: {}", e.getMessage(), e);
        }
    }

    /** Evaluate every open package's deadlines as of {@code today}. */
    @Transactional
    public int run(LocalDate today) {
        int sent = 0;
        for (ProcurementPackage pkg : packageRepository.findAll()) {
            if ("CLOSED".equalsIgnoreCase(pkg.getStatus())) {
                continue; // a closed package has no deadlines left to miss
            }
            for (Deadline deadline : deadlinesFor(pkg)) {
                sent += evaluate(pkg, deadline, today);
            }
        }
        return sent;
    }

    /** What this package currently owes, whether or not the date has been reached. */
    public List<Deadline> deadlinesFor(ProcurementPackage pkg) {
        List<Deadline> deadlines = new ArrayList<>();
        Optional<Noa> noa = approvalRepository.findByPackageId(pkg.getId())
                .flatMap(a -> noaRepository.findByApprovalId(a.getId()));
        Optional<Contract> contract = stageDataService.findContract(pkg.getId());

        noa.ifPresent(n -> {
            if (n.getPgSubmissionLastDate() != null) {
                boolean submitted = !psRepository.findByNoaId(n.getId()).isEmpty();
                deadlines.add(new Deadline(PG_SUBMISSION, "Performance security submission",
                        n.getPgSubmissionLastDate(), submitted,
                        "the performance security has been recorded"));
            }
            if (n.getContractSigningLastDate() != null) {
                boolean signed = contract.map(c -> c.getContractDate() != null).orElse(false);
                deadlines.add(new Deadline(CONTRACT_SIGNING, "Contract signing",
                        n.getContractSigningLastDate(), signed, "the contract has been signed"));
            }
        });

        contract.ifPresent(c -> {
            LocalDate due = validationService.contractualDeliveryDeadline(c);
            if (due != null) {
                boolean complete = deliveryRepository.findByContractId(c.getId()).stream()
                        .anyMatch(d -> Boolean.TRUE.equals(d.getIsFinal()));
                deadlines.add(new Deadline(DELIVERY_WINDOW, "Delivery window",
                        due, complete, "delivery has been declared final"));
            }
        });
        return deadlines;
    }

    private int evaluate(ProcurementPackage pkg, Deadline deadline, LocalDate today) {
        DeadlineAlertState state = stateRepository
                .findByPackageIdAndDeadlineKey(pkg.getId(), deadline.key)
                .orElseGet(() -> {
                    DeadlineAlertState fresh = new DeadlineAlertState();
                    fresh.setPackageId(pkg.getId());
                    fresh.setDeadlineKey(deadline.key);
                    return fresh;
                });

        if (deadline.satisfied) {
            if (!Boolean.TRUE.equals(state.getSatisfied())) {
                state.setSatisfied(Boolean.TRUE);
                state.setSatisfiedAt(LocalDateTime.now());
                state.setDeadlineDate(deadline.date);
                state.setUpdatedAt(LocalDateTime.now());
                stateRepository.save(state);
            }
            return 0;
        }

        Set<String> alreadySent = state.sentMarkers();
        if (state.dateChanged(deadline.date)) {
            // The deadline moved. Start again rather than carry over warnings that were
            // about a date nobody is working to any more.
            alreadySent = new LinkedHashSet<>();
        }
        state.setDeadlineDate(deadline.date);
        state.setSatisfied(Boolean.FALSE);

        long daysRemaining = ChronoUnit.DAYS.between(today, deadline.date);
        boolean overdue = daysRemaining < 0;

        if (overdue) {
            if (!alreadySent.add(DeadlineAlertState.OVERDUE)) {
                return 0;
            }
        } else {
            Integer due = null;
            for (Integer threshold : warningDays()) {
                if (daysRemaining <= threshold && !alreadySent.contains(String.valueOf(threshold))) {
                    due = threshold;
                }
            }
            if (due == null) {
                persist(state, alreadySent);
                return 0;
            }
            for (Integer threshold : warningDays()) {
                if (threshold >= due) {
                    alreadySent.add(String.valueOf(threshold));
                }
            }
        }

        notify(pkg, deadline, daysRemaining, overdue);
        persist(state, alreadySent);
        return 1;
    }

    private void persist(DeadlineAlertState state, Set<String> markers) {
        state.setSentMarkers(markers);
        state.setUpdatedAt(LocalDateTime.now());
        stateRepository.save(state);
    }

    private void notify(ProcurementPackage pkg, Deadline deadline, long daysRemaining,
                        boolean overdue) {
        String where = pkg.getPackageNumber() == null
                ? "package " + pkg.getId() : pkg.getPackageNumber();
        String title = overdue
                ? deadline.label + " deadline has passed"
                : deadline.label + " is due in " + daysRemaining
                  + (daysRemaining == 1 ? " day" : " days");
        String message = "Package " + where + ": " + deadline.label.toLowerCase()
                + " is due by " + deadline.date
                + (overdue
                    ? ". The date has passed and " + deadline.satisfiedWhen + " is still outstanding."
                    : ". This closes once " + deadline.satisfiedWhen + ".")
                + " (REQ-X8)";

        NotificationPriority priority = overdue || daysRemaining <= 3
                ? NotificationPriority.HIGH : NotificationPriority.MEDIUM;

        List<User> recipients = recipients(pkg);
        if (recipients.isEmpty()) {
            log.warn("Deadline alert for package {} ({}) has no recipient",
                    pkg.getId(), deadline.key);
            return;
        }
        for (User recipient : recipients) {
            notificationService.createNotification(recipient, title, message,
                    NotificationType.COMPLIANCE_ALERT, priority);
        }
    }

    private List<User> recipients(ProcurementPackage pkg) {
        List<User> recipients = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        if (pkg.getCreatedBy() != null) {
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

    List<Integer> warningDays() {
        List<Integer> days = new ArrayList<>();
        String config = warningDaysConfig == null || warningDaysConfig.isBlank()
                ? "30,14,7,3,1" : warningDaysConfig;
        for (String part : config.split(",")) {
            try {
                days.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException e) {
                log.warn("Ignoring unparseable deadline warning interval '{}'", part);
            }
        }
        days.sort(java.util.Comparator.reverseOrder());
        return days;
    }

    void setWarningDaysConfig(String config) {
        this.warningDaysConfig = config;
    }

    /** One obligation with a date on it. */
    public static class Deadline {
        public final String key;
        public final String label;
        public final LocalDate date;
        public final boolean satisfied;
        public final String satisfiedWhen;

        public Deadline(String key, String label, LocalDate date, boolean satisfied,
                        String satisfiedWhen) {
            this.key = key;
            this.label = label;
            this.date = date;
            this.satisfied = satisfied;
            this.satisfiedWhen = satisfiedWhen;
        }

        public long daysRemaining(LocalDate today) {
            return ChronoUnit.DAYS.between(today, date);
        }

        public boolean isOverdue(LocalDate today) {
            return !satisfied && date.isBefore(today);
        }
    }

    /** Deadlines across every open package, for the dashboard (REQ-X5). */
    public List<PackageDeadline> openDeadlines(LocalDate today) {
        List<PackageDeadline> all = new ArrayList<>();
        for (ProcurementPackage pkg : packageRepository.findAll()) {
            if ("CLOSED".equalsIgnoreCase(pkg.getStatus())) {
                continue;
            }
            for (Deadline deadline : deadlinesFor(pkg)) {
                if (!deadline.satisfied) {
                    all.add(new PackageDeadline(pkg, deadline, today));
                }
            }
        }
        all.sort(java.util.Comparator.comparing(d -> d.dueDate));
        return all;
    }

    /** A deadline with enough package context to be rendered on its own. */
    public static class PackageDeadline {
        public final Long packageId;
        public final String packageNumber;
        public final String deadlineKey;
        public final String label;
        public final LocalDate dueDate;
        public final long daysRemaining;
        public final boolean overdue;

        /**
         * Public so callers outside this package can state a deadline directly — the
         * executive rollup's "delayed" count is defined by these rows, and pinning that
         * definition in a test means being able to hand one in.
         */
        public PackageDeadline(Long packageId, String packageNumber, String deadlineKey,
                               String label, LocalDate dueDate, long daysRemaining, boolean overdue) {
            this.packageId = packageId;
            this.packageNumber = packageNumber;
            this.deadlineKey = deadlineKey;
            this.label = label;
            this.dueDate = dueDate;
            this.daysRemaining = daysRemaining;
            this.overdue = overdue;
        }

        PackageDeadline(ProcurementPackage pkg, Deadline deadline, LocalDate today) {
            this.packageId = pkg.getId();
            this.packageNumber = pkg.getPackageNumber();
            this.deadlineKey = deadline.key;
            this.label = deadline.label;
            this.dueDate = deadline.date;
            this.daysRemaining = deadline.daysRemaining(today);
            this.overdue = deadline.isOverdue(today);
        }
    }

    static List<Integer> parseDays(String config) {
        List<Integer> days = new ArrayList<>();
        for (String part : Arrays.asList(config.split(","))) {
            days.add(Integer.parseInt(part.trim()));
        }
        return days;
    }
}
