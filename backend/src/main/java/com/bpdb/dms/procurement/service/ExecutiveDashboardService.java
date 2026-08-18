package com.bpdb.dms.procurement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.bpdb.dms.entity.ExpiryStatus;
import com.bpdb.dms.entity.ExpiryTracking;
import com.bpdb.dms.procurement.entity.BudgetConsumption;
import com.bpdb.dms.procurement.entity.BudgetEntry;
import com.bpdb.dms.procurement.entity.DepartmentBudget;
import com.bpdb.dms.procurement.entity.OcrJob;
import com.bpdb.dms.procurement.entity.PackageStage;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.entity.Tender;
import com.bpdb.dms.procurement.repository.BudgetConsumptionRepository;
import com.bpdb.dms.procurement.repository.BudgetEntryRepository;
import com.bpdb.dms.procurement.repository.DepartmentBudgetRepository;
import com.bpdb.dms.procurement.repository.OcrJobRepository;
import com.bpdb.dms.procurement.repository.PackageStageRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.repository.TenderRepository;
import com.bpdb.dms.repository.DocumentIndexRepository;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.ExpiryTrackingRepository;

/**
 * The executive rollup (REQ-X5): the whole estate on one page, derived from the stage
 * gates rather than from a parallel set of status columns.
 *
 * <p>Every figure here is computed from the lifecycle records the workspace already
 * writes — {@code package_stage} rows the StageEngine owns, the tender attempts, the
 * budget ledger, the expiry trackers and the OCR jobs. Nothing on this page is a stored
 * "dashboard number" that could drift from the packages it claims to describe; the cost
 * of that is that the definitions below <em>are</em> the contract, so they are spelled
 * out one by one.
 *
 * <p>A package is counted as having passed a stage when that stage is
 * {@link PackageStage#isSatisfied() satisfied} — completed, or declared Not Applicable
 * (REQ-9.5). A package whose LC stage does not apply has still reached delivery, and an
 * executive count that said otherwise would be wrong in the direction that matters.
 *
 * <p>This reads the whole estate into memory once per call rather than issuing a count
 * query per tile. At BPDB's package volumes (hundreds to low thousands) that is one pass
 * over five small tables and cheaper than twenty round trips; if the estate grows an
 * order of magnitude, the grouping below is what turns into projections.
 */
@Service
public class ExecutiveDashboardService {

    private static final Logger log = LoggerFactory.getLogger(ExecutiveDashboardService.class);

    /** Stage codes the rollup keys off, named so the definitions below read as English. */
    private static final short STAGE_TENDER_ADVERTISEMENT = 2;
    private static final short STAGE_TENDER_OPENING = 3;
    private static final short STAGE_TENDER_EVALUATION = 4;
    private static final short STAGE_NOA = 6;
    private static final short STAGE_CONTRACT_SIGNING = 8;
    private static final short STAGE_CONTRACT_CLOSE = 16;

    /** One crore = 10^7. BPDB reports budget in crore taka, so the API carries both. */
    private static final BigDecimal CRORE = new BigDecimal("10000000");
    /** package.price_lac_bdt is in lakh (10^5), the unit the APP workbook uses. */
    private static final BigDecimal LAKH = new BigDecimal("100000");

    private final ProcurementPackageRepository packageRepository;
    private final PackageStageRepository stageRepository;
    private final TenderRepository tenderRepository;
    private final BudgetEntryRepository budgetEntryRepository;
    private final BudgetConsumptionRepository consumptionRepository;
    private final DepartmentBudgetRepository departmentBudgetRepository;
    private final ExpiryTrackingRepository expiryRepository;
    private final DocumentRepository documentRepository;
    private final OcrJobRepository ocrJobRepository;
    private final DeadlineAlertService deadlineAlertService;

    /**
     * Elasticsearch is optional at runtime — the stack runs without it and the search
     * page degrades rather than failing. The dashboard does the same: the "Indexed" tile
     * reports null instead of taking the whole page down with it.
     */
    private final DocumentIndexRepository documentIndexRepository;

