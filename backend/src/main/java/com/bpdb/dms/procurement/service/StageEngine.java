package com.bpdb.dms.procurement.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.entity.DocumentTypeField;
import com.bpdb.dms.procurement.entity.DocumentLink;
import com.bpdb.dms.procurement.entity.ExtractedField;
import com.bpdb.dms.procurement.entity.PackageStage;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.entity.StageDocumentRequirement;
import com.bpdb.dms.procurement.entity.Tender;
import com.bpdb.dms.procurement.repository.ContractClosureRepository;
import com.bpdb.dms.procurement.repository.DocumentLinkRepository;
import com.bpdb.dms.procurement.repository.ExtractedFieldRepository;
import com.bpdb.dms.procurement.repository.PackageStageRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.repository.TenderRepository;

/**
 * Owns stage progression. Nothing else in the system writes package_stage.status.
 *
 * A stage is not "marked" complete - completion is computed: every mandatory document
 * present, every mandatory field confirmed, and every validation passing. If a caller
 * asks to complete a stage that is not ready, they get back the list of reasons rather
 * than a silent failure (WF-01 .. WF-10).
 */
@Service
public class StageEngine {

    private static final Logger log = LoggerFactory.getLogger(StageEngine.class);

    /** Letter of Credit. Applicable to international tenders only (Q-5, REQ-9.5). */
    private static final short LC_STAGE = 9;

    private final ProcurementPackageRepository packageRepository;
    private final PackageStageRepository stageRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final ExtractedFieldRepository fieldRepository;
    private final TenderRepository tenderRepository;
    private final StageDefinitionService definitions;
    private final ValidationService validationService;
    private final ProcurementAuditService auditService;
    private final ProcurementExpiryService expiryService;
    private final ConditionalRequirementService conditionalRequirements;
    private final BudgetService budgetService;
    private final ContractClosureRepository closureRepository;

    public StageEngine(ProcurementPackageRepository packageRepository,
                       PackageStageRepository stageRepository,
                       DocumentLinkRepository documentLinkRepository,
                       ExtractedFieldRepository fieldRepository,
                       TenderRepository tenderRepository,
                       StageDefinitionService definitions,
                       ValidationService validationService,
                       ProcurementAuditService auditService,
                       ProcurementExpiryService expiryService,
                       ConditionalRequirementService conditionalRequirements,
                       BudgetService budgetService,
                       ContractClosureRepository closureRepository) {
        this.packageRepository = packageRepository;
        this.stageRepository = stageRepository;
        this.documentLinkRepository = documentLinkRepository;
        this.fieldRepository = fieldRepository;
        this.tenderRepository = tenderRepository;
        this.definitions = definitions;
        this.validationService = validationService;
        this.auditService = auditService;
        this.expiryService = expiryService;
        this.conditionalRequirements = conditionalRequirements;
        this.budgetService = budgetService;
        this.closureRepository = closureRepository;
    }

    /** Create the 16 stage rows for a new package and open stage 1 (WF-02). */
    @Transactional
    public List<PackageStage> initialiseStages(Long packageId) {
        List<PackageStage> stages = new ArrayList<>();
        for (short code = StageDefinitionService.FIRST_STAGE; code <= StageDefinitionService.LAST_STAGE; code++) {
            PackageStage s = new PackageStage();
            s.setPackageId(packageId);
            s.setStageCode(code);
            s.setStatus(code == StageDefinitionService.FIRST_STAGE
                    ? PackageStage.IN_PROGRESS : PackageStage.NOT_STARTED);
            if (code == StageDefinitionService.FIRST_STAGE) {
                s.setEnteredAt(LocalDateTime.now());
            }
            stages.add(stageRepository.save(s));
        }
        return stages;
    }

    public List<PackageStage> stagesOf(Long packageId) {
        return stageRepository.findByPackageIdOrderByStageCodeAsc(packageId);
    }

