package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * BerBidder - procurement lifecycle entity mapped to ber_bidder.
 */
@Entity
@Table(name = "ber_bidder")
public class BerBidder extends BaseProcurementEntity {

    @Column(name = "evaluation_id")
    private Long evaluationId;

    @Column(name = "bidder_name")
    private String bidderName;

    @Column(name = "bidding_price")
    private BigDecimal biddingPrice;

    @Column(name = "currency")
    private String currency;

    @Column(name = "is_responsive")
    private Boolean isResponsive;

    /** Percentage versus OCE (REQ-4.2). NUMERIC(18,4) after changeset 049. */
    @Column(name = "deviation_pct", precision = 18, scale = 4)
    private BigDecimal deviationPct;

    @Column(name = "bid_rank")
    private Integer bidRank;

    @Column(name = "is_awarded")
    private Boolean isAwarded;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    public Long getEvaluationId() { return evaluationId; }
    public void setEvaluationId(Long evaluationId) { this.evaluationId = evaluationId; }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }

    public BigDecimal getBiddingPrice() { return biddingPrice; }
    public void setBiddingPrice(BigDecimal biddingPrice) { this.biddingPrice = biddingPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Boolean getIsResponsive() { return isResponsive; }
    public void setIsResponsive(Boolean isResponsive) { this.isResponsive = isResponsive; }

    public BigDecimal getDeviationPct() { return deviationPct; }
    public void setDeviationPct(BigDecimal deviationPct) { this.deviationPct = deviationPct; }

    public Integer getBidRank() { return bidRank; }
    public void setBidRank(Integer bidRank) { this.bidRank = bidRank; }

    public Boolean getIsAwarded() { return isAwarded; }
    public void setIsAwarded(Boolean isAwarded) { this.isAwarded = isAwarded; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
