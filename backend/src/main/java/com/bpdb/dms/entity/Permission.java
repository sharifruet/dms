package com.bpdb.dms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Permission entity for RBAC system
 */
@Entity
@Table(name = "permissions")
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    @Column(name = "display_name")
    private String displayName;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "resource")
    private String resource; // e.g., "document", "user", "system"
    
    @Column(name = "action")
    private String action; // e.g., "create", "read", "update", "delete"
    
    // Constructors
    public Permission() {}
    
    public Permission(String name, String displayName, String description, String resource, String action) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.resource = resource;
        this.action = action;
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
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getResource() {
        return resource;
    }
    
    public void setResource(String resource) {
        this.resource = resource;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
}