    public PackageStage stage(Long packageId, short stageCode) {
        return stageRepository.findByPackageIdAndStageCode(packageId, stageCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stage " + stageCode + " not found for package " + packageId));
    }

    /**
     * Why a stage cannot be completed right now. Empty list means it is ready.
     * This is what the UI shows next to a disabled Complete button.
     */
    public StageReadiness readiness(Long packageId, short stageCode) {
        StageReadiness result = new StageReadiness();
        result.packageId = packageId;
        result.stageCode = stageCode;
        result.stageName = definitions.stageName(stageCode);

        PackageStage stage = stage(packageId, stageCode);
        result.status = stage.getStatus();
        result.applicable = !Boolean.FALSE.equals(stage.getIsApplicable());
        // Stage 9 opens with applicability suggested by the tender's Procurement Type, so
        // the UI can pre-set the toggle rather than making the user guess (Q-5, REQ-2.5)
        result.applicabilitySuggested = stageCode != LC_STAGE || lcExpected(packageId);

        if (!result.applicable) {
            result.ready = true;
            return result;
        }

        // Gate 1 - the previous stage must be satisfied (WF-02)
        if (stageCode > StageDefinitionService.FIRST_STAGE) {
            PackageStage previous = stage(packageId, (short) (stageCode - 1));
            if (!previous.isSatisfied()) {
                result.blockers.add("Stage " + previous.getStageCode() + " ("
                        + definitions.stageName(previous.getStageCode()) + ") is not complete");
            }
        }

        // Gate 1b - closing the contract checks *every* prior stage, not just the one
        // before it, and names each one that is outstanding (REQ-16.1). Rework can leave
        // an earlier stage open while the ones after it are done, so "the previous stage
        // is complete" is not the same statement as "nothing is outstanding".
        if (stageCode == StageDefinitionService.LAST_STAGE) {
            for (PackageStage s : stagesOf(packageId)) {
                if (s.getStageCode() < StageDefinitionService.LAST_STAGE && !s.isSatisfied()) {
                    result.blockers.add("Stage " + s.getStageCode() + " ("
                            + definitions.stageName(s.getStageCode())
                            + ") is outstanding and must be completed or marked Not Applicable "
                            + "before closure (REQ-16.1)");
                }
            }
        }

        // Gate 2 - every mandatory document uploaded, plus any conditional document this
        // package has switched on (REQ-11.3: a declared SAT becomes mandatory)
        Set<String> uploaded = documentLinkRepository
                .findByPackageIdAndStageCode(packageId, stageCode).stream()
                .map(DocumentLink::getDocRole)
                .collect(Collectors.toCollection(HashSet::new));
        Set<String> activeConditionals =
                conditionalRequirements.activeConditionalRoles(packageId, stageCode);
        List<StageDocumentRequirement> blocking = conditionalRequirements.blockingWithConditionals(
                definitions.requiredDocuments(stageCode), activeConditionals);
        for (StageDocumentRequirement req : blocking) {
            if (!uploaded.contains(req.getDocRole())) {
                result.missingDocuments.add(activeConditionals.contains(req.getDocRole())
                        ? req.getDocLabel() + " (declared applicable)"
                        : req.getDocLabel());
            }
        }

        // Gate 3 - every mandatory field confirmed by a person (WF-03)
        List<ExtractedField> unconfirmed = fieldRepository.findUnconfirmedMandatory(packageId, stageCode);
        for (ExtractedField f : unconfirmed) {
            result.unconfirmedFields.add(f.getFieldLabel() == null ? f.getFieldKey() : f.getFieldLabel());
        }
        // A mandatory field with no row at all is also missing
        Set<String> present = fieldRepository.findByPackageIdAndStageCode(packageId, stageCode).stream()
                .map(ExtractedField::getFieldKey).collect(Collectors.toSet());
        for (DocumentTypeField def : definitions.mandatoryFields(stageCode)) {
            if (!present.contains(def.getFieldKey())) {
                result.unconfirmedFields.add(def.getFieldLabel() + " (not captured)");
            }
        }

        // Gate 4 - cross-stage validations
        result.validationErrors.addAll(validationService.validateStage(packageId, stageCode));

        result.ready = result.blockers.isEmpty()
                && result.missingDocuments.isEmpty()
                && result.unconfirmedFields.isEmpty()
                && result.validationErrors.isEmpty();
        return result;
    }

    /**
     * Complete a stage and open the next one. Throws with the blocking reasons if the
     * stage is not ready, unless an authorised caller supplies an override reason.
     */
    @Transactional
    public PackageStage complete(Long packageId, short stageCode, Long userId, String overrideReason) {
        StageReadiness readiness = readiness(packageId, stageCode);
        if (!readiness.ready && (overrideReason == null || overrideReason.isBlank())) {
            throw new StageNotReadyException(readiness);
        }

        PackageStage stage = stage(packageId, stageCode);
        String previousStatus = stage.getStatus();
        stage.setStatus(PackageStage.COMPLETED);
        stage.setCompletedAt(LocalDateTime.now());
        stage.setCompletedBy(userId);
        if (overrideReason != null && !overrideReason.isBlank()) {
            stage.setReworkReason("Completed with override: " + overrideReason);
            log.warn("Stage {} of package {} completed by override ({}) by user {}",
                    stageCode, packageId, overrideReason, userId);
        }
        stageRepository.save(stage);
        auditService.stageCompleted(userId, packageId, stageCode, previousStatus, overrideReason);

        openNextApplicableStage(packageId, stageCode);
        return stage;
    }

    /**
     * Mark a stage Not Applicable - in practice Stage 9 for a contract with no LC.
     *
     * Any authorised user may do this; there is no separate approval step (client answer
     * Q-5, REQ-9.6). A reason is mandatory and the action is logged, including when it
     * contradicts the applicability derived from the tender.
     */
    @Transactional
    public PackageStage markNotApplicable(Long packageId, short stageCode, Long userId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to mark a stage Not Applicable");
        }
        PackageStage stage = stage(packageId, stageCode);
        if (stageCode == LC_STAGE && lcExpected(packageId)) {
            log.warn("Package {} is an ICT tender but Stage 9 (LC) was marked Not Applicable "
                    + "by user {}: {} - overriding the derived default (REQ-9.6)",
                    packageId, userId, reason);
        }
        stage.setIsApplicable(Boolean.FALSE);
        stage.setNotApplicableReason(reason);
        stage.setCompletedBy(userId);
        stage.setCompletedAt(LocalDateTime.now());
        stageRepository.save(stage);
        auditService.stageMarkedNotApplicable(userId, packageId, stageCode, reason);
        openNextApplicableStage(packageId, stageCode);
        return stage;
    }

