package com.bpdb.dms.dto;

import com.bpdb.dms.entity.Correspondence;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Correspondence type-specific fields.
 */
public class CorrespondenceDto {

    private Long id;
    private Long documentId;
    private String referenceNo;
    private String subject;
    private LocalDate letterDate;
    private String fromParty;
    private String toParty;
    private String relatedTenderId;
    private String contractNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CorrespondenceDto fromEntity(Correspondence entity) {
        if (entity == null) {
            return null;
        }
        CorrespondenceDto dto = new CorrespondenceDto();
        dto.setId(entity.getId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setReferenceNo(entity.getReferenceNo());
        dto.setSubject(entity.getSubject());
        dto.setLetterDate(entity.getLetterDate());
        dto.setFromParty(entity.getFromParty());
        dto.setToParty(entity.getToParty());
        dto.setRelatedTenderId(entity.getRelatedTenderId());
        dto.setContractNumber(entity.getContractNumber());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public LocalDate getLetterDate() { return letterDate; }
    public void setLetterDate(LocalDate letterDate) { this.letterDate = letterDate; }

    public String getFromParty() { return fromParty; }
    public void setFromParty(String fromParty) { this.fromParty = fromParty; }

    public String getToParty() { return toParty; }
    public void setToParty(String toParty) { this.toParty = toParty; }

    public String getRelatedTenderId() { return relatedTenderId; }
    public void setRelatedTenderId(String relatedTenderId) { this.relatedTenderId = relatedTenderId; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
