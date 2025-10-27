package com.bpdb.dms.service;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.repository.DocumentTemplateRepository;
import com.bpdb.dms.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing document templates
 */
@Service
@Transactional
public class DocumentTemplateService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentTemplateService.class);
    
    @Autowired
    private DocumentTemplateRepository documentTemplateRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuditService auditService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Create a new document template
     */
    public DocumentTemplate createTemplate(String name, String description, TemplateType templateType,
                                         MultipartFile file, String templateContent, User createdBy) {
        try {
            // Save template file
            String filePath = saveTemplateFile(file);
            
            DocumentTemplate template = new DocumentTemplate(name, description, templateType, filePath, createdBy);
            template.setFileSize(file.getSize());
            template.setMimeType(file.getContentType());
            template.setTemplateContent(templateContent);
            
            DocumentTemplate savedTemplate = documentTemplateRepository.save(template);
            
            auditService.logActivity(createdBy.getUsername(), "TEMPLATE_CREATED", 
                "Template created: " + name, null);
            
            logger.info("Document template created: {} by user: {}", name, createdBy.getUsername());
            
            return savedTemplate;
            
        } catch (Exception e) {
            logger.error("Failed to create document template: {}", e.getMessage());
            throw new RuntimeException("Failed to create document template", e);
        }
    }
    
    /**
     * Update document template
     */
    public DocumentTemplate updateTemplate(Long templateId, String name, String description,
                                         TemplateType templateType, MultipartFile file,
                                         String templateContent, User updatedBy) {
        try {
            DocumentTemplate template = documentTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
            
            template.setName(name);
            template.setDescription(description);
            template.setTemplateType(templateType);
            template.setLastModifiedBy(updatedBy);
            
            if (file != null && !file.isEmpty()) {
                String filePath = saveTemplateFile(file);
                template.setFilePath(filePath);
                template.setFileSize(file.getSize());
                template.setMimeType(file.getContentType());
            }
            
            if (templateContent != null) {
                template.setTemplateContent(templateContent);
            }
            
            DocumentTemplate savedTemplate = documentTemplateRepository.save(template);
            
            auditService.logActivity(updatedBy.getUsername(), "TEMPLATE_UPDATED", 
                "Template updated: " + name, null);
            
            logger.info("Document template updated: {} by user: {}", name, updatedBy.getUsername());
            
            return savedTemplate;
            
        } catch (Exception e) {
            logger.error("Failed to update document template: {}", e.getMessage());
            throw new RuntimeException("Failed to update document template", e);
        }
    }
    
    /**
     * Delete document template
     */
    public void deleteTemplate(Long templateId) {
        try {
            DocumentTemplate template = documentTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
            
            // Delete template file
            deleteTemplateFile(template.getFilePath());
            
            documentTemplateRepository.delete(template);
            
            auditService.logActivity("SYSTEM", "TEMPLATE_DELETED", 
                "Template deleted: " + template.getName(), null);
            
            logger.info("Document template deleted: {}", template.getName());
            
        } catch (Exception e) {
            logger.error("Failed to delete document template: {}", e.getMessage());
            throw new RuntimeException("Failed to delete document template", e);
        }
    }
    
    /**
     * Generate document from template
     */
    public String generateDocumentFromTemplate(Long templateId, Map<String, Object> variables, User generatedBy) {
        try {
            DocumentTemplate template = documentTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
            
            // Update usage count
            template.setUsageCount(template.getUsageCount() + 1);
            template.setLastUsedAt(LocalDateTime.now());
            documentTemplateRepository.save(template);
            
            // Generate document content
            String generatedContent = generateContent(template, variables);
            
            auditService.logActivity(generatedBy.getUsername(), "DOCUMENT_GENERATED_FROM_TEMPLATE", 
                "Document generated from template: " + template.getName(), null);
            
            logger.info("Document generated from template: {} by user: {}", 
                template.getName(), generatedBy.getUsername());
            
            return generatedContent;
            
        } catch (Exception e) {
            logger.error("Failed to generate document from template: {}", e.getMessage());
            throw new RuntimeException("Failed to generate document from template", e);
        }
    }
    
    /**
     * Get templates by type
     */
    public Page<DocumentTemplate> getTemplatesByType(TemplateType templateType, Pageable pageable) {
        return documentTemplateRepository.findByTemplateType(templateType, pageable);
    }
    
    /**
     * Get public templates
     */
    public Page<DocumentTemplate> getPublicTemplates(Pageable pageable) {
        return documentTemplateRepository.findByIsPublicTrue(pageable);
    }
    
    /**
     * Get templates for user
     */
    public Page<DocumentTemplate> getTemplatesForUser(User user, Pageable pageable) {
        return documentTemplateRepository.findByCreatedBy(user, pageable);
    }
    
    /**
     * Get most used templates
     */
    public List<DocumentTemplate> getMostUsedTemplates(int limit) {
        return documentTemplateRepository.findMostUsedTemplates(Pageable.ofSize(limit));
    }
    
    /**
     * Get recently used templates
     */
    public List<DocumentTemplate> getRecentlyUsedTemplates(int limit) {
        return documentTemplateRepository.findRecentlyUsedTemplates(Pageable.ofSize(limit));
    }
    
    /**
     * Search templates
     */
    public Page<DocumentTemplate> searchTemplates(String query, Pageable pageable) {
        return documentTemplateRepository.findByNameContainingIgnoreCase(query, pageable);
    }
    
    /**
     * Get template statistics
     */
    public Map<String, Object> getTemplateStatistics() {
        try {
            return Map.of(
                "totalTemplates", documentTemplateRepository.count(),
                "activeTemplates", documentTemplateRepository.countByStatus(TemplateStatus.ACTIVE),
                "publicTemplates", documentTemplateRepository.countByIsPublicTrue(),
                "draftTemplates", documentTemplateRepository.countByStatus(TemplateStatus.DRAFT)
            );
            
        } catch (Exception e) {
            logger.error("Failed to get template statistics: {}", e.getMessage());
            throw new RuntimeException("Failed to get template statistics", e);
        }
    }
    
    /**
     * Validate template variables
     */
    public boolean validateTemplateVariables(Long templateId, Map<String, Object> variables) {
        try {
            DocumentTemplate template = documentTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
            
            if (template.getValidationRules() == null || template.getValidationRules().isEmpty()) {
                return true; // No validation rules defined
            }
            
            Map<String, Object> rules = objectMapper.readValue(template.getValidationRules(), Map.class);
            
            // Simple validation - check if required variables are present
            if (rules.containsKey("required")) {
                List<String> required = (List<String>) rules.get("required");
                for (String field : required) {
                    if (!variables.containsKey(field)) {
                        return false;
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to validate template variables: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get template variables
     */
    public Map<String, Object> getTemplateVariables(Long templateId) {
        try {
            DocumentTemplate template = documentTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
            
            if (template.getVariables() == null || template.getVariables().isEmpty()) {
                return Map.of();
            }
            
            return objectMapper.readValue(template.getVariables(), Map.class);
            
        } catch (Exception e) {
            logger.error("Failed to get template variables: {}", e.getMessage());
            return Map.of();
        }
    }
    
    /**
     * Save template file
     */
    private String saveTemplateFile(MultipartFile file) {
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/templates";
            Path uploadPath = Paths.get(uploadDir);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            
            Files.copy(file.getInputStream(), filePath);
            
            return filePath.toString();
            
        } catch (IOException e) {
            logger.error("Failed to save template file: {}", e.getMessage());
            throw new RuntimeException("Failed to save template file", e);
        }
    }
    
    /**
     * Delete template file
     */
    private void deleteTemplateFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            logger.error("Failed to delete template file: {}", e.getMessage());
        }
    }
    
    /**
     * Generate content from template
     */
    private String generateContent(DocumentTemplate template, Map<String, Object> variables) {
        try {
            String content = template.getTemplateContent();
            
            if (content == null || content.isEmpty()) {
                return "";
            }
            
            // Simple variable replacement
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                content = content.replace(placeholder, value);
            }
            
            return content;
            
        } catch (Exception e) {
            logger.error("Failed to generate content from template: {}", e.getMessage());
            return "";
        }
    }
}
