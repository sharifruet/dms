package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * OcrResult - procurement lifecycle entity mapped to ocr_result.
 */
@Entity
@Table(name = "ocr_result")
public class OcrResult extends BaseProcurementEntity {

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "version")
    private Integer version;

    @Column(name = "full_text", columnDefinition = "TEXT")
    private String fullText;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "avg_confidence")
    private BigDecimal avgConfidence;

    @Column(name = "language")
    private String language;

    @Column(name = "is_current")
    private Boolean isCurrent;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getFullText() { return fullText; }
    public void setFullText(String fullText) { this.fullText = fullText; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public BigDecimal getAvgConfidence() { return avgConfidence; }
    public void setAvgConfidence(BigDecimal avgConfidence) { this.avgConfidence = avgConfidence; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Boolean getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }
}
