package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * PriceScheduleLine - procurement lifecycle entity mapped to price_schedule_line.
 */
@Entity
@Table(name = "price_schedule_line")
public class PriceScheduleLine extends BaseProcurementEntity {

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "line_no")
    private Integer lineNo;

    @Column(name = "item_code")
    private String itemCode;

    @Column(name = "item_description", columnDefinition = "TEXT")
    private String itemDescription;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "uom")
    private String uom;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "line_amount")
    private BigDecimal lineAmount;

    @Column(name = "currency")
    private String currency;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public Integer getLineNo() { return lineNo; }
    public void setLineNo(Integer lineNo) { this.lineNo = lineNo; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getUom() { return uom; }
    public void setUom(String uom) { this.uom = uom; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getLineAmount() { return lineAmount; }
    public void setLineAmount(BigDecimal lineAmount) { this.lineAmount = lineAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
