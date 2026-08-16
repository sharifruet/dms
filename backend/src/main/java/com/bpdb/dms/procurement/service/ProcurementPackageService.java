package com.bpdb.dms.procurement.service;

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
                                     PaymentRepository paymentRepository) {
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

    /** Stage distribution for the lifecycle dashboard (REQ-X5). */
    public Map<String, Object> dashboard() {
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
        result.put("totalActive", packageRepository.findByStatus("ACTIVE").size());
        result.put("totalClosed", packageRepository.findByStatus("CLOSED").size());
        return result;
    }
}
