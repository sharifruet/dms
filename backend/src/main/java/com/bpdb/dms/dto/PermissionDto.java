package com.bpdb.dms.dto;

import com.bpdb.dms.entity.Permission;

/**
 * Data transfer object representing a permission.
 */
public class PermissionDto {
    
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String resource;
    private String action;
    
    public static PermissionDto fromEntity(Permission permission) {
        if (permission == null) {
            return null;
        }
        
        PermissionDto dto = new PermissionDto();
        dto.setId(permission.getId());
        dto.setName(permission.getName());
        dto.setDisplayName(permission.getDisplayName());
        dto.setDescription(permission.getDescription());
        dto.setResource(permission.getResource());
        dto.setAction(permission.getAction());
        return dto;
    }
    
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

