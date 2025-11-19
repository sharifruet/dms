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

            Row headerRow = rowIterator.next();
            Map<String, Integer> headerIndex = extractHeaderIndices(headerRow, formatter);

            if (!headerIndex.keySet().containsAll(EXPECTED_HEADERS)) {
                metadata.put("appStatus", "missing_headers");
                metadata.put("appHeadersDetected", String.join(",", headerIndex.keySet()));
                return metadata;
            }

            List<AppDocumentEntry> entries = new ArrayList<>();
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
}

