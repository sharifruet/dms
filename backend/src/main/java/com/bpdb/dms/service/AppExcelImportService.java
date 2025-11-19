package com.bpdb.dms.service;

import com.bpdb.dms.entity.AppHeader;
import com.bpdb.dms.entity.AppLine;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.AppHeaderRepository;
import com.bpdb.dms.repository.AppLineRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

            // Expect header row with known columns
            Iterator<Row> rowIterator = sheet.rowIterator();
            if (!rowIterator.hasNext()) {
                throw new IllegalArgumentException("APP sheet is empty");
            }
            Row headerRow = rowIterator.next();
            ColumnMap cm = ColumnMap.fromHeaderRow(headerRow);

            List<AppLine> lines = new ArrayList<>();
            Integer detectedYear = null;

            int rowNum = 1;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (isRowEmpty(row)) continue;

                AppLine line = new AppLine();
                line.setRowNumber(rowNum++);
                line.setProjectIdentifier(cm.text(row, "Project Identifier"));
                line.setProjectName(cm.text(row, "Project Name"));
                line.setDepartment(cm.text(row, "Department"));
                line.setCostCenter(cm.text(row, "Cost Center"));
                line.setCategory(cm.text(row, "Category"));
                line.setVendor(cm.text(row, "Vendor"));
                line.setContractRef(cm.text(row, "Contract Ref"));
                line.setBudgetAmount(cm.decimal(row, "Budget"));

                Integer year = cm.integer(row, "Year");
                if (year != null) {
                    if (detectedYear == null) detectedYear = year;
                    if (!detectedYear.equals(year)) {
                        line.setValidationErrors("Year mismatch in row; expected " + detectedYear + " found " + year);
                    }
                }

                lines.add(line);
            }

            if (detectedYear == null) {
                throw new IllegalArgumentException("Could not detect fiscal year from APP file");
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
            for (int i = header.getFirstCellNum(); i < header.getLastCellNum(); i++) {
                Cell cell = header.getCell(i);
                if (cell != null) {
                    cm.nameToIndex.put(cell.getStringCellValue().trim(), i);
                }
            }
            return cm;
        }

        String text(Row row, String name) {
            Integer idx = nameToIndex.get(name);
            if (idx == null) return null;
            Cell cell = row.getCell(idx);
            return cell == null ? null : cell.toString().trim();
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
}


