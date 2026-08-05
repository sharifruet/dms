package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * OcrPage - procurement lifecycle entity mapped to ocr_page.
 */
@Entity
@Table(name = "ocr_page")
public class OcrPage extends BaseProcurementEntity {

    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "page_no")
    private Integer pageNo;

    @Column(name = "page_text", columnDefinition = "TEXT")
    private String pageText;

    @Column(name = "confidence")
    private BigDecimal confidence;

    public Long getResultId() { return resultId; }
    public void setResultId(Long resultId) { this.resultId = resultId; }

    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }

    public String getPageText() { return pageText; }
    public void setPageText(String pageText) { this.pageText = pageText; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
}