    /**
     * Does this package's tender suggest a Letter of Credit is needed?
     *
     * Derived from Procurement Type on the Tender Notice: ICT means an international
     * tender, which may require an LC (Q-5, REQ-2.5). This is the default the Stage 9
     * panel opens with - a suggestion, not a lock.
     */
    public boolean lcExpected(Long packageId) {
        return tenderRepository.findByPackageIdAndIsCurrentTrue(packageId)
                .map(Tender::isInternational)
                .orElse(false);
    }

    /**
     * Send a stage back for rework. Downstream records keep their links; they are not
     * destroyed, they are marked as needing revalidation (REQ-L11, WF-07).
     */
    @Transactional
    public PackageStage rework(Long packageId, short stageCode, Long userId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to send a stage back for rework");
        }
        PackageStage stage = stage(packageId, stageCode);
        String previousStatus = stage.getStatus();
        stage.setStatus(PackageStage.REWORK);
        stage.setReworkReason(reason);
        stage.setCompletedAt(null);
        stage.setCompletedBy(null);
        stageRepository.save(stage);
        auditService.stageReworked(userId, packageId, stageCode, previousStatus, reason);

        for (PackageStage downstream : stagesOf(packageId)) {
            if (downstream.getStageCode() > stageCode
                    && !PackageStage.NOT_STARTED.equals(downstream.getStatus())) {
                downstream.setStatus(PackageStage.PENDING_VERIFICATION);
                downstream.setReworkReason("Upstream stage " + stageCode + " sent back for rework");
                stageRepository.save(downstream);
            }
        }

        ProcurementPackage pkg = packageRepository.findById(packageId).orElseThrow();
        pkg.setCurrentStage(stageCode);
        packageRepository.save(pkg);

