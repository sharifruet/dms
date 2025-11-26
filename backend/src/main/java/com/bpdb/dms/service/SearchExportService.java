package com.bpdb.dms.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting search results to PDF and Excel formats
 */
@Service
public class SearchExportService {

    private static final Logger logger = LoggerFactory.getLogger(SearchExportService.class);

    // Note: These services are not currently used but may be needed for future enhancements
    // @Autowired
    // private DocumentIndexingService documentIndexingService;
    // @Autowired
    // private DocumentRepository documentRepository;

    /**
     * Export search results to Excel format
     * 
     * @param searchResults The search results to export
     * @param searchQuery The original search query
     * @param filters The search filters applied
     * @return Excel file as byte array
     */
    public byte[] exportToExcel(DocumentIndexingService.SearchResult searchResults, 
                               String searchQuery, 
                               DocumentIndexingService.SearchFilters filters) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Search Results");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            int rowNum = 0;
            
            // Add search metadata
            Row metadataRow = sheet.createRow(rowNum++);
            Cell metadataCell = metadataRow.createCell(0);
            metadataCell.setCellValue("Search Query: " + (searchQuery != null ? searchQuery : "All Documents"));
            metadataCell.setCellStyle(headerStyle);
            
            rowNum++;
            Row filterRow = sheet.createRow(rowNum++);
            Cell filterCell = filterRow.createCell(0);
            filterCell.setCellValue("Filters: " + formatFilters(filters));
            
            rowNum++;
            Row totalRow = sheet.createRow(rowNum++);
            Cell totalCell = totalRow.createCell(0);
            totalCell.setCellValue("Total Results: " + searchResults.getTotalHits());
            
