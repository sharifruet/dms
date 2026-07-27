package com.bpdb.dms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tender_notices")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TenderNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false, unique = true)
    private Long documentId;

    @Column(name = "tender_id", length = 100)
    private String tenderId;

    @Column(name = "tender_type", length = 50)
    private String tenderType;

    @Column(name = "tender_date")
    private LocalDate tenderDate;

    @Column(name = "closing_date")
    private LocalDate closingDate;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "procurement_package_no", length = 255)
    private String procurementPackageNo;

    @Column(name = "tender_valid_up_to")
    private LocalDate tenderValidUpTo;

    @Column(name = "procurement_nature", length = 100)
    private String procurementNature;

    @Column(name = "procurement_type", length = 100)
    private String procurementType;

    @Column(name = "procurement_description", columnDefinition = "TEXT")
    private String procurementDescription;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
