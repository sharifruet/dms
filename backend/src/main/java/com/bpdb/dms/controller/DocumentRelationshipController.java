package com.bpdb.dms.controller;

import com.bpdb.dms.entity.DocumentRelationship;
import com.bpdb.dms.entity.DocumentRelationshipType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.DocumentRelationshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing document relationships
 */
@RestController
@RequestMapping("/api/documents/{documentId}/relationships")
@CrossOrigin(origins = "*")
public class DocumentRelationshipController {
    
    @Autowired
    private DocumentRelationshipService relationshipService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get all relationships for a document
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRelationships(@PathVariable Long documentId) {
        try {
            List<DocumentRelationship> relationships = relationshipService.getDocumentRelationships(documentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("relationships", relationships);
            response.put("count", relationships.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Create a relationship between two documents
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRelationship(
            @PathVariable Long documentId,
            @RequestBody CreateRelationshipRequest request,
            Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            DocumentRelationship relationship = relationshipService.createRelationship(
                documentId,
                request.getTargetDocumentId(),
                request.getRelationshipType(),
                request.getDescription(),
                user
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Relationship created successfully");
            response.put("relationship", relationship);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Delete a relationship
     */
    @DeleteMapping("/{relationshipId}")
    public ResponseEntity<Map<String, Object>> deleteRelationship(
            @PathVariable Long documentId,
            @PathVariable Long relationshipId,
            Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            relationshipService.deleteRelationship(relationshipId, user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Relationship deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get related document IDs
     */
    @GetMapping("/related")
    public ResponseEntity<Map<String, Object>> getRelatedDocuments(@PathVariable Long documentId) {
        try {
            List<Long> relatedIds = relationshipService.getRelatedDocumentIds(documentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("relatedDocumentIds", relatedIds);
            response.put("count", relatedIds.size());
            
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
     * Request DTO for creating relationships
     */
    public static class CreateRelationshipRequest {
        private Long targetDocumentId;
        private DocumentRelationshipType relationshipType;
        private String description;
        
        public Long getTargetDocumentId() {
            return targetDocumentId;
        }
        
        public void setTargetDocumentId(Long targetDocumentId) {
            this.targetDocumentId = targetDocumentId;
        }
        
        public DocumentRelationshipType getRelationshipType() {
            return relationshipType;
        }
        
        public void setRelationshipType(DocumentRelationshipType relationshipType) {
            this.relationshipType = relationshipType;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
}