            rowNum++;
            Row dateRow = sheet.createRow(rowNum++);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue("Export Date: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            rowNum += 2; // Skip a row
            
            // Create header row
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Document ID", "File Name", "Original Name", "Document Type", 
                              "Department", "Uploaded By", "Created Date", "OCR Confidence", 
                              "Classification Confidence", "Score"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Add data rows
            for (DocumentIndexingService.SearchResultItem item : searchResults.getItems()) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(item.getDocumentId() != null ? item.getDocumentId() : 0);
                row.createCell(1).setCellValue(item.getFileName() != null ? item.getFileName() : "");
                row.createCell(2).setCellValue(item.getOriginalName() != null ? item.getOriginalName() : "");
                row.createCell(3).setCellValue(item.getDocumentType() != null ? item.getDocumentType() : "");
                row.createCell(4).setCellValue(item.getDepartment() != null ? item.getDepartment() : "");
                row.createCell(5).setCellValue(item.getUploadedBy() != null ? item.getUploadedBy() : "");
                row.createCell(6).setCellValue(item.getCreatedAt() != null ? item.getCreatedAt() : "");
                row.createCell(7).setCellValue(item.getOcrConfidence() != null ? item.getOcrConfidence() : 0.0);
                row.createCell(8).setCellValue(item.getClassificationConfidence() != null ? item.getClassificationConfidence() : 0.0);
                row.createCell(9).setCellValue(item.getScore());
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            logger.error("Error exporting search results to Excel: {}", e.getMessage());
            throw new RuntimeException("Failed to export search results to Excel", e);
        }
    }

    /**
     * Export search results to PDF format using Apache PDFBox
     * 
     * @param searchResults The search results to export
     * @param searchQuery The original search query
     * @param filters The search filters applied
     * @return PDF file as byte array
     */
    public byte[] exportToPdf(DocumentIndexingService.SearchResult searchResults,
                             String searchQuery,
                             DocumentIndexingService.SearchFilters filters) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            // Create a landscape page (A4 rotated)
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float margin = 50;
                float yPosition = pageHeight - margin;
                
                PDType1Font titleFont = PDType1Font.HELVETICA_BOLD;
                PDType1Font headerFont = PDType1Font.HELVETICA_BOLD;
                PDType1Font textFont = PDType1Font.HELVETICA;
                PDType1Font smallFont = PDType1Font.HELVETICA;
                
                float titleSize = 16;
                float headerSize = 10;
                float textSize = 9;
                float smallSize = 8;
                float lineHeight = 15;
                
                // Title
                contentStream.beginText();
                contentStream.setFont(titleFont, titleSize);
                float titleWidth = titleFont.getStringWidth("Document Search Results") / 1000 * titleSize;
                contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, yPosition);
                contentStream.showText("Document Search Results");
                contentStream.endText();
                yPosition -= lineHeight * 2;
                
                // Metadata
                contentStream.beginText();
                contentStream.setFont(textFont, textSize);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Search Query: " + (searchQuery != null ? searchQuery : "All Documents"));
                contentStream.endText();
                yPosition -= lineHeight;
                
                contentStream.beginText();
                contentStream.setFont(textFont, textSize);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Filters: " + formatFilters(filters));
                contentStream.endText();
                yPosition -= lineHeight;
                
                contentStream.beginText();
                contentStream.setFont(textFont, textSize);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Total Results: " + searchResults.getTotalHits());
                contentStream.endText();
                yPosition -= lineHeight;
                
                contentStream.beginText();
                contentStream.setFont(textFont, textSize);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Export Date: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                contentStream.endText();
                yPosition -= lineHeight * 2;
                
                // Table headers
                String[] headers = {"ID", "File Name", "Original Name", "Type", 
                                  "Department", "Uploaded By", "Created", "OCR", 
                                  "Class", "Score"};
                
                float[] columnWidths = {40, 120, 120, 80, 80, 80, 80, 50, 50, 50};
                float tableStartX = margin;
                float tableWidth = pageWidth - (2 * margin);
                
                // Draw header background
                contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
                contentStream.addRect(tableStartX, yPosition - 15, tableWidth, 15);
                contentStream.fill();
                contentStream.setNonStrokingColor(0f, 0f, 0f);
                
                // Draw header text
                float currentX = tableStartX + 5;
                contentStream.beginText();
                contentStream.setFont(headerFont, headerSize);
                contentStream.newLineAtOffset(currentX, yPosition - 12);
                for (int i = 0; i < headers.length; i++) {
                    if (i > 0) {
                        // Move to next column
                        contentStream.newLineAtOffset(columnWidths[i - 1] + 5, 0);
                    }
                    contentStream.showText(headers[i]);
                }
                contentStream.endText();
                yPosition -= lineHeight * 1.5f;
                
                // Draw data rows
                contentStream.setFont(smallFont, smallSize);
                int rowsPerPage = (int) ((yPosition - margin) / lineHeight);
                List<DocumentIndexingService.SearchResultItem> items = searchResults.getItems();
                int itemsToShow = Math.min(items.size(), rowsPerPage);
                
                for (int row = 0; row < itemsToShow; row++) {
                    if (yPosition < margin + 50) {
                        break; // Not enough space
                    }
                    
                    DocumentIndexingService.SearchResultItem item = items.get(row);
                    String[] rowData = {
                        String.valueOf(item.getDocumentId() != null ? item.getDocumentId() : ""),
                        truncate(item.getFileName(), 20),
                        truncate(item.getOriginalName(), 20),
                        truncate(item.getDocumentType(), 15),
                        truncate(item.getDepartment(), 15),
                        truncate(item.getUploadedBy(), 15),
                        truncate(item.getCreatedAt(), 12),
                        item.getOcrConfidence() != null ? String.format("%.2f", item.getOcrConfidence()) : "",
                        item.getClassificationConfidence() != null ? String.format("%.2f", item.getClassificationConfidence()) : "",
                        String.format("%.2f", item.getScore())
                    };
                    
                    currentX = tableStartX + 5;
                    contentStream.beginText();
                    contentStream.newLineAtOffset(currentX, yPosition - 10);
                    for (int col = 0; col < rowData.length; col++) {
                        if (col > 0) {
                            // Move to next column
                            contentStream.newLineAtOffset(columnWidths[col - 1] + 5, 0);
                        }
                        contentStream.showText(rowData[col]);
                    }
                    contentStream.endText();
                    yPosition -= lineHeight;
                }
                
                // Add note if more results exist
                if (items.size() > itemsToShow) {
                    yPosition -= lineHeight;
                    contentStream.beginText();
                    contentStream.setFont(textFont, textSize);
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText("Note: Showing " + itemsToShow + " of " + items.size() + " results. Export to Excel for complete data.");
                    contentStream.endText();
                }
            }
            
            document.save(outputStream);
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            logger.error("Error exporting search results to PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to export search results to PDF", e);
        }
    }
    
    /**
     * Truncate string to max length
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

    /**
     * Format search filters as a string
     */
    private String formatFilters(DocumentIndexingService.SearchFilters filters) {
        if (filters == null) {
            return "None";
        }
        
        StringBuilder sb = new StringBuilder();
        if (filters.getDocumentTypes() != null && !filters.getDocumentTypes().isEmpty()) {
            sb.append("Types: ").append(String.join(", ", filters.getDocumentTypes())).append("; ");
        }
        if (filters.getDepartments() != null && !filters.getDepartments().isEmpty()) {
            sb.append("Departments: ").append(String.join(", ", filters.getDepartments())).append("; ");
        }
        if (filters.getIsActive() != null) {
            sb.append("Active: ").append(filters.getIsActive()).append("; ");
        }
        
        return sb.length() > 0 ? sb.toString() : "None";
    }
}

