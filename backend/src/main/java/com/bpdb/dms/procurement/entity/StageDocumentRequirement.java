package com.bpdb.dms.procurement.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * StageDocumentRequirement - procurement lifecycle entity mapped to stage_document_requirement.
 */
@Entity
@Table(name = "stage_document_requirement")
public class StageDocumentRequirement extends BaseProcurementEntity {

    @Column(name = "stage_code")
    private Short stageCode;

    @Column(name = "doc_role")
    private String docRole;

    @Column(name = "doc_label")
    private String docLabel;

    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    @Column(name = "is_conditional")
    private Boolean isConditional;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active")
    private Boolean isActive;

    public Short getStageCode() { return stageCode; }
    public void setStageCode(Short stageCode) { this.stageCode = stageCode; }

    public String getDocRole() { return docRole; }
    public void setDocRole(String docRole) { this.docRole = docRole; }

    public String getDocLabel() { return docLabel; }
    public void setDocLabel(String docLabel) { this.docLabel = docLabel; }

    public Boolean getIsMandatory() { return isMandatory; }
    public void setIsMandatory(Boolean isMandatory) { this.isMandatory = isMandatory; }

    public Boolean getIsConditional() { return isConditional; }
    public void setIsConditional(Boolean isConditional) { this.isConditional = isConditional; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
