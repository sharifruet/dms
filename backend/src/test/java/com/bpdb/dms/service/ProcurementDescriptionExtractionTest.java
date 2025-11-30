package com.bpdb.dms.service;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.DocumentTypeFieldRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify procurement description extraction
 */
@SpringBootTest
@ActiveProfiles("test")
public class ProcurementDescriptionExtractionTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcurementDescriptionExtractionTest.class);
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private DatabaseMetadataExtractionService databaseMetadataExtractionService;
    
    @Autowired
    private DocumentMetadataService documentMetadataService;
    
    @Autowired(required = false)
    private DocumentTypeFieldRepository documentTypeFieldRepository;
    
    @Test
    public void testProcurementDescriptionExtraction() {
        logger.info("=== Testing Procurement Description Extraction ===");
        
        // Get all documents
        List<Document> documents = documentRepository.findAll();
        logger.info("Found {} documents in database", documents.size());
        
        if (documents.isEmpty()) {
            logger.warn("No documents found in database. Skipping test.");
            return;
        }
        
        // Test with the first document
        Document document = documents.get(0);
        logger.info("Testing with document ID: {}, Type: {}, File: {}", 
            document.getId(), document.getDocumentType(), document.getFileName());
        
        // Check if document has extracted text
        String extractedText = document.getExtractedText();
        if (extractedText == null || extractedText.trim().isEmpty()) {
            logger.warn("Document {} has no extracted text. Skipping extraction test.", document.getId());
            return;
        }
        
        logger.info("Extracted text length: {} characters", extractedText.length());
        logger.info("First 500 characters of extracted text:\n{}", 
            extractedText.length() > 500 ? extractedText.substring(0, 500) + "..." : extractedText);
        
        // Check if document type is TENDER_NOTICE
        if (!"TENDER_NOTICE".equals(document.getDocumentType())) {
            logger.warn("Document type is '{}', not 'TENDER_NOTICE'. Extraction may not work.", document.getDocumentType());
        }
        
        // Check if procurementDescription field exists in document_type_fields
        if (documentTypeFieldRepository != null) {
            var fields = documentTypeFieldRepository
                .findByDocumentTypeAndIsActiveTrueOrderByDisplayOrderAsc(document.getDocumentType());
            boolean hasProcurementField = fields.stream()
                .anyMatch(f -> "procurementDescription".equals(f.getFieldKey()));
            logger.info("procurementDescription field found in document_type_fields: {}", hasProcurementField);
        }
        
        // Get current metadata before extraction
        Map<String, String> metadataBefore = documentMetadataService.getMetadataMap(document);
        logger.info("Metadata before extraction: {}", metadataBefore);
        String procurementDescBefore = metadataBefore.get("procurementDescription");
        logger.info("procurementDescription before: {}", 
            procurementDescBefore != null ? procurementDescBefore.substring(0, Math.min(100, procurementDescBefore.length())) + "..." : "null");
        
        // Run extraction
        logger.info("Running metadata extraction...");
        try {
            databaseMetadataExtractionService.extractMetadataForDocument(document.getId());
            logger.info("Extraction completed successfully");
        } catch (Exception e) {
            logger.error("Extraction failed: {}", e.getMessage(), e);
            fail("Extraction failed: " + e.getMessage());
        }
        
        // Reload document to get updated metadata
        document = documentRepository.findById(document.getId()).orElse(null);
        assertNotNull(document, "Document should still exist after extraction");
        
        // Get metadata after extraction
        Map<String, String> metadataAfter = documentMetadataService.getMetadataMap(document);
        logger.info("Metadata after extraction: {}", metadataAfter.keySet());
        String procurementDescAfter = metadataAfter.get("procurementDescription");
        
        if (procurementDescAfter != null && !procurementDescAfter.trim().isEmpty()) {
            logger.info("✓ SUCCESS: procurementDescription extracted!");
            logger.info("Length: {} characters", procurementDescAfter.length());
            logger.info("First 200 characters:\n{}", 
                procurementDescAfter.length() > 200 ? procurementDescAfter.substring(0, 200) + "..." : procurementDescAfter);
            
            // Verify it doesn't contain the labels we want to remove
            assertFalse(procurementDescAfter.toLowerCase().contains("tender/proposal"), 
                "Should not contain 'Tender/Proposal'");
            assertFalse(procurementDescAfter.toLowerCase().contains("package no. and"), 
                "Should not contain 'Package No. and'");
            assertFalse(procurementDescAfter.toLowerCase().startsWith("description :"), 
                "Should not start with 'Description :'");
            
            logger.info("✓ Verification passed: Labels removed correctly");
        } else {
            logger.error("✗ FAILED: procurementDescription was not extracted");
            logger.error("This could mean:");
            logger.error("1. The pattern didn't match the OCR text format");
            logger.error("2. The field is not configured in document_type_fields");
            logger.error("3. The document type is not TENDER_NOTICE");
            
            // Show a sample of the text around "Package" to help debug
            int packageIndex = extractedText.toLowerCase().indexOf("package");
            if (packageIndex >= 0) {
                int start = Math.max(0, packageIndex - 100);
                int end = Math.min(extractedText.length(), packageIndex + 500);
                logger.error("Text around 'Package':\n{}", extractedText.substring(start, end));
            }
            
            fail("procurementDescription was not extracted. Check logs for details.");
        }
        
        logger.info("=== Test Complete ===");
    }
}

