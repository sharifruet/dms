package com.bpdb.dms.procurement.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.service.AppWorkbookParser.AppRow;
import com.bpdb.dms.procurement.service.AppWorkbookParser.ParsedWorkbook;

/**
 * Stage 1: turn an APP workbook into procurement packages (REQ-1.1 … REQ-1.6).
 *
 * <p>This is the front door of the lifecycle - nothing reaches stages 2-16 that did not
 * start here. It is deliberately forgiving about the workbook and unforgiving about the
 * database: a row that cannot be understood is reported and skipped rather than aborting
 * the import, but a row that would collide with an existing package is never silently
 * overwritten (REQ-1.3).
 *
 * <p>The legacy {@code AppDocumentService} still parses a different, finance-shaped APP
 * into {@code app_headers}/{@code app_lines}; per client answer Q-6 the two run
 * independently and neither feeds the other.
 */
@Service
public class AppPackageImportService {

    private static final Logger log = LoggerFactory.getLogger(AppPackageImportService.class);

    /** Q-20: one department for now, and it is this one. */
    public static final String DEFAULT_DEPARTMENT = "BPDB";

    /** The APP lands at Stage 1; its fields are captured against that stage. */
    private static final short STAGE_ONE = 1;

    private final AppWorkbookParser parser;
    private final ProcurementPackageRepository packageRepository;
    private final ProcurementPackageService packageService;
    private final CaptureService captureService;

    public AppPackageImportService(AppWorkbookParser parser,
                                   ProcurementPackageRepository packageRepository,
                                   ProcurementPackageService packageService,
                                   CaptureService captureService) {
        this.parser = parser;
        this.packageRepository = packageRepository;
        this.packageService = packageService;
        this.captureService = captureService;
    }

    /**
     * Import an APP workbook.
     *
     * @param dryRun when true, nothing is written - the report says what would happen.
     *               Worth running first on a file nobody has imported before.
     */
    @Transactional
    public ImportReport importWorkbook(InputStream in, String department, Long userId, boolean dryRun)
            throws IOException {
        ParsedWorkbook parsed = parser.parse(in);
        String dept = (department == null || department.isBlank()) ? DEFAULT_DEPARTMENT : department;

        ImportReport report = new ImportReport();
        report.dryRun = dryRun;
        report.fiscalYear = parsed.fiscalYear;
        report.profileName = parsed.profileName;
        report.rowsRead = parsed.rows.size();
        report.skippedSheets = parsed.skippedSheets;

        // Guards against a workbook that repeats a package on two sheets: the first
        // occurrence wins and the rest are reported, rather than the second failing on a
        // constraint the user cannot see
        Set<String> seenInThisFile = new HashSet<>();

        for (AppRow row : parsed.rows) {
            String packageNumber = effectivePackageNumber(row);
            try {
                if (packageNumber == null || packageNumber.isBlank()) {
                    report.failed.add(new Outcome(row.origin(), null, "No package number"));
                    continue;
                }
                if (!seenInThisFile.add(packageNumber)) {
                    report.skipped.add(new Outcome(row.origin(), packageNumber,
                            "Appears more than once in this workbook - the first occurrence was used"));
                    continue;
                }
                if (packageRepository.existsByPackageNumber(packageNumber)) {
                    report.skipped.add(new Outcome(row.origin(), packageNumber,
                            "Already exists - left untouched (REQ-1.3)"));
                    continue;
                }
                if (row.totalCost == null || row.totalCost.signum() <= 0) {
                    report.failed.add(new Outcome(row.origin(), packageNumber,
                            "No usable Estd. Cost - a package must carry its APP value (REQ-1.4)"));
                    continue;
                }

                if (row.totalCostDerived) {
                    report.warnings.add(new Outcome(row.origin(), packageNumber,
                            "Total Cost was recomputed from quantity x unit cost; the workbook cached no value"));
                }

                if (!dryRun) {
                    ProcurementPackage created =
                            packageService.create(toPackage(row, packageNumber, dept), userId);
                    captureProvenance(created, row, userId);
                }
                report.created.add(new Outcome(row.origin(), packageNumber,
                        row.lotNumber == null ? "Created" : "Created as lot " + row.lotNumber));

            } catch (RuntimeException e) {
                // One bad row must not cost the user the other 99
                log.warn("APP import: {} could not be created: {}", row.origin(), e.getMessage());
                report.failed.add(new Outcome(row.origin(), packageNumber, e.getMessage()));
            }
        }

        log.info("APP import ({}): {} read, {} created, {} skipped, {} failed",
                dryRun ? "dry run" : "applied",
                report.rowsRead, report.created.size(), report.skipped.size(), report.failed.size());
        return report;
    }

