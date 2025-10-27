package com.bpdb.dms.controller;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.service.DocumentVersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for document versioning
 */
@RestController
@RequestMapping("/api/documents/{documentId}/versions")
@CrossOrigin(origins = "*")
public class DocumentVersioningController {
    
    @Autowired
    private DocumentVersioningService documentVersioningService;
    
    /**
     * Create a new version of a document
     */
    @PostMapping
    public ResponseEntity<DocumentVersion> createDocumentVersion(@PathVariable Long documentId,
                                                                @RequestParam("file") MultipartFile file,
                                                                @RequestParam("changeDescription") String changeDescription,
                                                                @RequestParam("versionType") VersionType versionType,
                                                                Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            DocumentVersion version = documentVersioningService.createDocumentVersion(
                documentId,
                file,
                changeDescription,
                versionType,
                user
            );
            
            return ResponseEntity.ok(version);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all versions of a document
     */
    @GetMapping
    public ResponseEntity<List<DocumentVersion>> getDocumentVersions(@PathVariable Long documentId) {
        try {
            List<DocumentVersion> versions = documentVersioningService.getDocumentVersions(documentId);
            
            return ResponseEntity.ok(versions);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get a specific version of a document
     */
    @GetMapping("/{versionNumber}")
    public ResponseEntity<DocumentVersion> getDocumentVersion(@PathVariable Long documentId,
                                                            @PathVariable String versionNumber) {
        try {
            Optional<DocumentVersion> version = documentVersioningService.getDocumentVersion(documentId, versionNumber);
            
            return version.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Restore a document to a specific version
     */
    @PostMapping("/{versionNumber}/restore")
    public ResponseEntity<Document> restoreDocumentToVersion(@PathVariable Long documentId,
                                                           @PathVariable String versionNumber,
                                                           Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            Document document = documentVersioningService.restoreDocumentToVersion(
                documentId,
                versionNumber,
                user
            );
            
            return ResponseEntity.ok(document);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Archive old versions
     */
    @PostMapping("/archive")
    public ResponseEntity<Void> archiveOldVersions(@PathVariable Long documentId,
                                                 @RequestParam(defaultValue = "5") int keepVersions) {
        try {
            documentVersioningService.archiveOldVersions(documentId, keepVersions);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get version count for a document
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getVersionCount(@PathVariable Long documentId) {
        try {
            long count = documentVersioningService.getVersionCount(documentId);
            
            return ResponseEntity.ok(count);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Compare two versions
     */
    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareVersions(@PathVariable Long documentId,
                                                            @RequestParam String version1,
                                                            @RequestParam String version2) {
        try {
            Map<String, Object> comparison = documentVersioningService.compareVersions(
                documentId,
                version1,
                version2
            );
            
            return ResponseEntity.ok(comparison);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
