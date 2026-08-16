package com.bpdb.dms.procurement.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bpdb.dms.procurement.service.AppColumnProfile.Field;

/**
 * Reads an APP workbook into plain rows (REQ-1.1). Knows about spreadsheets; knows
 * nothing about packages, stages or the database - that is {@link AppPackageImportService}.
 *
 * <p>The BPDB layout is not a flat table. Each package occupies two or three physical
 * rows: the first carries the data and the "Planned Dates" line, the ones beneath carry
 * "Planned Days" and sometimes "Actual Dates" with the package columns left blank. The
 * sheet also ends with a Grand Total line and a block of signature captions, which look
 * like data if you only check for a package number. A row is therefore taken as real only
 * when it has both a Status and a Package No., which excludes all of them.
 */
@Component
public class AppWorkbookParser {

    private static final Logger log = LoggerFactory.getLogger(AppWorkbookParser.class);

    /** "2022-2023", "2022-2023 (1st Revision)", "FY 2022-23" - take the first 4-digit year. */
    private static final Pattern FISCAL_YEAR = Pattern.compile("(20\\d{2})");
    private static final Pattern FISCAL_YEAR_LABEL = Pattern.compile("(?i)fiscal\\s*year");

    /** How far down to look for the header row before giving up on a sheet. */
    private static final int MAX_HEADER_SCAN_ROWS = 30;

