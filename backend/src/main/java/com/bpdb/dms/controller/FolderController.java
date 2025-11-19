package com.bpdb.dms.controller;

import com.bpdb.dms.entity.Folder;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.FolderService;
import com.bpdb.dms.service.FolderService.FolderSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing folders
 */
@RestController
@RequestMapping("/api/folders")
@CrossOrigin(origins = "*")
public class FolderController {
    
    @Autowired
    private FolderService folderService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get all root folders
     */
    @GetMapping("/root")
    public ResponseEntity<Map<String, Object>> getRootFolders() {
        try {
            List<Folder> folders = folderService.getRootFolders();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("folders", folders);
            response.put("count", folders.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get folder tree (hierarchy)
     */
    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> getFolderTree() {
        try {
            List<Folder> tree = folderService.getFolderTree();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tree", tree);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get folder by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFolder(@PathVariable Long id) {
        try {
            Folder folder = folderService.getFolderById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("folder", folder);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get subfolders of a folder
     */
    @GetMapping("/{id}/subfolders")
    public ResponseEntity<Map<String, Object>> getSubFolders(@PathVariable Long id) {
        try {
            List<Folder> subFolders = folderService.getSubFolders(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subFolders", subFolders);
            response.put("count", subFolders.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get folder summary
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<Map<String, Object>> getFolderSummary(@PathVariable Long id) {
        try {
            FolderSummary summary = folderService.getFolderSummary(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("summary", summary);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Create a new folder
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createFolder(
            @RequestBody CreateFolderRequest request,
            Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            Folder folder = folderService.createFolder(
                request.getName(),
                request.getDescription(),
                request.getParentFolderId(),
                request.getDepartment(),
                user
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Folder created successfully");
            response.put("folder", folder);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Update folder
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateFolder(
            @PathVariable Long id,
            @RequestBody UpdateFolderRequest request,
            Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            Folder folder = folderService.updateFolder(
                id,
                request.getName(),
                request.getDescription(),
                request.getDepartment(),
                user
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Folder updated successfully");
            response.put("folder", folder);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Delete folder
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFolder(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            folderService.deleteFolder(id, user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Folder deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Move folder to new parent
     */
    @PostMapping("/{id}/move")
    public ResponseEntity<Map<String, Object>> moveFolder(
            @PathVariable Long id,
            @RequestBody MoveFolderRequest request,
            Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            Folder folder = folderService.moveFolder(id, request.getNewParentFolderId(), user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Folder moved successfully");
            response.put("folder", folder);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Helper method to get User from Authentication
     */
    private User getUserFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    /**
     * Request DTOs
     */
    public static class CreateFolderRequest {
        private String name;
        private String description;
        private Long parentFolderId;
        private String department;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Long getParentFolderId() { return parentFolderId; }
        public void setParentFolderId(Long parentFolderId) { this.parentFolderId = parentFolderId; }
        
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
    }
    
    public static class UpdateFolderRequest {
        private String name;
        private String description;
        private String department;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
    }
    
    public static class MoveFolderRequest {
        private Long newParentFolderId;
        
        // Getters and setters
        public Long getNewParentFolderId() { return newParentFolderId; }
        public void setNewParentFolderId(Long newParentFolderId) { this.newParentFolderId = newParentFolderId; }
    }
}

