package com.bpdb.dms.util;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.service.DatabaseMetadataExtractionService;
import com.bpdb.dms.service.DocumentMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Utility class to test procurement description extraction
 * This component is automatically loaded when running the application
 * Add --test-procurement-extraction argument to execute the test
 */
@Component
public class TestProcurementExtraction implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(TestProcurementExtraction.class);
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private DatabaseMetadataExtractionService databaseMetadataExtractionService;
    
    @Autowired
    private DocumentMetadataService documentMetadataService;
    
    @Override
    public void run(String... args) {
        // Only run if --test-procurement-extraction flag is set
        boolean shouldRun = false;
        for (String arg : args) {
            if (arg.equals("--test-procurement-extraction")) {
                shouldRun = true;
                break;
            }
        }
        
        if (!shouldRun) {
            logger.info("Skipping procurement extraction test. Use --test-procurement-extraction to run.");
            return;
        }
        
        logger.info("=== Testing Procurement Description Extraction ===");
        
        // Get all documents
        List<Document> documents = documentRepository.findAll();
        logger.info("Found {} documents in database", documents.size());
        
        if (documents.isEmpty()) {
            logger.warn("No documents found in database. Skipping test.");
            System.exit(0);
        }
        
        // Test with the first document
        Document document = documents.get(0);
        logger.info("Testing with document ID: {}, Type: {}, File: {}", 
            document.getId(), document.getDocumentType(), document.getFileName());
        
        // Check if document has extracted text
        String extractedText = document.getExtractedText();
        if (extractedText == null || extractedText.trim().isEmpty()) {
            logger.warn("Document {} has no extracted text. Skipping extraction test.", document.getId());
            System.exit(0);
        }
        
        logger.info("Extracted text length: {} characters", extractedText != null ? extractedText.length() : 0);
        logger.info("First 500 characters of extracted text:\n{}", 
            extractedText != null && extractedText.length() > 500 ? extractedText.substring(0, 500) + "..." : extractedText);
        
        // Check if document type is TENDER_NOTICE
        if (!"TENDER_NOTICE".equals(document.getDocumentType())) {
            logger.warn("Document type is '{}', not 'TENDER_NOTICE'. Extraction may not work.", document.getDocumentType());
        }
        
        // Get current metadata before extraction
        Map<String, String> metadataBefore = documentMetadataService.getMetadataMap(document);
        logger.info("Metadata before extraction: {}", metadataBefore.keySet());
        String procurementDescBefore = metadataBefore.get("procurementDescription");
        logger.info("procurementDescription before: {}", 
            procurementDescBefore != null ? 
                (procurementDescBefore.length() > 100 ? procurementDescBefore.substring(0, 100) + "..." : procurementDescBefore) 
                : "null");
        
        // Run extraction
        logger.info("Running metadata extraction...");
        try {
            databaseMetadataExtractionService.extractMetadataForDocument(document.getId());
            logger.info("Extraction completed successfully");
        } catch (Exception e) {
            logger.error("Extraction failed: {}", e.getMessage(), e);
            System.exit(1);
        }
        
        // Reload document to get updated metadata
        document = documentRepository.findById(document.getId()).orElse(null);
        if (document == null) {
            logger.error("Document not found after extraction");
            System.exit(1);
        }
        
        // Get metadata after extraction
        Map<String, String> metadataAfter = documentMetadataService.getMetadataMap(document);
        logger.info("Metadata after extraction: {}", metadataAfter.keySet());
        String procurementDescAfter = metadataAfter.get("procurementDescription");
        
        if (procurementDescAfter != null && !procurementDescAfter.trim().isEmpty()) {
            logger.info("✓ SUCCESS: procurementDescription extracted!");
            logger.info("Length: {} characters", procurementDescAfter.length());
            logger.info("First 300 characters:\n{}", 
                procurementDescAfter.length() > 300 ? procurementDescAfter.substring(0, 300) + "..." : procurementDescAfter);
            
            // Verify it doesn't contain the labels we want to remove
            boolean hasTenderProposal = procurementDescAfter.toLowerCase().contains("tender/proposal");
            boolean hasPackageNo = procurementDescAfter.toLowerCase().contains("package no. and");
            boolean startsWithDesc = procurementDescAfter.toLowerCase().startsWith("description :");
            
            if (hasTenderProposal || hasPackageNo || startsWithDesc) {
                logger.warn("⚠️  Warning: Extracted text still contains labels that should be removed:");
                if (hasTenderProposal) logger.warn("  - Contains 'Tender/Proposal'");
                if (hasPackageNo) logger.warn("  - Contains 'Package No. and'");
                if (startsWithDesc) logger.warn("  - Starts with 'Description :'");
            } else {
                logger.info("✓ Verification passed: Labels removed correctly");
            }
            
            logger.info("=== Test PASSED ===");
            System.exit(0);
        } else {
            logger.error("✗ FAILED: procurementDescription was not extracted");
            logger.error("This could mean:");
            logger.error("1. The pattern didn't match the OCR text format");
            logger.error("2. The field is not configured in document_type_fields");
            logger.error("3. The document type is not TENDER_NOTICE");
            
            // Show a sample of the text around "Package" to help debug
            if (extractedText != null) {
                int packageIndex = extractedText.toLowerCase().indexOf("package");
                if (packageIndex >= 0) {
                    int start = Math.max(0, packageIndex - 100);
                    int end = Math.min(extractedText.length(), packageIndex + 500);
                    logger.error("Text around 'Package':\n{}", extractedText.substring(start, end));
                }
            }
            
            logger.info("=== Test FAILED ===");
            System.exit(1);
        }
    }
}

