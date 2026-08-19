package com.bpdb.dms.procurement;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import com.bpdb.dms.procurement.entity.ExtractedField;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.entity.Tender;
import com.bpdb.dms.procurement.repository.ExtractedFieldRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.service.AppPackageImportService;
import com.bpdb.dms.procurement.service.AppPackageImportService.ImportReport;
import com.bpdb.dms.procurement.service.CaptureService;
import com.bpdb.dms.procurement.service.StageDataService;
import com.bpdb.dms.procurement.service.StageEngine;
import com.bpdb.dms.procurement.service.TenderService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * One package, from the client's own workbook through the early stages, against the real
 * PostgreSQL schema built by Liquibase.
 *
 * <p>Deliberately not transactional and deliberately ordered: this is meant to behave like
 * a person using the system, where each step lands for real and the next one has to cope
 * with what the last one left behind. Rolling back after every method would hide exactly
 * the kind of problem it exists to find.
 *
 * <p>Everything here was verified by hand during the first end-to-end run. That run
 * uncovered three defects that the entire unit suite was blind to — an application that
 * would not start, a schema that rejected every field capture, and an importer whose
 * packages could never leave Stage 1. This is that run, automated, so the next one is
 * caught by a build rather than by a person.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LifecycleSmokeTest extends PostgresLiquibaseTest {

    private static final File WORKBOOK =
            new File("../requirements/APP 22-23 First Revision_2980.xls");

    private static final String PACKAGE_UNDER_TEST = "GRL-18";

    @Autowired
    private AppPackageImportService importService;

    @Autowired
    private ProcurementPackageRepository packageRepository;

    @Autowired
    private ExtractedFieldRepository fieldRepository;

    @Autowired
    private StageEngine stageEngine;

    @Autowired
    private StageDataService stageDataService;

    @Autowired
    private com.bpdb.dms.procurement.service.LinkageService linkageService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private TenderService tenderService;

    @Autowired
    private CaptureService captureService;

    /** A real row in documents, so the link's foreign key is satisfied honestly. */
    private long insertDocument(String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO documents (file_name, file_path, file_size, document_type, "
                        + "uploaded_by, is_active, is_archived, created_at, updated_at) "
                        + "VALUES (?, ?, ?, 'OTHER', "
                        + "(SELECT id FROM users ORDER BY id LIMIT 1), true, false, now(), now()) "
                        + "RETURNING id",
                Long.class, name, "uploads/" + name, 1024L);
    }

    private ProcurementPackage subject() {
        return packageRepository.findByPackageNumber(PACKAGE_UNDER_TEST).orElseThrow(
                () -> new AssertionError(PACKAGE_UNDER_TEST + " was not imported"));
    }

    @Test
    @Order(1)
    void theSuppliedWorkbookImports() throws Exception {
        assumeTrue(WORKBOOK.exists(), "Supplied APP workbook not found");

        ImportReport report;
        try (InputStream in = new FileInputStream(WORKBOOK)) {
            report = importService.importWorkbook(in, "BPDB", 1L, false);
        }

        assertEquals(7, report.getCreatedCount(), "the workbook holds 7 package rows");
        assertEquals(0, report.getFailedCount(), "and none of them should fail");
        assertEquals(2022, report.fiscalYear);
    }

    @Test
    @Order(2)
    void importedValuesSatisfyTheStageOneFieldGate() {
        // The defect this replaces: the package carried its values while the gate reported
        // every one of them as "not captured", so no imported package could ever advance
        ProcurementPackage pkg = subject();
        List<ExtractedField> captured =
                fieldRepository.findByPackageIdAndStageCode(pkg.getId(), (short) 1);

        assertEquals(4, captured.size());
        assertTrue(captured.stream().allMatch(ExtractedField::isConfirmed));
        assertTrue(stageEngine.readiness(pkg.getId(), (short) 1).unconfirmedFields.isEmpty());
    }

    @Test
    @Order(3)
    void anEmptyOcrPassDoesNotUndoTheImport() {
        // Uploading a document runs extraction. When it finds nothing — a poor scan, or no
        // OCR engine at all — it must leave the imported values alone. It used to null them.
        ProcurementPackage pkg = subject();

        CaptureService.CaptureRequest emptyRead = new CaptureService.CaptureRequest();
        emptyRead.entityType = "PACKAGE";
        emptyRead.entityId = pkg.getId();
        emptyRead.packageId = pkg.getId();
        emptyRead.stageCode = (short) 1;
        emptyRead.fieldKey = "package_number";
        emptyRead.dataType = "TEXT";
        captureService.captureFromOcr(emptyRead);

        ExtractedField number = fieldRepository
                .findByPackageIdAndStageCode(pkg.getId(), (short) 1).stream()
                .filter(f -> "package_number".equals(f.getFieldKey())).findFirst().orElseThrow();

        assertEquals(PACKAGE_UNDER_TEST, number.displayValue());
        assertTrue(stageEngine.readiness(pkg.getId(), (short) 1).unconfirmedFields.isEmpty());
    }

    @Test
    @Order(4)
    void importedWorkbookSatisfiesTheAppDocumentRequirement() {
        // The workbook is the APP Document; the approval memo is optional. After import
        // the only remaining Stage 1 action is to complete it (REQ-1 exit criteria).
        StageEngine.StageReadiness readiness = stageEngine.readiness(subject().getId(), (short) 1);

        assertTrue(readiness.missingDocuments.stream()
                        .noneMatch(d -> d.toLowerCase().contains("approval memo")),
                "APP Approval Memo must not block Stage 1");
        assertTrue(readiness.missingDocuments.isEmpty(),
                "the imported workbook should already satisfy APP Document");
    }

    @Test
    @Order(5)
    void stageOneCompletesOnceItIsGenuinelyReady() {
        ProcurementPackage pkg = subject();
        // Import already linked the workbook as APP_DOCUMENT. A second file is only
        // needed when that link is missing (H2 tests without a real user).
        StageEngine.StageReadiness before = stageEngine.readiness(pkg.getId(), (short) 1);
        if (!before.missingDocuments.isEmpty()) {
            long appDoc = insertDocument("app.pdf");
            linkageService.link(appDoc, "PACKAGE", pkg.getId(), pkg.getId(), null, (short) 1,
                    "APP_DOCUMENT", "STAGE_CONTEXT", 1L);
        }

        assertTrue(stageEngine.readiness(pkg.getId(), (short) 1).ready,
                "everything Stage 1 asks for is now present");

        stageEngine.complete(pkg.getId(), (short) 1, 1L, null);

        assertEquals("COMPLETED", stageEngine.stage(pkg.getId(), (short) 1).getStatus());
        assertEquals((short) 2,
                packageRepository.findById(pkg.getId()).orElseThrow().getCurrentStage(),
                "completing Stage 1 opens Stage 2");
    }

    @Test
    @Order(6)
    void anIctTenderMakesTheLetterOfCreditStageApplicable() {
        ProcurementPackage pkg = subject();
        stageDataService.saveStageValues(pkg.getId(), (short) 2,
                java.util.Map.of("procurement_type", "ICT", "procurement_method", "OTM"), 1L);

        assertTrue(stageEngine.lcExpected(pkg.getId()),
                "ICT is what makes Stage 9 applicable (Q-5)");
        assertTrue(stageEngine.readiness(pkg.getId(), (short) 9).applicabilitySuggested);
    }

    @Test
    @Order(7)
    void reTenderingKeepsTheFailedAttemptAndReopensTheTenderStages() {
        ProcurementPackage pkg = subject();

        Tender replacement = tenderService.reTender(pkg.getId(), "No responsive bidders", 1L);

        assertEquals(2, replacement.getAttemptNo());
        assertTrue(replacement.getIsCurrent());

        List<Tender> history = tenderService.history(pkg.getId());
        assertEquals(2, history.size(), "the failed attempt is kept, not replaced");
        Tender failed = history.stream().filter(t -> t.getAttemptNo() == 1).findFirst().orElseThrow();
        assertEquals("No responsive bidders", failed.getFailureReason());
        assertEquals("ICT", failed.getProcurementType(), "its captured data survives");

        // Stage 1 stands; the tender stages go back to be walked against the new attempt
        assertEquals("COMPLETED", stageEngine.stage(pkg.getId(), (short) 1).getStatus());
        assertEquals("IN_PROGRESS", stageEngine.stage(pkg.getId(), (short) 2).getStatus());
    }

    @Test
    @Order(8)
    void theApValueSurvivesTheWholeJourney() {
        // The number that matters: what the APP said this package is worth, unchanged by
        // everything that has happened to it
        ProcurementPackage pkg = subject();
        assertEquals(0, new BigDecimal("60.00").compareTo(pkg.getPriceLacBdt()));
        assertEquals("BPDB", pkg.getDepartment());
        assertEquals(2022, pkg.getFiscalYear());
    }
}
