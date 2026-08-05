package com.bpdb.dms.procurement.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * ContractApproval - procurement lifecycle entity mapped to contract_approval.
 */
@Entity
@Table(name = "contract_approval")
public class ContractApproval extends BaseProcurementEntity {

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "awarded_bidder_id")
    private Long awardedBidderId;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "approving_authority")
    private String approvingAuthority;

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public Long getAwardedBidderId() { return awardedBidderId; }
    public void setAwardedBidderId(Long awardedBidderId) { this.awardedBidderId = awardedBidderId; }

    public LocalDate getApprovalDate() { return approvalDate; }
    public void setApprovalDate(LocalDate approvalDate) { this.approvalDate = approvalDate; }

    public String getApprovingAuthority() { return approvingAuthority; }
    public void setApprovingAuthority(String approvingAuthority) { this.approvingAuthority = approvingAuthority; }
}
