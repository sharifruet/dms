package com.bpdb.dms.controller;

import com.bpdb.dms.entity.Role;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request, 
                                      Authentication authentication, HttpServletRequest httpRequest) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            User newUser = userManagementService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getRole(),
                request.getDepartment()
            );
            
            // Log the action
            auditService.logUserManagement(currentUser, "CREATE_USER", newUser.getId(), 
                                         newUser.getUsername(), httpRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("user", createUserResponse(newUser));
            
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<User> users = userManagementService.getAllUsers(pageable);
        Page<UserResponse> userResponses = users.map(this::createUserResponse);
        
        return ResponseEntity.ok(userResponses);
    }
    
    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userManagementService.getUserById(#userId).get().username == authentication.name")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        Optional<User> user = userManagementService.getUserById(userId);
        if (user.isPresent()) {
            return ResponseEntity.ok(createUserResponse(user.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Update user information
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userManagementService.getUserById(#userId).get().username == authentication.name")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, 
                                       @Valid @RequestBody UpdateUserRequest request,
                                       Authentication authentication, HttpServletRequest httpRequest) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            User targetUser = userManagementService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            User updatedUser = userManagementService.updateUser(
                userId,
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getDepartment(),
                request.getRole()
            );
            
            // Log the action
            auditService.logUserManagement(currentUser, "UPDATE_USER", userId, 
                                         targetUser.getUsername(), httpRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User updated successfully");
            response.put("user", createUserResponse(updatedUser));
            
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
    @PreAuthorize("@userManagementService.getUserById(#userId).get().username == authentication.name")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable Long userId, 
                                         @Valid @RequestBody ResetPasswordRequest request,
                                         Authentication authentication, HttpServletRequest httpRequest) {
        try {
            User currentUser = (User) authentication.getPrincipal();
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long userId,
                                            Authentication authentication, HttpServletRequest httpRequest) {
        try {
            User currentUser = (User) authentication.getPrincipal();
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserManagementService.UserStatistics> getUserStatistics() {
        UserManagementService.UserStatistics stats = userManagementService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Create user response DTO
     */
    private UserResponse createUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(user.getRole());
        response.setDepartment(user.getDepartment());
        response.setIsActive(user.getIsActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
    
    // DTOs
    public static class CreateUserRequest {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private Role role;
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
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
    }
    
    public static class UpdateUserRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String department;
        private Role role;
        
        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
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
    
    public static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private Role role;
        private String department;
        private Boolean isActive;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
