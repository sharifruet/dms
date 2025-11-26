package com.bpdb.dms.service;

import com.bpdb.dms.dto.BillOCRResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for extracting bill/invoice data from uploaded documents using OCR
 */
@Service
public class BillOCRService {
    
    private static final Logger logger = LoggerFactory.getLogger(BillOCRService.class);
    
    @Autowired
    private OCRService ocrService;
    
    /**
     * Extract bill data from uploaded invoice document and return as metadata map
     * This method extracts data and returns it in a format compatible with DocumentMetadataService
     * 
     * @param file The invoice file (PDF or image)
     * @return Map containing extracted bill fields with their values and confidence scores
     */
    public Map<String, String> extractBillDataAsMetadata(MultipartFile file) {
        Map<String, String> metadata = new HashMap<>();
        
        try {
            BillOCRResult ocrResult = extractBillData(file);
            
            if (ocrResult.isSuccess()) {
                // Store extracted values using document field keys
                if (ocrResult.getVendorName() != null) {
                    metadata.put("vendorName", ocrResult.getVendorName());
                    metadata.put("vendorName_confidence", String.valueOf(ocrResult.getVendorNameConfidence()));
                }
                if (ocrResult.getInvoiceNumber() != null) {
                    metadata.put("invoiceNumber", ocrResult.getInvoiceNumber());
                    metadata.put("invoiceNumber_confidence", String.valueOf(ocrResult.getInvoiceNumberConfidence()));
                }
                if (ocrResult.getInvoiceDate() != null) {
                    metadata.put("invoiceDate", ocrResult.getInvoiceDate().toString());
                    metadata.put("invoiceDate_confidence", String.valueOf(ocrResult.getInvoiceDateConfidence()));
                }
                if (ocrResult.getFiscalYear() != null) {
                    metadata.put("fiscalYear", String.valueOf(ocrResult.getFiscalYear()));
                    metadata.put("fiscalYear_confidence", String.valueOf(ocrResult.getFiscalYearConfidence()));
                }
                if (ocrResult.getTotalAmount() != null) {
                    metadata.put("totalAmount", ocrResult.getTotalAmount().toString());
                    metadata.put("totalAmount_confidence", String.valueOf(ocrResult.getTotalAmountConfidence()));
                }
                if (ocrResult.getTaxAmount() != null) {
                    metadata.put("taxAmount", ocrResult.getTaxAmount().toString());
                    metadata.put("taxAmount_confidence", String.valueOf(ocrResult.getTaxAmountConfidence()));
                }
                
                // Calculate net amount if both total and tax are available
                if (ocrResult.getTotalAmount() != null && ocrResult.getTaxAmount() != null) {
                    BigDecimal netAmount = ocrResult.getTotalAmount().subtract(ocrResult.getTaxAmount());
                    metadata.put("netAmount", netAmount.toString());
                }
                
                // Store overall confidence
                metadata.put("bill_ocr_overall_confidence", String.valueOf(ocrResult.getOverallConfidence()));
                metadata.put("bill_ocr_status", "success");
            } else {
                metadata.put("bill_ocr_status", "failed");
                metadata.put("bill_ocr_error", ocrResult.getErrorMessage() != null ? ocrResult.getErrorMessage() : "Unknown error");
            }
        } catch (Exception e) {
            logger.error("Error extracting bill data: {}", e.getMessage(), e);
            metadata.put("bill_ocr_status", "error");
            metadata.put("bill_ocr_error", e.getMessage());
        }
        
        return metadata;
    }
    
