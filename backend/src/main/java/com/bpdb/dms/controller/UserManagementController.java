package com.bpdb.dms.controller;

import com.bpdb.dms.dto.UserDto;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.service.UserManagementService;
import com.bpdb.dms.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for user management operations
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserManagementController {
    
    @Autowired
    private UserManagementService userManagementService;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Create a new user (Admin only)
     */
    @PostMapping
    @PreAuthorize("@userSecurity.canManageUsers(authentication)")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request, 
                                      Authentication authentication, HttpServletRequest httpRequest) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            
            User newUser = userManagementService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getRoleId(),
                request.getDepartment()
            );
            
            // Log the action
            auditService.logUserManagement(currentUser, "CREATE_USER", newUser.getId(), 
                                         newUser.getUsername(), httpRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("user", UserDto.fromEntity(newUser));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get all users with pagination
     */
    @GetMapping
    @PreAuthorize("@userSecurity.canManageUsers(authentication)")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<User> users = userManagementService.getAllUsers(pageable);
        Page<UserDto> userResponses = users.map(UserDto::fromEntity);
        
        return ResponseEntity.ok(userResponses);
    }
    
    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    @PreAuthorize("@userSecurity.canAccessUser(authentication, #userId)")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        Optional<User> user = userManagementService.getUserById(userId);
        if (user.isPresent()) {
            return ResponseEntity.ok(UserDto.fromEntity(user.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Update user information
     */
    @PutMapping("/{userId}")
    @PreAuthorize("@userSecurity.canAccessUser(authentication, #userId)")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, 
                                       @Valid @RequestBody UpdateUserRequest request,
                                       Authentication authentication, HttpServletRequest httpRequest) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            User targetUser = userManagementService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            User updatedUser = userManagementService.updateUser(
                userId,
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getDepartment(),
                request.getRoleId()
            );
            
            // Log the action
            auditService.logUserManagement(currentUser, "UPDATE_USER", userId, 
                                         targetUser.getUsername(), httpRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User updated successfully");
            response.put("user", UserDto.fromEntity(updatedUser));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Change user password
     */
    @PutMapping("/{userId}/password")
    @PreAuthorize("@userSecurity.canAccessUser(authentication, #userId)")
    public ResponseEntity<?> changePassword(@PathVariable Long userId, 
                                          @Valid @RequestBody ChangePasswordRequest request) {
        try {
            boolean success = userManagementService.changePassword(
                userId, request.getCurrentPassword(), request.getNewPassword());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Password changed successfully" : "Failed to change password");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Reset user password (Admin only)
     */
    @PutMapping("/{userId}/reset-password")
    @PreAuthorize("@userSecurity.canManageUsers(authentication)")
    public ResponseEntity<?> resetPassword(@PathVariable Long userId, 
                                         @Valid @RequestBody ResetPasswordRequest request,
                                         Authentication authentication, HttpServletRequest httpRequest) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            User targetUser = userManagementService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            boolean success = userManagementService.resetPassword(userId, request.getNewPassword());
            
            // Log the action
            auditService.logUserManagement(currentUser, "RESET_PASSWORD", userId, 
                                         targetUser.getUsername(), httpRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Password reset successfully" : "Failed to reset password");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Toggle user status (Admin only)
     */
    @PutMapping("/{userId}/toggle-status")
    @PreAuthorize("@userSecurity.canManageUsers(authentication)")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long userId,
                                            Authentication authentication, HttpServletRequest httpRequest) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            User targetUser = userManagementService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            boolean success = userManagementService.toggleUserStatus(userId);
            
            // Log the action
            auditService.logUserManagement(currentUser, "TOGGLE_USER_STATUS", userId, 
                                         targetUser.getUsername(), httpRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "User status updated successfully" : "Failed to update user status");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get user statistics (Admin only)
     */
    @GetMapping("/statistics")
    @PreAuthorize("@userSecurity.canManageUsers(authentication)")
    public ResponseEntity<UserManagementService.UserStatistics> getUserStatistics() {
        UserManagementService.UserStatistics stats = userManagementService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Create user response DTO
     */
    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Missing authentication context");
        }
        return userManagementService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + authentication.getName()));
    }
    
    // DTOs
    public static class CreateUserRequest {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private Long roleId;
        private String department;
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
    }
    
    public static class UpdateUserRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String department;
        private Long roleId;
        
        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
    }
    
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        
        // Getters and setters
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
    
    public static class ResetPasswordRequest {
        private String newPassword;
        
        // Getters and setters
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
    
}
