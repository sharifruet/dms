package com.bpdb.dms.dto;

import com.bpdb.dms.entity.WorkCompletionCertificate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Work Completion Certificate type-specific fields.
 */
public class WorkCompletionCertificateDto {

    private Long id;
    private Long documentId;
    private String certificateNumber;
    private String contractNumber;
    private String vendorName;
    private LocalDate completionDate;
    private String projectDescription;
    private BigDecimal contractAmount;
    private LocalDate issueDate;
    private String issuedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkCompletionCertificateDto fromEntity(WorkCompletionCertificate entity) {
        if (entity == null) {
            return null;
        }
        WorkCompletionCertificateDto dto = new WorkCompletionCertificateDto();
        dto.setId(entity.getId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setCertificateNumber(entity.getCertificateNumber());
        dto.setContractNumber(entity.getContractNumber());
        dto.setVendorName(entity.getVendorName());
        dto.setCompletionDate(entity.getCompletionDate());
        dto.setProjectDescription(entity.getProjectDescription());
        dto.setContractAmount(entity.getContractAmount());
        dto.setIssueDate(entity.getIssueDate());
        dto.setIssuedBy(entity.getIssuedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public LocalDate getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDate completionDate) { this.completionDate = completionDate; }

    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String projectDescription) { this.projectDescription = projectDescription; }

    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
