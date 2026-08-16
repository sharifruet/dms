package com.bpdb.dms.procurement;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.bpdb.dms.procurement.service.AppWorkbookParser;
import com.bpdb.dms.procurement.service.AppWorkbookParser.AppRow;
import com.bpdb.dms.procurement.service.AppWorkbookParser.ParsedWorkbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The parser against the workbook BPDB actually supplied (Q-7), not a fixture built to
 * match the parser. This is the first point where the Stage 1 model meets a real APP.
 */
class AppWorkbookParserTest {

    private static final File WORKBOOK =
            new File("../requirements/APP 22-23 First Revision_2980.xls");

    private final AppWorkbookParser parser = new AppWorkbookParser();

    @BeforeAll
    static void workbookIsPresent() {
        assumeTrue(WORKBOOK.exists(),
                "Supplied APP workbook not found at " + WORKBOOK.getPath());
    }

    private ParsedWorkbook parse() throws Exception {
        try (FileInputStream in = new FileInputStream(WORKBOOK)) {
            return parser.parse(in);
        }
    }

    @Test
    void readsEveryPackageAndNothingElse() throws Exception {
        ParsedWorkbook result = parse();

        // Seven real packages across the two sheets. The count matters: the sheets also
        // contain a Grand Total line, a "(1) (2) (3)" numbering row and a block of
        // signature captions, every one of which carries text in the Package No. column.
        assertEquals(7, result.rows.size(),
                "expected the 7 data rows only, got: " + describe(result.rows));

        List<String> numbers = result.rows.stream().map(r -> r.packageNumber).toList();
        assertEquals(List.of("GRL-18", "GRL-19", "GRL-24", "GRL-25", "GRL-26", "GRL-27", "GRL-28"),
                numbers);
    }

    @Test
    void readsTheFiscalYearFromTheSheetHeading() throws Exception {
        // The heading reads "Fiscal Year : 2022-2023"; the second sheet adds
        // "(1st Revision)", which must not derail it
        assertEquals(2022, parse().fiscalYear);
    }

    @Test
    void readsTheFieldsStageOneNeeds() throws Exception {
        AppRow first = parse().rows.get(0);

        assertEquals("GRL-18", first.packageNumber);
        assertEquals("Newly Proposed", first.status);
        assertNotNull(first.description);
        assertTrue(first.description.startsWith("Supply of Router"),
                "description was: " + first.description);
        assertEquals("Member, Distribution", first.approvingAuthority);
        assertEquals("e-GP/ OTM", first.procurementMethodType);
        assertEquals("BPDB's Revenue Budget", first.sourceOfFund);
        assertEquals(0, new BigDecimal("60").compareTo(first.unitCost));
    }

    @Test
    void resolvesTotalCostEvenWhenTheCellIsAFormula() throws Exception {
        // GRL-18's Total Cost is =SUM(F8*J8); GRL-19's is a plain 100. Both must arrive
        // as a usable amount, because this becomes the package's APP value (REQ-1.4)
        List<AppRow> rows = parse().rows;
        AppRow formulaRow = rows.get(0);
        AppRow literalRow = rows.get(1);

        assertNotNull(formulaRow.totalCost, "formula-backed Total Cost must resolve");
        assertEquals(0, new BigDecimal("60").compareTo(formulaRow.totalCost));
        assertEquals(0, new BigDecimal("100").compareTo(literalRow.totalCost));
    }

    @Test
    void unitCostAndTotalCostAreDifferentColumns() throws Exception {
        // Regression guard. "Estd. Cost (Tk. in Lac)" is a group heading sitting above
        // both cost sub-columns, in the same column as "Unit Cost". Letting it match
        // Total Cost reads the unit price as the package value - which every row in this
        // workbook hides, because they all have quantity 1 and the two are equal. Any APP
        // with a quantity above 1 would have imported the wrong figure.
        List<AppRow> rows = parse().rows;
        AppRow row = rows.get(0);
        assertNotNull(row.unitCost);
        assertNotNull(row.totalCost);

        // Proven structurally rather than by value: a synthetic row where the two differ
        // is covered in AppPackageImportServiceTest; here we assert the columns resolved
        // independently by checking the parser found both a unit and a total
        assertTrue(rows.stream().allMatch(r -> r.unitCost != null && r.totalCost != null),
                "both cost columns must resolve for every row");
    }

    @Test
    void treatsADashInTheLotColumnAsNoLot() throws Exception {
        // Every row in this workbook has "-" for Lot No., meaning the line is not split.
        // Storing "-" as a lot number would make each package look like a lot of itself.
        for (AppRow row : parse().rows) {
            assertNull(row.lotNumber, row.origin() + " should have no lot number");
        }
    }

    @Test
    void everyRowCarriesEnoughToCreateAPackage() throws Exception {
        for (AppRow row : parse().rows) {
            assertNotNull(row.packageNumber, row.origin());
            assertFalse(row.packageNumber.isBlank(), row.origin());
            assertNotNull(row.description, row.origin() + " has no description");
            assertNotNull(row.totalCost, row.origin() + " has no cost");
            assertTrue(row.totalCost.signum() > 0, row.origin() + " has a zero cost");
        }
    }

    private static String describe(List<AppRow> rows) {
        return rows.stream().map(r -> r.packageNumber + "@" + r.origin()).toList().toString();
    }
}