    /**
     * The number the package is stored under.
     *
     * <p>An unsplit line uses the APP's own Package No. A line tendered in lots produces
     * one package per lot (Q-1, REQ-L13), and since Package Number is the unique key the
     * whole lifecycle hangs off, each lot needs its own - so the lot is folded into it.
     * The lot is also kept in its own column, so the original APP line is still legible.
     */
    private String effectivePackageNumber(AppRow row) {
        if (row.packageNumber == null) {
            return null;
        }
        String base = row.packageNumber.trim();
        return row.lotNumber == null ? base : base + "-" + row.lotNumber.trim();
    }

    /**
     * Write the provenance rows that go with the typed columns (REQ-P4).
     *
     * Without this the package carries its values but the capture store is empty, so the
     * Stage 1 gate reports every mandatory field as "not captured" and the package cannot
     * be completed — the data is plainly there and the system cannot see it. The two are
     * written in the same transaction precisely so they cannot diverge.
     */
    private void captureProvenance(ProcurementPackage pkg, AppRow row, Long userId) {
        captureOne(pkg, row, userId, "package_number", "Package Number", "TEXT",
                pkg.getPackageNumber());
        captureOne(pkg, row, userId, "package_description", "Package Description", "TEXT",
                row.description);
        captureOne(pkg, row, userId, "approving_authority", "Approving Authority", "TEXT",
                row.approvingAuthority);
        captureOne(pkg, row, userId, "price_lac_bdt", "Price (lac BDT)", "CURRENCY",
                pkg.getPriceLacBdt() == null ? null : pkg.getPriceLacBdt().toPlainString());
    }

    private void captureOne(ProcurementPackage pkg, AppRow row, Long userId,
                            String fieldKey, String fieldLabel, String dataType, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        CaptureService.CaptureRequest req = new CaptureService.CaptureRequest();
        req.entityType = "PACKAGE";
        req.entityId = pkg.getId();
        req.packageId = pkg.getId();
        req.stageCode = STAGE_ONE;
        req.fieldKey = fieldKey;
        req.fieldLabel = fieldLabel;
        req.dataType = dataType;
        req.rawValue = value;
        req.mandatory = Boolean.TRUE;
        req.reason = "Imported from APP " + row.origin();
        captureService.captureImported(req, userId);
    }

    private ProcurementPackage toPackage(AppRow row, String packageNumber, String department) {
        ProcurementPackage pkg = new ProcurementPackage();
        pkg.setPackageNumber(packageNumber);
        pkg.setLotNumber(row.lotNumber);
        pkg.setLotDescription(row.lotNumber == null ? null : row.description);
        pkg.setPackageDescription(row.description);
        pkg.setApprovingAuthority(row.approvingAuthority);
        // The workbook is headed "Fig. In Lac Taka", which is the unit this column expects
        pkg.setPriceLacBdt(scaleAmount(row.totalCost));
        pkg.setFiscalYear(row.fiscalYear);
        pkg.setDepartment(department);
        return pkg;
    }

    private static BigDecimal scaleAmount(BigDecimal v) {
        return v == null ? null : v.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** What an import did, in the shape the Stage 1 screen reports it. */
    public static class ImportReport {
        public boolean dryRun;
        public Integer fiscalYear;
        public String profileName;
        public int rowsRead;
        public List<String> skippedSheets = new ArrayList<>();
        public List<Outcome> created = new ArrayList<>();
        public List<Outcome> skipped = new ArrayList<>();
        public List<Outcome> failed = new ArrayList<>();
        public List<Outcome> warnings = new ArrayList<>();

        public int getCreatedCount() {
            return created.size();
        }

        public int getSkippedCount() {
            return skipped.size();
        }

        public int getFailedCount() {
            return failed.size();
        }

        public boolean isClean() {
            return failed.isEmpty();
        }
    }

    /** One row's fate, with enough context to find it in the spreadsheet. */
    public static class Outcome {
        public String origin;
        public String packageNumber;
        public String reason;

        public Outcome(String origin, String packageNumber, String reason) {
            this.origin = origin;
            this.packageNumber = packageNumber;
            this.reason = reason;
        }
    }
}
