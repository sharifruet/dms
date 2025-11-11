package com.bpdb.dms.controller;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.DocumentTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controller for document template management
 */
@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "*")
public class DocumentTemplateController {
    
    @Autowired
    private DocumentTemplateService documentTemplateService;

    @Autowired
    private UserRepository userRepository;
    
    /**
     * Create a new document template
     */
    @PostMapping
    public ResponseEntity<DocumentTemplate> createTemplate(@RequestParam("name") String name,
                                                          @RequestParam("description") String description,
                                                          @RequestParam("templateType") TemplateType templateType,
                                                          @RequestParam("file") MultipartFile file,
                                                          @RequestParam(value = "templateContent", required = false) String templateContent,
                                                          Authentication authentication) {
        try {
            User user = resolveCurrentUser(authentication);
            
            DocumentTemplate template = documentTemplateService.createTemplate(
                name,
                description,
                templateType,
                file,
                templateContent,
                user
            );
            
            return ResponseEntity.ok(template);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update document template
     */
    @PutMapping("/{templateId}")
    public ResponseEntity<DocumentTemplate> updateTemplate(@PathVariable Long templateId,
                                                          @RequestParam(value = "name", required = false) String name,
                                                          @RequestParam(value = "description", required = false) String description,
                                                          @RequestParam(value = "templateType", required = false) TemplateType templateType,
                                                          @RequestParam(value = "file", required = false) MultipartFile file,
                                                          @RequestParam(value = "templateContent", required = false) String templateContent,
                                                          Authentication authentication) {
        try {
            User user = resolveCurrentUser(authentication);
            
            DocumentTemplate template = documentTemplateService.updateTemplate(
                templateId,
                name,
                description,
                templateType,
                file,
                templateContent,
                user
            );
            
            return ResponseEntity.ok(template);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete document template
     */
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId) {
        try {
            documentTemplateService.deleteTemplate(templateId);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generate document from template
     */
    @PostMapping("/{templateId}/generate")
    public ResponseEntity<String> generateDocumentFromTemplate(@PathVariable Long templateId,
                                                             @RequestBody Map<String, Object> variables,
                                                             Authentication authentication) {
        try {
            User user = resolveCurrentUser(authentication);
            
            String generatedContent = documentTemplateService.generateDocumentFromTemplate(
                templateId,
                variables,
                user
            );
            
            return ResponseEntity.ok(generatedContent);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get templates by type
     */
    @GetMapping("/type/{templateType}")
    public ResponseEntity<Page<DocumentTemplate>> getTemplatesByType(@PathVariable TemplateType templateType,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DocumentTemplate> templates = documentTemplateService.getTemplatesByType(templateType, pageable);
            
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get public templates
     */
    @GetMapping("/public")
    public ResponseEntity<Page<DocumentTemplate>> getPublicTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DocumentTemplate> templates = documentTemplateService.getPublicTemplates(pageable);
            
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get templates for user
     */
    @GetMapping("/user")
    public ResponseEntity<Page<DocumentTemplate>> getTemplatesForUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            User user = resolveCurrentUser(authentication);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<DocumentTemplate> templates = documentTemplateService.getTemplatesForUser(user, pageable);
            
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get most used templates
     */
    @GetMapping("/most-used")
    public ResponseEntity<List<DocumentTemplate>> getMostUsedTemplates(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<DocumentTemplate> templates = documentTemplateService.getMostUsedTemplates(limit);
            
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get recently used templates
     */
    @GetMapping("/recently-used")
    public ResponseEntity<List<DocumentTemplate>> getRecentlyUsedTemplates(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<DocumentTemplate> templates = documentTemplateService.getRecentlyUsedTemplates(limit);
            
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Search templates
     */
    @GetMapping("/search")
    public ResponseEntity<Page<DocumentTemplate>> searchTemplates(@RequestParam String query,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DocumentTemplate> templates = documentTemplateService.searchTemplates(query, pageable);
            
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get template statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getTemplateStatistics() {
        try {
            Map<String, Object> statistics = documentTemplateService.getTemplateStatistics();
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Validate template variables
     */
    @PostMapping("/{templateId}/validate")
    public ResponseEntity<Boolean> validateTemplateVariables(@PathVariable Long templateId,
                                                           @RequestBody Map<String, Object> variables) {
        try {
            boolean isValid = documentTemplateService.validateTemplateVariables(templateId, variables);
            
            return ResponseEntity.ok(isValid);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get template variables
     */
    @GetMapping("/{templateId}/variables")
    public ResponseEntity<Map<String, Object>> getTemplateVariables(@PathVariable Long templateId) {
        try {
            Map<String, Object> variables = documentTemplateService.getTemplateVariables(templateId);
            
            return ResponseEntity.ok(variables);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Missing authentication context");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + authentication.getName()));
    }
}
