package com.bpdb.dms.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for managing dashboard configurations
 */
@Entity
@Table(name = "dashboards")
@EntityListeners(AuditingEntityListener.class)
public class Dashboard {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DashboardType type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(name = "layout_config", length = 5000)
    private String layoutConfig; // JSON string for dashboard layout
    
    @Column(name = "widgets_config", length = 5000)
    private String widgetsConfig; // JSON string for widgets configuration
    
    @Column(name = "refresh_interval")
    private Integer refreshInterval; // in seconds
    
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    @Column(name = "is_default")
    private Boolean isDefault = false;
    
    @Column(name = "access_count")
    private Long accessCount = 0L;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Dashboard() {}
    
    public Dashboard(String name, String description, DashboardType type, User createdBy) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public DashboardType getType() { return type; }
    public void setType(DashboardType type) { this.type = type; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public String getLayoutConfig() { return layoutConfig; }
    public void setLayoutConfig(String layoutConfig) { this.layoutConfig = layoutConfig; }
    
    public String getWidgetsConfig() { return widgetsConfig; }
    public void setWidgetsConfig(String widgetsConfig) { this.widgetsConfig = widgetsConfig; }
    
    public Integer getRefreshInterval() { return refreshInterval; }
    public void setRefreshInterval(Integer refreshInterval) { this.refreshInterval = refreshInterval; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    
    public Long getAccessCount() { return accessCount; }
    public void setAccessCount(Long accessCount) { this.accessCount = accessCount; }
    
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
