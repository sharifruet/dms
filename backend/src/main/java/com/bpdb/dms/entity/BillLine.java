package com.bpdb.dms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "bill_lines")
public class BillLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "header_id", nullable = false)
    private BillHeader header;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_line_id")
    private AppLine appLine; // optional linkage to APP line

    @Column(name = "project_identifier")
    private String projectIdentifier;

    @Column(name = "department")
    private String department;

    @Column(name = "cost_center")
    private String costCenter;

    @Column(name = "category")
    private String category;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "tax_amount", precision = 18, scale = 2)
    private BigDecimal taxAmount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BillHeader getHeader() { return header; }
    public void setHeader(BillHeader header) { this.header = header; }
    public AppLine getAppLine() { return appLine; }
    public void setAppLine(AppLine appLine) { this.appLine = appLine; }
    public String getProjectIdentifier() { return projectIdentifier; }
    public void setProjectIdentifier(String projectIdentifier) { this.projectIdentifier = projectIdentifier; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
}


