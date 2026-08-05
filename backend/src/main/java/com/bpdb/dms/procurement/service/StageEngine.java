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
import com.bpdb.dms.procurement.repository.DocumentLinkRepository;
import com.bpdb.dms.procurement.repository.ExtractedFieldRepository;
import com.bpdb.dms.procurement.repository.PackageStageRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;

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

    private final ProcurementPackageRepository packageRepository;
    private final PackageStageRepository stageRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final ExtractedFieldRepository fieldRepository;
    private final StageDefinitionService definitions;
    private final ValidationService validationService;

    public StageEngine(ProcurementPackageRepository packageRepository,
                       PackageStageRepository stageRepository,
                       DocumentLinkRepository documentLinkRepository,
                       ExtractedFieldRepository fieldRepository,
                       StageDefinitionService definitions,
                       ValidationService validationService) {
        this.packageRepository = packageRepository;
        this.stageRepository = stageRepository;
        this.documentLinkRepository = documentLinkRepository;
        this.fieldRepository = fieldRepository;
        this.definitions = definitions;
        this.validationService = validationService;
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

        // Gate 2 - every mandatory document uploaded
        Set<String> uploaded = documentLinkRepository
                .findByPackageIdAndStageCode(packageId, stageCode).stream()
                .map(DocumentLink::getDocRole)
                .collect(Collectors.toCollection(HashSet::new));
        for (StageDocumentRequirement req : definitions.blockingDocuments(stageCode)) {
            if (!uploaded.contains(req.getDocRole())) {
                result.missingDocuments.add(req.getDocLabel());
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
        stage.setStatus(PackageStage.COMPLETED);
        stage.setCompletedAt(LocalDateTime.now());
        stage.setCompletedBy(userId);
        if (overrideReason != null && !overrideReason.isBlank()) {
            stage.setReworkReason("Completed with override: " + overrideReason);
            log.warn("Stage {} of package {} completed by override ({}) by user {}",
                    stageCode, packageId, overrideReason, userId);
        }
        stageRepository.save(stage);

        openNextApplicableStage(packageId, stageCode);
        return stage;
    }

    /** Mark a stage Not Applicable, e.g. Stage 9 for a non-LC contract (REQ-9.5). */
    @Transactional
    public PackageStage markNotApplicable(Long packageId, short stageCode, Long userId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to mark a stage Not Applicable");
        }
        PackageStage stage = stage(packageId, stageCode);
        stage.setIsApplicable(Boolean.FALSE);
        stage.setNotApplicableReason(reason);
        stage.setCompletedBy(userId);
        stage.setCompletedAt(LocalDateTime.now());
        stageRepository.save(stage);
        openNextApplicableStage(packageId, stageCode);
        return stage;
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
        stage.setStatus(PackageStage.REWORK);
        stage.setReworkReason(reason);
        stage.setCompletedAt(null);
        stage.setCompletedBy(null);
        stageRepository.save(stage);

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

    /** The reasons a stage is not ready, in the shape the UI renders them. */
    public static class StageReadiness {
        public Long packageId;
        public short stageCode;
        public String stageName;
        public String status;
        public boolean applicable = true;
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
