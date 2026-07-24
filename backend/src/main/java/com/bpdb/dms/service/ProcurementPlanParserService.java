package com.bpdb.dms.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bpdb.dms.entity.ProcurementPackage;
import com.bpdb.dms.entity.StageDuration;
import com.bpdb.dms.entity.StageTimeline;

@Service
public class ProcurementPlanParserService {

    public List<ProcurementPackage> parse(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
            Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Row> rows = new ArrayList<>();
            for (Row r : sheet) {
                if (r != null) rows.add(r);
            }
            HeaderContext header = locateHeaderRow(rows);
            if (header == null) {
                throw new IllegalArgumentException("Could not locate required header columns in the Excel file.");
            }
            return extractPackages(rows, header);
        }
    }

    public List<ProcurementPackage> parse(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Row> rows = new ArrayList<>();
            for (Row r : sheet) {
                if (r != null) rows.add(r);
            }
            HeaderContext header = locateHeaderRow(rows);
            if (header == null) {
                throw new IllegalArgumentException("Could not locate required header columns in the Excel file.");
            }
            return extractPackages(rows, header);
        }
    }

    private List<ProcurementPackage> extractPackages(List<Row> rows, HeaderContext header) {
        List<ProcurementPackage> packages = new ArrayList<>();

        for (int i = header.rowIndex + 1; i < rows.size(); i++) {
            Row row = rows.get(i);

            String packageNo = getCellValue(row, header.map.get("Package No."));
            if (isNullOrEmpty(packageNo)) {
                continue;
            }

            ProcurementPackage pkg = new ProcurementPackage();
            pkg.setStatus(getCellValue(row, header.map.get("Status")));
            pkg.setPackageNo(packageNo.trim());
            pkg.setLotNo(getCellValue(row, header.map.get("Lot. No")));
            pkg.setDescription(getCellValue(row, header.map.get("Description of the Materials")));
            pkg.setUnit(getCellValue(row, header.map.get("Unit")));
            pkg.setQuantity(parseInteger(getCellValue(row, header.map.get("Quantity"))));
            pkg.setProcurementMethod(getCellValue(row, header.map.get("Procurement Method & Type")));
            pkg.setContractApprovingAuthority(getCellValue(row, header.map.get("Contract Approving Authority")));
            pkg.setSourceOfFund(getCellValue(row, header.map.get("Source of Fund")));
            pkg.setUnitCost(parseDouble(getCellValue(row, header.map.get("Unit Cost"))));
            pkg.setTotalCost(parseDouble(getCellValue(row, header.map.get("Total Cost"))));

            List<String> timelineHeaders = Arrays.asList(
                    "Advertise Tender",
                    "Tender Opening",
                    "Tender Evaluation",
                    "Approval to Award",
                    "Notification of Award",
                    "Signing of the Contract",
                    "Completion of Contract",
                    "Total Time (Date/Days)"
            );

            pkg.setPlannedDates(readTimeline(row, header.map, timelineHeaders));

            StageDuration plannedDays = null;
            if (i + 1 < rows.size() && containsLabel(rows.get(i + 1), "Planned Days")) {
                plannedDays = readDuration(rows.get(i + 1), header.map, timelineHeaders);
                i++;
            }
            pkg.setPlannedDays(plannedDays);

            StageTimeline actualDates = null;
            if (i + 1 < rows.size() && containsLabel(rows.get(i + 1), "Actual Dates")) {
                actualDates = readTimeline(rows.get(i + 1), header.map, timelineHeaders);
                i++;
            }
            pkg.setActualDates(actualDates);

            packages.add(pkg);
        }

        return packages;
    }

    private static class HeaderContext {
        int rowIndex;
        Map<String, Integer> map = new HashMap<>();
    }

    private HeaderContext locateHeaderRow(List<Row> rows) {
        List<String> primaryKeywords = Arrays.asList(
                "Package No.",
                "Description of the Materials",
                "Unit",
                "Quantity"
        );

        List<String> alternativeNames = Arrays.asList(
                "Estd. Cost",
                "Estimated Cost",
                "Cost (Tk.",
                "Unit Cost",
                "Total Cost"
        );

        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            String rowText = getRowText(row);

            boolean allPrimaryPresent = primaryKeywords.stream().allMatch(rowText::contains);
            if (!allPrimaryPresent) continue;

            HeaderContext ctx = new HeaderContext();
            ctx.rowIndex = i;

            for (Cell cell : row) {
                String val = getCellStringValue(cell);
                if (val == null) continue;
                val = val.trim();
                if (val.isEmpty()) continue;
                ctx.map.putIfAbsent(val, cell.getColumnIndex());
            }

            boolean hasPackageNo = ctx.map.containsKey("Package No.");
            boolean hasDescription = ctx.map.containsKey("Description of the Materials");

            if (hasPackageNo && hasDescription) {
                for (int j = i + 1; j < Math.min(i + 3, rows.size()); j++) {
                    Row subRow = rows.get(j);
                    for (Cell cell : subRow) {
                        String val = getCellStringValue(cell);
                        if (val == null) continue;
                        val = val.trim();
                        if (val.isEmpty()) continue;
                        if (!ctx.map.containsKey(val)) {
                            ctx.map.put(val, cell.getColumnIndex());
                        }
                    }
                }

                boolean hasCostColumn = ctx.map.containsKey("Unit Cost") || ctx.map.containsKey("Total Cost");
                if (!hasCostColumn) {
                    for (String alt : alternativeNames) {
                        if (ctx.map.containsKey(alt)) {
                            hasCostColumn = true;
                            break;
                        }
                    }
                }

                if (hasCostColumn) {
                    return ctx;
                }
            }
        }
        return null;
    }

    private StageTimeline readTimeline(Row row, Map<String, Integer> map, List<String> timelineHeaders) {
        StageTimeline timeline = new StageTimeline();
        for (String header : timelineHeaders) {
            Integer col = map.get(header);
            if (col != null && col < row.getLastCellNum()) {
                String val = getCellValue(row, col);
                switch (header) {
                    case "Advertise Tender":
                        timeline.setAdvertiseTender(val);
                        break;
                    case "Tender Opening":
                        timeline.setTenderOpening(val);
                        break;
                    case "Tender Evaluation":
                        timeline.setTenderEvaluation(val);
                        break;
                    case "Approval to Award":
                        timeline.setApprovalToAward(val);
                        break;
                    case "Notification of Award":
                        timeline.setNotificationOfAward(val);
                        break;
                    case "Signing of the Contract":
                        timeline.setSigningOfContract(val);
                        break;
                    case "Completion of Contract":
                        timeline.setCompletionOfContract(val);
                        break;
                    case "Total Time (Date/Days)":
                        timeline.setTotalTime(val);
                        break;
                }
            }
        }
        return timeline;
    }

    private StageDuration readDuration(Row row, Map<String, Integer> map, List<String> timelineHeaders) {
        StageDuration duration = new StageDuration();
        for (String header : timelineHeaders) {
            Integer col = map.get(header);
            if (col != null && col < row.getLastCellNum()) {
                String val = getCellValue(row, col);
                Integer parsed = parseInteger(val);
                switch (header) {
                    case "Advertise Tender":
                        duration.setAdvertiseTender(parsed);
                        break;
                    case "Tender Opening":
                        duration.setTenderOpening(parsed);
                        break;
                    case "Tender Evaluation":
                        duration.setTenderEvaluation(parsed);
                        break;
                    case "Approval to Award":
                        duration.setApprovalToAward(parsed);
                        break;
                    case "Notification of Award":
                        duration.setNotificationOfAward(parsed);
                        break;
                    case "Signing of the Contract":
                        duration.setSigningOfContract(parsed);
                        break;
                    case "Completion of Contract":
                        duration.setCompletionOfContract(parsed);
                        break;
                    case "Total Time (Date/Days)":
                        duration.setTotalTime(parsed);
                        break;
                }
            }
        }
        return duration;
    }

    private boolean containsLabel(Row row, String label) {
        for (Cell cell : row) {
            String val = getCellStringValue(cell);
            if (val != null && val.trim().equalsIgnoreCase(label)) {
                return true;
            }
        }
        return false;
    }

    private String getCellValue(Row row, Integer columnIndex) {
        if (columnIndex == null || columnIndex < 0) return null;
        if (row == null) return null;
        if (columnIndex >= row.getLastCellNum()) return null;
        Cell cell = row.getCell(columnIndex);
        return getCellStringValue(cell);
    }

    private final DataFormatter dataFormatter = new DataFormatter();
    private FormulaEvaluator evaluator;

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        if (evaluator == null && cell.getSheet() != null) {
            evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
        }
        return dataFormatter.formatCellValue(cell, evaluator);
    }

    private String getRowText(Row row) {
        if (row == null) return "";
        StringBuilder sb = new StringBuilder();
        for (Cell cell : row) {
            String val = getCellStringValue(cell);
            if (val != null) {
                sb.append(val).append(" ");
            }
        }
        return sb.toString();
    }

    private boolean isNullOrEmpty(String val) {
        return val == null || val.trim().isEmpty();
    }

    private Integer parseInteger(String val) {
        if (isNullOrEmpty(val)) return null;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String val) {
        if (isNullOrEmpty(val)) return null;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