    public ExecutiveDashboardService(ProcurementPackageRepository packageRepository,
                                     PackageStageRepository stageRepository,
                                     TenderRepository tenderRepository,
                                     BudgetEntryRepository budgetEntryRepository,
                                     BudgetConsumptionRepository consumptionRepository,
                                     DepartmentBudgetRepository departmentBudgetRepository,
                                     ExpiryTrackingRepository expiryRepository,
                                     DocumentRepository documentRepository,
                                     OcrJobRepository ocrJobRepository,
                                     DeadlineAlertService deadlineAlertService,
                                     @Nullable DocumentIndexRepository documentIndexRepository) {
        this.packageRepository = packageRepository;
        this.stageRepository = stageRepository;
        this.tenderRepository = tenderRepository;
        this.budgetEntryRepository = budgetEntryRepository;
        this.consumptionRepository = consumptionRepository;
        this.departmentBudgetRepository = departmentBudgetRepository;
        this.expiryRepository = expiryRepository;
        this.documentRepository = documentRepository;
        this.ocrJobRepository = ocrJobRepository;
        this.deadlineAlertService = deadlineAlertService;
        this.documentIndexRepository = documentIndexRepository;
    }

    /**
     * The whole page in one response.
     *
     * @param fiscalYear restrict the package-derived panels to one APP year; null for the
     *                   estate as a whole. Document and OCR counts are estate-wide either
     *                   way — a document is not filed against a fiscal year.
     */
    public Map<String, Object> snapshot(@Nullable Integer fiscalYear) {
        LocalDate today = LocalDate.now();

        List<ProcurementPackage> allPackages = packageRepository.findAll();
        List<ProcurementPackage> packages = fiscalYear == null
                ? allPackages
                : allPackages.stream()
                        .filter(p -> fiscalYear.equals(p.getFiscalYear()))
                        .collect(Collectors.toList());
        Set<Long> packageIds = packages.stream()
                .map(ProcurementPackage::getId).collect(Collectors.toSet());

        Map<Long, List<PackageStage>> stagesByPackage = stageRepository.findAll().stream()
                .filter(s -> packageIds.contains(s.getPackageId()))
                .collect(Collectors.groupingBy(PackageStage::getPackageId));

        List<Tender> tenders = tenderRepository.findAll().stream()
                .filter(t -> packageIds.contains(t.getPackageId()))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generatedAt", LocalDateTime.now());
        result.put("fiscalYear", fiscalYear);
        result.put("fiscalYears", fiscalYearsPresent(allPackages));
        result.put("currency", "BDT");

        Map<String, Object> budget = budgetPosition(packageIds, fiscalYear);
        result.put("kpis", kpis(packages, stagesByPackage, budget));
        result.put("appProgress", appProgress(packages, stagesByPackage, allPackages, fiscalYear, today));
        result.put("tenderStatistics", tenderStatistics(packages, stagesByPackage, tenders));
        result.put("procurementNature", groupCount(tenders, Tender::getProcurementNature));
        result.put("procurementMethod", groupCount(tenders, Tender::getProcurementMethod));
        result.put("budget", budget);

        List<ExpiryTracking> expiries = expiryRepository.findAll();
        result.put("performanceSecurity", performanceSecurity(expiries, today));

        Map<String, Object> documents = documentAnalytics();
        result.put("documents", documents);
        result.put("alerts", alerts(expiries, today, documents));
        return result;
    }

    // ------------------------------------------------------------------- KPI row

