package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Contract - procurement lifecycle entity mapped to contract.
 */
@Entity
@Table(name = "contract")
public class Contract extends BaseProcurementEntity {

    @Column(name = "noa_id")
    private Long noaId;

    @Column(name = "contract_number")
    private String contractNumber;

    @Column(name = "contract_date")
    private LocalDate contractDate;

    @Column(name = "contract_value")
    private BigDecimal contractValue;

    @Column(name = "currency")
    private String currency;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "delivery_period_days")
    private Integer deliveryPeriodDays;

    @Column(name = "warranty_period_months")
    private Integer warrantyPeriodMonths;

    @Column(name = "supplier_name")
    private String supplierName;

    public Long getNoaId() { return noaId; }
    public void setNoaId(Long noaId) { this.noaId = noaId; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public LocalDate getContractDate() { return contractDate; }
    public void setContractDate(LocalDate contractDate) { this.contractDate = contractDate; }

    public BigDecimal getContractValue() { return contractValue; }
    public void setContractValue(BigDecimal contractValue) { this.contractValue = contractValue; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDate completionDate) { this.completionDate = completionDate; }

    public Integer getDeliveryPeriodDays() { return deliveryPeriodDays; }
    public void setDeliveryPeriodDays(Integer deliveryPeriodDays) { this.deliveryPeriodDays = deliveryPeriodDays; }

    public Integer getWarrantyPeriodMonths() { return warrantyPeriodMonths; }
    public void setWarrantyPeriodMonths(Integer warrantyPeriodMonths) { this.warrantyPeriodMonths = warrantyPeriodMonths; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
}
