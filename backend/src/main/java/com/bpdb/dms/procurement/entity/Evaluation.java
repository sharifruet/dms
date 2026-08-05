package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Evaluation - procurement lifecycle entity mapped to evaluation.
 */
@Entity
@Table(name = "evaluation")
public class Evaluation extends BaseProcurementEntity {

    @Column(name = "opening_id")
    private Long openingId;

    @Column(name = "oce_value")
    private BigDecimal oceValue;

    @Column(name = "currency")
    private String currency;

    @Column(name = "evaluation_date")
    private LocalDate evaluationDate;

    public Long getOpeningId() { return openingId; }
    public void setOpeningId(Long openingId) { this.openingId = openingId; }

    public BigDecimal getOceValue() { return oceValue; }
    public void setOceValue(BigDecimal oceValue) { this.oceValue = oceValue; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getEvaluationDate() { return evaluationDate; }
    public void setEvaluationDate(LocalDate evaluationDate) { this.evaluationDate = evaluationDate; }
}
