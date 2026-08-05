package com.bpdb.dms.procurement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Attaches a document to the record that owns it and, transitively, to the package.
 * No document in the lifecycle may exist without one of these (REQ-L1, REQ-P1).
 */
@Entity
@Table(name = "document_link")
public class DocumentLink extends BaseProcurementEntity {

    // link_origin
    public static final String OCR_KEY_MATCH = "OCR_KEY_MATCH";
    public static final String STAGE_CONTEXT = "STAGE_CONTEXT";
    public static final String MANUAL = "MANUAL";

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "stage_code", nullable = false)
    private Short stageCode;

    /** Catalogue role, e.g. TENDER_NOTICE, GRN, PAYMENT_VOUCHER (REQ-P2). */
    @Column(name = "doc_role", nullable = false)
    private String docRole;

    @Column(name = "is_mandatory")
    private Boolean isMandatory = Boolean.FALSE;

    @Column(name = "is_applicable")
    private Boolean isApplicable = Boolean.TRUE;

    @Column(name = "link_origin", nullable = false)
    private String linkOrigin = STAGE_CONTEXT;

    @Column(name = "created_by")
    private Long createdBy;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public Short getStageCode() { return stageCode; }
    public void setStageCode(Short stageCode) { this.stageCode = stageCode; }
    public String getDocRole() { return docRole; }
    public void setDocRole(String docRole) { this.docRole = docRole; }
    public Boolean getIsMandatory() { return isMandatory; }
    public void setIsMandatory(Boolean isMandatory) { this.isMandatory = isMandatory; }
    public Boolean getIsApplicable() { return isApplicable; }
    public void setIsApplicable(Boolean isApplicable) { this.isApplicable = isApplicable; }
    public String getLinkOrigin() { return linkOrigin; }
    public void setLinkOrigin(String linkOrigin) { this.linkOrigin = linkOrigin; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
