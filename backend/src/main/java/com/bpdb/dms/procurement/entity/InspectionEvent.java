package com.bpdb.dms.procurement.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * InspectionEvent - procurement lifecycle entity mapped to inspection_event.
 */
@Entity
@Table(name = "inspection_event")
public class InspectionEvent extends BaseProcurementEntity {

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "inspection_type")
    private String inspectionType;

    @Column(name = "inspection_date")
    private LocalDate inspectionDate;

    @Column(name = "location")
    private String location;

    @Column(name = "fat_done")
    private Boolean fatDone;

    @Column(name = "sat_applicable")
    private Boolean satApplicable;

    @Column(name = "sat_done")
    private Boolean satDone;

    @Column(name = "result")
    private String result;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }

    public String getInspectionType() { return inspectionType; }
    public void setInspectionType(String inspectionType) { this.inspectionType = inspectionType; }

    public LocalDate getInspectionDate() { return inspectionDate; }
    public void setInspectionDate(LocalDate inspectionDate) { this.inspectionDate = inspectionDate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Boolean getFatDone() { return fatDone; }
    public void setFatDone(Boolean fatDone) { this.fatDone = fatDone; }

    public Boolean getSatApplicable() { return satApplicable; }
    public void setSatApplicable(Boolean satApplicable) { this.satApplicable = satApplicable; }

    public Boolean getSatDone() { return satDone; }
    public void setSatDone(Boolean satDone) { this.satDone = satDone; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
