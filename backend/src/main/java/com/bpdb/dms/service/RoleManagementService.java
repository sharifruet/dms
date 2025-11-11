package com.bpdb.dms.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.entity.Permission;
import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.RolePermission;
import com.bpdb.dms.repository.PermissionRepository;
import com.bpdb.dms.repository.RolePermissionRepository;
import com.bpdb.dms.repository.RoleRepository;

/**
 * Service for managing roles and their associated permissions.
 */
@Service
@Transactional
public class RoleManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleManagementService.class);
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    
    /**
     * Retrieve all roles with their permissions.
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
    
    /**
     * Retrieve a role with its permissions.
     */
    public Optional<Role> getRoleById(Long roleId) {
        return roleRepository.findWithPermissionsById(roleId);
    }
    
    /**
     * Update role metadata.
     */
    public Role updateRole(Long roleId, String displayName, String description, Boolean isActive) {
        Role role = roleRepository.findWithPermissionsById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        
        if (displayName != null) {
            role.setDisplayName(displayName);
        }
        if (description != null) {
            role.setDescription(description);
        }
        if (isActive != null) {
            role.setIsActive(isActive);
        }
        role.setUpdatedAt(LocalDateTime.now());
        
        roleRepository.save(role);
        logger.info("Updated role metadata for {}", role.getName());
        return roleRepository.findWithPermissionsById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found after update"));
    }
    
    /**
     * Replace the set of permissions assigned to a role.
     */
    public Role updateRolePermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleRepository.findWithPermissionsById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        
        if (permissionIds == null) {
            throw new IllegalArgumentException("permissionIds must not be null");
        }
        
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new RuntimeException("One or more permissions were not found");
        }
        
        // Remove existing associations
        rolePermissionRepository.deleteByRole(role);
        role.getRolePermissions().clear();
        
        // Create new associations
        List<RolePermission> rolePermissions = permissions.stream()
                .map(permission -> new RolePermission(role, permission))
                .collect(Collectors.toList());
        
        role.setRolePermissions(rolePermissions);
        role.setUpdatedAt(LocalDateTime.now());
        
        Role savedRole = roleRepository.save(role);
        logger.info("Updated permissions for role {}", savedRole.getName());
        
        return roleRepository.findWithPermissionsById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found after update"));
    }
    
    /**
     * Retrieve all permissions.
     */
    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }
}

