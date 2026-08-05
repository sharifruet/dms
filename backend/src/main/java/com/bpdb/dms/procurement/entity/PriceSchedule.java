package com.bpdb.dms.procurement.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * PriceSchedule - procurement lifecycle entity mapped to price_schedule.
 */
@Entity
@Table(name = "price_schedule")
public class PriceSchedule extends BaseProcurementEntity {

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "source")
    private String source;

    @Column(name = "delivery_period_days")
    private Integer deliveryPeriodDays;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Integer getDeliveryPeriodDays() { return deliveryPeriodDays; }
    public void setDeliveryPeriodDays(Integer deliveryPeriodDays) { this.deliveryPeriodDays = deliveryPeriodDays; }
}
