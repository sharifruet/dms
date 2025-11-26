package com.bpdb.dms.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Entity representing folders for organizing documents
 */
@Entity
@Table(name = "folders")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "parentFolder", "workflow"})
public class Folder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    @JsonIgnoreProperties({"parentFolder", "subFolders", "documents"})
    private Folder parentFolder;
    
    @OneToMany(mappedBy = "parentFolder", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"parentFolder", "documents", "workflow", "hibernateLazyInitializer", "handler"})
    private List<Folder> subFolders;
    
    @Column(name = "folder_path", length = 1000)
    private String folderPath; // Computed full path like "/Root/Department/Project"
    
    @Column(name = "department")
    private String department;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({"role", "hibernateLazyInitializer", "handler"})
    private User createdBy;
    
    @OneToOne(mappedBy = "folder", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"folder", "hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Workflow workflow;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_system_folder")
    private Boolean isSystemFolder = false; // System folders cannot be deleted
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Folder() {}
    
    public Folder(String name, User createdBy) {
        this.name = name;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Folder getParentFolder() {
        return parentFolder;
    }
    
    public void setParentFolder(Folder parentFolder) {
        this.parentFolder = parentFolder;
    }
    
    public List<Folder> getSubFolders() {
        return subFolders;
    }
    
    public void setSubFolders(List<Folder> subFolders) {
        this.subFolders = subFolders;
    }
    
    public String getFolderPath() {
        return folderPath;
    }
    
    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public User getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsSystemFolder() {
        return isSystemFolder;
    }
    
    public void setIsSystemFolder(Boolean isSystemFolder) {
        this.isSystemFolder = isSystemFolder;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Workflow getWorkflow() {
        return workflow;
    }
    
    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }
}

