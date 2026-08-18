package com.bpdb.dms.procurement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.entity.AppLine;
import com.bpdb.dms.procurement.entity.BerBidder;
import com.bpdb.dms.procurement.entity.Contract;
import com.bpdb.dms.procurement.entity.Delivery;
import com.bpdb.dms.procurement.entity.Invoice;
import com.bpdb.dms.procurement.entity.PackageStage;
import com.bpdb.dms.procurement.entity.Payment;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.BerBidderRepository;
import com.bpdb.dms.procurement.repository.DeliveryRepository;
import com.bpdb.dms.procurement.repository.EvaluationRepository;
import com.bpdb.dms.procurement.repository.InvoiceRepository;
import com.bpdb.dms.procurement.repository.LetterOfCreditRepository;
import com.bpdb.dms.procurement.repository.PackageStageRepository;
import com.bpdb.dms.procurement.repository.PaymentRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.repository.TenderOpeningRepository;
import com.bpdb.dms.procurement.repository.TenderRepository;
import com.bpdb.dms.repository.AppLineRepository;

/**
 * Package lifecycle: creation (manually or from an APP line), listing, and reading the
 * linked graph back out for the workspace and the linkage view (REQ-L8).
 */
@Service
public class ProcurementPackageService {

    private static final Logger log = LoggerFactory.getLogger(ProcurementPackageService.class);

    private final ProcurementPackageRepository packageRepository;
    private final AppLineRepository appLineRepository;
    private final StageEngine stageEngine;
    private final StageDefinitionService definitions;
    private final StageDataService stageDataService;
    private final LinkageService linkageService;
    private final BudgetService budgetService;
    private final TenderRepository tenderRepository;
    private final TenderOpeningRepository openingRepository;
    private final EvaluationRepository evaluationRepository;
    private final BerBidderRepository bidderRepository;
    private final LetterOfCreditRepository lcRepository;
    private final DeliveryRepository deliveryRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PackageStageRepository stageRepository;
    private final DeadlineAlertService deadlineAlertService;
    private final ProcurementExpiryService expiryService;

    public ProcurementPackageService(ProcurementPackageRepository packageRepository,
                                     AppLineRepository appLineRepository,
                                     StageEngine stageEngine,
                                     StageDefinitionService definitions,
                                     StageDataService stageDataService,
                                     LinkageService linkageService,
                                     BudgetService budgetService,
                                     TenderRepository tenderRepository,
                                     TenderOpeningRepository openingRepository,
                                     EvaluationRepository evaluationRepository,
                                     BerBidderRepository bidderRepository,
                                     LetterOfCreditRepository lcRepository,
                                     DeliveryRepository deliveryRepository,
                                     InvoiceRepository invoiceRepository,
                                     PaymentRepository paymentRepository,
                                     PackageStageRepository stageRepository,
                                     DeadlineAlertService deadlineAlertService,
                                     ProcurementExpiryService expiryService) {
        this.packageRepository = packageRepository;
        this.appLineRepository = appLineRepository;
        this.stageEngine = stageEngine;
        this.definitions = definitions;
        this.stageDataService = stageDataService;
        this.linkageService = linkageService;
        this.budgetService = budgetService;
        this.tenderRepository = tenderRepository;
        this.openingRepository = openingRepository;
        this.evaluationRepository = evaluationRepository;
        this.bidderRepository = bidderRepository;
        this.lcRepository = lcRepository;
        this.deliveryRepository = deliveryRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.stageRepository = stageRepository;
        this.deadlineAlertService = deadlineAlertService;
        this.expiryService = expiryService;
    }

    @Transactional
    public ProcurementPackage create(ProcurementPackage pkg, Long userId) {
        if (pkg.getPackageNumber() == null || pkg.getPackageNumber().isBlank()) {
            throw new IllegalArgumentException("Package Number is required - it is the key the whole lifecycle hangs off");
        }
        if (packageRepository.existsByPackageNumber(pkg.getPackageNumber())) {
            throw new IllegalArgumentException(
                    "Package " + pkg.getPackageNumber() + " already exists (REQ-1.3)");
        }
        pkg.setCreatedBy(userId);
        pkg.setCurrentStage((short) 1);
        pkg.setStatus("ACTIVE");
        ProcurementPackage saved = packageRepository.save(pkg);
        stageEngine.initialiseStages(saved.getId());
        log.info("Created package {} (id {})", saved.getPackageNumber(), saved.getId());
        return saved;
    }