    public ParsedWorkbook parse(InputStream in) throws IOException {
        ParsedWorkbook result = new ParsedWorkbook();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                Integer fiscalYear = findFiscalYear(sheet);
                if (result.fiscalYear == null) {
                    result.fiscalYear = fiscalYear;
                }
                AppColumnProfile profile = AppColumnProfile.forFiscalYear(fiscalYear);
                result.profileName = profile.getName();

                Map<Field, Integer> columns = resolveColumns(sheet, profile);
                if (columns.get(Field.PACKAGE_NUMBER) == null) {
                    log.debug("Sheet '{}' has no recognisable APP header - skipped", sheet.getSheetName());
                    result.skippedSheets.add(sheet.getSheetName());
                    continue;
                }
                readRows(sheet, columns, fiscalYear, result);
            }
        }
        return result;
    }

    /**
     * Locate each field's column by scanning the first rows for its header label.
     *
     * <p>Two passes. The first accepts only exact headings, so a group heading spanning
     * several sub-columns cannot claim one of them. The second fills anything still
     * missing from the looser aliases, which is what handles a sheet whose cost column is
     * undivided.
     */
    private Map<Field, Integer> resolveColumns(Sheet sheet, AppColumnProfile profile) {
        Map<Field, Integer> columns = new EnumMap<>(Field.class);
        int lastScan = Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + MAX_HEADER_SCAN_ROWS);

        for (int r = sheet.getFirstRowNum(); r <= lastScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            for (int c = row.getFirstCellNum(); c >= 0 && c < row.getLastCellNum(); c++) {
                String text = stringValue(row.getCell(c));
                if (text.isEmpty()) {
                    continue;
                }
                for (Field field : Field.values()) {
                    if (!columns.containsKey(field) && profile.matches(field, text)) {
                        columns.put(field, c);
                    }
                }
            }
        }

        for (int r = sheet.getFirstRowNum(); r <= lastScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            for (int c = row.getFirstCellNum(); c >= 0 && c < row.getLastCellNum(); c++) {
                String text = stringValue(row.getCell(c));
                if (text.isEmpty()) {
                    continue;
                }
                for (Field field : Field.values()) {
                    if (!columns.containsKey(field) && profile.matchesFallback(field, text)) {
                        log.debug("Sheet '{}': {} resolved to column {} by fallback heading '{}'",
                                sheet.getSheetName(), field, c, text);
                        columns.put(field, c);
                    }
                }
            }
        }
        return columns;
    }

    private void readRows(Sheet sheet, Map<Field, Integer> columns, Integer fiscalYear,
                          ParsedWorkbook result) {
        Integer statusCol = columns.get(Field.STATUS);
        int packageCol = columns.get(Field.PACKAGE_NUMBER);

        for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            String packageNumber = stringValue(row.getCell(packageCol));
            if (packageNumber.isEmpty()) {
                continue;
            }
            String status = statusCol == null ? "" : stringValue(row.getCell(statusCol));

            // Status + Package No. together is what separates a real row from the header,
            // the "(1) (2) (3)" numbering line, the Grand Total and the signature block
            if (status.isEmpty()) {
                continue;
            }
            if (AppColumnProfile.normalize(packageNumber).startsWith("package no")
                    || AppColumnProfile.normalize(status).startsWith("status")) {
                continue;
            }

            AppRow parsed = new AppRow();
            parsed.sheetName = sheet.getSheetName();
            parsed.rowNumber = r + 1;
            parsed.fiscalYear = fiscalYear;
            parsed.status = status;
            parsed.packageNumber = packageNumber;
            parsed.lotNumber = cleanLot(text(row, columns, Field.LOT_NUMBER));
            parsed.description = text(row, columns, Field.DESCRIPTION);
            parsed.unit = text(row, columns, Field.UNIT);
            parsed.quantity = number(row, columns, Field.QUANTITY);
            parsed.procurementMethodType = text(row, columns, Field.PROCUREMENT_METHOD_TYPE);
            parsed.approvingAuthority = text(row, columns, Field.APPROVING_AUTHORITY);
            parsed.sourceOfFund = text(row, columns, Field.SOURCE_OF_FUND);
            parsed.unitCost = number(row, columns, Field.UNIT_COST);
            parsed.totalCost = number(row, columns, Field.TOTAL_COST);

            // Total Cost is usually a formula (quantity x unit cost). POI hands back the
            // value Excel last cached, which is normally there - but a file written by a
            // tool that does not cache results leaves it empty, so recompute rather than
            // importing a package worth nothing.
            if (isBlankAmount(parsed.totalCost)
                    && parsed.unitCost != null && parsed.quantity != null) {
                parsed.totalCost = parsed.unitCost.multiply(parsed.quantity);
                parsed.totalCostDerived = true;
            }
            result.rows.add(parsed);
        }
    }

    private static boolean isBlankAmount(BigDecimal v) {
        return v == null || v.signum() == 0;
    }

    /** "-" and "n/a" are how the workbook says "no lot", not a lot called "-". */
    private static String cleanLot(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty() || "-".equals(t) || "--".equals(t)
                || "n/a".equalsIgnoreCase(t) || "na".equalsIgnoreCase(t)) {
            return null;
        }
        return t;
    }

    private Integer findFiscalYear(Sheet sheet) {
        int lastScan = Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + MAX_HEADER_SCAN_ROWS);
        for (int r = sheet.getFirstRowNum(); r <= lastScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            boolean labelSeen = false;
            for (int c = row.getFirstCellNum(); c >= 0 && c < row.getLastCellNum(); c++) {
                String text = stringValue(row.getCell(c));
                if (text.isEmpty()) {
                    continue;
                }
                if (FISCAL_YEAR_LABEL.matcher(text).find()) {
                    labelSeen = true;
                    // the year sometimes sits in the same cell: "Fiscal Year : 2022-2023"
                    Matcher inline = FISCAL_YEAR.matcher(text);
                    if (inline.find()) {
                        return Integer.parseInt(inline.group(1));
                    }
                    continue;
                }
                if (labelSeen) {
                    Matcher m = FISCAL_YEAR.matcher(text);
                    if (m.find()) {
                        // "2022-2023" and "2022-2023 (1st Revision)" both give 2022
                        return Integer.parseInt(m.group(1));
                    }
                }
            }
        }
        return null;
    }

    private String text(Row row, Map<Field, Integer> columns, Field field) {
        Integer c = columns.get(field);
        return c == null ? null : emptyToNull(stringValue(row.getCell(c)));
    }

    private BigDecimal number(Row row, Map<Field, Integer> columns, Field field) {
        Integer c = columns.get(field);
        return c == null ? null : numericValue(row.getCell(c));
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static String stringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> trimTrailingZeros(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cachedString(cell);
            default -> "";
        };
    }

    private static String cachedString(Cell cell) {
        try {
            return switch (cell.getCachedFormulaResultType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> trimTrailingZeros(cell.getNumericCellValue());
                default -> "";
            };
        } catch (IllegalStateException e) {
            return "";
        }
    }

    private static String trimTrailingZeros(double d) {
        if (d == Math.rint(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }

    private static BigDecimal numericValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCellType() == CellType.FORMULA
                    && cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCellType() == CellType.STRING) {
                String raw = cell.getStringCellValue().replaceAll("[,\\s]", "").trim();
                return raw.isEmpty() ? null : new BigDecimal(raw);
            }
        } catch (IllegalStateException | NumberFormatException e) {
            return null;
        }
        return null;
    }

    /** One APP line, as read. Nothing here is interpreted yet. */
    public static class AppRow {
        public String sheetName;
        public int rowNumber;
        public Integer fiscalYear;
        public String status;
        public String packageNumber;
        public String lotNumber;
        public String description;
        public String unit;
        public BigDecimal quantity;
        public String procurementMethodType;
        public String approvingAuthority;
        public String sourceOfFund;
        public BigDecimal unitCost;
        public BigDecimal totalCost;
        /** True when Total Cost was recomputed because the workbook cached no value. */
        public boolean totalCostDerived;

        /** Where this came from, for error messages the user can act on. */
        public String origin() {
            return "'" + sheetName + "' row " + rowNumber;
        }
    }

    public static class ParsedWorkbook {
        public Integer fiscalYear;
        public String profileName;
        public List<AppRow> rows = new ArrayList<>();
        public List<String> skippedSheets = new ArrayList<>();
    }
}
