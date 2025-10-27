package com.bpdb.dms.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * RolePermission entity for mapping roles to permissions
 */
@Entity
@Table(name = "role_permissions")
public class RolePermission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    @ManyToOne
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;
    
    // Constructors
    public RolePermission() {}
    
    public RolePermission(Role role, Permission permission) {
        this.role = role;
        this.permission = permission;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public Permission getPermission() {
        return permission;
    }
    
    public void setPermission(Permission permission) {
        this.permission = permission;
    }
}