    /**
     * The eight headline tiles.
     *
     * <p>"Contracts" are counted from the signing gate rather than from rows in the
     * contract table: {@code StageDataService} creates a DRAFT contract row the moment
     * anyone types a value into a Stage 8 form, so counting rows would report contracts
     * that nobody has signed.
     */
    private Map<String, Object> kpis(List<ProcurementPackage> packages,
                                     Map<Long, List<PackageStage>> stagesByPackage,
                                     Map<String, Object> budget) {
        long signed = 0;
        long live = 0;
        long running = 0;
        long completed = 0;

        for (ProcurementPackage pkg : packages) {
            List<PackageStage> stages = stagesByPackage.getOrDefault(pkg.getId(), List.of());
            boolean isSigned = satisfied(stages, STAGE_CONTRACT_SIGNING);
            boolean isClosed = satisfied(stages, STAGE_CONTRACT_CLOSE) || "CLOSED".equals(pkg.getStatus());
            boolean cancelled = "CANCELLED".equals(pkg.getStatus());

            if (isSigned) {
                signed++;
                if (isClosed) {
                    completed++;
                } else if (!cancelled) {
                    running++;
                }
            }
            // Advertised, and not yet through evaluation: the tender is on the street
            if (!cancelled && satisfied(stages, STAGE_TENDER_ADVERTISEMENT)
                    && !satisfied(stages, STAGE_TENDER_EVALUATION)) {
                live++;
            }
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalContracts", signed);
        kpis.put("liveTenders", live);
        kpis.put("runningContracts", running);
        kpis.put("completedContracts", completed);
        kpis.put("approvedBudget", budget.get("approved"));
        kpis.put("allocatedBudget", budget.get("released"));
        kpis.put("expenditure", budget.get("expenditure"));
        kpis.put("remainingBudget", budget.get("remaining"));
        return kpis;
    }

    // -------------------------------------------------------------- APP progress

    /**
     * The Annual Procurement Plan as a funnel: how far the year's packages have walked
     * down the sixteen stages (REQ-1.1, REQ-X5).
     *
     * <p>"Delayed" is a package carrying at least one overdue deadline — a performance
     * security that never arrived, a contract nobody signed in time, a delivery window
     * that has passed. It is deliberately not "has been sitting a while": the deadlines
     * are the dates the client actually holds people to.
     */
    private Map<String, Object> appProgress(List<ProcurementPackage> packages,
                                            Map<Long, List<PackageStage>> stagesByPackage,
                                            List<ProcurementPackage> allPackages,
                                            @Nullable Integer fiscalYear,
                                            LocalDate today) {
        long total = packages.size();
        long published = 0;
        long awarded = 0;
        long underExecution = 0;
        long completed = 0;
        BigDecimal planned = BigDecimal.ZERO;

        for (ProcurementPackage pkg : packages) {
            List<PackageStage> stages = stagesByPackage.getOrDefault(pkg.getId(), List.of());
            boolean closed = satisfied(stages, STAGE_CONTRACT_CLOSE) || "CLOSED".equals(pkg.getStatus());
            if (satisfied(stages, STAGE_TENDER_ADVERTISEMENT)) published++;
            if (satisfied(stages, STAGE_NOA)) awarded++;
            if (satisfied(stages, STAGE_CONTRACT_SIGNING) && !closed) underExecution++;
            if (closed) completed++;
            if (pkg.getPriceLacBdt() != null) {
                planned = planned.add(pkg.getPriceLacBdt().multiply(LAKH));
            }
        }

        Set<Long> overduePackages = deadlineAlertService.openDeadlines(today).stream()
                .filter(d -> d.overdue)
                .map(d -> d.packageId)
                .collect(Collectors.toSet());
        long delayed = packages.stream().filter(p -> overduePackages.contains(p.getId())).count();

        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("totalPackages", total);
        progress.put("plannedBudget", planned);
        progress.put("plannedBudgetCrore", toCrore(planned));
        progress.put("publishedTenders", published);
        progress.put("awarded", awarded);
        progress.put("underExecution", underExecution);
        progress.put("completed", completed);
        progress.put("delayed", delayed);
        progress.put("completionPct", pct(completed, total));

        // Year-on-year movement, the "vs Last Year" line. Null rather than zero when the
        // previous year holds no packages — "no comparison" and "no change" are different
        // answers and the tile should not claim the second one.
        BigDecimal previous = fiscalYear == null ? null
                : completionPctFor(allPackages, fiscalYear - 1);
        progress.put("previousYearCompletionPct", previous);
        progress.put("completionTrendPct", previous == null || fiscalYear == null
                ? null : pct(completed, total).subtract(previous));
        return progress;
    }

    private BigDecimal completionPctFor(List<ProcurementPackage> allPackages, int fiscalYear) {
        List<ProcurementPackage> year = allPackages.stream()
                .filter(p -> Integer.valueOf(fiscalYear).equals(p.getFiscalYear()))
                .collect(Collectors.toList());
        if (year.isEmpty()) {
            return null;
        }
        Set<Long> ids = year.stream().map(ProcurementPackage::getId).collect(Collectors.toSet());
        Map<Long, List<PackageStage>> stages = stageRepository.findAll().stream()
                .filter(s -> ids.contains(s.getPackageId()))
                .collect(Collectors.groupingBy(PackageStage::getPackageId));
        long completed = year.stream()
                .filter(p -> satisfied(stages.getOrDefault(p.getId(), List.of()), STAGE_CONTRACT_CLOSE)
                        || "CLOSED".equals(p.getStatus()))
                .count();
        return pct(completed, year.size());
    }

    // -------------------------------------------------------- tender statistics

    /**
     * Where the year's tenders stand.
     *
     * <p>The buckets are exclusive and tested in this order, so the donut reads as a
     * whole rather than as five overlapping counts. A package that has not been
     * advertised and has never been to market falls in none of them: it is an APP line,
     * not a tender, and padding the total with it would overstate the year's activity.
     *
     * <p>Retendered is a package with more than one attempt on file (Q-2) that is back at
     * the start — a failed attempt is superseded rather than deleted, so the attempt
     * number is the record of how many times a package went back to market. One that has
     * since been re-advertised is reported by where it is now, which is what the panel is
     * asking.
     */
    private List<Map<String, Object>> tenderStatistics(List<ProcurementPackage> packages,
                                                       Map<Long, List<PackageStage>> stagesByPackage,
                                                       List<Tender> tenders) {
        Set<Long> retendered = tenders.stream()
                .filter(t -> t.getAttemptNo() != null && t.getAttemptNo() > 1)
                .map(Tender::getPackageId)
                .collect(Collectors.toSet());

        long awarded = 0;
        long live = 0;
        long underEvaluation = 0;
        long cancelled = 0;
        long retenderedCount = 0;

        for (ProcurementPackage pkg : packages) {
            List<PackageStage> stages = stagesByPackage.getOrDefault(pkg.getId(), List.of());
            if ("CANCELLED".equals(pkg.getStatus())) {
                cancelled++;
            } else if (satisfied(stages, STAGE_NOA)) {
                awarded++;
            } else if (satisfied(stages, STAGE_TENDER_OPENING)) {
                underEvaluation++;
            } else if (satisfied(stages, STAGE_TENDER_ADVERTISEMENT)) {
                live++;
            } else if (retendered.contains(pkg.getId())) {
                retenderedCount++;
            }
        }

        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(bucket("Awarded", awarded));
        stats.add(bucket("Live Tender", live));
        stats.add(bucket("Under Evaluation", underEvaluation));
        stats.add(bucket("Cancelled", cancelled));
        stats.add(bucket("Retendered", retenderedCount));
        return stats;
    }

    /**
     * Count tenders by one of their master-list columns — Nature (Goods/Works/Services)
     * or Method (OTM/LTM/RFQ/DPM). The values come from the master lists (Q-8), so the
     * chart shows whatever the client has configured rather than a hard-coded set.
     */
    private List<Map<String, Object>> groupCount(List<Tender> tenders,
                                                 java.util.function.Function<Tender, String> column) {
        Map<String, Long> counts = new TreeMap<>();
        for (Tender t : tenders) {
            String value = column.apply(t);
            if (value == null || value.isBlank()) {
                continue; // not captured yet — absent, not "Unknown"
            }
            counts.merge(value.trim(), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> bucket(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------- budget

    /**
     * The money position (requirements section 5).
     *
     * <p>Approved is the departments' annual budgets (REQ-B0, Q-13). Released is what
     * package-level RELEASE entries have actually made available, and Expenditure is
     * posted from verified invoices only — never typed (REQ-B5).
     *
     * <p>Remaining is released less expenditure, not available less expenditure: at this
     * altitude the question is how much of the money that has actually been released is
     * still unspent. The per-package figure BudgetService reports is the other one, and
     * both are derived, never stored (REQ-P15).
     */
    private Map<String, Object> budgetPosition(Set<Long> packageIds, @Nullable Integer fiscalYear) {
        BigDecimal approved = departmentBudgetRepository.findAll().stream()
                .filter(db -> fiscalYear == null || fiscalYear.equals(db.getFiscalYear()))
                .map(DepartmentBudget::getAllocatedAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal released = budgetEntryRepository.findAll().stream()
                .filter(e -> packageIds.contains(e.getPackageId()))
                .filter(e -> BudgetService.RELEASE.equals(e.getEntryType()))
                .map(BudgetEntry::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenditure = consumptionRepository.findAll().stream()
                .filter(c -> packageIds.contains(c.getPackageId()))
                .map(BudgetConsumption::getConsumedAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = released.subtract(expenditure);

        Map<String, Object> budget = new LinkedHashMap<>();
        budget.put("approved", approved);
        budget.put("released", released);
        budget.put("expenditure", expenditure);
        budget.put("remaining", remaining);
        budget.put("approvedCrore", toCrore(approved));
        budget.put("releasedCrore", toCrore(released));
        budget.put("expenditureCrore", toCrore(expenditure));
        budget.put("remainingCrore", toCrore(remaining));
        budget.put("utilizationPct", released.signum() == 0 ? BigDecimal.ZERO
                : expenditure.multiply(new BigDecimal("100"))
                        .divide(released, 2, RoundingMode.HALF_UP));
        return budget;
    }

    // ------------------------------------------------------ performance security

    /**
     * The PS/BG position (REQ-E3). Windows are cumulative — a guarantee expiring in five
     * days is inside the 7, 15 and 30 day counts — which is how the client reads them and
     * why the numbers nest rather than sum.
     */
    private Map<String, Object> performanceSecurity(List<ExpiryTracking> expiries, LocalDate today) {
        long active = 0;
        long in30 = 0;
        long in15 = 0;
        long in7 = 0;
        long expired = 0;
        long renewed = 0;

        for (ExpiryTracking t : expiries) {
            if (!ProcurementExpiryService.PERFORMANCE_SECURITY.equals(t.getEntityType())) {
                continue;
            }
            if (t.getStatus() == ExpiryStatus.RENEWED) {
                renewed++;
                continue;
            }
            if (t.getStatus() == ExpiryStatus.EXPIRED) {
                expired++;
                continue;
            }
            if (t.getStatus() != ExpiryStatus.ACTIVE || t.getExpiryDate() == null) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(today, t.getExpiryDate().toLocalDate());
            if (days < 0) {
                // Still marked ACTIVE but the date has passed — the warning pass has not
                // caught up with it. Reporting it as active would hide exactly the row
                // somebody needs to see.
                expired++;
                continue;
            }
            active++;
            if (days <= 30) in30++;
            if (days <= 15) in15++;
            if (days <= 7) in7++;
        }

        Map<String, Object> ps = new LinkedHashMap<>();
        ps.put("active", active);
        ps.put("expiringIn30Days", in30);
        ps.put("expiringIn15Days", in15);
        ps.put("expiringIn7Days", in7);
        ps.put("expired", expired);
        ps.put("renewed", renewed);
        return ps;
    }

    // ------------------------------------------------------- document analytics

    /**
     * The repository's own numbers. These are estate-wide: a document belongs to a
     * package, not to a fiscal year, so filtering them by the FY selector would produce a
     * figure that answers no question anybody asked.
     */
    private Map<String, Object> documentAnalytics() {
        Map<String, Object> docs = new LinkedHashMap<>();
        docs.put("total", documentRepository.countByIsActiveTrue());
        docs.put("todayUploads",
                documentRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay()));

        List<OcrJob> jobs = ocrJobRepository.findAll();
        // Distinct documents, not job rows: a re-run is a second attempt at the same
        // document (REQ-P10) and must not count as a second processed document.
        docs.put("ocrProcessed", jobs.stream()
                .filter(j -> "SUCCESS".equals(j.getStatus()))
                .map(OcrJob::getDocumentId).distinct().count());
        docs.put("ocrPending", jobs.stream().filter(j -> "RUNNING".equals(j.getStatus())).count());
        docs.put("ocrFailed", jobs.stream().filter(j -> "FAILED".equals(j.getStatus())).count());
        docs.put("archived", documentRepository.countArchivedDocuments());
        docs.put("indexed", indexedCount());
        return docs;
    }

    /** Null when Elasticsearch is not reachable — the page renders a dash, not an error. */
    private Long indexedCount() {
        if (documentIndexRepository == null) {
            return null;
        }
        try {
            return documentIndexRepository.count();
        } catch (RuntimeException e) {
            log.debug("Elasticsearch unavailable, reporting an unknown index count: {}", e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------- alerts

    /**
     * The alert list, in the order the client reads it: soonest and most severe first.
     *
     * <p>Rows with a count of zero stay in the list. An executive scanning the panel
     * needs to see that tender validity <em>was</em> checked and found clear, which is a
     * different statement from the row being absent because nothing computed it.
     */
    private List<Map<String, Object>> alerts(List<ExpiryTracking> expiries, LocalDate today,
                                             Map<String, Object> documents) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        alerts.add(alert("TENDER_VALIDITY_7", "danger", "Tender Validity Expiring (7 Days)",
                "tenders require immediate action before validity lapses.",
                expiringWithin(expiries, ProcurementExpiryService.TENDER, today, 7)));
        alerts.add(alert("TENDER_VALIDITY_15", "warn", "Tender Validity Expiring (15 Days)",
                "tenders approaching validity deadline.",
                expiringWithin(expiries, ProcurementExpiryService.TENDER, today, 15)));
        alerts.add(alert("TENDER_VALIDITY_30", "info", "Tender Validity Expiring (30 Days)",
                "tenders to review within the month.",
                expiringWithin(expiries, ProcurementExpiryService.TENDER, today, 30)));
        alerts.add(alert("PS_7", "danger", "Performance Security Expiring (7 Days)",
                "PS/BG instruments require renewal urgently.",
                expiringWithin(expiries, ProcurementExpiryService.PERFORMANCE_SECURITY, today, 7)));
        alerts.add(alert("PS_30", "warn", "Performance Security Expiring (30 Days)",
                "PS/BG instruments to review this month.",
                expiringWithin(expiries, ProcurementExpiryService.PERFORMANCE_SECURITY, today, 30)));
        alerts.add(alert("OCR_PENDING", "neutral", "Documents Pending for OCR",
                "uploaded documents awaiting OCR processing.",
                ((Number) documents.get("ocrPending")).longValue()));
        return alerts;
    }

    private long expiringWithin(List<ExpiryTracking> expiries, String entityType,
                                LocalDate today, int days) {
        return expiries.stream()
                .filter(t -> entityType.equals(t.getEntityType()))
                .filter(t -> t.getStatus() == ExpiryStatus.ACTIVE && t.getExpiryDate() != null)
                .filter(t -> {
                    long remaining = ChronoUnit.DAYS.between(today, t.getExpiryDate().toLocalDate());
                    return remaining >= 0 && remaining <= days;
                })
                .count();
    }

    // -------------------------------------------------------------------- shared

    private static boolean satisfied(List<PackageStage> stages, short stageCode) {
        return stages.stream()
                .filter(s -> s.getStageCode() != null && s.getStageCode() == stageCode)
                .anyMatch(PackageStage::isSatisfied);
    }

    private static Map<String, Object> bucket(String label, long value) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("label", label);
        entry.put("value", value);
        return entry;
    }

    private static Map<String, Object> alert(String key, String tone, String title,
                                             String descriptionSuffix, long count) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("tone", tone);
        entry.put("title", title);
        entry.put("description", count + " " + descriptionSuffix);
        entry.put("count", count);
        return entry;
    }

    private static BigDecimal pct(long part, long whole) {
        if (whole <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part).multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal toCrore(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.divide(CRORE, 2, RoundingMode.HALF_UP);
    }

    /** Fiscal years that actually have packages, newest first — the FY selector's options. */
    private static List<Integer> fiscalYearsPresent(List<ProcurementPackage> packages) {
        return packages.stream()
                .map(ProcurementPackage::getFiscalYear)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }
}
