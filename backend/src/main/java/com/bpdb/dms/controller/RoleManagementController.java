package com.bpdb.dms.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bpdb.dms.dto.RoleDto;
import com.bpdb.dms.entity.Role;
import com.bpdb.dms.service.RoleManagementService;

import jakarta.validation.Valid;

/**
 * Controller providing administrative endpoints for managing roles and permissions.
 */
@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleManagementController {
    
    @Autowired
    private RoleManagementService roleManagementService;
    
    /**
     * Retrieve all roles with their permissions.
     */
    @GetMapping
    @PreAuthorize("@userSecurity.canManageUsers(authentication)")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        List<RoleDto> roles = roleManagementService.getAllRoles().stream()
                .map(role -> RoleDto.fromEntity(role, true))
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }
    
    /**
     * Retrieve a specific role by id.
     */
    @GetMapping("/{roleId}")
    @PreAuthorize("@userSecurity.canManageUsers(authentication)")
    public ResponseEntity<RoleDto> getRole(@PathVariable Long roleId) {
        return roleManagementService.getRoleById(roleId)
                .map(role -> ResponseEntity.ok(RoleDto.fromEntity(role, true)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Update role metadata.
     */
    @PutMapping("/{roleId}")
    @PreAuthorize("@userSecurity.canManageUsers(authentication)")
    public ResponseEntity<RoleDto> updateRole(@PathVariable Long roleId,
                                              @Valid @RequestBody UpdateRoleRequest request) {
        Role updatedRole = roleManagementService.updateRole(
                roleId,
                request.getDisplayName(),
                request.getDescription(),
                request.getIsActive()
        );
        return ResponseEntity.ok(RoleDto.fromEntity(updatedRole, true));
    }
    
    /**
     * Replace permissions assigned to a role.
     */
    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("@userSecurity.canManageUsers(authentication)")
    public ResponseEntity<RoleDto> updateRolePermissions(@PathVariable Long roleId,
                                                         @Valid @RequestBody UpdateRolePermissionsRequest request) {
        Role updatedRole = roleManagementService.updateRolePermissions(roleId, request.getPermissionIds());
        return ResponseEntity.ok(RoleDto.fromEntity(updatedRole, true));
    }
    
    /**
     * Request payload for updating role metadata.
     */
    public static class UpdateRoleRequest {
        private String displayName;
        private String description;
        private Boolean isActive;
        
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
    }
    
    /**
     * Request payload for replacing role permissions.
     */
    public static class UpdateRolePermissionsRequest {
        private List<Long> permissionIds;
        
        public List<Long> getPermissionIds() {
            return permissionIds;
        }
        
        public void setPermissionIds(List<Long> permissionIds) {
            this.permissionIds = permissionIds;
        }
    }
}

