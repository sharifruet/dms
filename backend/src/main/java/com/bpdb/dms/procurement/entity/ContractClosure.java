package com.bpdb.dms.procurement.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * ContractClosure - procurement lifecycle entity mapped to contract_closure.
 */
@Entity
@Table(name = "contract_closure")
public class ContractClosure extends BaseProcurementEntity {

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "contract_close_date")
    private LocalDate contractCloseDate;

    @Column(name = "outstanding_notes", columnDefinition = "TEXT")
    private String outstandingNotes;

    @Column(name = "closed_by")
    private Long closedBy;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }

    public LocalDate getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDate completionDate) { this.completionDate = completionDate; }

    public LocalDate getContractCloseDate() { return contractCloseDate; }
    public void setContractCloseDate(LocalDate contractCloseDate) { this.contractCloseDate = contractCloseDate; }

    public String getOutstandingNotes() { return outstandingNotes; }
    public void setOutstandingNotes(String outstandingNotes) { this.outstandingNotes = outstandingNotes; }

    public Long getClosedBy() { return closedBy; }
    public void setClosedBy(Long closedBy) { this.closedBy = closedBy; }
}