    /**
     * Extract bill data from uploaded invoice document
     * 
     * @param file The invoice file (PDF or image)
     * @return BillOCRResult containing extracted data with confidence scores
     */
    public BillOCRResult extractBillData(MultipartFile file) {
        BillOCRResult result = new BillOCRResult();
        result.setSuccess(false);
        
        try {
            // First extract text using OCR
            String extractedText = extractTextFromFile(file);
            if (extractedText == null || extractedText.trim().isEmpty()) {
                result.setErrorMessage("Failed to extract text from invoice. Please ensure the document is readable.");
                return result;
            }
            
            result.setRawOcrText(extractedText);
            
            // Extract vendor name
            extractVendorName(extractedText, result);
            
            // Extract invoice number
            extractInvoiceNumber(extractedText, result);
            
            // Extract invoice date
            extractInvoiceDate(extractedText, result);
            
            // Extract fiscal year (derived from invoice date)
            extractFiscalYear(result);
            
            // Extract total amount
            extractTotalAmount(extractedText, result);
            
            // Extract tax amount
            extractTaxAmount(extractedText, result);
            
            // Extract line items (basic extraction - can be enhanced)
            extractLineItems(extractedText, result);
            
            // Calculate overall confidence
            calculateOverallConfidence(result);
            
            result.setSuccess(true);
            logger.info("Bill OCR extraction completed. Vendor: {}, Invoice: {}, Total: {}", 
                       result.getVendorName(), result.getInvoiceNumber(), result.getTotalAmount());
            
        } catch (Exception e) {
            logger.error("Error extracting bill data from invoice: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage("Error processing invoice: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Extract text from file using OCR service
     */
    private String extractTextFromFile(MultipartFile file) {
        try {
            // Use existing OCR service to extract text
            var ocrResult = ocrService.extractText(file);
            return ocrResult.getExtractedText();
        } catch (Exception e) {
            logger.error("Failed to extract text using OCR: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract vendor name from OCR text
     */
    private void extractVendorName(String text, BillOCRResult result) {
        // Common patterns for vendor/supplier name
        Pattern[] patterns = {
            Pattern.compile("(?i)(?:vendor|supplier|from|bill\\s*to|sold\\s*to)[:;]?\\s*([A-Z][A-Za-z0-9\\s&.,-]+(?:Limited|Ltd|Corp|Corporation|Inc|Company|Co\\.)?)", Pattern.MULTILINE),
            Pattern.compile("(?i)^([A-Z][A-Za-z0-9\\s&.,-]{10,50})$", Pattern.MULTILINE), // Line starting with capitalized name
            Pattern.compile("(?i)(?:company|business)[:;]?\\s*([A-Z][A-Za-z0-9\\s&.,-]+)", Pattern.MULTILINE)
        };
        
        String vendor = null;
        double confidence = 0.0;
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                vendor = matcher.group(1).trim();
                confidence = 0.7; // Medium confidence for pattern matching
                break;
            }
        }
        
        result.setVendorName(vendor);
        result.setVendorNameConfidence(confidence);
    }
    
    /**
     * Extract invoice number from OCR text
     */
    private void extractInvoiceNumber(String text, BillOCRResult result) {
        Pattern[] patterns = {
            Pattern.compile("(?i)(?:invoice|bill|inv)[\\s#:]+([A-Z0-9\\-/]+)", Pattern.MULTILINE),
            Pattern.compile("(?i)(?:invoice\\s*number|inv\\s*no|bill\\s*number)[:;]?\\s*([A-Z0-9\\-/]+)", Pattern.MULTILINE),
            Pattern.compile("(?i)#\\s*([A-Z0-9\\-/]{5,20})", Pattern.MULTILINE)
        };
        
        String invoiceNumber = null;
        double confidence = 0.0;
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                invoiceNumber = matcher.group(1).trim();
                confidence = 0.8; // High confidence for invoice number patterns
                break;
            }
        }
        
        result.setInvoiceNumber(invoiceNumber);
        result.setInvoiceNumberConfidence(confidence);
    }
    
    /**
     * Extract invoice date from OCR text
     */
    private void extractInvoiceDate(String text, BillOCRResult result) {
        // Common date patterns
        Pattern[] patterns = {
            Pattern.compile("(?i)(?:date|invoice\\s*date|bill\\s*date|dated)[:;]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.MULTILINE),
            Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.MULTILINE), // General date pattern
            Pattern.compile("(\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{2,4})", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
        };
        
        LocalDate invoiceDate = null;
        double confidence = 0.0;
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String dateStr = matcher.group(1).trim();
                invoiceDate = parseDate(dateStr);
                if (invoiceDate != null) {
                    confidence = 0.75;
                    break;
                }
            }
        }
        
        result.setInvoiceDate(invoiceDate);
        result.setInvoiceDateConfidence(confidence);
    }
    
    /**
     * Parse date string to LocalDate
     */
    private LocalDate parseDate(String dateStr) {
        // Try common date formats
        String[] formats = {
            "dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy", "dd-MM-yy",
            "MM/dd/yyyy", "MM-dd-yyyy", "MM/dd/yy", "MM-dd-yy",
            "yyyy-MM-dd", "yyyy/MM/dd",
            "dd MMM yyyy", "dd MMMM yyyy"
        };
        
        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        return null;
    }
    
    /**
     * Extract fiscal year from invoice date
     */
    private void extractFiscalYear(BillOCRResult result) {
        if (result.getInvoiceDate() != null) {
            LocalDate date = result.getInvoiceDate();
            int year = date.getYear();
            int month = date.getMonthValue();
            
            // Fiscal year in Bangladesh typically runs from July to June
            // Adjust if invoice date is before July, fiscal year is previous year
            int fiscalYear = (month < 7) ? year : year + 1;
            
            result.setFiscalYear(fiscalYear);
            result.setFiscalYearConfidence(result.getInvoiceDateConfidence());
        }
    }
    
    /**
     * Extract total amount from OCR text
     */
    private void extractTotalAmount(String text, BillOCRResult result) {
        Pattern[] patterns = {
            Pattern.compile("(?i)(?:total|grand\\s*total|amount\\s*due|total\\s*amount)[:;]?\\s*(?:BDT|Tk|Taka|\\$|USD)?\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.MULTILINE),
            Pattern.compile("(?i)total[\\s:]+(?:BDT|Tk|Taka|\\$|USD)?[\\s:]*([\\d,]+(?:\\.\\d{2})?)", Pattern.MULTILINE),
            Pattern.compile("(?:BDT|Tk|Taka|\\$|USD)\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.MULTILINE)
        };
        
        BigDecimal totalAmount = null;
        double confidence = 0.0;
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String amountStr = matcher.group(1).replace(",", "");
                try {
                    totalAmount = new BigDecimal(amountStr);
                    confidence = 0.8;
                    break;
                } catch (NumberFormatException e) {
                    // Try next pattern
                }
            }
        }
        
        result.setTotalAmount(totalAmount);
        result.setTotalAmountConfidence(confidence);
    }
    
    /**
     * Extract tax amount from OCR text
     */
    private void extractTaxAmount(String text, BillOCRResult result) {
        Pattern[] patterns = {
            Pattern.compile("(?i)(?:tax|vat|gst|sales\\s*tax)[:;]?\\s*(?:BDT|Tk|Taka|\\$|USD)?\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.MULTILINE),
            Pattern.compile("(?i)(?:tax|vat|gst)\\s*amount[:;]?\\s*(?:BDT|Tk|Taka|\\$|USD)?\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.MULTILINE)
        };
        
        BigDecimal taxAmount = null;
        double confidence = 0.0;
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String amountStr = matcher.group(1).replace(",", "");
                try {
                    taxAmount = new BigDecimal(amountStr);
                    confidence = 0.7;
                    break;
                } catch (NumberFormatException e) {
                    // Try next pattern
                }
            }
        }
        
        result.setTaxAmount(taxAmount);
        result.setTaxAmountConfidence(confidence);
    }
    
    /**
     * Extract line items from OCR text (basic extraction)
     */
    private void extractLineItems(String text, BillOCRResult result) {
        // Basic line item extraction - can be enhanced later
        // For now, just set empty list - can be enhanced to parse line items
        result.setLineItems(new java.util.ArrayList<>());
    }
    
    /**
     * Calculate overall confidence score based on extracted fields
     */
    private void calculateOverallConfidence(BillOCRResult result) {
        double totalConfidence = 0.0;
        int fieldCount = 0;
        
        if (result.getVendorName() != null) {
            totalConfidence += result.getVendorNameConfidence();
            fieldCount++;
        }
        if (result.getInvoiceNumber() != null) {
            totalConfidence += result.getInvoiceNumberConfidence();
            fieldCount++;
        }
        if (result.getInvoiceDate() != null) {
            totalConfidence += result.getInvoiceDateConfidence();
            fieldCount++;
        }
        if (result.getTotalAmount() != null) {
            totalConfidence += result.getTotalAmountConfidence();
            fieldCount++;
        }
        
        if (fieldCount > 0) {
            result.setOverallConfidence(totalConfidence / fieldCount);
        } else {
            result.setOverallConfidence(0.0);
        }
    }
}
