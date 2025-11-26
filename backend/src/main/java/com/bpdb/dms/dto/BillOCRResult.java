package com.bpdb.dms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTO for bill/invoice OCR extraction results
 */
public class BillOCRResult {
    
    private boolean success;
    private String errorMessage;
    private String rawOcrText;
    
    // Extracted fields
    private String vendorName;
    private Double vendorNameConfidence;
    
    private String invoiceNumber;
    private Double invoiceNumberConfidence;
    
    private LocalDate invoiceDate;
    private Double invoiceDateConfidence;
    
    private Integer fiscalYear;
    private Double fiscalYearConfidence;
    
    private BigDecimal totalAmount;
    private Double totalAmountConfidence;
    
    private BigDecimal taxAmount;
    private Double taxAmountConfidence;
    
    private List<BillLineItem> lineItems;
    
    private Double overallConfidence;
    
    // Corrected values (after user verification)
    private String correctedVendorName;
    private String correctedInvoiceNumber;
    private LocalDate correctedInvoiceDate;
    private Integer correctedFiscalYear;
    private BigDecimal correctedTotalAmount;
    private BigDecimal correctedTaxAmount;
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getRawOcrText() { return rawOcrText; }
    public void setRawOcrText(String rawOcrText) { this.rawOcrText = rawOcrText; }
    
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    
    public Double getVendorNameConfidence() { return vendorNameConfidence; }
    public void setVendorNameConfidence(Double vendorNameConfidence) { this.vendorNameConfidence = vendorNameConfidence; }
    
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    
    public Double getInvoiceNumberConfidence() { return invoiceNumberConfidence; }
    public void setInvoiceNumberConfidence(Double invoiceNumberConfidence) { this.invoiceNumberConfidence = invoiceNumberConfidence; }
    
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    
    public Double getInvoiceDateConfidence() { return invoiceDateConfidence; }
    public void setInvoiceDateConfidence(Double invoiceDateConfidence) { this.invoiceDateConfidence = invoiceDateConfidence; }
    
    public Integer getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(Integer fiscalYear) { this.fiscalYear = fiscalYear; }
    
    public Double getFiscalYearConfidence() { return fiscalYearConfidence; }
    public void setFiscalYearConfidence(Double fiscalYearConfidence) { this.fiscalYearConfidence = fiscalYearConfidence; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public Double getTotalAmountConfidence() { return totalAmountConfidence; }
    public void setTotalAmountConfidence(Double totalAmountConfidence) { this.totalAmountConfidence = totalAmountConfidence; }
    
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    
    public Double getTaxAmountConfidence() { return taxAmountConfidence; }
    public void setTaxAmountConfidence(Double taxAmountConfidence) { this.taxAmountConfidence = taxAmountConfidence; }
    
    public List<BillLineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<BillLineItem> lineItems) { this.lineItems = lineItems; }
    
    public Double getOverallConfidence() { return overallConfidence; }
    public void setOverallConfidence(Double overallConfidence) { this.overallConfidence = overallConfidence; }
    
    public String getCorrectedVendorName() { return correctedVendorName; }
    public void setCorrectedVendorName(String correctedVendorName) { this.correctedVendorName = correctedVendorName; }
    
    public String getCorrectedInvoiceNumber() { return correctedInvoiceNumber; }
    public void setCorrectedInvoiceNumber(String correctedInvoiceNumber) { this.correctedInvoiceNumber = correctedInvoiceNumber; }
    
    public LocalDate getCorrectedInvoiceDate() { return correctedInvoiceDate; }
    public void setCorrectedInvoiceDate(LocalDate correctedInvoiceDate) { this.correctedInvoiceDate = correctedInvoiceDate; }
    
    public Integer getCorrectedFiscalYear() { return correctedFiscalYear; }
    public void setCorrectedFiscalYear(Integer correctedFiscalYear) { this.correctedFiscalYear = correctedFiscalYear; }
    
    public BigDecimal getCorrectedTotalAmount() { return correctedTotalAmount; }
    public void setCorrectedTotalAmount(BigDecimal correctedTotalAmount) { this.correctedTotalAmount = correctedTotalAmount; }
    
    public BigDecimal getCorrectedTaxAmount() { return correctedTaxAmount; }
    public void setCorrectedTaxAmount(BigDecimal correctedTaxAmount) { this.correctedTaxAmount = correctedTaxAmount; }
    
    /**
     * Inner class for bill line items
     */
    public static class BillLineItem {
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal total;
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        
        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }
    }
}
