package com.bpdb.dms.dto;

import com.bpdb.dms.entity.BankGuarantee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Bank Guarantee type-specific fields.
 */
public class BankGuaranteeDto {

    private Long id;
    private Long documentId;
    private String bgNumber;
    private String bankName;
    private BigDecimal bgAmount;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BankGuaranteeDto fromEntity(BankGuarantee entity) {
        if (entity == null) {
            return null;
        }
        BankGuaranteeDto dto = new BankGuaranteeDto();
        dto.setId(entity.getId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setBgNumber(entity.getBgNumber());
        dto.setBankName(entity.getBankName());
        dto.setBgAmount(entity.getBgAmount());
        dto.setIssueDate(entity.getIssueDate());
        dto.setExpiryDate(entity.getExpiryDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getBgNumber() { return bgNumber; }
    public void setBgNumber(String bgNumber) { this.bgNumber = bgNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public BigDecimal getBgAmount() { return bgAmount; }
    public void setBgAmount(BigDecimal bgAmount) { this.bgAmount = bgAmount; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
