package com.bpdb.dms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.entity.Permission;
import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.RolePermission;

/**
 * Repository interface for RolePermission entity
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    
    /**
     * Find permissions by role
     */
    List<RolePermission> findByRole(Role role);
    
    /**
     * Find roles by permission
     */
    List<RolePermission> findByPermission(Permission permission);
    
    /**
     * Check if role has specific permission
     */
    boolean existsByRoleAndPermission(Role role, Permission permission);
    
    /**
     * Delete role permission mapping
     */
    void deleteByRoleAndPermission(Role role, Permission permission);
    
    /**
     * Delete all permissions associated with a role.
     */
    void deleteByRole(Role role);
}
