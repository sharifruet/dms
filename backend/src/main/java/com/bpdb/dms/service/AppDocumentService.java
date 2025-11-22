package com.bpdb.dms.service;

import com.bpdb.dms.entity.AppDocumentEntry;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.repository.AppDocumentEntryRepository;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class AppDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(AppDocumentService.class);
    private static final List<String> EXPECTED_HEADERS = List.of("DATE", "TITLE", "AMOUNT");

    private final AppDocumentEntryRepository appDocumentEntryRepository;

    public AppDocumentService(AppDocumentEntryRepository appDocumentEntryRepository) {
        this.appDocumentEntryRepository = appDocumentEntryRepository;
    }

    /**
     * Process an uploaded APP Excel file, persist structured entries, and return summary metadata.
     * Supports two formats:
     * 1. Simple format: DATE, TITLE, AMOUNT
     * 2. APP format: Project Identifier, Project Name, Department, etc. (uses Project Name as title, Estimated Cost as amount)
     */
    public Map<String, String> processAndStoreEntries(Document document, MultipartFile file) {
        Map<String, String> metadata = new HashMap<>();
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {
                metadata.put("appStatus", "empty_workbook");
                return metadata;
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                metadata.put("appStatus", "missing_sheet");
                return metadata;
            }

            DataFormatter formatter = new DataFormatter();
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                metadata.put("appStatus", "empty_sheet");
                return metadata;
            }

            // Find the header row - it might be row 1 or row 2 (skip empty first row)
            Row headerRow = rowIterator.next();
            Map<String, Integer> headerIndex = extractHeaderIndices(headerRow, formatter);
            
            // If first row is empty or has very few headers, try next row
            if (headerIndex.size() < 3 && rowIterator.hasNext()) {
                Row nextRow = rowIterator.next();
                Map<String, Integer> nextHeaderIndex = extractHeaderIndices(nextRow, formatter);
                if (nextHeaderIndex.size() > headerIndex.size()) {
                    headerRow = nextRow;
                    headerIndex = nextHeaderIndex;
                    logger.info("Using row 2 as header row for APP document {}", document.getId());
                }
            }
            
            // Log detected headers for debugging - this will help identify the actual column names
            logger.info("Detected headers in APP document {}: {}", document.getId(), headerIndex.keySet());
            logger.info("Full header map for APP document {}: {}", document.getId(), headerIndex);

            // Check if this is the simple format (DATE, TITLE, AMOUNT)
            boolean isSimpleFormat = headerIndex.keySet().containsAll(EXPECTED_HEADERS);
            
            // Check if this is the APP format (has Project Identifier, Project Name, Description, etc.)
            // Try multiple variations of column names - case insensitive matching
            boolean isAppFormat = headerIndex.containsKey("PROJECT IDENTIFIER") || 
                                 headerIndex.containsKey("PROJECT NAME") ||
                                 headerIndex.containsKey("PROJECT_IDENTIFIER") ||
                                 headerIndex.containsKey("PROJECT_NAME") ||
                                 headerIndex.keySet().stream().anyMatch(h -> h.contains("PROJECT") && h.contains("NAME")) ||
                                 headerIndex.keySet().stream().anyMatch(h -> h.contains("PROJECT") && h.contains("IDENTIFIER")) ||
                                 // Check for Description column (common in APP files)
                                 headerIndex.keySet().stream().anyMatch(h -> h.contains("DESCRIPTION") && 
                                     (h.contains("PROCUREMENT") || h.contains("ITEM") || h.contains("GOODS"))) ||
                                 // Also check for common variations
                                 headerIndex.keySet().stream().anyMatch(h -> h.matches(".*PROJECT.*NAME.*")) ||
                                 headerIndex.keySet().stream().anyMatch(h -> h.matches(".*PROJECT.*IDENTIFIER.*"));
            
            logger.info("APP document {} - isSimpleFormat: {}, isAppFormat: {}", document.getId(), isSimpleFormat, isAppFormat);

            List<AppDocumentEntry> entries = new ArrayList<>();
            
            if (isSimpleFormat) {
                // Process simple format: DATE, TITLE, AMOUNT
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    if (isRowEmpty(row, formatter)) {
                        continue;
                    }

                    LocalDate date = parseDate(formatter.formatCellValue(row.getCell(headerIndex.get("DATE"))));
                    String title = formatter.formatCellValue(row.getCell(headerIndex.get("TITLE")));
                    BigDecimal amount = parseAmount(formatter.formatCellValue(row.getCell(headerIndex.get("AMOUNT"))));

                    if (title == null || title.isBlank()) {
                        continue;
                    }

                    AppDocumentEntry entry = new AppDocumentEntry(document, date, title, amount);
                    entries.add(entry);
                }
            } else if (isAppFormat) {
                // Process APP format: Project Identifier, Project Name, Description, etc.
                // Try to find name/title column with various name variations
                Integer projectNameIdx = findColumnIndex(headerIndex, 
                    "PROJECT NAME", "PROJECT_NAME", "PROJECTNAME",
                    "DESCRIPTION", "DESCRIPTION OF PROCUREMENT ITEMS GOODS",
                    "ITEM DESCRIPTION", "ITEM_DESCRIPTION");
                
                // Try to find Estimated Cost column with various name variations
                Integer estimatedCostIdx = findColumnIndex(headerIndex,
                    "ESTIMATED COST (LAKH)", "ESTIMATED_COST_LAKH", "ESTIMATED COST LAKH",
                    "ESTIMATED COST IN LAKH TK", "ESTIMATED COST IN LAKH",
                    "ESTIMATED COST", "ESTIMATED_COST", "ESTIMATEDCOST");
                
                // Fallback: try Budget column if Estimated Cost not found
                if (estimatedCostIdx == null) {
                    estimatedCostIdx = findColumnIndex(headerIndex, "BUDGET", "BUDGET AMOUNT", "BUDGET_AMOUNT");
                }
                
                logger.info("APP document {} - projectNameIdx: {}, estimatedCostIdx: {}", 
                    document.getId(), projectNameIdx, estimatedCostIdx);

                int rowCount = 0;
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
                    
                    // If no project name column found, try to use Project Identifier or first non-empty column
                    if ((projectName == null || projectName.isBlank()) && projectNameIdx == null) {
                        // Try Project Identifier
                        Integer projectIdIdx = findColumnIndex(headerIndex, 
                            "PROJECT IDENTIFIER", "PROJECT_IDENTIFIER", "PROJECTIDENTIFIER");
                        if (projectIdIdx != null) {
                            Cell cell = row.getCell(projectIdIdx);
                            if (cell != null) {
                                projectName = formatter.formatCellValue(cell).trim();
                            }
                        }
                        // If still no name, use first non-empty text column
                        if (projectName == null || projectName.isBlank()) {
                            for (int i = 0; i < row.getLastCellNum(); i++) {
                                Cell cell = row.getCell(i);
                                if (cell != null) {
                                    String value = formatter.formatCellValue(cell).trim();
                                    if (!value.isBlank() && !value.matches("^\\d+$")) {
                                        projectName = value;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    if (projectName == null || projectName.isBlank()) {
                        logger.debug("Skipping row {} - no project name found", rowCount);
                        continue;
                    }

                    BigDecimal estimatedCostLakh = null;
                    if (estimatedCostIdx != null) {
                        Cell cell = row.getCell(estimatedCostIdx);
                        if (cell != null) {
                            estimatedCostLakh = parseAmount(formatter.formatCellValue(cell));
                        }
                    }
                    
                    // Convert lakh to actual amount (multiply by 100000) if it's in lakh
                    // Otherwise use the value directly
                    BigDecimal amount;
                    if (estimatedCostIdx != null && 
                        (headerIndex.containsKey("ESTIMATED COST (LAKH)") || 
                         headerIndex.containsKey("ESTIMATED_COST_LAKH") ||
                         headerIndex.keySet().stream().anyMatch(h -> h.contains("LAKH")))) {
                        // It's in lakh, multiply by 100000
                        amount = estimatedCostLakh != null ? 
                            estimatedCostLakh.multiply(java.math.BigDecimal.valueOf(100000)) : 
                            java.math.BigDecimal.ZERO;
                    } else {
                        // It's already in actual amount
                        amount = estimatedCostLakh != null ? estimatedCostLakh : java.math.BigDecimal.ZERO;
                    }

                    // Use current date or null for entry date
                    AppDocumentEntry entry = new AppDocumentEntry(document, null, projectName, amount);
                    entries.add(entry);
                }
                
                logger.info("APP document {} - processed {} rows, created {} entries", 
                    document.getId(), rowCount, entries.size());
            } else {
                // If neither format matches, log all headers and try to process anyway with best guess
                logger.warn("APP document {} - Unsupported format detected. Headers: {}", 
                    document.getId(), headerIndex.keySet());
                metadata.put("appStatus", "unsupported_format");
                metadata.put("appHeadersDetected", String.join(",", headerIndex.keySet()));
                
                // Try to process anyway - look for any column that might be a name/title
                // and any column that might be an amount
                Integer nameIdx = null;
                Integer amountIdx = null;
                String amountHeaderName = null;
                
                for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
                    String header = entry.getKey();
                    if (nameIdx == null && (header.contains("NAME") || header.contains("TITLE") || 
                        header.contains("DESCRIPTION") || header.contains("PROJECT"))) {
                        nameIdx = entry.getValue();
                    }
                    if (amountIdx == null && (header.contains("AMOUNT") || header.contains("COST") || 
                        header.contains("BUDGET") || header.contains("PRICE") || header.contains("VALUE"))) {
                        amountIdx = entry.getValue();
                        amountHeaderName = header;
                    }
                }
                
                if (nameIdx != null) {
                    final Integer finalAmountIdx = amountIdx;
                    final String finalAmountHeaderName = amountHeaderName;
                    
                    logger.info("Attempting to process APP document {} with guessed columns: nameIdx={}, amountIdx={}", 
                        document.getId(), nameIdx, finalAmountIdx);
                    
                    rowIterator = sheet.iterator();
                    rowIterator.next(); // Skip header
                    
                    int rowCount = 0;
                    while (rowIterator.hasNext()) {
                        Row row = rowIterator.next();
                        if (isRowEmpty(row, formatter)) {
                            continue;
                        }
                        rowCount++;
                        
                        Cell nameCell = row.getCell(nameIdx);
                        String name = nameCell != null ? formatter.formatCellValue(nameCell).trim() : null;
                        
                        if (name == null || name.isBlank()) {
                            continue;
                        }
                        
                        BigDecimal amount = BigDecimal.ZERO;
                        if (finalAmountIdx != null) {
                            Cell amountCell = row.getCell(finalAmountIdx);
                            if (amountCell != null) {
                                BigDecimal parsed = parseAmount(formatter.formatCellValue(amountCell));
                                if (parsed != null) {
                                    // Check if header contains "LAKH" to determine if we need to multiply
                                    if (finalAmountHeaderName != null && finalAmountHeaderName.contains("LAKH")) {
                                        amount = parsed.multiply(BigDecimal.valueOf(100000));
                                    } else {
                                        amount = parsed;
                                    }
                                }
                            }
                        }
                        
                        AppDocumentEntry entry = new AppDocumentEntry(document, null, name, amount);
                        entries.add(entry);
                    }
                    
                    logger.info("Processed {} rows from APP document {} with guessed format, created {} entries", 
                        rowCount, document.getId(), entries.size());
                }
                
                if (entries.isEmpty()) {
                    return metadata;
                }
            }

            if (!entries.isEmpty()) {
                appDocumentEntryRepository.deleteByDocument(document);
                appDocumentEntryRepository.saveAll(entries);
                metadata.put("appStatus", "processed");
                metadata.put("appEntryCount", String.valueOf(entries.size()));
            } else {
                metadata.put("appStatus", "no_entries");
            }
        } catch (Exception ex) {
            logger.error("Failed to process APP document for document {}: {}", document.getId(), ex.getMessage());
            metadata.put("appStatus", "failed");
            metadata.put("appError", ex.getMessage());
        }
        return metadata;
    }

    private Map<String, Integer> extractHeaderIndices(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = formatter.formatCellValue(cell);
            if (header != null) {
                headerIndex.put(header.trim().toUpperCase(Locale.ROOT), cell.getColumnIndex());
            }
        }
        return headerIndex;
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
            DateTimeFormatter.ISO_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(raw.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        logger.warn("Unable to parse date value '{}' in APP document", raw);
        return null;
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.replaceAll("[^0-9,.-]", "").replace(",", "");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            logger.warn("Unable to parse amount value '{}' in APP document", raw);
            return null;
        }
    }
    
    /**
     * Helper method to find column index by trying multiple possible column name variations
     */
    private Integer findColumnIndex(Map<String, Integer> headerIndex, String... possibleNames) {
        for (String name : possibleNames) {
            Integer idx = headerIndex.get(name.toUpperCase(Locale.ROOT));
            if (idx != null) {
                return idx;
            }
        }
        return null;
    }
}

