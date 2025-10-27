package com.bpdb.dms.repository;

import com.bpdb.dms.entity.User;
import com.bpdb.dms.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Find users by role
     */
    Page<User> findByRole(Role role, Pageable pageable);
    
    /**
     * Find users by department
     */
    Page<User> findByDepartment(String department, Pageable pageable);
    
    /**
     * Find active users
     */
    Page<User> findByIsActiveTrue(Pageable pageable);
    
    /**
     * Search users by username or email
     */
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String username, String email, Pageable pageable);
    
    /**
     * Count active users
     */
    long countByIsActiveTrue();
}
