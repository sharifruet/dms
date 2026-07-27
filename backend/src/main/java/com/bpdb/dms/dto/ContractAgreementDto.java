package com.bpdb.dms.dto;

import com.bpdb.dms.entity.ContractAgreement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Contract Agreement type-specific fields.
 */
public class ContractAgreementDto {

    private Long id;
    private Long documentId;
    private String contractNumber;
    private String vendorName;
    private LocalDate contractDate;
    private BigDecimal contractAmount;
    private String validityPeriod;
    private LocalDate expiryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ContractAgreementDto fromEntity(ContractAgreement entity) {
        if (entity == null) {
            return null;
        }
        ContractAgreementDto dto = new ContractAgreementDto();
        dto.setId(entity.getId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setContractNumber(entity.getContractNumber());
        dto.setVendorName(entity.getVendorName());
        dto.setContractDate(entity.getContractDate());
        dto.setContractAmount(entity.getContractAmount());
        dto.setValidityPeriod(entity.getValidityPeriod());
        dto.setExpiryDate(entity.getExpiryDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public LocalDate getContractDate() { return contractDate; }
    public void setContractDate(LocalDate contractDate) { this.contractDate = contractDate; }

    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }

    public String getValidityPeriod() { return validityPeriod; }
    public void setValidityPeriod(String validityPeriod) { this.validityPeriod = validityPeriod; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
