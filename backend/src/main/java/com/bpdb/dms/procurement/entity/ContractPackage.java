package com.bpdb.dms.procurement.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Links a contract to a package. A contract may cover several packages (client answer Q-1).
 */
@Entity
@Table(name = "contract_package")
public class ContractPackage extends BaseProcurementEntity {

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "allocation_pct")
    private BigDecimal allocationPct;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }

    public BigDecimal getAllocationPct() { return allocationPct; }
    public void setAllocationPct(BigDecimal allocationPct) { this.allocationPct = allocationPct; }
}
