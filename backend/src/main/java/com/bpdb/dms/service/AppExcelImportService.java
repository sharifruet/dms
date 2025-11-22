package com.bpdb.dms.service;

import com.bpdb.dms.entity.AppHeader;
import com.bpdb.dms.entity.AppLine;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.AppHeaderRepository;
import com.bpdb.dms.repository.AppLineRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class AppExcelImportService {

    private static final Logger logger = LoggerFactory.getLogger(AppExcelImportService.class);

    @Autowired
    private AppHeaderRepository appHeaderRepository;

    @Autowired
    private AppLineRepository appLineRepository;

    public AppHeader importApp(MultipartFile file, User user) {
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("No sheet found in APP file");
            }

            // Find header row - it might be row 1 or row 2 (skip empty first row)
            Iterator<Row> rowIterator = sheet.rowIterator();
            if (!rowIterator.hasNext()) {
                throw new IllegalArgumentException("APP sheet is empty");
            }
            Row headerRow = rowIterator.next();
            ColumnMap cm = ColumnMap.fromHeaderRow(headerRow);
            
            // If first row is empty or has very few headers, try next row
            if (cm.nameToIndex.size() < 3 && rowIterator.hasNext()) {
                Row nextRow = rowIterator.next();
                ColumnMap nextCm = ColumnMap.fromHeaderRow(nextRow);
                if (nextCm.nameToIndex.size() > cm.nameToIndex.size()) {
                    headerRow = nextRow;
                    cm = nextCm;
                    logger.info("Using row 2 as header row for APP import");
                }
            }
            
            logger.info("Detected {} columns in APP file: {}", cm.nameToIndex.size(), cm.nameToIndex.keySet());

            List<AppLine> lines = new ArrayList<>();
            Integer detectedYear = null;

            int rowNum = 1;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (isRowEmpty(row)) continue;

                AppLine line = new AppLine();
                line.setRowNumber(rowNum++);
                
                // Try multiple column name variations for each field
                line.setProjectIdentifier(findText(cm, row, "Project Identifier", "PROJECT IDENTIFIER", "PROJECT_IDENTIFIER"));
                line.setProjectName(findText(cm, row, "Project Name", "PROJECT NAME", "PROJECT_NAME"));
                line.setDepartment(findText(cm, row, "Department", "DEPARTMENT"));
                line.setCostCenter(findText(cm, row, "Cost Center", "COST CENTER", "COST_CENTER"));
                line.setCategory(findText(cm, row, "Category", "CATEGORY"));
                line.setVendor(findText(cm, row, "Vendor", "VENDOR"));
                line.setContractRef(findText(cm, row, "Contract Ref", "CONTRACT REF", "CONTRACT_REF"));
                line.setBudgetAmount(findDecimal(cm, row, "Budget", "BUDGET", "BUDGET AMOUNT", "BUDGET_AMOUNT"));
                
                // New fields - try actual column names from file
                line.setPackageNo(findText(cm, row, 
                    "Package No", "PACKAGE NO", "PACKAGE_NO",
                    "Packege No", "PACKEGE NO"));  // Handle typo in actual file
                line.setItemDescription(findText(cm, row,
                    "Item Description", "ITEM DESCRIPTION", "ITEM_DESCRIPTION",
                    "Description", "DESCRIPTION",
                    "Description of Procurement Items Goods", "DESCRIPTION OF PROCUREMENT ITEMS GOODS",
                    "Description of  Procurement Items Goods"));  // Handle double space
                line.setUnit(findText(cm, row, "Unit", "UNIT"));
                line.setQuantity(findDecimal(cm, row, 
                    "Quantity", "QUANTITY",
                    "Qty", "QTY"));  // Handle abbreviation
                line.setProcurementMethod(findText(cm, row, 
                    "Procurement Method", "PROCUREMENT METHOD", "PROCUREMENT_METHOD"));
                line.setApprovingAuthority(findText(cm, row, 
                    "Approving Authority", "APPROVING AUTHORITY", "APPROVING_AUTHORITY"));
                line.setSourceOfFund(findText(cm, row,
                    "Source of Fund", "SOURCE OF FUND", "SOURCE_OF_FUND",
                    "Source Of Fund", "SOURCE OF FUND"));  // Handle capitalization variation
                line.setEstimatedCostLakh(findDecimal(cm, row,
                    "Estimated Cost (Lakh)", "ESTIMATED COST (LAKH)",
                    "Estimated Cost In lakh tk", "ESTIMATED COST IN LAKH TK",
                    "Estimated Cost In Lakh", "ESTIMATED COST IN LAKH",
                    "Estimated Cost Lakh", "ESTIMATED COST LAKH"));

                // Year is optional - try to find it, but don't fail if missing
                Integer year = findInteger(cm, row, "Year", "YEAR", "Fiscal Year", "FISCAL YEAR");
                if (year != null) {
                    if (detectedYear == null) detectedYear = year;
                    if (!detectedYear.equals(year)) {
                        line.setValidationErrors("Year mismatch in row; expected " + detectedYear + " found " + year);
                    }
                }

                lines.add(line);
            }

            // Year is optional - use current year if not found
            if (detectedYear == null) {
                detectedYear = java.time.LocalDate.now().getYear();
                logger.warn("Could not detect fiscal year from APP file, using current year: {}", detectedYear);
            }

            // Upsert header for year (one per year)
            final Integer fiscalYear = detectedYear;
            AppHeader header = appHeaderRepository.findByFiscalYear(fiscalYear)
                .orElseGet(() -> {
                    AppHeader h = new AppHeader();
                    h.setFiscalYear(fiscalYear);
                    h.setDepartment(user.getDepartment());
                    h.setCreatedBy(user);
                    return h;
                });

            header = appHeaderRepository.save(header);
            for (AppLine line : lines) {
                line.setHeader(header);
            }
            appLineRepository.saveAll(lines);

            logger.info("Imported APP: year={}, lines={}", detectedYear, lines.size());
            return header;
        } catch (Exception e) {
            logger.error("Failed to import APP Excel: {}", e.getMessage());
            throw new RuntimeException("APP import failed: " + e.getMessage(), e);
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String s = cell.toString();
                if (s != null && !s.trim().isEmpty()) return false;
            }
        }
        return true;
    }

    private static class ColumnMap {
        private final java.util.Map<String, Integer> nameToIndex = new java.util.HashMap<>();

        static ColumnMap fromHeaderRow(Row header) {
            ColumnMap cm = new ColumnMap();
            DataFormatter formatter = new DataFormatter();
            for (int i = header.getFirstCellNum(); i < header.getLastCellNum(); i++) {
                Cell cell = header.getCell(i);
                if (cell != null) {
                    String headerValue = formatter.formatCellValue(cell).trim();
                    if (!headerValue.isEmpty()) {
                        // Store both original and uppercase for flexible matching
                        cm.nameToIndex.put(headerValue, i);
                        cm.nameToIndex.put(headerValue.toUpperCase(), i);
                    }
                }
            }
            return cm;
        }

        String text(Row row, String name) {
            // Try exact match first
            Integer idx = nameToIndex.get(name);
            if (idx == null) {
                // Try case-insensitive match
                idx = nameToIndex.get(name.toUpperCase());
            }
            if (idx == null) {
                // Try partial match for common variations
                for (Map.Entry<String, Integer> entry : nameToIndex.entrySet()) {
                    String key = entry.getKey().toUpperCase();
                    String searchName = name.toUpperCase();
                    if (key.contains(searchName) || searchName.contains(key)) {
                        idx = entry.getValue();
                        break;
                    }
                }
            }
            if (idx == null) return null;
            Cell cell = row.getCell(idx);
            if (cell == null) return null;
            DataFormatter formatter = new DataFormatter();
            String value = formatter.formatCellValue(cell);
            return value == null ? null : value.trim();
        }

        Integer integer(Row row, String name) {
            String val = text(row, name);
            if (val == null || val.isEmpty()) return null;
            try {
                return (int) Double.parseDouble(val);
            } catch (Exception e) {
                return null;
            }
        }

        BigDecimal decimal(Row row, String name) {
            String val = text(row, name);
            if (val == null || val.isEmpty()) return null;
            try {
                return new BigDecimal(val.replaceAll(",", ""));
            } catch (Exception e) {
                return null;
            }
        }
    }
    
    // Helper methods to find columns with multiple name variations
    private String findText(ColumnMap cm, Row row, String... possibleNames) {
        for (String name : possibleNames) {
            String value = cm.text(row, name);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }
    
    private BigDecimal findDecimal(ColumnMap cm, Row row, String... possibleNames) {
        for (String name : possibleNames) {
            BigDecimal value = cm.decimal(row, name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
    
    private Integer findInteger(ColumnMap cm, Row row, String... possibleNames) {
        for (String name : possibleNames) {
            Integer value = cm.integer(row, name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}


