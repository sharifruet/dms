package com.bpdb.dms.procurement.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * BidSecurity - procurement lifecycle entity mapped to bid_security.
 */
@Entity
@Table(name = "bid_security")
public class BidSecurity extends BaseProcurementEntity {

    @Column(name = "opening_id")
    private Long openingId;

    @Column(name = "instrument_type")
    private String instrumentType;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "issuing_bank")
    private String issuingBank;

    @Column(name = "bidder_name")
    private String bidderName;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    public Long getOpeningId() { return openingId; }
    public void setOpeningId(Long openingId) { this.openingId = openingId; }

    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public String getIssuingBank() { return issuingBank; }
    public void setIssuingBank(String issuingBank) { this.issuingBank = issuingBank; }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }
}
