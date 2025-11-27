package com.bpdb.dms.service;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentTypeField;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.DocumentTypeFieldRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for extracting metadata from documents using PostgreSQL regex patterns
 * This runs after extracted_text is saved to populate document_metadata fields
 * Uses regex patterns stored in document_type_fields table
 */
@Service
@Transactional
public class DatabaseMetadataExtractionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMetadataExtractionService.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private DocumentMetadataService documentMetadataService;
    
    @Autowired(required = false)
    private DocumentTypeFieldRepository documentTypeFieldRepository;
    
    /**
     * Extract metadata from a document using PostgreSQL regex patterns
     * This should be called after extracted_text is saved
     */
    public void extractMetadataForDocument(Long documentId) {
        try {
            Optional<Document> documentOpt = documentRepository.findById(documentId);
            if (documentOpt.isEmpty()) {
                logger.warn("Document not found for metadata extraction: {}", documentId);
                return;
            }
            
            Document document = documentOpt.get();
            String documentType = document.getDocumentType();
            
            // Skip if document type is not set
            if (documentType == null || documentType.isBlank()) {
                logger.debug("Document type is null, skipping DB metadata extraction for document: {}", documentId);
                return;
            }
            
            String extractedText = document.getExtractedText();
            if (extractedText == null || extractedText.trim().isEmpty()) {
                logger.debug("No extracted text found for document: {}", documentId);
                return;
            }
            
            logger.info("Extracting metadata from database using regex patterns from document_type_fields for document: {}", documentId);
            
            Map<String, String> extractedFields = extractFieldsUsingDatabasePatterns(document, extractedText);
            
            if (!extractedFields.isEmpty()) {
                // Apply extracted metadata to document
                documentMetadataService.applyAutoMetadata(document, extractedFields);
                logger.info("Successfully extracted {} fields for document: {}", extractedFields.size(), documentId);
            } else {
                logger.debug("No fields extracted for document: {}", documentId);
            }
            
        } catch (Exception e) {
            logger.error("Error extracting metadata from database for document {}: {}", documentId, e.getMessage(), e);
        }
    }
    
    /**
     * Extract fields using regex patterns from document_type_fields table
     * This reads patterns from the database and uses PostgreSQL regex to extract values
     */
    private Map<String, String> extractFieldsUsingDatabasePatterns(Document document, String extractedText) {
        Map<String, String> fields = new HashMap<>();
        
        if (documentTypeFieldRepository == null) {
            logger.warn("DocumentTypeFieldRepository not available, skipping database pattern extraction");
            return fields;
        }
        
        String documentType = document.getDocumentType();
        if (documentType == null || documentType.isBlank()) {
            logger.debug("Document type is null, skipping database pattern extraction");
            return fields;
        }
        
        try {
            // Get all OCR-mappable fields for this document type from database
            List<DocumentTypeField> documentFields = documentTypeFieldRepository
                .findByDocumentTypeAndIsActiveTrueOrderByDisplayOrderAsc(documentType);
            
            logger.debug("Found {} active fields for document type: {}", documentFields.size(), documentType);
            
            for (DocumentTypeField field : documentFields) {
                // Only process fields that are OCR mappable and have a pattern
                if (!field.getIsOcrMappable() || field.getOcrPattern() == null || field.getOcrPattern().isBlank()) {
                    continue;
                }
                
                String fieldKey = field.getFieldKey();
                String regexPattern = field.getOcrPattern();
                
                try {
                    // Extract field value using PostgreSQL regex
                    String value = extractFieldUsingPostgreSQLRegex(extractedText, regexPattern, fieldKey);
                    if (value != null && !value.trim().isEmpty()) {
                        fields.put(fieldKey, value);
                        logger.debug("Extracted {} = {} for document {}", fieldKey, value, document.getId());
                    }
                } catch (Exception e) {
                    logger.debug("Failed to extract field {} using pattern '{}': {}", fieldKey, regexPattern, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error extracting fields using database patterns for document {}: {}", 
                document.getId(), e.getMessage(), e);
        }
        
        return fields;
    }
    
    /**
     * Extract a single field value using PostgreSQL regexp_matches function
     * This uses the regex patterns stored in document_type_fields table
     */
    private String extractFieldUsingPostgreSQLRegex(String extractedText, String regexPattern, String fieldKey) {
        try {
            String sql;
            
            // For procurementDescription field, apply regexp_replace to normalize whitespace
            if ("procurementDescription".equals(fieldKey)) {
                // Apply regexp_replace to normalize multiple spaces to single space
                sql = "SELECT regexp_replace((regexp_matches(:extractedText, :regexPattern, 'is'))[1], '\\s+', ' ', 'g')";
            } else {
                // Use PostgreSQL regexp_matches function
                // regexp_matches returns a text array, we extract the first capture group [1]
                sql = "SELECT (regexp_matches(:extractedText, :regexPattern, 'i'))[1]";
            }
            
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("extractedText", extractedText);
            query.setParameter("regexPattern", regexPattern);
            
            @SuppressWarnings("unchecked")
            java.util.List<Object> results = query.getResultList();
            
            if (!results.isEmpty() && results.get(0) != null) {
                String value = results.get(0).toString().trim();
                
                // Clean up value based on field type
                if (value.isEmpty()) {
                    return null;
                }
                
                // Remove commas from numeric fields
                if (fieldKey.contains("Price") || fieldKey.contains("Amount")) {
                    value = value.replace(",", "");
                }
                
                return value;
            }
            
        } catch (Exception e) {
            // regexp_matches returns no rows if no match found - this is expected
            logger.debug("No match found for field {} using pattern '{}': {}", fieldKey, regexPattern, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Batch extract metadata for all TENDER_NOTICE documents that have extracted_text but missing metadata
     * This can be run as a scheduled job or manually
     */
    public int batchExtractMetadataForTenderNotices() {
        try {
            String sql = """
                SELECT d.id 
                FROM documents d
                WHERE d.document_type = 'TENDER_NOTICE'
                AND d.extracted_text IS NOT NULL
                AND d.extracted_text != ''
                AND NOT EXISTS (
                    SELECT 1 FROM document_metadata dm
                    WHERE dm.document_id = d.id
                    AND dm.key = 'tenderId'
                )
                ORDER BY d.id
                """;
            
            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            java.util.List<Object[]> results = query.getResultList();
            
            int count = 0;
            for (Object[] row : results) {
                Long documentId = ((Number) row[0]).longValue();
                try {
                    extractMetadataForDocument(documentId);
                    count++;
                } catch (Exception e) {
                    logger.error("Error extracting metadata for document {} in batch: {}", 
                        documentId, e.getMessage());
                }
            }
            
            logger.info("Batch extracted metadata for {} Tender Notice documents", count);
            return count;
            
        } catch (Exception e) {
            logger.error("Error in batch metadata extraction: {}", e.getMessage(), e);
            return 0;
        }
    }
}

