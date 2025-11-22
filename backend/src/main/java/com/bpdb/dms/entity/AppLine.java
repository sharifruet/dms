package com.bpdb.dms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "app_lines")
public class AppLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "header_id", nullable = false)
    private AppHeader header;

    @Column(name = "project_identifier")
    private String projectIdentifier;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "department")
    private String department;

    @Column(name = "cost_center")
    private String costCenter;

    @Column(name = "category")
    private String category;

    @Column(name = "vendor")
    private String vendor;

    @Column(name = "contract_ref")
    private String contractRef;

    @Column(name = "budget_amount", precision = 18, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(name = "validation_errors", length = 1000)
    private String validationErrors;

    @Column(name = "package_no", length = 100)
    private String packageNo;

    @Column(name = "item_description", length = 1000)
    private String itemDescription;

    @Column(name = "unit_label", length = 100)
    private String unit;

    @Column(name = "quantity", precision = 18, scale = 2)
    private BigDecimal quantity;

    @Column(name = "procurement_method", length = 255)
    private String procurementMethod;

    @Column(name = "approving_authority", length = 255)
    private String approvingAuthority;

    @Column(name = "source_of_fund", length = 255)
    private String sourceOfFund;

    @Column(name = "estimated_cost_lakh", precision = 18, scale = 2)
    private BigDecimal estimatedCostLakh;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AppHeader getHeader() { return header; }
    public void setHeader(AppHeader header) { this.header = header; }
    public String getProjectIdentifier() { return projectIdentifier; }
    public void setProjectIdentifier(String projectIdentifier) { this.projectIdentifier = projectIdentifier; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public String getContractRef() { return contractRef; }
    public void setContractRef(String contractRef) { this.contractRef = contractRef; }
    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }
    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
    public String getValidationErrors() { return validationErrors; }
    public void setValidationErrors(String validationErrors) { this.validationErrors = validationErrors; }

    public String getPackageNo() { return packageNo; }
    public void setPackageNo(String packageNo) { this.packageNo = packageNo; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getProcurementMethod() { return procurementMethod; }
    public void setProcurementMethod(String procurementMethod) { this.procurementMethod = procurementMethod; }

    public String getApprovingAuthority() { return approvingAuthority; }
    public void setApprovingAuthority(String approvingAuthority) { this.approvingAuthority = approvingAuthority; }

    public String getSourceOfFund() { return sourceOfFund; }
    public void setSourceOfFund(String sourceOfFund) { this.sourceOfFund = sourceOfFund; }

    public BigDecimal getEstimatedCostLakh() { return estimatedCostLakh; }
    public void setEstimatedCostLakh(BigDecimal estimatedCostLakh) { this.estimatedCostLakh = estimatedCostLakh; }
}


