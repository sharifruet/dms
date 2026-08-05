package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * DeliveryLine - procurement lifecycle entity mapped to delivery_line.
 */
@Entity
@Table(name = "delivery_line")
public class DeliveryLine extends BaseProcurementEntity {

    @Column(name = "delivery_id")
    private Long deliveryId;

    @Column(name = "price_schedule_line_id")
    private Long priceScheduleLineId;

    @Column(name = "quantity")
    private BigDecimal quantity;

    public Long getDeliveryId() { return deliveryId; }
    public void setDeliveryId(Long deliveryId) { this.deliveryId = deliveryId; }

    public Long getPriceScheduleLineId() { return priceScheduleLineId; }
    public void setPriceScheduleLineId(Long priceScheduleLineId) { this.priceScheduleLineId = priceScheduleLineId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