    /**
     * Create packages from an imported APP line. One line may yield several packages
     * when the procurement is tendered in lots (client answer Q-1), so lot suffixes are
     * supported; pass an empty list for a single package.
     */
    @Transactional
    public List<ProcurementPackage> createFromAppLine(Long appLineId, List<String> lotNumbers, Long userId) {
        AppLine line = appLineRepository.findById(appLineId)
                .orElseThrow(() -> new IllegalArgumentException("APP line not found: " + appLineId));

        List<ProcurementPackage> created = new ArrayList<>();
        List<String> lots = (lotNumbers == null || lotNumbers.isEmpty())
                ? List.of("") : lotNumbers;

        for (String lot : lots) {
            ProcurementPackage pkg = new ProcurementPackage();
            pkg.setAppLineId(appLineId);
            String base = line.getProjectIdentifier() == null
                    ? "PKG-" + appLineId : line.getProjectIdentifier();
            pkg.setPackageNumber(lot.isBlank() ? base : base + "-" + lot);
            pkg.setLotNumber(lot.isBlank() ? null : lot);
            pkg.setPackageDescription(line.getProjectName());
            pkg.setDepartment(line.getDepartment());
            if (line.getBudgetAmount() != null) {
                pkg.setPriceLacBdt(line.getBudgetAmount());
            }
            created.add(create(pkg, userId));
        }
        return created;
    }

    public Page<ProcurementPackage> search(String query, Short stage, String status,
                                           String department, Pageable pageable) {
        return packageRepository.search(
                query == null || query.isBlank() ? null : query, stage, status, department, pageable);
    }

    public Optional<ProcurementPackage> findById(Long id) {
        return packageRepository.findById(id);
    }

    public Optional<ProcurementPackage> findByNumber(String packageNumber) {
        return packageRepository.findByPackageNumber(packageNumber);
    }

    /** Stage-by-stage progress with the blocking reasons already resolved. */
    public List<StageEngine.StageReadiness> progress(Long packageId) {
        List<StageEngine.StageReadiness> result = new ArrayList<>();
        for (PackageStage s : stageEngine.stagesOf(packageId)) {
            result.add(stageEngine.readiness(packageId, s.getStageCode()));
        }
        return result;
    }

    /**
     * The linked graph for a package: everything from the APP down to closure, in the
     * shape the linkage view renders (REQ-L8).
     */
    public Map<String, Object> graph(Long packageId) {
        Map<String, Object> graph = new LinkedHashMap<>();
        ProcurementPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Package not found: " + packageId));
        graph.put("package", pkg);
        graph.put("budget", budgetService.summary(packageId));

        // The current attempt only - superseded re-tenders stay readable through the
        // Stage 2 history panel, not through the live graph (REQ-L15)
        tenderRepository.findByPackageIdAndIsCurrentTrue(packageId).ifPresent(tender -> {
            graph.put("tender", tender);
            graph.put("tenderAttempts", tenderRepository.findByPackageIdOrderByAttemptNoDesc(packageId));
            openingRepository.findByTenderId(tender.getId()).ifPresent(opening -> {
                graph.put("opening", opening);
                evaluationRepository.findByOpeningId(opening.getId()).ifPresent(evaluation -> {
                    graph.put("evaluation", evaluation);
                    List<BerBidder> bidders =
                            bidderRepository.findByEvaluationIdOrderByBidRankAsc(evaluation.getId());
                    graph.put("bidders", bidders);
                });
            });
        });

        Optional<Contract> contract = stageDataService.findContract(packageId);
        contract.ifPresent(c -> {
            graph.put("contract", c);
            graph.put("letterOfCredits", lcRepository.findByContractId(c.getId()));
            List<Delivery> deliveries = deliveryRepository.findByContractId(c.getId());
            graph.put("deliveries", deliveries);
            List<Invoice> invoices = invoiceRepository.findByContractId(c.getId());
            graph.put("invoices", invoices);
            List<Payment> payments = paymentRepository.findByContractId(c.getId());
            graph.put("payments", payments);
        });

        graph.put("documents", linkageService.documentsFor(packageId));
        return graph;
    }

