package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for tracking document and contract expiries
 */
@Entity
@Table(name = "expiry_tracking")
@EntityListeners(AuditingEntityListener.class)
public class ExpiryTracking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_type", nullable = false)
    private ExpiryType expiryType;
    
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(name = "alert_30_days")
    private Boolean alert30Days = false;
    
    @Column(name = "alert_15_days")
    private Boolean alert15Days = false;
    
    @Column(name = "alert_7_days")
    private Boolean alert7Days = false;
    
    @Column(name = "alert_expired")
    private Boolean alertExpired = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExpiryStatus status = ExpiryStatus.ACTIVE;
    
    @Column(name = "renewal_date")
    private LocalDateTime renewalDate;
    
    @Column(name = "renewal_document_id")
    private Long renewalDocumentId;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    @Column(name = "department")
    private String department;
    
    @Column(name = "vendor_name")
    private String vendorName;
    
    @Column(name = "contract_value")
    private Double contractValue;
    
    @Column(name = "currency")
    private String currency;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ExpiryTracking() {}
    
    public ExpiryTracking(Document document, ExpiryType expiryType, LocalDateTime expiryDate) {
        this.document = document;
        this.expiryType = expiryType;
        this.expiryDate = expiryDate;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    
    public ExpiryType getExpiryType() { return expiryType; }
    public void setExpiryType(ExpiryType expiryType) { this.expiryType = expiryType; }
    
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    
    public Boolean getAlert30Days() { return alert30Days; }
    public void setAlert30Days(Boolean alert30Days) { this.alert30Days = alert30Days; }
    
    public Boolean getAlert15Days() { return alert15Days; }
    public void setAlert15Days(Boolean alert15Days) { this.alert15Days = alert15Days; }
    
    public Boolean getAlert7Days() { return alert7Days; }
    public void setAlert7Days(Boolean alert7Days) { this.alert7Days = alert7Days; }
    
    public Boolean getAlertExpired() { return alertExpired; }
    public void setAlertExpired(Boolean alertExpired) { this.alertExpired = alertExpired; }
    
    public ExpiryStatus getStatus() { return status; }
    public void setStatus(ExpiryStatus status) { this.status = status; }
    
    public LocalDateTime getRenewalDate() { return renewalDate; }
    public void setRenewalDate(LocalDateTime renewalDate) { this.renewalDate = renewalDate; }
    
    public Long getRenewalDocumentId() { return renewalDocumentId; }
    public void setRenewalDocumentId(Long renewalDocumentId) { this.renewalDocumentId = renewalDocumentId; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    
    public Double getContractValue() { return contractValue; }
    public void setContractValue(Double contractValue) { this.contractValue = contractValue; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
