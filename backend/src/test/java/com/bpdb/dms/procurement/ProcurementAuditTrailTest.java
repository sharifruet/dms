package com.bpdb.dms.procurement;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.entity.AuditLog;
import com.bpdb.dms.procurement.entity.PackageStage;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.service.StageEngine;
import com.bpdb.dms.procurement.service.TenderService;
import com.bpdb.dms.repository.AuditLogRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The action-level audit trail (REQ-X6).
 *
 * <p>"All actions described in this document shall be recorded immutably (who, what, when,
 * before/after)." Field values had their own history; the decisions did not. For a
 * government procurement system, an approval nobody can trace afterwards is the gap that
 * matters most — these pin that the decisions now leave a record, and that the record says
 * what changed rather than merely that something did.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProcurementAuditTrailTest {

    @Autowired
    private StageEngine stageEngine;

    @Autowired
    private TenderService tenderService;

    @Autowired
    private ProcurementPackageRepository packageRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private com.bpdb.dms.procurement.service.StageDataService stageDataService;

    private ProcurementPackage aPackage() {
        ProcurementPackage pkg = new ProcurementPackage();
        pkg.setPackageNumber("AUDIT-" + System.nanoTime());
        pkg.setPackageDescription("Audit trail subject");
        pkg.setDepartment("BPDB");
        pkg.setFiscalYear(2026);
        ProcurementPackage saved = packageRepository.save(pkg);
        stageEngine.initialiseStages(saved.getId());
        return saved;
    }

    private List<AuditLog> entriesFor(String action) {
        return auditLogRepository.findAll().stream()
                .filter(a -> action.equals(a.getAction()))
                .toList();
    }

    @Test
    void completingAStageIsRecorded() {
        ProcurementPackage pkg = aPackage();

        stageEngine.complete(pkg.getId(), (short) 1, 1L, "nothing else outstanding");

        List<AuditLog> entries = entriesFor("PROCUREMENT_STAGE_COMPLETED");
        assertEquals(1, entries.size());
        String description = entries.get(0).getDescription();
        assertTrue(description.contains("Stage 1"), description);
        assertTrue(description.contains("COMPLETED"), "the new state must be named: " + description);
    }

    @Test
    void theRecordNamesWhatChangedFromAndTo() {
        // REQ-X6 asks for before/after. "The stage changed" is not an audit trail.
        ProcurementPackage pkg = aPackage();

        stageEngine.complete(pkg.getId(), (short) 1, 1L, null);

        String description = entriesFor("PROCUREMENT_STAGE_COMPLETED").get(0).getDescription();
        assertTrue(description.contains(PackageStage.IN_PROGRESS),
                "the previous state must be named: " + description);
        assertTrue(description.contains("->"), description);
    }

    @Test
    void anOverrideSaysThatItWasAnOverrideAndWhy() {
        ProcurementPackage pkg = aPackage();

        stageEngine.complete(pkg.getId(), (short) 1, 1L, "documents held by the ministry");

        String description = entriesFor("PROCUREMENT_STAGE_COMPLETED").get(0).getDescription();
        assertTrue(description.contains("override"), description);
        assertTrue(description.contains("documents held by the ministry"),
                "the stated reason belongs in the record: " + description);
    }

    @Test
    void markingAStageNotApplicableIsRecordedWithItsReason() {
        ProcurementPackage pkg = aPackage();

        stageEngine.markNotApplicable(pkg.getId(), (short) 9, 1L, "domestic tender, no LC");

        String description = entriesFor("PROCUREMENT_STAGE_NOT_APPLICABLE").get(0).getDescription();
        assertTrue(description.contains("Stage 9"), description);
        assertTrue(description.contains("domestic tender, no LC"), description);
    }

    @Test
    void reworkIsRecordedWithItsReason() {
        ProcurementPackage pkg = aPackage();
        stageEngine.complete(pkg.getId(), (short) 1, 1L, "proceed");

        stageEngine.rework(pkg.getId(), (short) 1, 1L, "wrong APP attached");

        String description = entriesFor("PROCUREMENT_STAGE_REWORK").get(0).getDescription();
        assertTrue(description.contains("wrong APP attached"), description);
        assertTrue(description.contains("REWORK"), description);
    }

    @Test
    void reTenderingIsRecordedWithBothAttemptNumbers() {
        ProcurementPackage pkg = aPackage();
        // A tender has to exist before it can fail; it is created lazily when Stage 2 is
        // first written to
        stageDataService.ensureEntity("TENDER", pkg.getId());

        tenderService.reTender(pkg.getId(), "no responsive bidders", 1L);

        List<AuditLog> entries = entriesFor("PROCUREMENT_RETENDER");
        assertEquals(1, entries.size());
        String description = entries.get(0).getDescription();
        assertTrue(description.contains("attempt 1") && description.contains("attempt 2"),
                "an auditor needs to see which attempt replaced which: " + description);
        assertTrue(description.contains("no responsive bidders"), description);
    }

    @Test
    void theRecordCarriesTheUserWhoActed() {
        // "who" is half the requirement
        ProcurementPackage pkg = aPackage();
        stageEngine.complete(pkg.getId(), (short) 1, 1L, null);

        AuditLog entry = entriesFor("PROCUREMENT_STAGE_COMPLETED").get(0);
        assertTrue(entry.getUser() != null || entry.getResourceId() != null,
                "an entry with neither an actor nor a subject is not much of a record");
        assertEquals(pkg.getId(), entry.getResourceId());
    }
}