    /**
     * The lifecycle view (REQ-X5): where every package is, how long it has been there,
     * what is overdue, the money position and the expiries still open.
     *
     * <p>Stage distribution alone answers "how busy are we". The requirement asks the
     * harder question — which packages are in trouble — and that needs elapsed time and
     * deadlines beside the counts. A package that has sat at Stage 7 for ninety days is
     * invisible in a bar chart and obvious in a list sorted by how long it has been stuck.
     */
    public Map<String, Object> dashboard() {
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> byStage = new ArrayList<>();
        for (Object[] row : packageRepository.countByStage()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            Short stage = (Short) row[0];
            entry.put("stageCode", stage);
            entry.put("stageName", definitions.stageName(stage == null ? 0 : stage));
            entry.put("count", row[1]);
            byStage.add(entry);
        }
        result.put("byStage", byStage);

        List<ProcurementPackage> active = packageRepository.findByStatus("ACTIVE");
        result.put("totalActive", active.size());
        result.put("totalClosed", packageRepository.findByStatus("CLOSED").size());

        // Elapsed time in the current stage, per package (REQ-X5)
        List<Map<String, Object>> ageing = new ArrayList<>();
        BigDecimal totalAvailable = BigDecimal.ZERO;
        BigDecimal totalConsumed = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;
        int lowBudgetPackages = 0;

        for (ProcurementPackage pkg : active) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("packageId", pkg.getId());
            row.put("packageNumber", pkg.getPackageNumber());
            row.put("stageCode", pkg.getCurrentStage());
            row.put("stageName", pkg.getCurrentStage() == null
                    ? null : definitions.stageName(pkg.getCurrentStage()));

            LocalDateTime enteredAt = currentStageEnteredAt(pkg);
            row.put("stageEnteredAt", enteredAt);
            row.put("daysInStage", enteredAt == null
                    ? null : ChronoUnit.DAYS.between(enteredAt.toLocalDate(), today));
            ageing.add(row);

            BudgetService.BudgetSummary budget = budgetService.summary(pkg.getId());
            totalAvailable = totalAvailable.add(budget.totalAvailable);
            totalConsumed = totalConsumed.add(budget.totalConsumption);
            totalRemaining = totalRemaining.add(budget.remaining);
            if (budget.lowBudget || budget.overspent) {
                lowBudgetPackages++;
            }
        }
        ageing.sort((a, b) -> Long.compare(
                b.get("daysInStage") == null ? -1 : (Long) b.get("daysInStage"),
                a.get("daysInStage") == null ? -1 : (Long) a.get("daysInStage")));
        result.put("stageAgeing", ageing);

        Map<String, Object> budgetPosition = new LinkedHashMap<>();
        budgetPosition.put("available", totalAvailable);
        budgetPosition.put("consumed", totalConsumed);
        budgetPosition.put("remaining", totalRemaining);
        budgetPosition.put("packagesBelowThreshold", lowBudgetPackages);
        result.put("budget", budgetPosition);

        // Overdue deadlines, and the ones about to be (REQ-X5, REQ-X8)
        List<DeadlineAlertService.PackageDeadline> deadlines =
                deadlineAlertService.openDeadlines(today);
        result.put("deadlines", deadlines);
        result.put("overdueDeadlines", deadlines.stream().filter(d -> d.overdue).toList());

        // Open expiries, so the two kinds of date sit side by side (REQ-E4)
        result.put("openExpiries", expiryService.expiringWithin(90));
        return result;
    }

    /**
     * The package's history as a sequence: when each stage opened, when it closed, how
     * long it took, and what is recorded against it.
     *
     * <p>The stage rows already hold this; nothing assembled them in time order before, so
     * the question "how long did tendering actually take" had no answer short of reading
     * the table by hand.
     */
    public List<Map<String, Object>> timeline(Long packageId) {
        List<Map<String, Object>> events = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (PackageStage stage : stageEngine.stagesOf(packageId)) {
            if (stage.getEnteredAt() == null && !stage.isSatisfied()) {
                continue; // never started, nothing to say about it yet
            }
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("stageCode", stage.getStageCode());
            event.put("stageName", definitions.stageName(stage.getStageCode()));
            event.put("status", stage.getStatus());
            event.put("applicable", !Boolean.FALSE.equals(stage.getIsApplicable()));
            event.put("enteredAt", stage.getEnteredAt());
            event.put("completedAt", stage.getCompletedAt());
            event.put("completedBy", stage.getCompletedBy());
            event.put("reworkReason", stage.getReworkReason());

            LocalDateTime from = stage.getEnteredAt();
            LocalDateTime to = stage.getCompletedAt();
            if (from != null) {
                event.put("elapsedDays", ChronoUnit.DAYS.between(
                        from.toLocalDate(), to == null ? today : to.toLocalDate()));
                event.put("open", to == null);
            }
            events.add(event);
        }
        events.sort((a, b) -> Short.compare((Short) a.get("stageCode"), (Short) b.get("stageCode")));
        return events;
    }

    /**
     * When the package entered the stage it is sitting in now.
     *
     * <p>Rework reopens a stage, so this is the last entry rather than the first: "how
     * long has this been stuck" means since it was last picked up, not since it was first
     * touched months ago.
     */
    private LocalDateTime currentStageEnteredAt(ProcurementPackage pkg) {
        if (pkg.getCurrentStage() == null) {
            return null;
        }
        return stageRepository
                .findByPackageIdAndStageCode(pkg.getId(), pkg.getCurrentStage())
                .map(PackageStage::getEnteredAt)
                .orElse(null);
    }
}
