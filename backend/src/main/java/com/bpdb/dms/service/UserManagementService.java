package com.bpdb.dms.service;

import com.bpdb.dms.entity.Role;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for user management operations
 */
@Service
@Transactional
public class UserManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private RoleRepository roleRepository;
    
    /**
     * Create a new user
     */
    public User createUser(String username, String email, String password, String firstName, 
                          String lastName, Long roleId, String department) {
        try {
            // Check if username already exists
            if (userRepository.existsByUsername(username)) {
                throw new RuntimeException("Username already exists");
            }
            
            // Check if email already exists
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("Email already exists");
            }
            
            if (roleId == null) {
                throw new RuntimeException("Role is required");
            }
            
            Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
            
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setRole(role);
            user.setDepartment(department);
            user.setIsActive(true);
            
            User savedUser = userRepository.save(user);
            logger.info("User created successfully: {}", username);
            
            return savedUser;
            
        } catch (Exception e) {
            logger.error("Failed to create user: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Update user information
     */
    public User updateUser(Long userId, String firstName, String lastName, String email, 
                          String department, Long roleId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if email is being changed and if it already exists
            if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
                throw new RuntimeException("Email already exists");
            }
            
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setDepartment(department);
            
            if (roleId != null) {
                Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found"));
                user.setRole(role);
            }
            
            User updatedUser = userRepository.save(user);
            logger.info("User updated successfully: {}", user.getUsername());
            
            return updatedUser;
            
        } catch (Exception e) {
            logger.error("Failed to update user: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Change user password
     */
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }
            
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            logger.info("Password changed successfully for user: {}", user.getUsername());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to change password: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Reset user password (admin function)
     */
    public boolean resetPassword(Long userId, String newPassword) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            logger.info("Password reset successfully for user: {}", user.getUsername());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to reset password: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Activate/deactivate user
     */
    public boolean toggleUserStatus(Long userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setIsActive(!user.getIsActive());
            userRepository.save(user);
            
            logger.info("User status toggled for: {} - Active: {}", user.getUsername(), user.getIsActive());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to toggle user status: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Delete user (soft delete)
     */
    public boolean deleteUser(Long userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setIsActive(false);
            userRepository.save(user);
            
            logger.info("User deactivated: {}", user.getUsername());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to delete user: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }
    
    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Get all users with pagination
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    /**
     * Get users by role
     */
    public Page<User> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }
    
    /**
     * Get users by department
     */
    public Page<User> getUsersByDepartment(String department, Pageable pageable) {
        return userRepository.findByDepartment(department, pageable);
    }
    
    /**
     * Get active users
     */
    public Page<User> getActiveUsers(Pageable pageable) {
        return userRepository.findByIsActiveTrue(pageable);
    }
    
    /**
     * Search users by username or email
     */
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            searchTerm, searchTerm, pageable);
    }
    
    /**
     * Get user statistics
     */
    public UserStatistics getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long inactiveUsers = totalUsers - activeUsers;
        
        return new UserStatistics(totalUsers, activeUsers, inactiveUsers);
    }
    
    /**
     * User statistics DTO
     */
    public static class UserStatistics {
        private final long totalUsers;
        private final long activeUsers;
        private final long inactiveUsers;
        
        public UserStatistics(long totalUsers, long activeUsers, long inactiveUsers) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.inactiveUsers = inactiveUsers;
        }
        
        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getInactiveUsers() { return inactiveUsers; }
    }
}
