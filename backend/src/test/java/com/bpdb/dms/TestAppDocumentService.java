package com.bpdb.dms;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * Test utility to verify AppDocumentService parsing logic
 */
public class TestAppDocumentService {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java TestAppDocumentService <path-to-excel-file>");
            System.exit(1);
        }
        
        String filePath = args[0];
        System.out.println("Testing Java parsing logic on: " + filePath);
        System.out.println("==========================================\n");
        
        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {
            
            if (workbook.getNumberOfSheets() == 0) {
                System.out.println("ERROR: No sheets found");
                return;
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Iterator<Row> rowIterator = sheet.iterator();
            
            if (!rowIterator.hasNext()) {
                System.out.println("ERROR: Sheet is empty");
                return;
            }
            
            // Find header row (simulating AppDocumentService logic)
            Row headerRow = rowIterator.next();
            Map<String, Integer> headerIndex = extractHeaderIndices(headerRow, formatter);
            
            System.out.println("=== HEADER ROW 1 ===");
            System.out.println("Headers found: " + headerIndex.size());
            headerIndex.forEach((k, v) -> System.out.println("  " + k + " -> " + v));
            System.out.println();
            
            // If first row is empty or has very few headers, try next row
            if (headerIndex.size() < 3 && rowIterator.hasNext()) {
                Row nextRow = rowIterator.next();
                Map<String, Integer> nextHeaderIndex = extractHeaderIndices(nextRow, formatter);
                System.out.println("=== HEADER ROW 2 ===");
                System.out.println("Headers found: " + nextHeaderIndex.size());
                nextHeaderIndex.forEach((k, v) -> System.out.println("  " + k + " -> " + v));
                System.out.println();
                
                if (nextHeaderIndex.size() > headerIndex.size()) {
                    headerRow = nextRow;
                    headerIndex = nextHeaderIndex;
                    System.out.println("Using row 2 as header row");
                }
            }
            System.out.println();
            
            // Check format detection
            boolean isSimpleFormat = headerIndex.keySet().containsAll(Arrays.asList("DATE", "TITLE", "AMOUNT"));
            boolean isAppFormat = headerIndex.containsKey("PROJECT IDENTIFIER") || 
                                 headerIndex.containsKey("PROJECT NAME") ||
                                 headerIndex.keySet().stream().anyMatch(h -> h.contains("DESCRIPTION") && 
                                     (h.contains("PROCUREMENT") || h.contains("ITEM") || h.contains("GOODS")));
            
            System.out.println("=== FORMAT DETECTION ===");
            System.out.println("isSimpleFormat: " + isSimpleFormat);
            System.out.println("isAppFormat: " + isAppFormat);
            System.out.println();
            
            if (!isAppFormat) {
                System.out.println("ERROR: APP format not detected!");
                return;
            }
            
            // Find columns
            Integer projectNameIdx = findColumnIndex(headerIndex, 
                "PROJECT NAME", "PROJECT_NAME", "PROJECTNAME",
                "DESCRIPTION", "DESCRIPTION OF PROCUREMENT ITEMS GOODS",
                "ITEM DESCRIPTION", "ITEM_DESCRIPTION");
            
            Integer estimatedCostIdx = findColumnIndex(headerIndex,
                "ESTIMATED COST (LAKH)", "ESTIMATED_COST_LAKH", "ESTIMATED COST LAKH",
                "ESTIMATED COST IN LAKH TK", "ESTIMATED COST IN LAKH",
                "ESTIMATED COST", "ESTIMATED_COST", "ESTIMATEDCOST");
            
            System.out.println("=== COLUMN DETECTION ===");
            System.out.println("projectNameIdx: " + projectNameIdx);
            System.out.println("estimatedCostIdx: " + estimatedCostIdx);
            System.out.println();
            
            // Process rows
            System.out.println("=== PROCESSING ROWS ===");
            int rowCount = 0;
            int entryCount = 0;
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (isRowEmpty(row, formatter)) {
                    continue;
                }
                rowCount++;
                
                String projectName = null;
                if (projectNameIdx != null) {
                    Cell cell = row.getCell(projectNameIdx);
                    if (cell != null) {
                        projectName = formatter.formatCellValue(cell).trim();
                    }
                }
                
                // Skip separator rows
                if (projectName == null || projectName.isBlank() || 
                    projectName.matches("^\\d+$") || 
                    projectName.equals("Days") || 
                    projectName.equals("Actual Date") ||
                    projectName.contains("Planned Date")) {
                    continue;
                }
                
                BigDecimal estimatedCostLakh = null;
                if (estimatedCostIdx != null) {
                    Cell cell = row.getCell(estimatedCostIdx);
                    if (cell != null) {
                        estimatedCostLakh = parseAmount(formatter.formatCellValue(cell));
                    }
                }
                
                BigDecimal amount = estimatedCostLakh != null ? 
                    estimatedCostLakh.multiply(BigDecimal.valueOf(100000)) : 
                    BigDecimal.ZERO;
                
                entryCount++;
                System.out.println("Entry " + entryCount + " (Row " + (rowCount + 2) + "):");
                System.out.println("  Title: " + projectName);
                System.out.println("  Estimated Cost (Lakh): " + estimatedCostLakh);
                System.out.println("  Amount: " + amount);
                System.out.println();
            }
            
            System.out.println("=== SUMMARY ===");
            System.out.println("Total rows processed: " + rowCount);
            System.out.println("Entries created: " + entryCount);
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static Map<String, Integer> extractHeaderIndices(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = formatter.formatCellValue(cell);
            if (header != null) {
                headerIndex.put(header.trim().toUpperCase(Locale.ROOT), cell.getColumnIndex());
            }
        }
        return headerIndex;
    }
    
    private static boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }
    
    private static Integer findColumnIndex(Map<String, Integer> headerIndex, String... possibleNames) {
        for (String name : possibleNames) {
            Integer idx = headerIndex.get(name.toUpperCase(Locale.ROOT));
            if (idx != null) {
                return idx;
            }
        }
        return null;
    }
    
    private static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.replaceAll("[^0-9,.-]", "").replace(",", "");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}


