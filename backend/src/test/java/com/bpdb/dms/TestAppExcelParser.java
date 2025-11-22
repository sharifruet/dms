import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Quick test utility to parse APP Excel file and show column structure
 */
public class TestAppExcelParser {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java TestAppExcelParser <path-to-excel-file>");
            System.exit(1);
        }
        
        String filePath = args[0];
        System.out.println("Reading Excel file: " + filePath);
        System.out.println("==========================================\n");
        
        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {
            
            if (workbook.getNumberOfSheets() == 0) {
                System.out.println("ERROR: No sheets found in workbook");
                return;
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("Sheet name: " + sheet.getSheetName());
            System.out.println("Total rows: " + (sheet.getLastRowNum() + 1));
            System.out.println();
            
            DataFormatter formatter = new DataFormatter();
            Iterator<Row> rowIterator = sheet.iterator();
            
            if (!rowIterator.hasNext()) {
                System.out.println("ERROR: Sheet is empty");
                return;
            }
            
            // Read header row
            Row headerRow = rowIterator.next();
            Map<String, Integer> headerIndex = new HashMap<>();
            
            System.out.println("=== HEADER ROW ===");
            for (int i = headerRow.getFirstCellNum(); i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String header = formatter.formatCellValue(cell);
                    String headerUpper = header.trim().toUpperCase();
                    headerIndex.put(headerUpper, i);
                    System.out.println("Column " + i + ": '" + header + "' -> '" + headerUpper + "'");
                }
            }
            System.out.println();
            
            System.out.println("=== DETECTED COLUMNS (Uppercase) ===");
            for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
                System.out.println(entry.getKey() + " -> Index " + entry.getValue());
            }
            System.out.println();
            
            // Check format detection
            boolean hasDate = headerIndex.containsKey("DATE");
            boolean hasTitle = headerIndex.containsKey("TITLE");
            boolean hasAmount = headerIndex.containsKey("AMOUNT");
            boolean hasProjectName = headerIndex.containsKey("PROJECT NAME") || 
                                    headerIndex.containsKey("PROJECT_NAME");
            boolean hasProjectId = headerIndex.containsKey("PROJECT IDENTIFIER") || 
                                  headerIndex.containsKey("PROJECT_IDENTIFIER");
            boolean hasEstimatedCost = headerIndex.keySet().stream()
                .anyMatch(h -> h.contains("ESTIMATED") && h.contains("COST"));
            
            System.out.println("=== FORMAT DETECTION ===");
            System.out.println("Simple format (DATE, TITLE, AMOUNT): " + (hasDate && hasTitle && hasAmount));
            System.out.println("APP format (Project Name/Identifier): " + (hasProjectName || hasProjectId));
            System.out.println("Has Project Name: " + hasProjectName);
            System.out.println("Has Project Identifier: " + hasProjectId);
            System.out.println("Has Estimated Cost: " + hasEstimatedCost);
            System.out.println();
            
            // Read first few data rows
            System.out.println("=== SAMPLE DATA ROWS (first 5) ===");
            int rowCount = 0;
            while (rowIterator.hasNext() && rowCount < 5) {
                Row row = rowIterator.next();
                boolean isEmpty = true;
                for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null && !formatter.formatCellValue(cell).isBlank()) {
                        isEmpty = false;
                        break;
                    }
                }
                if (isEmpty) continue;
                
                rowCount++;
                System.out.println("\nRow " + rowCount + ":");
                for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
                    Cell cell = row.getCell(entry.getValue());
                    String value = cell != null ? formatter.formatCellValue(cell) : "";
                    if (!value.isBlank()) {
                        System.out.println("  " + entry.getKey() + ": " + value);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

