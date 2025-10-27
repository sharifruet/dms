package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.Role.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

