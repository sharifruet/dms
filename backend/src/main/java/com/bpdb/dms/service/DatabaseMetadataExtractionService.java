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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
            
            logger.info("Extracting metadata from database using regex patterns from document_type_fields for document: {} (type: {})", 
                documentId, documentType);
            
            // For TENDER_NOTICE documents, log that we're specifically looking for procurementDescription
            if ("TENDER_NOTICE".equals(documentType)) {
                logger.info("Processing TENDER_NOTICE document - will extract procurementDescription and other fields");
            }
            
            Map<String, String> extractedFields = extractFieldsUsingDatabasePatterns(document, extractedText);
            
            if (!extractedFields.isEmpty()) {
                // Apply extracted metadata to document
                logger.info("Applying {} extracted fields to document {}: {}", 
                    extractedFields.size(), documentId, extractedFields.keySet());
                documentMetadataService.applyAutoMetadata(document, extractedFields);
                
                // Reload document to verify metadata was saved
                document = documentRepository.findById(documentId).orElse(null);
                if (document != null) {
                    Map<String, String> savedMetadata = documentMetadataService.getMetadataMap(document);
                    logger.info("Metadata after save - keys: {}", savedMetadata.keySet());
                    
                    // For TENDER_NOTICE, specifically verify procurementDescription was saved
                    if ("TENDER_NOTICE".equals(documentType)) {
                        String procurementDesc = extractedFields.get("procurementDescription");
                        String savedProcurementDesc = savedMetadata.get("procurementDescription");
                        
                        if (procurementDesc != null && !procurementDesc.trim().isEmpty()) {
                            if (savedProcurementDesc != null && !savedProcurementDesc.trim().isEmpty()) {
                                logger.info("✓✓ VERIFIED: procurementDescription extracted AND saved for TENDER_NOTICE document {} (length: {} chars)", 
                                    documentId, savedProcurementDesc.length());
                            } else {
                                logger.error("✗✗ ERROR: procurementDescription was extracted but NOT saved to database for document {}!", documentId);
                                logger.error("  Extracted value (first 200 chars): {}", 
                                    procurementDesc.length() > 200 ? procurementDesc.substring(0, 200) + "..." : procurementDesc);
                            }
                        } else {
                            logger.warn("⚠️  procurementDescription was NOT extracted for TENDER_NOTICE document {}. Check OCR text format.", documentId);
                        }
                    }
                }
                
                logger.info("Successfully extracted {} fields for document {}: {}", 
                    extractedFields.size(), documentId, extractedFields.keySet());
            } else {
                logger.warn("No fields extracted for document: {} (type: {})", documentId, documentType);
                if ("TENDER_NOTICE".equals(documentType)) {
                    logger.warn("⚠️  No fields extracted for TENDER_NOTICE document. This may indicate:");
                    logger.warn("   1. Field configuration missing in document_type_fields table");
                    logger.warn("   2. OCR text format doesn't match expected patterns");
                    logger.warn("   3. Document type field not properly configured");
                }
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
            
            logger.info("Found {} active fields for document type: {}", documentFields.size(), documentType);
            
            // Check if procurementDescription field exists
            boolean hasProcurementDescriptionField = documentFields.stream()
                .anyMatch(f -> "procurementDescription".equals(f.getFieldKey()) && f.getIsOcrMappable());
            logger.info("procurementDescription field found: {}", hasProcurementDescriptionField);
            
            for (DocumentTypeField field : documentFields) {
                // Only process fields that are OCR mappable and have a pattern
                if (!field.getIsOcrMappable() || field.getOcrPattern() == null || field.getOcrPattern().isBlank()) {
                    logger.debug("Skipping field {} - not OCR mappable or no pattern", field.getFieldKey());
                    continue;
                }
                
                String fieldKey = field.getFieldKey();
                String regexPattern = field.getOcrPattern();
                
                logger.debug("Processing field: {} with pattern: {}", fieldKey, regexPattern);
                
                try {
                    String value;
                    // For procurementDescription, use Java-based extraction instead of PostgreSQL regex
                    if ("procurementDescription".equals(fieldKey)) {
                        logger.info("Attempting to extract procurementDescription for document {} (TENDER_NOTICE)", document.getId());
                        value = extractProcurementDescription(extractedText);
                        if (value != null && !value.trim().isEmpty()) {
                            fields.put(fieldKey, value);
                            logger.info("✓ SUCCESS: Extracted procurement description for document {} (length: {} chars)", 
                                document.getId(), value.length());
                            logger.debug("Procurement description value (first 200 chars): {}", 
                                value.length() > 200 ? value.substring(0, 200) + "..." : value);
                        } else {
                            logger.warn("✗ FAILED: No procurement description extracted for document {}. Check logs above for pattern matching details.", 
                                document.getId());
                        }
                    } else {
                        // Extract field value using PostgreSQL regex
                        value = extractFieldUsingPostgreSQLRegex(extractedText, regexPattern, fieldKey);
                        if (value != null && !value.trim().isEmpty()) {
                            fields.put(fieldKey, value);
                            logger.debug("Extracted {} = {} for document {}", fieldKey, value, document.getId());
                        }
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
     * Extract procurement description from OCR text using Java-based extraction
     * This method finds the section starting with "Tender/Proposal" and containing "Package No. and Description :"
     * and extracts the description, removing the prefix labels.
     * 
     * The pattern looks for:
     * - "Tender/Proposal" followed by text (like "GD-69 FY 25-26")
     * - Then "Package No. and Description :"
     * - Then the items list
     * 
     * It removes "Tender/Proposal", "Package No. and", and "Description :" from the result.
     * 
     * @param extractedText The OCR extracted text
     * @return The cleaned procurement description, or null if not found
     */
    private String extractProcurementDescription(String extractedText) {
        if (extractedText == null || extractedText.trim().isEmpty()) {
            logger.warn("Extracted text is null or empty - cannot extract procurement description");
            return null;
        }
        
        try {
            logger.info("Attempting to extract procurement description from text (length: {} chars)", extractedText.length());
            
            // First, let's check if we can find "Package" and "Description" keywords
            String lowerText = extractedText.toLowerCase();
            boolean hasPackage = lowerText.contains("package");
            boolean hasDescription = lowerText.contains("description");
            boolean hasTender = lowerText.contains("tender") || lowerText.contains("proposal");
            
            logger.info("Keyword check - Package: {}, Description: {}, Tender/Proposal: {}", 
                hasPackage, hasDescription, hasTender);
            
            if (!hasPackage && !hasDescription) {
                logger.warn("Neither 'Package' nor 'Description' found in OCR text. Extraction unlikely to succeed.");
            }
            
            // First, try to find the section with "Tender/Proposal" and "Package No. and Description"
            // Pattern 1: "Tender/Proposal" followed by text, then "Package No. and Description :"
            // This pattern is more flexible - allows for variations in spacing and line breaks
            Pattern pattern1 = Pattern.compile(
                "(?i)Tender/Proposal\\s+(.*?)\\s+Package\\s+No\\.\\s+and\\s+Description\\s*:\\s*(.*?)(?=\\n\\s*[A-Z][a-z]+\\s+[^:]*:|\\n\\s*[A-Z][A-Z\\s]+:|$)",
                Pattern.DOTALL | Pattern.MULTILINE
            );
            
            Matcher matcher1 = pattern1.matcher(extractedText);
            
            if (matcher1.find()) {
                logger.debug("Pattern 1 matched - found Tender/Proposal section");
                // Group 1: text between "Tender/Proposal" and "Package No. and Description :" (e.g., "GD-69 FY 25-26")
                // Group 2: text after "Description :" (the items list)
                String beforeDescription = matcher1.group(1).trim();
                String afterDescription = matcher1.group(2).trim();
                
                logger.debug("Before description: '{}' (length: {})", beforeDescription, beforeDescription.length());
                logger.debug("After description: '{}' (length: {})", 
                    afterDescription.length() > 100 ? afterDescription.substring(0, 100) + "..." : afterDescription, 
                    afterDescription.length());
                
                // Combine the parts
                String result = beforeDescription + "\n\n " + afterDescription;
                
                // Clean up: remove any remaining occurrences of the labels
                result = result.replaceFirst("(?i)^Tender/Proposal\\s+", "");
                result = result.replaceFirst("(?i)^Package\\s+No\\.\\s+and\\s+", "");
                result = result.replaceFirst("(?i)^Description\\s*:\\s*", "");
                
                // Normalize whitespace: preserve line breaks but normalize multiple spaces
                result = result.replaceAll("[ \\t]+", " ");
                // Normalize multiple newlines to double newline
                result = result.replaceAll("\\n{3,}", "\n\n");
                
                result = result.trim();
                
                if (!result.isEmpty()) {
                    logger.info("Successfully extracted procurement description (length: {})", result.length());
                    return result;
                }
            }
            
            // Pattern 2: More flexible - "Tender/Proposal" might be on a different line
            Pattern pattern2 = Pattern.compile(
                "(?i)Tender/Proposal\\s*\\n\\s*(.*?)\\s*\\n\\s*Package\\s+No\\.\\s+and\\s+Description\\s*:\\s*(.*?)(?=\\n\\s*[A-Z][a-z]+\\s+[^:]*:|\\n\\s*[A-Z][A-Z\\s]+:|$)",
                Pattern.DOTALL | Pattern.MULTILINE
            );
            
            Matcher matcher2 = pattern2.matcher(extractedText);
            
            if (matcher2.find()) {
                logger.debug("Pattern 2 matched - found Tender/Proposal on separate line");
                String beforeDesc = matcher2.group(1).trim();
                String afterDesc = matcher2.group(2).trim();
                
                String result = beforeDesc + "\n\n " + afterDesc;
                result = result.replaceAll("[ \\t]+", " ");
                result = result.replaceAll("\\n{3,}", "\n\n");
                result = result.trim();
                
                if (!result.isEmpty()) {
                    logger.info("Successfully extracted procurement description using pattern 2 (length: {})", result.length());
                    return result;
                }
            }
            
            // Pattern 3: Just look for "Package No. and Description :" (in case "Tender/Proposal" is missing or OCR error)
            Pattern pattern3 = Pattern.compile(
                "(?i)Package\\s+No\\.\\s+and\\s+Description\\s*:\\s*(.*?)(?=\\n\\s*[A-Z][a-z]+\\s+[^:]*:|\\n\\s*[A-Z][A-Z\\s]+:|Category\\s*:|Evaluation\\s+Type|Document\\s+Available|$)",
                Pattern.DOTALL | Pattern.MULTILINE
            );
            
            Matcher matcher3 = pattern3.matcher(extractedText);
            
            if (matcher3.find()) {
                logger.debug("Pattern 3 matched - found Package No. and Description without Tender/Proposal");
                String description = matcher3.group(1);
                
                if (description != null) {
                    description = description.trim();
                    description = description.replaceAll("[ \\t]+", " ");
                    description = description.replaceAll("\\n{3,}", "\n\n");
                    description = description.trim();
                    
                    if (!description.isEmpty()) {
                        logger.info("Successfully extracted procurement description using pattern 3 (length: {})", description.length());
                        return description;
                    }
                }
            }
            
            // Pattern 4: Even more flexible - look for just "Description :" after "Package No. and"
            Pattern pattern4 = Pattern.compile(
                "(?i)Package\\s+No\\.\\s+and\\s+Description\\s*:\\s*(.*?)(?=\\n|$)",
                Pattern.DOTALL | Pattern.MULTILINE
            );
            
            Matcher matcher4 = pattern4.matcher(extractedText);
            
            if (matcher4.find()) {
                logger.debug("Pattern 4 matched - simple Package No. and Description pattern");
                String description = matcher4.group(1);
                
                if (description != null) {
                    description = description.trim();
                    // Try to find where description ends - look for next field label
                    int endIndex = description.length();
                    String[] fieldMarkers = {"Category", "Evaluation Type", "Document Available", "Document Fees", 
                                            "Tender/Proposal Document Price", "Mode of Payment", "Lot", "Procuring Entity"};
                    for (String marker : fieldMarkers) {
                        int idx = description.indexOf("\n" + marker);
                        if (idx > 0 && idx < endIndex) {
                            endIndex = idx;
                        }
                        // Also check without newline
                        idx = description.indexOf(marker);
                        if (idx > 100 && idx < endIndex) { // Only if it's not at the start
                            endIndex = idx;
                        }
                    }
                    if (endIndex < description.length()) {
                        description = description.substring(0, endIndex).trim();
                    }
                    
                    description = description.replaceAll("[ \\t]+", " ");
                    description = description.replaceAll("\\n{3,}", "\n\n");
                    description = description.trim();
                    
                    if (!description.isEmpty()) {
                        logger.info("Successfully extracted procurement description using pattern 4 (length: {})", description.length());
                        return description;
                    }
                }
            }
            
            // Pattern 5: Very flexible - just look for "Package" and "Description" anywhere
            // This is a last resort pattern that tries to extract everything between "Package" and common end markers
            Pattern pattern5 = Pattern.compile(
                "(?i)(?:Tender/Proposal\\s+)?(?:GD-[0-9A-Za-z\\-\\s]+?[0-9]{2}-[0-9]{2})?\\s*Package\\s+No\\.?\\s+and\\s+Description\\s*:?\\s*(.*?)(?=\\n\\s*(?:Category|Evaluation|Document|Mode|Lot|Procuring|Information|Scheduled|Pre|Tender|Last|Security|Valid)|$)",
                Pattern.DOTALL | Pattern.MULTILINE
            );
            
            Matcher matcher5 = pattern5.matcher(extractedText);
            
            if (matcher5.find()) {
                logger.debug("Pattern 5 matched - very flexible pattern");
                String description = matcher5.group(1);
                
                if (description != null) {
                    description = description.trim();
                    
                    // Remove any remaining labels
                    description = description.replaceFirst("(?i)^Tender/Proposal\\s+", "");
                    description = description.replaceFirst("(?i)^Package\\s+No\\.?\\s+and\\s+", "");
                    description = description.replaceFirst("(?i)^Description\\s*:?\\s*", "");
                    
                    description = description.replaceAll("[ \\t]+", " ");
                    description = description.replaceAll("\\n{3,}", "\n\n");
                    description = description.trim();
                    
                    if (!description.isEmpty() && description.length() > 10) { // At least 10 chars to be valid
                        logger.info("Successfully extracted procurement description using pattern 5 (length: {})", description.length());
                        return description;
                    }
                }
            }
            
            // Pattern 6: Ultra-simple - just find "Description" and extract everything after it
            // This is the most aggressive pattern - should catch almost anything
            Pattern pattern6 = Pattern.compile(
                "(?i).*?Description\\s*:?\\s*(.*?)(?=\\n\\s*(?:Category|Evaluation|Document|Mode|Lot|Procuring|Information|Scheduled|Pre|Tender|Last|Security|Valid|Brief|Eligibility|Particular|Key|Funding|Source|Budget|Method|Event|Invitation|App|Procurement|Scheduled|Pre|Tender|Proposal|Closing|Opening|Last|Date|Time)|$)",
                Pattern.DOTALL | Pattern.MULTILINE
            );
            
            Matcher matcher6 = pattern6.matcher(extractedText);
            if (matcher6.find()) {
                logger.debug("Pattern 6 (ultra-simple) matched");
                String description = matcher6.group(1);
                
                if (description != null) {
                    description = description.trim();
                    
                    // Remove labels
                    description = description.replaceFirst("(?i)^Tender/Proposal\\s+", "");
                    description = description.replaceFirst("(?i)^Package\\s+No\\.?\\s+and\\s+", "");
                    description = description.replaceFirst("(?i)^Description\\s*:?\\s*", "");
                    
                    // Clean up
                    description = description.replaceAll("[ \\t]+", " ");
                    description = description.replaceAll("\\n{3,}", "\n\n");
                    description = description.trim();
                    
                    // Check if it looks like a valid description (has some content, not just whitespace)
                    if (!description.isEmpty() && description.length() > 20) { // At least 20 chars
                        // Make sure it's not just the word "Description" or similar
                        if (!description.toLowerCase().matches("^(description|package|tender|proposal).*")) {
                            logger.info("Successfully extracted procurement description using pattern 6 (ultra-simple) (length: {})", description.length());
                            return description;
                        }
                    }
                }
            }
            
            // Log detailed debugging information
            logger.error("❌ ALL PATTERNS FAILED: No procurement description pattern matched. Debugging information:");
            
            // Check if "Package" exists in text
            int packageIndex = extractedText.toLowerCase().indexOf("package");
            if (packageIndex >= 0) {
                int start = Math.max(0, packageIndex - 200);
                int end = Math.min(extractedText.length(), packageIndex + 600);
                logger.error("Found 'Package' at index {}. Text around it ({} chars):\n{}", 
                    packageIndex, end - start, extractedText.substring(start, end));
            } else {
                logger.error("'Package' not found in extracted text. Searching for variations...");
                // Try to find variations
                String[] variations = {"Package", "package", "PACKAGE", "Package No", "Package No.", "Description", "description", "DESCRIPTION"};
                for (String variant : variations) {
                    int idx = extractedText.indexOf(variant);
                    if (idx >= 0) {
                        logger.error("Found '{}' at index {}", variant, idx);
                        // Show context around this variant
                        int start = Math.max(0, idx - 100);
                        int end = Math.min(extractedText.length(), idx + 200);
                        logger.error("  Context: ...{}...", extractedText.substring(start, end));
                    }
                }
            }
            
            // Check if "Tender/Proposal" exists
            int tenderIndex = extractedText.toLowerCase().indexOf("tender/proposal");
            if (tenderIndex < 0) {
                tenderIndex = extractedText.toLowerCase().indexOf("tender");
            }
            if (tenderIndex >= 0) {
                int start = Math.max(0, tenderIndex - 100);
                int end = Math.min(extractedText.length(), tenderIndex + 300);
                logger.error("Found 'Tender' at index {}. Text around it:\n{}", 
                    tenderIndex, extractedText.substring(start, end));
            }
            
            logger.error("Full extracted text length: {} characters", extractedText.length());
            logger.error("First 1500 characters of extracted text:\n{}", 
                extractedText.length() > 1500 ? extractedText.substring(0, 1500) + "..." : extractedText);
            
        } catch (Exception e) {
            logger.error("Error extracting procurement description: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Extract a single field value using PostgreSQL regexp_matches function
     * This uses the regex patterns stored in document_type_fields table
     */
    private String extractFieldUsingPostgreSQLRegex(String extractedText, String regexPattern, String fieldKey) {
        try {
            // Use PostgreSQL regexp_matches function
            // regexp_matches returns a text array, we extract the first capture group [1]
            String sql = "SELECT (regexp_matches(:extractedText, :regexPattern, 'i'))[1]";
            
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

