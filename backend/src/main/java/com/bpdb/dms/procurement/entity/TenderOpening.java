package com.bpdb.dms.procurement.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * TenderOpening - procurement lifecycle entity mapped to tender_opening.
 */
@Entity
@Table(name = "tender_opening")
public class TenderOpening extends BaseProcurementEntity {

    @Column(name = "tender_id")
    private Long tenderId;

    @Column(name = "opening_date")
    private LocalDate openingDate;

    @Column(name = "number_of_bidders")
    private Integer numberOfBidders;

    @Column(name = "participating_bidders", columnDefinition = "TEXT")
    private String participatingBidders;

    public Long getTenderId() { return tenderId; }
    public void setTenderId(Long tenderId) { this.tenderId = tenderId; }

    public LocalDate getOpeningDate() { return openingDate; }
    public void setOpeningDate(LocalDate openingDate) { this.openingDate = openingDate; }

    public Integer getNumberOfBidders() { return numberOfBidders; }
    public void setNumberOfBidders(Integer numberOfBidders) { this.numberOfBidders = numberOfBidders; }

    public String getParticipatingBidders() { return participatingBidders; }
    public void setParticipatingBidders(String participatingBidders) { this.participatingBidders = participatingBidders; }
}
