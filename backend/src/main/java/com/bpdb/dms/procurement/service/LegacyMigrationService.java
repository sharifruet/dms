package com.bpdb.dms.procurement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.procurement.entity.DocumentLink;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.DocumentLinkRepository;
import com.bpdb.dms.procurement.repository.ExtractedFieldRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;

/**
 * Phase 7: bringing what the old system holds into the new one.
 *
 * <p>Three moves, each independently runnable and each reporting before it writes:
 *
 * <ul>
 *   <li><b>{@code document_metadata} → {@code extracted_field}</b>. The old store was
 *       untyped key/value with a confidence and nothing else — no verification state, no
 *       history, no owning entity. Rows arrive as {@code OCR_SUGGESTED} rather than
 *       confirmed, because nobody ever confirmed them: importing them as verified would
 *       fabricate a review that never happened.</li>
 *   <li><b>{@code app_lines} → packages</b>, for APP rows imported before the procurement
 *       module existed.</li>
 *   <li><b>Document re-linking</b>, attaching existing documents to a package by the
 *       package number found in their extracted text. Deliberately conservative: only an
 *       unambiguous single match is linked, and everything else is listed for a person.</li>
 * </ul>
 *
 * <p><b>Dry run is the default everywhere.</b> The plan's own instruction on this phase is
 * "do not skip the dry run", and a migration that cannot tell you what it is about to do
 * is not one you can sign off.
 *
 * <p>Nothing here deletes or overwrites. A row that already exists in the new store is
 * left alone and counted as skipped, so the migration can be run twice without harm —
 * which matters, because it will be: once in staging, once for real.
 */
@Service
public class LegacyMigrationService {

    private static final Logger log = LoggerFactory.getLogger(LegacyMigrationService.class);

    private final JdbcTemplate jdbc;
    private final ExtractedFieldRepository fieldRepository;
    private final ProcurementPackageRepository packageRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final CaptureService captureService;
    private final ProcurementPackageService packageService;
    private final ProcurementAuditService auditService;

    public LegacyMigrationService(JdbcTemplate jdbc,
                                  ExtractedFieldRepository fieldRepository,
                                  ProcurementPackageRepository packageRepository,
                                  DocumentLinkRepository documentLinkRepository,
                                  CaptureService captureService,
                                  ProcurementPackageService packageService,
                                  ProcurementAuditService auditService) {
        this.jdbc = jdbc;
        this.fieldRepository = fieldRepository;
        this.packageRepository = packageRepository;
        this.documentLinkRepository = documentLinkRepository;
        this.captureService = captureService;
        this.packageService = packageService;
        this.auditService = auditService;
    }

    // ------------------------------------- document_metadata -> extracted_field

    @Transactional
    public MigrationReport migrateDocumentMetadata(boolean dryRun, Long userId) {
        MigrationReport report = new MigrationReport("document_metadata -> extracted_field", dryRun);

        List<LegacyMetadata> rows = jdbc.query(
                "SELECT dm.id, dm.document_id, dm.metadata_key, dm.metadata_value, dm.source, "
                        + "dm.confidence, dl.package_id, dl.stage_code "
                        + "FROM document_metadata dm "
                        + "LEFT JOIN document_link dl ON dl.document_id = dm.document_id "
                        + "WHERE dm.metadata_value IS NOT NULL AND dm.metadata_value <> ''",
                (rs, i) -> {
                    LegacyMetadata m = new LegacyMetadata();
                    m.id = rs.getLong("id");
                    m.documentId = rs.getLong("document_id");
                    m.key = rs.getString("metadata_key");
                    m.value = rs.getString("metadata_value");
                    m.source = rs.getString("source");
                    m.confidence = rs.getBigDecimal("confidence");
                    m.packageId = (Long) rs.getObject("package_id");
                    m.stageCode = (Integer) rs.getObject("stage_code");
                    return m;
                });
        report.read = rows.size();

        for (LegacyMetadata row : rows) {
            // Without a package the value has nowhere to live in the new model: every
            // captured field hangs off a package (REQ-P8, REQ-L1). These are reported so
            // somebody can link the document first, not silently dropped.
            if (row.packageId == null) {
                report.skipped++;
                report.needsAttention.add("metadata #" + row.id + " ('" + row.key
                        + "') is on document " + row.documentId
                        + ", which is not linked to any package");
                continue;
            }
            short stage = row.stageCode == null ? 1 : row.stageCode.shortValue();

            boolean exists = fieldRepository
                    .findByEntityTypeAndEntityIdAndFieldKey("PACKAGE", row.packageId, row.key)
                    .isPresent();
            if (exists) {
                report.skipped++;
                continue;
            }

            if (!dryRun) {
                CaptureService.CaptureRequest req = new CaptureService.CaptureRequest();
                req.entityType = "PACKAGE";
                req.entityId = row.packageId;
                req.packageId = row.packageId;
                req.stageCode = stage;
                req.fieldKey = row.key;
                req.fieldLabel = row.key;
                req.dataType = "TEXT";
                req.rawValue = row.value;
                req.documentId = row.documentId;
                req.confidence = row.confidence;
                // Migrated, never reviewed: it arrives as a suggestion so the verify
                // screen still asks somebody to stand behind it
                captureService.captureFromOcr(req);
            }
            report.migrated++;
        }

        finish(report, userId);
        return report;
    }

    // ------------------------------------------------ app_lines -> packages

