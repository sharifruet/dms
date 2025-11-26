package com.bpdb.dms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a bill from verified OCR data
 */
public class CreateBillFromOCRRequest {
    private BillOCRResult ocrResult;
    private Long documentId;

    public BillOCRResult getOcrResult() { return ocrResult; }
    public void setOcrResult(BillOCRResult ocrResult) { this.ocrResult = ocrResult; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    /**
     * Simplified DTO for verified bill data (from frontend)
     */
    public static class VerifiedBillData {
        private String vendorName;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private Integer fiscalYear;
        private BigDecimal totalAmount;
        private BigDecimal taxAmount;
        private BigDecimal subtotalAmount;
        private List<BillLineItemData> lineItems;

        // Getters and setters
        public String getVendorName() { return vendorName; }
        public void setVendorName(String vendorName) { this.vendorName = vendorName; }
        
        public String getInvoiceNumber() { return invoiceNumber; }
        public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
        
        public LocalDate getInvoiceDate() { return invoiceDate; }
        public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
        
        public Integer getFiscalYear() { return fiscalYear; }
        public void setFiscalYear(Integer fiscalYear) { this.fiscalYear = fiscalYear; }
        
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        
        public BigDecimal getTaxAmount() { return taxAmount; }
        public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
        
        public BigDecimal getSubtotalAmount() { return subtotalAmount; }
        public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }
        
        public List<BillLineItemData> getLineItems() { return lineItems; }
        public void setLineItems(List<BillLineItemData> lineItems) { this.lineItems = lineItems; }
    }

    public static class BillLineItemData {
        private String projectIdentifier;
        private String department;
        private String costCenter;
        private String category;
        private String description;
        private BigDecimal amount;
        private BigDecimal taxAmount;

        // Getters and setters
        public String getProjectIdentifier() { return projectIdentifier; }
        public void setProjectIdentifier(String projectIdentifier) { this.projectIdentifier = projectIdentifier; }
        
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        
        public String getCostCenter() { return costCenter; }
        public void setCostCenter(String costCenter) { this.costCenter = costCenter; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public BigDecimal getTaxAmount() { return taxAmount; }
        public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    }
}

