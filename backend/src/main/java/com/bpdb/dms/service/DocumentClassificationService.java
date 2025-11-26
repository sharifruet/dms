package com.bpdb.dms.service;

import com.bpdb.dms.model.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Service for automatic document type classification based on content analysis
 */
@Service
public class DocumentClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentClassificationService.class);

    // Keyword patterns for each document type
    private static final Map<DocumentType, List<String>> KEYWORD_PATTERNS = new HashMap<>();
    
    // Regex patterns for each document type
    private static final Map<DocumentType, List<Pattern>> REGEX_PATTERNS = new HashMap<>();

    static {
        initializePatterns();
    }

    /**
     * Initialize keyword and regex patterns for each document type
     */
    private static void initializePatterns() {
        // TENDER_NOTICE patterns
        KEYWORD_PATTERNS.put(DocumentType.TENDER_NOTICE, Arrays.asList(
            "tender notice", "invitation to tender", "request for proposal", "rfp",
            "tender document", "tender package", "tender no", "tender number",
            "eoi", "expression of interest", "tender opening", "tender closing"
        ));
        REGEX_PATTERNS.put(DocumentType.TENDER_NOTICE, Arrays.asList(
            Pattern.compile("(?i)tender\\s*(?:notice|no\\.?|number|#)\\s*:?\\s*[A-Z0-9\\-/]+"),
            Pattern.compile("(?i)invitation\\s+to\\s+tender", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)request\\s+for\\s+proposal", Pattern.CASE_INSENSITIVE)
        ));

        // TENDER_DOCUMENT patterns
        KEYWORD_PATTERNS.put(DocumentType.TENDER_DOCUMENT, Arrays.asList(
            "tender document", "tender specification", "technical specification",
            "tender schedule", "bill of quantities", "boq", "tender conditions"
        ));
        REGEX_PATTERNS.put(DocumentType.TENDER_DOCUMENT, Arrays.asList(
            Pattern.compile("(?i)tender\\s+document", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)technical\\s+specification", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)bill\\s+of\\s+quantities", Pattern.CASE_INSENSITIVE)
        ));

        // CONTRACT_AGREEMENT patterns
        KEYWORD_PATTERNS.put(DocumentType.CONTRACT_AGREEMENT, Arrays.asList(
            "contract agreement", "contract no", "contract number", "agreement",
            "contract between", "contract date", "contract value", "contract period"
        ));
        REGEX_PATTERNS.put(DocumentType.CONTRACT_AGREEMENT, Arrays.asList(
            Pattern.compile("(?i)contract\\s*(?:agreement|no\\.?|number|#)\\s*:?\\s*[A-Z0-9\\-/]+"),
            Pattern.compile("(?i)contract\\s+between", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)this\\s+agreement", Pattern.CASE_INSENSITIVE)
        ));

        // BANK_GUARANTEE_BG patterns
        KEYWORD_PATTERNS.put(DocumentType.BANK_GUARANTEE_BG, Arrays.asList(
            "bank guarantee", "bg no", "bg number", "guarantee no", "guarantee number",
            "performance guarantee", "advance guarantee", "bid bond", "guarantee amount"
        ));
        REGEX_PATTERNS.put(DocumentType.BANK_GUARANTEE_BG, Arrays.asList(
            Pattern.compile("(?i)bank\\s+guarantee", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)bg\\s*(?:no\\.?|number|#)\\s*:?\\s*[A-Z0-9\\-/]+"),
            Pattern.compile("(?i)guarantee\\s+no\\.?\\s*:?\\s*[A-Z0-9\\-/]+")
        ));

        // PERFORMANCE_SECURITY_PS patterns
        KEYWORD_PATTERNS.put(DocumentType.PERFORMANCE_SECURITY_PS, Arrays.asList(
            "performance security", "ps no", "ps number", "security deposit",
            "performance bond", "security amount", "retention money"
        ));
        REGEX_PATTERNS.put(DocumentType.PERFORMANCE_SECURITY_PS, Arrays.asList(
            Pattern.compile("(?i)performance\\s+security", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)ps\\s*(?:no\\.?|number|#)\\s*:?\\s*[A-Z0-9\\-/]+"),
            Pattern.compile("(?i)security\\s+deposit", Pattern.CASE_INSENSITIVE)
        ));

        // PERFORMANCE_GUARANTEE_PG patterns
        KEYWORD_PATTERNS.put(DocumentType.PERFORMANCE_GUARANTEE_PG, Arrays.asList(
            "performance guarantee", "pg no", "pg number", "guarantee certificate",
            "performance bond", "guarantee amount"
        ));
        REGEX_PATTERNS.put(DocumentType.PERFORMANCE_GUARANTEE_PG, Arrays.asList(
            Pattern.compile("(?i)performance\\s+guarantee", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)pg\\s*(?:no\\.?|number|#)\\s*:?\\s*[A-Z0-9\\-/]+")
        ));

        // BILL patterns
        KEYWORD_PATTERNS.put(DocumentType.BILL, Arrays.asList(
            "invoice", "bill", "bill no", "bill number", "invoice no", "invoice number",
            "tax invoice", "commercial invoice", "vendor invoice", "supplier invoice",
            "amount due", "total amount", "bill amount", "invoice amount"
        ));
        REGEX_PATTERNS.put(DocumentType.BILL, Arrays.asList(
            Pattern.compile("(?i)(?:invoice|bill)\\s*(?:no\\.?|number|#)\\s*:?\\s*[A-Z0-9\\-/]+"),
            Pattern.compile("(?i)tax\\s+invoice", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)commercial\\s+invoice", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)total\\s+amount\\s*:?\\s*[\\d,]+", Pattern.CASE_INSENSITIVE)
        ));

        // Note: CORRESPONDENCE is not a separate document type, classified as OTHER
        // Letter/memo patterns can help identify documents but will be classified as OTHER

        // STATIONERY_RECORD patterns
        KEYWORD_PATTERNS.put(DocumentType.STATIONERY_RECORD, Arrays.asList(
            "stationery", "stationary", "office supplies", "inventory", "stock",
            "stationery record", "supplies record"
        ));
        REGEX_PATTERNS.put(DocumentType.STATIONERY_RECORD, Arrays.asList(
            Pattern.compile("(?i)stationery\\s+record", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)office\\s+supplies", Pattern.CASE_INSENSITIVE)
        ));
    }

    /**
     * Classify document type based on extracted text content
     * 
     * @param extractedText The OCR-extracted or parsed text from the document
     * @param fileName The original filename (can provide additional clues)
     * @return ClassificationResult containing the detected type and confidence score
     */
    public ClassificationResult classify(String extractedText, String fileName) {
        if (extractedText == null || extractedText.trim().isEmpty()) {
            // Fallback to filename-based classification if no text available
            return classifyByFileName(fileName);
        }

        String normalizedText = extractedText.toLowerCase();
        Map<DocumentType, Double> scores = new HashMap<>();

        // Score each document type based on keyword matches
        for (Map.Entry<DocumentType, List<String>> entry : KEYWORD_PATTERNS.entrySet()) {
            DocumentType docType = entry.getKey();
            List<String> keywords = entry.getValue();
            
            double keywordScore = 0.0;
            int matches = 0;
            
            for (String keyword : keywords) {
                if (normalizedText.contains(keyword.toLowerCase())) {
                    matches++;
                    // Weight by keyword length (longer keywords are more specific)
                    keywordScore += keyword.length() * 0.1;
                }
            }
            
            // Normalize keyword score (0.0 to 1.0)
            if (matches > 0) {
                keywordScore = Math.min(1.0, keywordScore / 10.0);
                // Bonus for multiple matches
                keywordScore += Math.min(0.2, matches * 0.05);
            }
            
            scores.put(docType, keywordScore);
        }

        // Score each document type based on regex pattern matches
        for (Map.Entry<DocumentType, List<Pattern>> entry : REGEX_PATTERNS.entrySet()) {
            DocumentType docType = entry.getKey();
            List<Pattern> patterns = entry.getValue();
            
            double regexScore = 0.0;
            
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(extractedText);
                if (matcher.find()) {
                    regexScore += 0.3; // Regex matches are more reliable
                }
            }
            
            // Combine with keyword score
            double currentScore = scores.getOrDefault(docType, 0.0);
            scores.put(docType, currentScore + Math.min(0.5, regexScore));
        }

        // Find the document type with highest score
        DocumentType bestMatch = DocumentType.OTHER;
        double bestScore = 0.0;

        for (Map.Entry<DocumentType, Double> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestMatch = entry.getKey();
            }
        }

        // Normalize confidence score (0.0 to 1.0)
        double confidence = Math.min(1.0, bestScore);

        // If confidence is too low, default to OTHER
        if (confidence < 0.3) {
            bestMatch = DocumentType.OTHER;
            confidence = 0.0;
        }

        logger.debug("Document classification: type={}, confidence={}, fileName={}", 
            bestMatch, confidence, fileName);

        return new ClassificationResult(bestMatch, confidence, scores);
    }

    /**
     * Classify document type based on filename patterns
     * 
     * @param fileName The filename
     * @return ClassificationResult
     */
    private ClassificationResult classifyByFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return new ClassificationResult(DocumentType.OTHER, 0.0, new HashMap<>());
        }

        String lowerFileName = fileName.toLowerCase();

        // Check filename for document type indicators
        if (lowerFileName.contains("tender") && lowerFileName.contains("notice")) {
            return new ClassificationResult(DocumentType.TENDER_NOTICE, 0.6, new HashMap<>());
        }
        if (lowerFileName.contains("tender") && lowerFileName.contains("doc")) {
            return new ClassificationResult(DocumentType.TENDER_DOCUMENT, 0.6, new HashMap<>());
        }
        if (lowerFileName.contains("contract")) {
            return new ClassificationResult(DocumentType.CONTRACT_AGREEMENT, 0.6, new HashMap<>());
        }
        if (lowerFileName.contains("bank") && lowerFileName.contains("guarantee")) {
            return new ClassificationResult(DocumentType.BANK_GUARANTEE_BG, 0.6, new HashMap<>());
        }
        if (lowerFileName.contains("performance") && lowerFileName.contains("security")) {
            return new ClassificationResult(DocumentType.PERFORMANCE_SECURITY_PS, 0.6, new HashMap<>());
        }
        if (lowerFileName.contains("performance") && lowerFileName.contains("guarantee")) {
            return new ClassificationResult(DocumentType.PERFORMANCE_GUARANTEE_PG, 0.6, new HashMap<>());
        }
        if (lowerFileName.contains("invoice") || lowerFileName.contains("bill")) {
            return new ClassificationResult(DocumentType.BILL, 0.6, new HashMap<>());
        }
        if (lowerFileName.contains("letter") || lowerFileName.contains("memo")) {
            return new ClassificationResult(DocumentType.OTHER, 0.5, new HashMap<>());
        }

        return new ClassificationResult(DocumentType.OTHER, 0.0, new HashMap<>());
    }

    /**
     * Result of document classification
     */
    public static class ClassificationResult {
        private final DocumentType documentType;
        private final double confidence;
        private final Map<DocumentType, Double> allScores;

        public ClassificationResult(DocumentType documentType, double confidence, Map<DocumentType, Double> allScores) {
            this.documentType = documentType;
            this.confidence = confidence;
            this.allScores = allScores != null ? new HashMap<>(allScores) : new HashMap<>();
        }

        public DocumentType getDocumentType() {
            return documentType;
        }

        public double getConfidence() {
            return confidence;
        }

        public Map<DocumentType, Double> getAllScores() {
            return new HashMap<>(allScores);
        }

        public String getConfidenceLabel() {
            if (confidence >= 0.8) return "High";
            if (confidence >= 0.6) return "Medium";
            if (confidence >= 0.3) return "Low";
            return "Very Low";
        }
    }
}