    @Transactional
    public MigrationReport backfillPackagesFromAppLines(boolean dryRun, String department, Long userId) {
        MigrationReport report = new MigrationReport("app_lines -> procurement_package", dryRun);

        List<LegacyAppLine> lines = jdbc.query(
                "SELECT al.id, al.package_no, al.item_description, al.estimated_cost_lakh, "
                        + "al.approving_authority, coalesce(al.department, ah.department) AS department, "
                        + "ah.fiscal_year "
                        + "FROM app_lines al LEFT JOIN app_headers ah ON ah.id = al.header_id",
                (rs, i) -> {
                    LegacyAppLine l = new LegacyAppLine();
                    l.id = rs.getLong("id");
                    l.packageNumber = rs.getString("package_no");
                    l.description = rs.getString("item_description");
                    // Already in lakh, which is the unit procurement_package.price_lac_bdt
                    // expects - no conversion, and none should be invented
                    l.estimatedCost = rs.getBigDecimal("estimated_cost_lakh");
                    l.approvingAuthority = rs.getString("approving_authority");
                    l.department = rs.getString("department");
                    l.fiscalYear = (Integer) rs.getObject("fiscal_year");
                    return l;
                });
        report.read = lines.size();

        for (LegacyAppLine line : lines) {
            if (line.packageNumber == null || line.packageNumber.isBlank()) {
                report.skipped++;
                report.needsAttention.add("app_line #" + line.id + " has no package number");
                continue;
            }
            if (packageRepository.existsByPackageNumber(line.packageNumber)) {
                report.skipped++;
                continue;
            }
            if (!dryRun) {
                ProcurementPackage pkg = new ProcurementPackage();
                pkg.setAppLineId(line.id);
                pkg.setPackageNumber(line.packageNumber);
                pkg.setPackageDescription(line.description);
                pkg.setPriceLacBdt(line.estimatedCost);
                pkg.setApprovingAuthority(line.approvingAuthority);
                pkg.setFiscalYear(line.fiscalYear);
                // The line's own department wins; the parameter is only a fallback for
                // legacy rows that never recorded one
                pkg.setDepartment(line.department == null || line.department.isBlank()
                        ? department : line.department);
                packageService.create(pkg, userId);
            }
            report.migrated++;
        }

        finish(report, userId);
        return report;
    }

    // ------------------------------------------------- documents -> packages

    /**
     * Attach unlinked documents to a package by the package number in their extracted text.
     *
     * <p>Only an unambiguous match links. A document mentioning two package numbers, or
     * none, goes on the list for a person to place — guessing here would put a contract on
     * the wrong package, and a wrong link is worse than an absent one because nobody goes
     * looking for it.
     */
    @Transactional
    public MigrationReport relinkOrphanDocuments(boolean dryRun, Long userId) {
        MigrationReport report = new MigrationReport("documents -> document_link", dryRun);

        List<Long> orphans = jdbc.queryForList(
                "SELECT d.id FROM documents d "
                        + "WHERE d.is_active = true AND NOT EXISTS "
                        + "(SELECT 1 FROM document_link dl WHERE dl.document_id = d.id)",
                Long.class);
        report.read = orphans.size();

        List<ProcurementPackage> packages = packageRepository.findAll();

        for (Long documentId : orphans) {
            String text = Optional.ofNullable(jdbc.queryForObject(
                    "SELECT coalesce(extracted_text, '') FROM documents WHERE id = ?",
                    String.class, documentId)).orElse("");

            List<ProcurementPackage> matches = new ArrayList<>();
            for (ProcurementPackage pkg : packages) {
                if (pkg.getPackageNumber() != null && !pkg.getPackageNumber().isBlank()
                        && text.toUpperCase().contains(pkg.getPackageNumber().toUpperCase())) {
                    matches.add(pkg);
                }
            }

            if (matches.size() != 1) {
                report.skipped++;
                report.needsAttention.add("document " + documentId + (matches.isEmpty()
                        ? " names no known package number"
                        : " names " + matches.size() + " package numbers - link it by hand"));
                continue;
            }

            if (!dryRun) {
                DocumentLink link = new DocumentLink();
                link.setDocumentId(documentId);
                link.setEntityType("PACKAGE");
                link.setEntityId(matches.get(0).getId());
                link.setPackageId(matches.get(0).getId());
                link.setStageCode((short) 1);
                link.setDocRole("MIGRATED");
                link.setLinkOrigin("OCR_KEY_MATCH");
                documentLinkRepository.save(link);
            }
            report.migrated++;
        }

        finish(report, userId);
        return report;
    }

    private void finish(MigrationReport report, Long userId) {
        String description = report.toString();
        auditService.record(userId, "PROCUREMENT_MIGRATION", "PROCUREMENT_MIGRATION", null,
                description);
        log.info(description);
    }

    /** What a migration step did, or would do. */
    public static class MigrationReport {
        public final String step;
        public final boolean dryRun;
        public int read;
        public int migrated;
        public int skipped;
        /** Rows a person has to deal with; the migration will not guess at these. */
        public List<String> needsAttention = new ArrayList<>();

        public MigrationReport(String step, boolean dryRun) {
            this.step = step;
            this.dryRun = dryRun;
        }

        /** True when every row was either migrated or deliberately skipped. */
        public boolean reconciles() {
            return read == migrated + skipped;
        }

        @Override
        public String toString() {
            return (dryRun ? "[dry run] " : "") + step + ": read " + read
                    + ", " + (dryRun ? "would migrate " : "migrated ") + migrated
                    + ", skipped " + skipped
                    + ", needing attention " + needsAttention.size()
                    + (reconciles() ? "" : " - COUNTS DO NOT RECONCILE");
        }
    }

    private static class LegacyMetadata {
        Long id;
        Long documentId;
        String key;
        String value;
        String source;
        java.math.BigDecimal confidence;
        Long packageId;
        Integer stageCode;
    }

    private static class LegacyAppLine {
        Long id;
        String packageNumber;
        String description;
        java.math.BigDecimal estimatedCost;
        String approvingAuthority;
        String department;
        Integer fiscalYear;
    }
}
