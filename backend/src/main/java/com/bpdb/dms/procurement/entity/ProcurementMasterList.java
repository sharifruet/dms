package com.bpdb.dms.procurement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A permitted value for one of the Stage 2 master lists (client answer Q-8, REQ-2.4).
 *
 * <p>Procurement Type, Method and Nature come from fixed lists BPDB supplied. Holding them
 * as rows rather than an enum is deliberate: the requirement is that unmatched values are
 * <em>flagged for manual selection</em>, not rejected, and that the lists can change
 * without a code change.
 */
@Entity
@Table(name = "procurement_master_list")
public class ProcurementMasterList {

    /** The three lists Q-8 defines. */
    public static final String PROCUREMENT_TYPE = "PROCUREMENT_TYPE";
    public static final String PROCUREMENT_METHOD = "PROCUREMENT_METHOD";
    public static final String PROCUREMENT_NATURE = "PROCUREMENT_NATURE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "list_key")
    private String listKey;

    @Column(name = "value_code")
    private String valueCode;

    @Column(name = "value_label")
    private String valueLabel;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getListKey() { return listKey; }
    public void setListKey(String listKey) { this.listKey = listKey; }

    public String getValueCode() { return valueCode; }
    public void setValueCode(String valueCode) { this.valueCode = valueCode; }

    public String getValueLabel() { return valueLabel; }
    public void setValueLabel(String valueLabel) { this.valueLabel = valueLabel; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
