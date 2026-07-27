package com.bpdb.dms.dto;

import com.bpdb.dms.entity.TenderNotice;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Tender Notice type-specific fields.
 */
public class TenderNoticeDto {

    private Long id;
    private Long documentId;
    private String tenderId;
    private String tenderType;
    private LocalDate tenderDate;
    private LocalDate closingDate;
    private LocalDate publicationDate;
    private String procurementPackageNo;
    private LocalDate tenderValidUpTo;
    private String procurementNature;
    private String procurementType;
    private String procurementDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TenderNoticeDto fromEntity(TenderNotice entity) {
        if (entity == null) {
            return null;
        }
        TenderNoticeDto dto = new TenderNoticeDto();
        dto.setId(entity.getId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setTenderId(entity.getTenderId());
        dto.setTenderType(entity.getTenderType());
        dto.setTenderDate(entity.getTenderDate());
        dto.setClosingDate(entity.getClosingDate());
        dto.setPublicationDate(entity.getPublicationDate());
        dto.setProcurementPackageNo(entity.getProcurementPackageNo());
        dto.setTenderValidUpTo(entity.getTenderValidUpTo());
        dto.setProcurementNature(entity.getProcurementNature());
        dto.setProcurementType(entity.getProcurementType());
        dto.setProcurementDescription(entity.getProcurementDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getTenderId() { return tenderId; }
    public void setTenderId(String tenderId) { this.tenderId = tenderId; }

    public String getTenderType() { return tenderType; }
    public void setTenderType(String tenderType) { this.tenderType = tenderType; }

    public LocalDate getTenderDate() { return tenderDate; }
    public void setTenderDate(LocalDate tenderDate) { this.tenderDate = tenderDate; }

    public LocalDate getClosingDate() { return closingDate; }
    public void setClosingDate(LocalDate closingDate) { this.closingDate = closingDate; }

    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }

    public String getProcurementPackageNo() { return procurementPackageNo; }
    public void setProcurementPackageNo(String procurementPackageNo) { this.procurementPackageNo = procurementPackageNo; }

    public LocalDate getTenderValidUpTo() { return tenderValidUpTo; }
    public void setTenderValidUpTo(LocalDate tenderValidUpTo) { this.tenderValidUpTo = tenderValidUpTo; }

    public String getProcurementNature() { return procurementNature; }
    public void setProcurementNature(String procurementNature) { this.procurementNature = procurementNature; }

    public String getProcurementType() { return procurementType; }
    public void setProcurementType(String procurementType) { this.procurementType = procurementType; }

    public String getProcurementDescription() { return procurementDescription; }
    public void setProcurementDescription(String procurementDescription) { this.procurementDescription = procurementDescription; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
