package com.bpdb.dms.procurement.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.procurement.entity.DocumentLink;
import com.bpdb.dms.procurement.entity.ExtractedField;
import com.bpdb.dms.procurement.entity.ExtractedFieldHistory;
import com.bpdb.dms.procurement.entity.OcrResult;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.DocumentLinkRepository;
import com.bpdb.dms.procurement.repository.ExtractedFieldHistoryRepository;
import com.bpdb.dms.procurement.repository.ExtractedFieldRepository;
import com.bpdb.dms.procurement.repository.OcrPageRepository;
import com.bpdb.dms.procurement.repository.OcrResultRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;

/**
 * Retention (REQ-P17, client answer Q-18).
 *
 * <p>The client answered "1 year". That is short for procurement records — shorter than
 * the warranty period on many of these contracts, and far shorter than the statutory
 * retention such records usually attract — so purging is built as something a person does
 * on purpose, never as a scheduled job. Nothing here runs on a clock. A clock cannot be
 * argued with after the fact.
 *
 * <p>Three further safeguards, all deliberate:
 * <ul>
 *   <li>A dry run is the default. You have to ask for the deletion separately, having seen
 *       the counts.</li>
 *   <li>Only <em>closed</em> packages are ever in scope. An open package is live work
 *       whatever its age.</li>
 *   <li>Documents and the values themselves are never deleted — only the accumulated
 *       history behind them: superseded OCR results, and field-change history. Closure
 *       archives, it does not destroy (REQ-P17), and the audit trail of who did what is
 *       out of scope entirely.</li>
 * </ul>
 */
@Service
public class RetentionService {

    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

    /** Q-18. Configurable, because a year is a decision the client may revisit. */
    @Value("${app.procurement.retention-years:1}")
    private int retentionYears;

    private final ProcurementPackageRepository packageRepository;
    private final ExtractedFieldHistoryRepository historyRepository;
    private final ExtractedFieldRepository fieldRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final OcrResultRepository ocrResultRepository;
    private final OcrPageRepository ocrPageRepository;
    private final ProcurementAuditService auditService;

    public RetentionService(ProcurementPackageRepository packageRepository,
                            ExtractedFieldHistoryRepository historyRepository,
                            ExtractedFieldRepository fieldRepository,
                            DocumentLinkRepository documentLinkRepository,
                            OcrResultRepository ocrResultRepository,
                            OcrPageRepository ocrPageRepository,
                            ProcurementAuditService auditService) {
        this.packageRepository = packageRepository;
        this.historyRepository = historyRepository;
        this.fieldRepository = fieldRepository;
        this.documentLinkRepository = documentLinkRepository;
        this.ocrResultRepository = ocrResultRepository;
        this.ocrPageRepository = ocrPageRepository;
        this.auditService = auditService;
    }

    /**
     * What is eligible, and — if {@code dryRun} is false — remove it.
     *
     * @param dryRun true to report only. The caller has to opt into deletion explicitly.
     */
    @Transactional
    public PurgeReport purge(boolean dryRun, Long userId) {
        LocalDateTime cutoff = LocalDateTime.now().minusYears(retentionYears);
        PurgeReport report = new PurgeReport();
        report.dryRun = dryRun;
        report.retentionYears = retentionYears;
        report.cutoff = cutoff;

        List<ProcurementPackage> eligible = new ArrayList<>();
        for (ProcurementPackage pkg : packageRepository.findAll()) {
            // Only closed packages, and only those closed longer ago than the period.
            // updatedAt stands in for "when it was last touched" - a package still being
            // amended is not stale however old it is.
            if (!"CLOSED".equalsIgnoreCase(pkg.getStatus())) {
                continue;
            }
            LocalDateTime lastTouched = pkg.getUpdatedAt() == null
                    ? pkg.getCreatedAt() : pkg.getUpdatedAt();
            if (lastTouched != null && lastTouched.isBefore(cutoff)) {
                eligible.add(pkg);
            }
        }
        report.packagesInScope = eligible.size();

        for (ProcurementPackage pkg : eligible) {
            report.packageNumbers.add(pkg.getPackageNumber());

            // History hangs off the field, and the field carries the package
            for (ExtractedField field : fieldRepository.findByPackageId(pkg.getId())) {
                List<ExtractedFieldHistory> history =
                        historyRepository.findByExtractedFieldIdOrderByVersionDesc(field.getId());
                report.fieldHistoryRows += history.size();
                if (!dryRun) {
                    historyRepository.deleteAll(history);
                }
            }

            // OCR results hang off the document, and documents reach the package through
            // their link rows
            for (DocumentLink link : documentLinkRepository.findByPackageId(pkg.getId())) {
                if (link.getDocumentId() == null) {
                    continue;
                }
                for (OcrResult result
                        : ocrResultRepository.findByDocumentIdOrderByVersionDesc(link.getDocumentId())) {
                    // The current result stays: it is what the extracted values came from.
                    // Only the superseded versions behind it are cleared.
                    if (Boolean.TRUE.equals(result.getIsCurrent())) {
                        continue;
                    }
                    report.supersededOcrResults++;
                    if (!dryRun) {
                        ocrPageRepository.deleteAll(
                                ocrPageRepository.findByResultIdOrderByPageNoAsc(result.getId()));
                        ocrResultRepository.delete(result);
                    }
                }
            }
        }

        String description = (dryRun ? "Retention dry run: " : "Retention purge: ")
                + report.packagesInScope + " closed packages older than " + retentionYears
                + " year(s), " + report.fieldHistoryRows + " field history rows and "
                + report.supersededOcrResults + " superseded OCR results "
                + (dryRun ? "would be removed" : "removed");

        // Purging is itself an auditable act, and the audit entry outlives what it describes
        auditService.retentionPurge(userId, description);
        log.info(description);
        return report;
    }

    /** What a purge would remove, or did. */
    public static class PurgeReport {
        public boolean dryRun;
        public int retentionYears;
        public LocalDateTime cutoff;
        public int packagesInScope;
        public int fieldHistoryRows;
        public int supersededOcrResults;
        public List<String> packageNumbers = new ArrayList<>();
    }
}
