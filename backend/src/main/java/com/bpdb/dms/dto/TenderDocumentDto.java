package com.bpdb.dms.dto;

import com.bpdb.dms.entity.TenderDocument;

import java.time.LocalDateTime;

/**
 * DTO for Tender Document type-specific fields.
 */
public class TenderDocumentDto {

    private Long id;
    private Long documentId;
    private String tenderId;
    private String documentCategory;
    private String tenderType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TenderDocumentDto fromEntity(TenderDocument entity) {
        if (entity == null) {
            return null;
        }
        TenderDocumentDto dto = new TenderDocumentDto();
        dto.setId(entity.getId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setTenderId(entity.getTenderId());
        dto.setDocumentCategory(entity.getDocumentCategory());
        dto.setTenderType(entity.getTenderType());
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

    public String getDocumentCategory() { return documentCategory; }
    public void setDocumentCategory(String documentCategory) { this.documentCategory = documentCategory; }

    public String getTenderType() { return tenderType; }
    public void setTenderType(String tenderType) { this.tenderType = tenderType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
