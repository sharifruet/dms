package com.bpdb.dms.procurement.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.procurement.entity.PackageStage;
import com.bpdb.dms.procurement.entity.Tender;
import com.bpdb.dms.procurement.repository.PackageStageRepository;
import com.bpdb.dms.procurement.repository.TenderRepository;

/**
 * Tender attempts (client answer Q-2, REQ-L14/L15).
 *
 * A failed tender is never deleted or edited into the replacement. Re-tendering opens a
 * new attempt under the same package: the package number, its APP linkage and its budget
 * are untouched, and the failed attempt keeps its documents, bidders and BER for the
 * record. Only one attempt is current, and that is the one the stage gates read.
 */
@Service
public class TenderService {

    private static final Logger log = LoggerFactory.getLogger(TenderService.class);

    /** Re-tendering rewinds the package to Stage 2 and reopens Stages 2-4. */
    private static final short TENDER_STAGE = 2;
    private static final short LAST_TENDER_DEPENDENT_STAGE = 7;

    private final TenderRepository tenderRepository;
    private final PackageStageRepository stageRepository;
    private final ProcurementAuditService auditService;

    public TenderService(TenderRepository tenderRepository,
                         PackageStageRepository stageRepository,
                         ProcurementAuditService auditService) {
        this.tenderRepository = tenderRepository;
        this.stageRepository = stageRepository;
        this.auditService = auditService;
    }

    /** The live attempt. Stages 3-7 resolve through this and never see a failed one. */
    public Optional<Tender> current(Long packageId) {
        return tenderRepository.findByPackageIdAndIsCurrentTrue(packageId);
    }

    /** Every attempt, newest first - what the Stage 2 panel lists as re-tender history. */
    public List<Tender> history(Long packageId) {
        return tenderRepository.findByPackageIdOrderByAttemptNoDesc(packageId);
    }

    /**
     * Declare the current tender failed and open a fresh attempt under the same package.
     *
     * The previous attempt is retained: its row stays, its documents stay linked to it,
     * and its bidders and BER remain readable. It is simply no longer current, so it
     * stops driving stage gates and stops appearing in the package's live figures.
     */
    @Transactional
    public Tender reTender(Long packageId, String reason, Long userId) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "A reason is required to declare a tender failed and re-tender (REQ-L14)");
        }

        Tender failed = current(packageId).orElseThrow(() -> new IllegalArgumentException(
                "Package " + packageId + " has no current tender to re-tender"));

        failed.setIsCurrent(Boolean.FALSE);
        failed.setFailureReason(reason);
        tenderRepository.save(failed);

        Tender replacement = new Tender();
        replacement.setPackageId(packageId);
        replacement.setAttemptNo(nextAttemptNo(packageId));
        replacement.setIsCurrent(Boolean.TRUE);
        Tender saved = tenderRepository.save(replacement);

        reopenTenderStages(packageId);
        auditService.reTendered(userId, packageId, failed.getAttemptNo(), saved.getAttemptNo(), reason);

        log.info("Package {} re-tendered by user {}: attempt {} failed ({}), attempt {} opened",
                packageId, userId, failed.getAttemptNo(), reason, saved.getAttemptNo());
        return saved;
    }

    private Integer nextAttemptNo(Long packageId) {
        return tenderRepository.findByPackageIdOrderByAttemptNoDesc(packageId).stream()
                .map(t -> t.getAttemptNo() == null ? 1 : t.getAttemptNo())
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    /**
     * Send Stages 2-7 back to the start of the tender cycle. Their records from the failed
     * attempt are not deleted - they belong to that attempt and stay with it - but the
     * stages themselves must be walked again against the new one.
     */
    private void reopenTenderStages(Long packageId) {
        for (PackageStage stage : stageRepository.findByPackageIdOrderByStageCodeAsc(packageId)) {
            if (stage.getStageCode() < TENDER_STAGE
                    || stage.getStageCode() > LAST_TENDER_DEPENDENT_STAGE) {
                continue;
            }
            stage.setStatus(stage.getStageCode() == TENDER_STAGE
                    ? PackageStage.IN_PROGRESS : PackageStage.NOT_STARTED);
            stage.setCompletedAt(null);
            stage.setCompletedBy(null);
            stage.setReworkReason("Re-tendered - previous tender attempt failed");
            stageRepository.save(stage);
        }
    }
}