        log.info("Package {} stage {} sent back for rework by user {}: {}",
                packageId, stageCode, userId, reason);
        return stage;
    }

    private void openNextApplicableStage(Long packageId, short completedStage) {
        short next = (short) (completedStage + 1);
        if (next > StageDefinitionService.LAST_STAGE) {
            ProcurementPackage pkg = packageRepository.findById(packageId).orElseThrow();
            pkg.setStatus("CLOSED");
            packageRepository.save(pkg);
            // The instruments the trackers guard are released at contract close, so the
            // trackers go with them - otherwise a closed package keeps raising warnings
            // about an LC nobody is holding any more (REQ-E6, REQ-16.2).
            int closedTrackers = expiryService.closeAllForPackage(packageId);
            if (closedTrackers > 0) {
                log.info("Package {} closed at stage {}; {} expiry tracker(s) closed with it",
                        packageId, completedStage, closedTrackers);
            }
            recordClosureReconciliation(packageId);
            return;
        }
        Optional<PackageStage> nextStage = stageRepository.findByPackageIdAndStageCode(packageId, next);
        if (nextStage.isPresent()) {
            PackageStage s = nextStage.get();
            if (PackageStage.NOT_STARTED.equals(s.getStatus())) {
                s.setStatus(PackageStage.IN_PROGRESS);
                s.setEnteredAt(LocalDateTime.now());
                stageRepository.save(s);
            }
        }
        ProcurementPackage pkg = packageRepository.findById(packageId).orElseThrow();
        if (pkg.getCurrentStage() == null || pkg.getCurrentStage() < next) {
            pkg.setCurrentStage(next);
            packageRepository.save(pkg);
        }
    }

    /**
     * Reconcile the money at closure and write the result where it will still be readable
     * in a year (REQ-16.3).
     *
     * <p>The residual is what was released but never consumed — the figure the finance
     * side needs in order to give the money back. Computing it at closure and storing it
     * on the closure record matters because every input to it can move afterwards: a
     * budget line can be revised, an invoice can be corrected. The reconciliation is a
     * statement about the moment the package closed, so it is recorded, not derived on
     * demand.
     */
    private void recordClosureReconciliation(Long packageId) {
        BudgetService.ClosureReconciliation reconciliation =
                budgetService.reconcileAtClosure(packageId);
        if (reconciliation == null) {
            // Reporting the money is part of closing, but it is not what closing *is*:
            // a package whose figures cannot be summarised is still closed, and failing
            // here would roll the closure back over a note
            log.warn("Package {} closed but no budget reconciliation was produced", packageId);
            return;
        }

        Long contractId = reconciliation.contractId;
        if (contractId != null) {
            closureRepository.findByContractId(contractId).ifPresent(closure -> {
                closure.setOutstandingNotes(reconciliation.summary());
                closureRepository.save(closure);
            });
        }

        log.info("Package {} closed. {}", packageId, reconciliation.summary());
        auditService.recordChange(null, "PROCUREMENT_PACKAGE_CLOSED",
                ProcurementAuditService.PACKAGE, packageId,
                "Package closed", null, reconciliation.summary(),
                "Budget reconciled at closure (REQ-16.3)");
    }

    /** The reasons a stage is not ready, in the shape the UI renders them. */
    public static class StageReadiness {
        public Long packageId;
        public short stageCode;
        public String stageName;
        public String status;
        public boolean applicable = true;
        /** What the system derives this stage's applicability should be (Q-5, REQ-2.5). */
        public boolean applicabilitySuggested = true;
        public boolean ready;
        public List<String> blockers = new ArrayList<>();
        public List<String> missingDocuments = new ArrayList<>();
        public List<String> unconfirmedFields = new ArrayList<>();
        public List<String> validationErrors = new ArrayList<>();

        public List<String> allReasons() {
            List<String> all = new ArrayList<>(blockers);
            missingDocuments.forEach(d -> all.add("Missing document: " + d));
            unconfirmedFields.forEach(f -> all.add("Unverified field: " + f));
            all.addAll(validationErrors);
            return all;
        }
    }

    public static class StageNotReadyException extends RuntimeException {
        private final transient StageReadiness readiness;

        public StageNotReadyException(StageReadiness readiness) {
            super("Stage " + readiness.stageCode + " is not ready: "
                    + String.join("; ", readiness.allReasons()));
            this.readiness = readiness;
        }

        public StageReadiness getReadiness() {
            return readiness;
        }
    }
}
