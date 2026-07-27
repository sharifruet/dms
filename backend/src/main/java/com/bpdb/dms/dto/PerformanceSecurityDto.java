package com.bpdb.dms.dto;

import com.bpdb.dms.entity.PerformanceSecurity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Performance Security type-specific fields.
 */
public class PerformanceSecurityDto {

    private Long id;
    private Long documentId;
    private String psNumber;
    private BigDecimal psAmount;
    private LocalDate expiryDate;
    private String bankName;
    private LocalDate issueDate;
    private String contractNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PerformanceSecurityDto fromEntity(PerformanceSecurity entity) {
        if (entity == null) {
            return null;
        }
        PerformanceSecurityDto dto = new PerformanceSecurityDto();
        dto.setId(entity.getId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setPsNumber(entity.getPsNumber());
        dto.setPsAmount(entity.getPsAmount());
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

    public String getPsNumber() { return psNumber; }
    public void setPsNumber(String psNumber) { this.psNumber = psNumber; }

    public BigDecimal getPsAmount() { return psAmount; }
    public void setPsAmount(BigDecimal psAmount) { this.psAmount = psAmount; }

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
