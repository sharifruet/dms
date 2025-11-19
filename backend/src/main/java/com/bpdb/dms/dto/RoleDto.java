package com.bpdb.dms.dto;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.RolePermission;

/**
 * Data transfer object representing a role and its permissions.
 */
public class RoleDto {
    
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private Boolean isActive;
    private List<PermissionDto> permissions;
    
    public static RoleDto fromEntity(Role role) {
        return fromEntity(role, false);
    }
    
    public static RoleDto fromEntity(Role role, boolean includePermissions) {
        if (role == null) {
            return null;
        }
        
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName() != null ? role.getName().name() : null);
        dto.setDisplayName(role.getDisplayName());
        dto.setDescription(role.getDescription());
        dto.setIsActive(role.getIsActive());
        
        if (includePermissions && role.getRolePermissions() != null) {
            dto.setPermissions(
                role.getRolePermissions().stream()
                    .map(RolePermission::getPermission)
                    .filter(Objects::nonNull)
                    .map(PermissionDto::fromEntity)
                    .collect(Collectors.toList())
            );
        } else {
            dto.setPermissions(Collections.emptyList());
        }
        
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
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public List<PermissionDto> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(List<PermissionDto> permissions) {
        this.permissions = permissions;
    }
}

