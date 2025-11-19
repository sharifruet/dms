package com.bpdb.dms.service;

import com.bpdb.dms.entity.DocumentTypeField;
import com.bpdb.dms.repository.DocumentTypeFieldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentTypeFieldService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTypeFieldService.class);

    private final DocumentTypeFieldRepository repository;

    public DocumentTypeFieldService(DocumentTypeFieldRepository repository) {
        this.repository = repository;
    }

    /**
     * Get all active fields for a document type
     */
    public List<DocumentTypeField> getFieldsForDocumentType(String documentType) {
        return repository.findByDocumentTypeAndIsActiveTrueOrderByDisplayOrderAsc(documentType);
    }

    /**
     * Get all fields for a document type (including inactive)
     */
    public List<DocumentTypeField> getAllFieldsForDocumentType(String documentType) {
        return repository.findByDocumentTypeOrderByDisplayOrderAsc(documentType);
    }

    /**
     * Get field by document type and field key
     */
    public Optional<DocumentTypeField> getField(String documentType, String fieldKey) {
        return repository.findByDocumentTypeAndFieldKey(documentType, fieldKey);
    }

    /**
     * Create or update a field configuration
     */
    public DocumentTypeField saveField(DocumentTypeField field) {
        if (field.getId() == null) {
            // Check if field already exists
            if (repository.existsByDocumentTypeAndFieldKey(field.getDocumentType(), field.getFieldKey())) {
                throw new IllegalArgumentException(
                    String.format("Field '%s' already exists for document type '%s'", 
                        field.getFieldKey(), field.getDocumentType()));
            }
        }
        return repository.save(field);
    }

    /**
     * Delete a field configuration
     */
    public void deleteField(Long id) {
        repository.deleteById(id);
    }

    /**
     * Deactivate a field (soft delete)
     */
    public DocumentTypeField deactivateField(Long id) {
        DocumentTypeField field = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Field not found: " + id));
        field.setIsActive(false);
        return repository.save(field);
    }

    /**
     * Map OCR extracted data to document type fields
     */
    public Map<String, String> mapOcrDataToFields(String documentType, String ocrText) {
        List<DocumentTypeField> fields = repository
            .findByDocumentTypeAndIsActiveTrueOrderByDisplayOrderAsc(documentType);
        
        return fields.stream()
            .filter(DocumentTypeField::getIsOcrMappable)
            .filter(field -> field.getOcrPattern() != null && !field.getOcrPattern().isBlank())
            .collect(Collectors.toMap(
                DocumentTypeField::getFieldKey,
                field -> extractValueFromOcr(ocrText, field.getOcrPattern()),
                (existing, replacement) -> existing // Keep first match
            ));
    }

    /**
     * Extract value from OCR text using pattern
     */
    private String extractValueFromOcr(String ocrText, String pattern) {
        if (ocrText == null || ocrText.isBlank() || pattern == null || pattern.isBlank()) {
            return null;
        }
        try {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = regex.matcher(ocrText);
            if (matcher.find()) {
                // Try to get the last group (usually the value)
                int groupCount = matcher.groupCount();
                if (groupCount > 0) {
                    String value = matcher.group(groupCount);
                    return value != null ? value.trim() : null;
                }
                return matcher.group(0);
            }
        } catch (Exception e) {
            logger.warn("Failed to extract value using pattern '{}': {}", pattern, e.getMessage());
        }
        return null;
    }

    /**
     * Validate field values against field configurations
     */
    public void validateFieldValues(String documentType, Map<String, String> fieldValues) {
        List<DocumentTypeField> fields = getFieldsForDocumentType(documentType);
        
        for (DocumentTypeField field : fields) {
            if (field.getIsRequired() && 
                (fieldValues == null || !fieldValues.containsKey(field.getFieldKey()) || 
                 fieldValues.get(field.getFieldKey()) == null || 
                 fieldValues.get(field.getFieldKey()).isBlank())) {
                throw new IllegalArgumentException(
                    String.format("Required field '%s' (%s) is missing", 
                        field.getFieldLabel(), field.getFieldKey()));
            }
        }
    }
}

