package com.bpdb.dms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.entity.Permission;

/**
 * Repository interface for Permission entity
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    /**
     * Find permission by name
     */
    Permission findByName(String name);
    
    /**
     * Find permissions by resource
     */
    List<Permission> findByResource(String resource);
    
    /**
     * Find permissions by action
     */
    List<Permission> findByAction(String action);
}
