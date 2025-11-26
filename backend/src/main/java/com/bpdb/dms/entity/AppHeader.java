package com.bpdb.dms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_headers", 
       uniqueConstraints = @UniqueConstraint(name = "uk_app_headers_fiscal_year_installment", 
                                            columnNames = {"fiscal_year", "release_installment_no"}))
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AppHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "department")
    private String department;

    @Column(name = "allocation_type", length = 50)
    private String allocationType; // Annual, Revised, Additional, Emergency

    @Column(name = "budget_release_date")
    private LocalDate budgetReleaseDate;

    @Column(name = "allocation_amount", precision = 18, scale = 2)
    private BigDecimal allocationAmount;

    @Column(name = "release_installment_no")
    private Integer releaseInstallmentNo;

    @Column(name = "reference_memo_number", length = 255)
    private String referenceMemoNumber;

    @Column(name = "attachment_file_path", length = 1000)
    private String attachmentFilePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "email", "roles"})
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"header", "hibernateLazyInitializer", "handler"})
    private List<AppLine> lines = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(Integer fiscalYear) { this.fiscalYear = fiscalYear; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<AppLine> getLines() { return lines; }
    public void setLines(List<AppLine> lines) { this.lines = lines; }

    // New field getters and setters
    public String getAllocationType() { return allocationType; }
    public void setAllocationType(String allocationType) { this.allocationType = allocationType; }
    
    public LocalDate getBudgetReleaseDate() { return budgetReleaseDate; }
    public void setBudgetReleaseDate(LocalDate budgetReleaseDate) { this.budgetReleaseDate = budgetReleaseDate; }
    
    public BigDecimal getAllocationAmount() { return allocationAmount; }
    public void setAllocationAmount(BigDecimal allocationAmount) { this.allocationAmount = allocationAmount; }
    
    public Integer getReleaseInstallmentNo() { return releaseInstallmentNo; }
    public void setReleaseInstallmentNo(Integer releaseInstallmentNo) { this.releaseInstallmentNo = releaseInstallmentNo; }
    
    public String getReferenceMemoNumber() { return referenceMemoNumber; }
    public void setReferenceMemoNumber(String referenceMemoNumber) { this.referenceMemoNumber = referenceMemoNumber; }
    
    public String getAttachmentFilePath() { return attachmentFilePath; }
    public void setAttachmentFilePath(String attachmentFilePath) { this.attachmentFilePath = attachmentFilePath; }
}


