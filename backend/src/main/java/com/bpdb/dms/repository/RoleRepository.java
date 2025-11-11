package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.Role.RoleType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Role entity
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * Find role by name
     */
    Optional<Role> findByName(RoleType name);
    
    /**
     * Check if role exists by name
     */
    boolean existsByName(RoleType name);
    
    /**
     * Retrieve all roles with their permissions eagerly loaded.
     */
    @EntityGraph(attributePaths = {"rolePermissions", "rolePermissions.permission"})
    List<Role> findAll();
    
    /**
     * Retrieve a specific role with its permissions eagerly loaded.
     */
    @EntityGraph(attributePaths = {"rolePermissions", "rolePermissions.permission"})
    Optional<Role> findWithPermissionsById(Long id);
}

