package com.bpdb.dms.procurement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.procurement.entity.ExtractedField;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.ExtractedFieldRepository;
import com.bpdb.dms.procurement.repository.PackageStageRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.service.AppPackageImportService;
import com.bpdb.dms.procurement.service.AppPackageImportService.ImportReport;
import com.bpdb.dms.procurement.service.CaptureService;
import com.bpdb.dms.procurement.service.StageEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Stage 1 import, end to end against the database.
 *
 * <p>Two sources are used deliberately: the real BPDB workbook, which proves the importer
 * works on what the client actually sent, and small synthetic workbooks for the paths the
 * real file does not contain - lots, duplicates and unusable rows.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppPackageImportServiceTest {

    private static final File WORKBOOK =
            new File("../requirements/APP 22-23 First Revision_2980.xls");

    @Autowired
    private AppPackageImportService importService;

    @Autowired
    private ProcurementPackageRepository packageRepository;

    @Autowired
    private PackageStageRepository stageRepository;

    @Autowired
    private ExtractedFieldRepository fieldRepository;

    @Autowired
    private CaptureService captureService;

    @Autowired
    private StageEngine stageEngine;

    // ------------------------------------------------ the real supplied workbook

    @Test
    void importsTheSuppliedWorkbook() throws Exception {
        assumeTrue(WORKBOOK.exists(), "Supplied APP workbook not found");

        ImportReport report;
        try (InputStream in = new FileInputStream(WORKBOOK)) {
            report = importService.importWorkbook(in, "BPDB", 1L, false);
        }

        assertEquals(7, report.getCreatedCount(), "every package row should have been created");
        assertEquals(0, report.getFailedCount(), "nothing in the supplied file should fail");
        assertEquals(2022, report.fiscalYear);

        ProcurementPackage grl18 = packageRepository.findByPackageNumber("GRL-18").orElseThrow();
        assertEquals("BPDB", grl18.getDepartment());
        assertEquals(2022, grl18.getFiscalYear());
        assertEquals("Member, Distribution", grl18.getApprovingAuthority());
        assertEquals(0, new BigDecimal("60.00").compareTo(grl18.getPriceLacBdt()));
        assertNull(grl18.getLotNumber(), "this line is not split into lots");
        assertTrue(grl18.getPackageDescription().startsWith("Supply of Router"));
    }

    @Test
    void importedValuesAreCapturedSoStageOneCanActuallyBeCompleted() throws Exception {
        // The whole point of importing. Writing the package row without the provenance
        // rows leaves the gate reporting every mandatory field as "not captured" while the
        // data sits in plain sight, and the package stuck at Stage 1 forever (REQ-P4).
        assumeTrue(WORKBOOK.exists(), "Supplied APP workbook not found");
        try (InputStream in = new FileInputStream(WORKBOOK)) {
            importService.importWorkbook(in, "BPDB", 1L, false);
        }

        ProcurementPackage pkg = packageRepository.findByPackageNumber("GRL-18").orElseThrow();
        List<ExtractedField> captured = fieldRepository.findByPackageIdAndStageCode(pkg.getId(), (short) 1);

        assertEquals(4, captured.size(), "all four Stage 1 fields should be captured");
        for (ExtractedField f : captured) {
            assertEquals("IMPORT", f.getCaptureSource(), f.getFieldKey() + " should be marked imported");
            assertTrue(f.isConfirmed(), f.getFieldKey() + " should count towards the gate");
        }

        // and the gate agrees - no unconfirmed fields left standing
        assertTrue(stageEngine.readiness(pkg.getId(), (short) 1).unconfirmedFields.isEmpty(),
                "the Stage 1 field gate should be satisfied by the import alone");
    }

    @Test
    void capturedValuesMatchTheTypedColumns() throws Exception {
        assumeTrue(WORKBOOK.exists(), "Supplied APP workbook not found");
        try (InputStream in = new FileInputStream(WORKBOOK)) {
            importService.importWorkbook(in, "BPDB", 1L, false);
        }
        ProcurementPackage pkg = packageRepository.findByPackageNumber("GRL-18").orElseThrow();

        Map<String, ExtractedField> byKey = fieldRepository
                .findByPackageIdAndStageCode(pkg.getId(), (short) 1).stream()
                .collect(Collectors.toMap(ExtractedField::getFieldKey, f -> f));

        // REQ-P4: the working value and the evidence must say the same thing
        assertEquals(pkg.getPackageNumber(), byKey.get("package_number").displayValue());
        assertEquals(pkg.getPackageDescription(), byKey.get("package_description").displayValue());
        assertEquals(pkg.getApprovingAuthority(), byKey.get("approving_authority").displayValue());
        assertEquals(0, pkg.getPriceLacBdt().compareTo(byKey.get("price_lac_bdt").getNumericValue()));
    }

    @Test
    void anImportedValueSurvivesALaterEmptyOcrPass() throws Exception {
        // E-3 and E-4 together: capturing imported values is only worth doing if the first
        // document upload does not wipe them, which is exactly what used to happen
        assumeTrue(WORKBOOK.exists(), "Supplied APP workbook not found");
        try (InputStream in = new FileInputStream(WORKBOOK)) {
            importService.importWorkbook(in, "BPDB", 1L, false);
        }
        ProcurementPackage pkg = packageRepository.findByPackageNumber("GRL-18").orElseThrow();

        CaptureService.CaptureRequest emptyRead = new CaptureService.CaptureRequest();
        emptyRead.entityType = "PACKAGE";
        emptyRead.entityId = pkg.getId();
        emptyRead.packageId = pkg.getId();
        emptyRead.stageCode = (short) 1;
        emptyRead.fieldKey = "package_number";
        emptyRead.dataType = "TEXT";
        emptyRead.rawValue = null;
        captureService.captureFromOcr(emptyRead);

        List<ExtractedField> after = fieldRepository.findByPackageIdAndStageCode(pkg.getId(), (short) 1);
        ExtractedField number = after.stream()
                .filter(f -> "package_number".equals(f.getFieldKey())).findFirst().orElseThrow();
        assertEquals("GRL-18", number.displayValue(), "the imported value must survive");
        assertTrue(stageEngine.readiness(pkg.getId(), (short) 1).unconfirmedFields.isEmpty(),
                "and the stage must not fall back to blocked");
    }

    @Test
    void everyImportedPackageOpensAtStageOne() throws Exception {
        assumeTrue(WORKBOOK.exists(), "Supplied APP workbook not found");
        try (InputStream in = new FileInputStream(WORKBOOK)) {
            importService.importWorkbook(in, "BPDB", 1L, false);
        }

        ProcurementPackage pkg = packageRepository.findByPackageNumber("GRL-24").orElseThrow();
        assertEquals((short) 1, pkg.getCurrentStage());
        assertEquals("ACTIVE", pkg.getStatus());
        // An imported package must be ready to work on, not a bare row: all 16 stages
        assertEquals(16, stageRepository.findByPackageIdOrderByStageCodeAsc(pkg.getId()).size());
    }

    @Test
    void aDryRunReportsWithoutWriting() throws Exception {
        assumeTrue(WORKBOOK.exists(), "Supplied APP workbook not found");

        ImportReport report;
        try (InputStream in = new FileInputStream(WORKBOOK)) {
            report = importService.importWorkbook(in, "BPDB", 1L, true);
        }

        assertTrue(report.dryRun);
        assertEquals(7, report.getCreatedCount(), "the report should say what would happen");
        assertTrue(packageRepository.findByPackageNumber("GRL-18").isEmpty(),
                "a dry run must not create anything");
    }

    @Test
    void reimportingLeavesExistingPackagesAlone() throws Exception {
        assumeTrue(WORKBOOK.exists(), "Supplied APP workbook not found");

        try (InputStream in = new FileInputStream(WORKBOOK)) {
            importService.importWorkbook(in, "BPDB", 1L, false);
        }
        ImportReport second;
        try (InputStream in = new FileInputStream(WORKBOOK)) {
            second = importService.importWorkbook(in, "BPDB", 1L, false);
        }

        // REQ-1.3: a duplicate is refused, not overwritten - re-uploading the same file
        // must never quietly reset work already done on those packages
        assertEquals(0, second.getCreatedCount());
        assertEquals(7, second.getSkippedCount());
        assertEquals(0, second.getFailedCount());
    }

    // ------------------------------------------- paths the real workbook lacks

    @Test
    void aLineSplitIntoLotsBecomesOnePackagePerLot() throws Exception {
        // Q-1 / REQ-L13. The supplied file has "-" in every Lot No. cell, so this path
        // has no coverage from the real data - but it is the answer that reshaped the
        // model, so it needs its own proof.
        byte[] wb = workbookWith(new String[][] {
            {"Newly Proposed", "PKG-77", "1", "Transformers lot 1", "Lot", "1", "e-GP/ OTM",
             "Director, Purchase", "Revenue", "50", "50"},
            {"Newly Proposed", "PKG-77", "2", "Transformers lot 2", "Lot", "1", "e-GP/ OTM",
             "Director, Purchase", "Revenue", "70", "70"},
        });

        ImportReport report = importService.importWorkbook(new ByteArrayInputStream(wb), "BPDB", 1L, false);
        assertEquals(2, report.getCreatedCount());

        ProcurementPackage lot1 = packageRepository.findByPackageNumber("PKG-77-1").orElseThrow();
        ProcurementPackage lot2 = packageRepository.findByPackageNumber("PKG-77-2").orElseThrow();
        assertEquals("1", lot1.getLotNumber());
        assertEquals("2", lot2.getLotNumber());
        // Each lot carries its own value; they are not two views of one budget
        assertEquals(0, new BigDecimal("50.00").compareTo(lot1.getPriceLacBdt()));
        assertEquals(0, new BigDecimal("70.00").compareTo(lot2.getPriceLacBdt()));
    }

    @Test
    void aRowRepeatedInTheSameFileIsImportedOnce() throws Exception {
        byte[] wb = workbookWith(new String[][] {
            {"Newly Proposed", "PKG-88", "-", "First occurrence", "Lot", "1", "e-GP/ OTM",
             "Director", "Revenue", "10", "10"},
            {"Newly Proposed", "PKG-88", "-", "Second occurrence", "Lot", "1", "e-GP/ OTM",
             "Director", "Revenue", "10", "10"},
        });

        ImportReport report = importService.importWorkbook(new ByteArrayInputStream(wb), "BPDB", 1L, false);
        assertEquals(1, report.getCreatedCount());
        assertEquals(1, report.getSkippedCount());
        assertEquals("First occurrence",
                packageRepository.findByPackageNumber("PKG-88").orElseThrow().getPackageDescription());
    }

    @Test
    void aRowWithNoCostIsReportedAndTheRestStillImport() throws Exception {
        // One unusable row must not cost the user the rest of the file
        byte[] wb = workbookWith(new String[][] {
            {"Newly Proposed", "PKG-90", "-", "No cost at all", "Lot", "", "e-GP/ OTM",
             "Director", "Revenue", "", ""},
            {"Newly Proposed", "PKG-91", "-", "Perfectly fine", "Lot", "1", "e-GP/ OTM",
             "Director", "Revenue", "25", "25"},
        });

        ImportReport report = importService.importWorkbook(new ByteArrayInputStream(wb), "BPDB", 1L, false);

        assertEquals(1, report.getCreatedCount());
        assertEquals(1, report.getFailedCount());
        assertTrue(report.failed.get(0).reason.contains("Estd. Cost"),
                "the report must say why: " + report.failed.get(0).reason);
        assertNotNull(packageRepository.findByPackageNumber("PKG-91").orElse(null));
    }

    @Test
    void takesTotalCostNotUnitCostWhenTheyDiffer() throws Exception {
        // The supplied workbook cannot catch this: every row there has quantity 1, so unit
        // cost and total cost are equal and reading the wrong column looks correct. With a
        // quantity above 1 the mistake is worth 4x the package value.
        byte[] wb = workbookWith(new String[][] {
            {"Newly Proposed", "PKG-99", "-", "Four transformers", "Nos", "4", "e-GP/ OTM",
             "Director", "Revenue", "25", "100"},
        });

        importService.importWorkbook(new ByteArrayInputStream(wb), "BPDB", 1L, false);

        ProcurementPackage pkg = packageRepository.findByPackageNumber("PKG-99").orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo(pkg.getPriceLacBdt()),
                "the package must carry the total, not the per-unit price");
    }

    @Test
    void totalCostIsDerivedWhenTheWorkbookOnlyGivesUnitCostAndQuantity() throws Exception {
        byte[] wb = workbookWith(new String[][] {
            {"Newly Proposed", "PKG-95", "-", "Priced per unit", "Nos", "4", "e-GP/ OTM",
             "Director", "Revenue", "25", ""},
        });

        ImportReport report = importService.importWorkbook(new ByteArrayInputStream(wb), "BPDB", 1L, false);
        assertEquals(1, report.getCreatedCount());
        assertEquals(1, report.warnings.size(), "a derived total should be surfaced, not silent");
        assertEquals(0, new BigDecimal("100.00").compareTo(
                packageRepository.findByPackageNumber("PKG-95").orElseThrow().getPriceLacBdt()));
    }

    /**
     * Builds a workbook shaped like the BPDB one - the same headings in the same two-row
     * arrangement - so the tests exercise the real header resolution rather than a
     * simplified stand-in.
     */
    private static byte[] workbookWith(String[][] dataRows) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Local");

            Row title = sheet.createRow(1);
            title.createCell(8).setCellValue("Fiscal Year :");
            title.createCell(10).setCellValue("2022-2023");

            Row header = sheet.createRow(4);
            String[] headings = {"Status", "Package No.", "Lot. No.", "Description of the Materials",
                "Unit", "Quantity", "Procurement Method & Type", "Contract Approving Authority",
                "Source of Fund", "Estd. Cost (Tk. in Lac)", ""};
            for (int c = 0; c < headings.length; c++) {
                header.createCell(c).setCellValue(headings[c]);
            }
            Row subHeader = sheet.createRow(5);
            subHeader.createCell(9).setCellValue("Unit Cost");
            subHeader.createCell(10).setCellValue("Total Cost");

            int r = 7;
            for (String[] data : dataRows) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < data.length; c++) {
                    if (data[c] == null || data[c].isEmpty()) {
                        continue;
                    }
                    if (c == 5 || c == 9 || c == 10) {
                        row.createCell(c).setCellValue(Double.parseDouble(data[c]));
                    } else {
                        row.createCell(c).setCellValue(data[c]);
                    }
                }
                // the blank "Planned Days" line that follows every package in the real file
                sheet.createRow(r + 1).createCell(11).setCellValue("Planned Days");
                r += 2;
            }

            wb.write(out);
            return out.toByteArray();
        }
    }
}
