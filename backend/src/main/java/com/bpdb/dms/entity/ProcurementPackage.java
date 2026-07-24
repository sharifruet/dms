package com.bpdb.dms.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "procurement_packages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id")
    private Long documentId;
    
    @Column(name = "status")
    private String status;

    @Column(name = "package_no", unique = true, nullable = false)
    private String packageNo;

    @Column(name = "lot_no")
    private String lotNo;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "unit")
    private String unit;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "procurement_method")
    private String procurementMethod;

    @Column(name = "contract_approving_authority")
    private String contractApprovingAuthority;

    @Column(name = "source_of_fund")
    private String sourceOfFund;

    @Column(name = "unit_cost")
    private Double unitCost;

    @Column(name = "total_cost")
    private Double totalCost;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "planned_dates_id")
    private StageTimeline plannedDates;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "planned_days_id")
    private StageDuration plannedDays;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "actual_dates_id")
    private StageTimeline actualDates;
}
