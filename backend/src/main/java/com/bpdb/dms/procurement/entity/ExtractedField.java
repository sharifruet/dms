package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One row per captured value, whether read by OCR or typed by a user.
 *
 * This is the evidence record behind every figure in the system: what the document
 * literally said (rawValue, immutable after insert), what we parsed it into, who
 * confirmed it, and where on the page it came from.
 *
 * See requirements section 3.5 (REQ-P3 .. REQ-P8).
 */
@Entity
@Table(name = "extracted_field")
public class ExtractedField extends BaseProcurementEntity {

    // capture_source
    public static final String SOURCE_OCR = "OCR";
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_DERIVED = "DERIVED";
    public static final String SOURCE_IMPORT = "IMPORT";

    // status
    public static final String OCR_SUGGESTED = "OCR_SUGGESTED";
    public static final String VERIFIED = "VERIFIED";
    public static final String MANUAL_OVERRIDE = "MANUAL_OVERRIDE";
    public static final String REJECTED = "REJECTED";

    // validation_state
    public static final String VALID = "VALID";
    public static final String NEEDS_REVIEW = "NEEDS_REVIEW";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INVALID = "INVALID";
    /**
     * A later OCR pass read something different from the value a person confirmed.
     * The confirmed value is kept and this flags the disagreement for review - neither
     * reading is thrown away, and neither silently wins.
     */
    public static final String CONFLICT = "CONFLICT";

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "stage_code", nullable = false)
    private Short stageCode;

    @Column(name = "field_key", nullable = false)
    private String fieldKey;

    @Column(name = "field_label")
    private String fieldLabel;

    /** TEXT | NUMBER | DATE | BOOL | CURRENCY | ENUM */
    @Column(name = "data_type", nullable = false)
    private String dataType = "TEXT";

    /** Literal text as read. Never updated after insert (REQ-P5). */
    @Column(name = "raw_value", columnDefinition = "TEXT", updatable = false)
    private String rawValue;

    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    @Column(name = "numeric_value")
    private BigDecimal numericValue;

    @Column(name = "date_value")
    private LocalDate dateValue;

    @Column(name = "bool_value")
    private Boolean boolValue;

    @Column(name = "currency")
    private String currency;

    /** e.g. LAC_BDT - an amount is never a bare number (REQ-P7). */
    @Column(name = "unit")
    private String unit;

    @Column(name = "fx_rate")
    private BigDecimal fxRate;

    @Column(name = "capture_source", nullable = false)
    private String captureSource;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "ocr_result_id")
    private Long ocrResultId;

    @Column(name = "page_no")
    private Integer pageNo;

    /** x,y,w,h of the source region, for click-to-highlight in the verify screen. */
    @Column(name = "bbox")
    private String bbox;

    @Column(name = "ocr_confidence")
    private BigDecimal ocrConfidence;

    @Column(name = "status", nullable = false)
    private String status = OCR_SUGGESTED;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "is_mandatory")
    private Boolean isMandatory = Boolean.FALSE;

    @Column(name = "validation_state")
    private String validationState;

    @Column(name = "validation_message", columnDefinition = "TEXT")
    private String validationMessage;

    /** A field counts towards stage completion only once a user has stood behind it. */
    public boolean isConfirmed() {
        return VERIFIED.equals(status) || MANUAL_OVERRIDE.equals(status);
    }

    public boolean hasValue() {
        return textValue != null || numericValue != null || dateValue != null || boolValue != null;
    }

    /** Current value as display text, whichever typed column holds it. */
    public String displayValue() {
        if (textValue != null) return textValue;
        if (numericValue != null) return numericValue.toPlainString();
        if (dateValue != null) return dateValue.toString();
        if (boolValue != null) return boolValue.toString();
        return null;
    }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public Short getStageCode() { return stageCode; }
    public void setStageCode(Short stageCode) { this.stageCode = stageCode; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public String getRawValue() { return rawValue; }
    public void setRawValue(String rawValue) { this.rawValue = rawValue; }
    public String getTextValue() { return textValue; }
    public void setTextValue(String textValue) { this.textValue = textValue; }
    public BigDecimal getNumericValue() { return numericValue; }
    public void setNumericValue(BigDecimal numericValue) { this.numericValue = numericValue; }
    public LocalDate getDateValue() { return dateValue; }
    public void setDateValue(LocalDate dateValue) { this.dateValue = dateValue; }
    public Boolean getBoolValue() { return boolValue; }
    public void setBoolValue(Boolean boolValue) { this.boolValue = boolValue; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getFxRate() { return fxRate; }
    public void setFxRate(BigDecimal fxRate) { this.fxRate = fxRate; }
    public String getCaptureSource() { return captureSource; }
    public void setCaptureSource(String captureSource) { this.captureSource = captureSource; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Long getOcrResultId() { return ocrResultId; }
    public void setOcrResultId(Long ocrResultId) { this.ocrResultId = ocrResultId; }
    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public String getBbox() { return bbox; }
    public void setBbox(String bbox) { this.bbox = bbox; }
    public BigDecimal getOcrConfidence() { return ocrConfidence; }
    public void setOcrConfidence(BigDecimal ocrConfidence) { this.ocrConfidence = ocrConfidence; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(Long verifiedBy) { this.verifiedBy = verifiedBy; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public Boolean getIsMandatory() { return isMandatory; }
    public void setIsMandatory(Boolean isMandatory) { this.isMandatory = isMandatory; }
    public String getValidationState() { return validationState; }
    public void setValidationState(String validationState) { this.validationState = validationState; }
    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
}
