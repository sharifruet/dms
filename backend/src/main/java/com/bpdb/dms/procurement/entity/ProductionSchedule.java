package com.bpdb.dms.procurement.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * ProductionSchedule - procurement lifecycle entity mapped to production_schedule.
 */
@Entity
@Table(name = "production_schedule")
public class ProductionSchedule extends BaseProcurementEntity {

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "delivery_period_days")
    private Integer deliveryPeriodDays;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getDeliveryPeriodDays() { return deliveryPeriodDays; }
    public void setDeliveryPeriodDays(Integer deliveryPeriodDays) { this.deliveryPeriodDays = deliveryPeriodDays; }
}
