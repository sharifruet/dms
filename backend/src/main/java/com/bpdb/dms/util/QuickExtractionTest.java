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
 * Quick test component to test procurement description extraction
 * This will run automatically when the application starts if --test-extraction flag is passed
 */
@Component
public class QuickExtractionTest implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(QuickExtractionTest.class);
    
    @Autowired(required = false)
    private DocumentRepository documentRepository;
    
    @Autowired(required = false)
    private DatabaseMetadataExtractionService databaseMetadataExtractionService;
    
    @Autowired(required = false)
    private DocumentMetadataService documentMetadataService;
    
    @Override
    public void run(String... args) {
        // Only run if flag is set
        boolean shouldRun = false;
        for (String arg : args) {
            if (arg.equals("--test-extraction")) {
                shouldRun = true;
                break;
            }
        }
        
        if (!shouldRun || documentRepository == null || databaseMetadataExtractionService == null) {
            return;
        }
        
        logger.info("\n" + "=".repeat(80));
        logger.info("TESTING PROCUREMENT DESCRIPTION EXTRACTION");
        logger.info("=".repeat(80));
        
        try {
            // Get all documents
            List<Document> documents = documentRepository.findAll();
            logger.info("Found {} document(s) in database", documents.size());
            
            if (documents.isEmpty()) {
                logger.warn("No documents found. Cannot test extraction.");
                return;
            }
            
            // Test with the first document
            Document document = documents.get(0);
            Long docId = document.getId();
            
            logger.info("\nDocument Information:");
            logger.info("  ID: {}", docId);
            logger.info("  Type: {}", document.getDocumentType());
            logger.info("  File: {}", document.getFileName());
            
            // Check extracted text
            String extractedText = document.getExtractedText();
            if (extractedText == null || extractedText.trim().isEmpty()) {
                logger.warn("\n⚠️  Document has no extracted text. Cannot test extraction.");
                return;
            }
            
            logger.info("  Extracted text length: {} characters", extractedText.length());
            
            // Show sample of OCR text around "Package"
            int packageIdx = extractedText.toLowerCase().indexOf("package");
            if (packageIdx >= 0) {
                int start = Math.max(0, packageIdx - 150);
                int end = Math.min(extractedText.length(), packageIdx + 400);
                logger.info("\nSample OCR text around 'Package':");
                logger.info("  ...{}...", extractedText.substring(start, end));
            }
            
            // Get metadata before
            Map<String, String> metadataBefore = documentMetadataService.getMetadataMap(document);
            String procDescBefore = metadataBefore.get("procurementDescription");
            logger.info("\nMetadata BEFORE extraction:");
            logger.info("  procurementDescription: {}", 
                procDescBefore != null && procDescBefore.length() > 0 ? 
                    procDescBefore.substring(0, Math.min(100, procDescBefore.length())) + "..." : 
                    "NOT FOUND");
            
            // Run extraction
            logger.info("\n" + "-".repeat(80));
            logger.info("Running extraction...");
            logger.info("-".repeat(80));
            
            databaseMetadataExtractionService.extractMetadataForDocument(docId);
            
            // Reload and check results
            document = documentRepository.findById(docId).orElse(null);
            if (document == null) {
                logger.error("Document not found after extraction!");
                return;
            }
            
            Map<String, String> metadataAfter = documentMetadataService.getMetadataMap(document);
            String procDescAfter = metadataAfter.get("procurementDescription");
            
            logger.info("\n" + "=".repeat(80));
            logger.info("EXTRACTION RESULTS");
            logger.info("=".repeat(80));
            
            if (procDescAfter != null && !procDescAfter.trim().isEmpty()) {
                logger.info("\n✅ SUCCESS: procurementDescription extracted!");
                logger.info("  Length: {} characters", procDescAfter.length());
                logger.info("\n  Extracted value (first 400 chars):");
                logger.info("  " + "-".repeat(76));
                String preview = procDescAfter.length() > 400 ? 
                    procDescAfter.substring(0, 400) + "..." : procDescAfter;
                for (String line : preview.split("\n")) {
                    logger.info("  {}", line);
                }
                logger.info("  " + "-".repeat(76));
                
                // Verify labels are removed
                boolean hasIssues = false;
                if (procDescAfter.toLowerCase().contains("tender/proposal")) {
                    logger.warn("\n  ⚠️  Warning: Still contains 'Tender/Proposal'");
                    hasIssues = true;
                }
                if (procDescAfter.toLowerCase().contains("package no. and")) {
                    logger.warn("  ⚠️  Warning: Still contains 'Package No. and'");
                    hasIssues = true;
                }
                if (procDescAfter.toLowerCase().startsWith("description :")) {
                    logger.warn("  ⚠️  Warning: Starts with 'Description :'");
                    hasIssues = true;
                }
                
                if (!hasIssues) {
                    logger.info("\n  ✅ Verification: All labels removed correctly");
                }
                
                logger.info("\n✅ TEST PASSED: Procurement description extracted successfully!");
                
            } else {
                logger.error("\n❌ FAILED: procurementDescription was NOT extracted");
                logger.error("\nPossible reasons:");
                logger.error("  1. Pattern didn't match the OCR text format");
                logger.error("  2. Field not configured in document_type_fields table");
                logger.error("  3. Document type is not TENDER_NOTICE");
                logger.error("\nCheck application logs above for pattern matching details.");
            }
            
            logger.info("\n" + "=".repeat(80));
            
        } catch (Exception e) {
            logger.error("Error during test: {}", e.getMessage(), e);
        }
    }
}

