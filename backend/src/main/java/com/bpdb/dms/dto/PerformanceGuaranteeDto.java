package com.bpdb.dms.dto;

import com.bpdb.dms.entity.PerformanceGuarantee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Performance Guarantee type-specific fields.
 */
public class PerformanceGuaranteeDto {

    private Long id;
    private Long documentId;
    private String pgNumber;
    private BigDecimal pgAmount;
    private LocalDate expiryDate;
    private String bankName;
    private LocalDate issueDate;
    private String contractNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PerformanceGuaranteeDto fromEntity(PerformanceGuarantee entity) {
        if (entity == null) {
            return null;
        }
        PerformanceGuaranteeDto dto = new PerformanceGuaranteeDto();
        dto.setId(entity.getId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setPgNumber(entity.getPgNumber());
        dto.setPgAmount(entity.getPgAmount());
        dto.setExpiryDate(entity.getExpiryDate());
        dto.setBankName(entity.getBankName());
        dto.setIssueDate(entity.getIssueDate());
        dto.setContractNumber(entity.getContractNumber());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getPgNumber() { return pgNumber; }
    public void setPgNumber(String pgNumber) { this.pgNumber = pgNumber; }

    public BigDecimal getPgAmount() { return pgAmount; }
    public void setPgAmount(BigDecimal pgAmount) { this.pgAmount = pgAmount; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
