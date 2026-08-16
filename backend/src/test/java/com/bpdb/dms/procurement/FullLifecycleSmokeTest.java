package com.bpdb.dms.procurement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import com.bpdb.dms.entity.DocumentTypeField;
import com.bpdb.dms.procurement.entity.BerBidder;
import com.bpdb.dms.procurement.entity.Delivery;
import com.bpdb.dms.procurement.entity.InspectionEvent;
import com.bpdb.dms.procurement.entity.Invoice;
import com.bpdb.dms.procurement.entity.Payment;
import com.bpdb.dms.procurement.entity.PackageStage;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.entity.StageDocumentRequirement;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.service.BudgetService;
import com.bpdb.dms.procurement.service.LinkageService;
import com.bpdb.dms.procurement.service.ProcurementRecordService;
import com.bpdb.dms.procurement.service.StageDataService;
import com.bpdb.dms.procurement.service.StageDefinitionService;
import com.bpdb.dms.procurement.service.StageEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * One package driven the whole way: Stage 1 to Stage 16, against the real PostgreSQL
 * schema built by Liquibase.
 *
 * <p>Stages 3 to 16 had never been executed — not by a test, not by a person. Every stage
 * that <em>has</em> been driven turned up defects the unit suite was blind to, so the
 * remainder was the largest piece of unexamined ground left in the project.
 *
 * <p>The walk is deliberately generic. Rather than hard-coding the fields each stage wants,
 * it reads the catalogue and the document requirements from the database and satisfies
 * whatever it finds. That way the test exercises what the system actually asks for, and a
 * stage that gains a mandatory field is covered without anyone remembering to update this.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullLifecycleSmokeTest extends PostgresLiquibaseTest {

    private static final String PACKAGE_NUMBER = "SMOKE-16-STAGE";

    @Autowired private StageEngine stageEngine;
    @Autowired private StageDefinitionService definitions;
    @Autowired private StageDataService stageDataService;
    @Autowired private ProcurementRecordService recordService;
    @Autowired private LinkageService linkageService;
    @Autowired private BudgetService budgetService;
    @Autowired private ProcurementPackageRepository packageRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /** Collected as we go, so one report names every stage that failed, not just the first. */
    private static final List<String> FAILURES = new ArrayList<>();

    private ProcurementPackage subject() {
        return packageRepository.findByPackageNumber(PACKAGE_NUMBER).orElseThrow();
    }

    // ------------------------------------------------------------------ the walk

    @Test
    @Order(1)
    void createThePackage() {
        ProcurementPackage pkg = new ProcurementPackage();
        pkg.setPackageNumber(PACKAGE_NUMBER);
        pkg.setPackageDescription("Full lifecycle smoke");
        pkg.setDepartment("BPDB");
        pkg.setFiscalYear(2026);
        pkg.setPriceLacBdt(new BigDecimal("500.00"));
        ProcurementPackage saved = packageRepository.save(pkg);
        stageEngine.initialiseStages(saved.getId());

        budgetService.addEntry(saved.getId(), BudgetService.ALLOCATION,
                new BigDecimal("500.00"), "BDT", LocalDate.now(), "APP allocation", 1L);
    }

    @Test
    @Order(2)
    void walkEveryStage() {
        Long packageId = subject().getId();

        for (short stage = 1; stage <= StageDefinitionService.LAST_STAGE; stage++) {
            try {
                satisfyDocuments(packageId, stage);
                satisfyFields(packageId, stage);
                satisfyRepeatingRecords(packageId, stage);

                StageEngine.StageReadiness readiness = stageEngine.readiness(packageId, stage);
                if (!readiness.ready) {
                    FAILURES.add("Stage " + stage + " (" + definitions.stageName(stage)
                            + ") would not go ready: " + readiness.allReasons());
                    // Push on regardless - the point is to find every stage that breaks,
                    // not to stop at the first
                    stageEngine.complete(packageId, stage, 1L, "smoke test override");
                } else {
                    stageEngine.complete(packageId, stage, 1L, null);
                }
            } catch (Exception e) {
                FAILURES.add("Stage " + stage + " (" + definitions.stageName(stage) + ") threw "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
                forceStageComplete(packageId, stage);
            }
        }

        if (!FAILURES.isEmpty()) {
            fail("The lifecycle does not run clean end to end:\n  " + String.join("\n  ", FAILURES));
        }
    }

    @Test
    @Order(3)
    void thePackageKeepsItsNumberThroughout() {
        // The first run of this walk renamed the package to "Smoke test value": capturing
        // Stage 1 wrote the sample straight onto procurement_package.package_number, and
        // every later lookup lost it. Package Number is the key the whole lifecycle hangs
        // off (REQ-L2), so it is now held against capture and the difference reported.
        assertTrue(packageRepository.findByPackageNumber(PACKAGE_NUMBER).isPresent(),
                "the package must still be findable by the number it was created with");
    }

    @Test
    @Order(4)
    void thePackageEndsClosed() {
        ProcurementPackage pkg = subject();
        assertEquals("CLOSED", pkg.getStatus(),
                "completing Stage 16 should close the package (REQ-16.4)");
    }

    @Test
    @Order(5)
    void everyStageIsSatisfied() {
        for (PackageStage stage : stageEngine.stagesOf(subject().getId())) {
            assertTrue(stage.isSatisfied(),
                    "Stage " + stage.getStageCode() + " ended as " + stage.getStatus());
        }
    }

    @Test
    @Order(6)
    void invoicingConsumedTheBudget() {
        // REQ-B5: consumption is posted from the invoice, never typed. If the money never
        // arrives here, the budget view is decorative.
        BudgetService.BudgetSummary summary = budgetService.summary(subject().getId());
        assertTrue(summary.totalConsumption.signum() > 0,
                "an invoice was recorded, so consumption should have been posted");
    }

    // -------------------------------------------------------------- satisfying a stage

    /** Link a real document for every blocking requirement the stage declares. */
    private void satisfyDocuments(Long packageId, short stage) {
        for (StageDocumentRequirement req : definitions.blockingDocuments(stage)) {
            long documentId = insertDocument(req.getDocRole().toLowerCase() + ".pdf");
            linkageService.link(documentId, "PACKAGE", packageId, packageId, null, stage,
                    req.getDocRole(), "STAGE_CONTEXT", 1L);
        }
    }

    /** Fill every mandatory catalogue field with something of the right shape. */
    private void satisfyFields(Long packageId, short stage) {
        Map<String, String> values = new LinkedHashMap<>();
        for (DocumentTypeField field : definitions.mandatoryFields(stage)) {
            values.put(field.getFieldKey(), sampleFor(field));
        }
        if (!values.isEmpty()) {
            stageDataService.saveStageValues(packageId, stage, values, 1L);
        }
    }

    /** A value the parser will accept for this field's declared type. */
    private String sampleFor(DocumentTypeField field) {
        String key = field.getFieldKey() == null ? "" : field.getFieldKey();
        String type = field.getFieldType() == null ? "TEXT" : field.getFieldType().toUpperCase();

        // A few fields carry meaning the type alone does not
        if (key.equals("procurement_type")) return "ICT";
        if (key.equals("procurement_method")) return "OTM";
        if (key.equals("procurement_nature")) return "GOODS";
        if (key.contains("currency")) return "BDT";
        if (key.equals("contract_number")) return "C-SMOKE-001";

        return switch (type) {
            case "DATE" -> LocalDate.now().toString();
            case "NUMBER" -> "1";
            case "CURRENCY" -> "100";
            case "BOOL" -> "true";
            default -> "Smoke test value";
        };
    }

    /** The rows a stage holds many of, which are not catalogue fields. */
    private void satisfyRepeatingRecords(Long packageId, short stage) {
        switch (stage) {
            case 4 -> {
                BerBidder winner = new BerBidder();
                winner.setBidderName("ABC Ltd");
                winner.setBiddingPrice(new BigDecimal("450"));
                winner.setIsResponsive(Boolean.TRUE);
                winner.setIsAwarded(Boolean.TRUE);
                recordService.saveBidders(packageId, List.of(winner));
            }
            case 11 -> {
                InspectionEvent inspection = new InspectionEvent();
                inspection.setInspectionType("PDI");
                inspection.setInspectionDate(LocalDate.now());
                recordService.saveInspection(packageId, inspection);
            }
            case 12 -> {
                Delivery delivery = new Delivery();
                delivery.setDeliveryReferenceNumber("DC-SMOKE-1");
                delivery.setDeliveryDate(LocalDate.now());
                delivery.setDeliveredQuantity(new BigDecimal("1"));
                Delivery saved = recordService.saveDelivery(packageId, delivery);
                // Finality is a Checker's decision, made through its own route (REQ-12.5)
                recordService.declareFinal(packageId, saved.getId(), 1L);
            }
            case 13 -> {
                Invoice invoice = new Invoice();
                invoice.setInvoiceNumber("INV-SMOKE-1");
                invoice.setInvoiceDate(LocalDate.now());
                invoice.setInvoiceAmount(new BigDecimal("100"));
                invoice.setSupplierName("ABC Ltd");
                recordService.saveInvoice(packageId, invoice, List.of());
            }
            case 14 -> {
                Payment payment = new Payment();
                payment.setVoucherNumber("PV-SMOKE-1");
                payment.setPaymentDate(LocalDate.now());
                payment.setPaymentAmount(new BigDecimal("100"));
                recordService.savePayment(packageId, payment, List.of());
            }
            default -> { /* nothing repeating on this stage */ }
        }
    }

    /**
     * Move past a stage that could not be completed properly, so the walk can carry on and
     * report the stages behind it. Writes the status directly: this is a diagnostic, not a
     * supported path.
     */
    private void forceStageComplete(Long packageId, short stage) {
        jdbcTemplate.update(
                "UPDATE package_stage SET status = 'COMPLETED' WHERE package_id = ? AND stage_code = ?",
                packageId, stage);
        if (stage < StageDefinitionService.LAST_STAGE) {
            jdbcTemplate.update(
                    "UPDATE package_stage SET status = 'IN_PROGRESS' "
                            + "WHERE package_id = ? AND stage_code = ? AND status = 'NOT_STARTED'",
                    packageId, (short) (stage + 1));
        }
    }

    private long insertDocument(String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO documents (file_name, file_path, file_size, document_type, "
                        + "uploaded_by, is_active, is_archived, created_at, updated_at) "
                        + "VALUES (?, ?, ?, 'OTHER', (SELECT id FROM users ORDER BY id LIMIT 1), "
                        + "true, false, now(), now()) RETURNING id",
                Long.class, name, "uploads/" + name, 2048L);
    }
}
