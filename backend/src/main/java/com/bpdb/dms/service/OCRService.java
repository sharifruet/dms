package com.bpdb.dms.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * Service for OCR processing and text extraction
 */
@Service
public class OCRService {
    
    private static final Logger logger = LoggerFactory.getLogger(OCRService.class);
    
    @Value("${app.tesseract.data.path:/usr/share/tesseract-ocr/4.00/tessdata}")
    private String tesseractDataPath;
    
    @Value("${app.tesseract.language:eng}")
    private String tesseractLanguage;
    
    @Value("${app.ocr.enabled:true}")
    private boolean ocrEnabled;
    
    @Autowired
    private AuditService auditService;
    
    private final ITesseract tesseract;
    private final Tika tika;
    private volatile boolean ocrAvailable;
    
    public OCRService() {
        this.tesseract = new Tesseract();
        this.tika = new Tika();
        this.ocrAvailable = false;
    }
    
    @PostConstruct
    public void setUp() {
        initializeTesseract();
    }
    
    /**
     * Initialize Tesseract OCR engine
     */
    private void initializeTesseract() {
        if (!ocrEnabled) {
            ocrAvailable = false;
            logger.info("OCR processing disabled via configuration");
            return;
        }
        
        try {
            Path dataPath = Paths.get(tesseractDataPath);
            if (!Files.exists(dataPath)) {
                logger.warn("Tesseract data path not found at {}. OCR will be disabled.", tesseractDataPath);
                ocrAvailable = false;
                return;
            }
            
            tesseract.setDatapath(tesseractDataPath);
            tesseract.setLanguage(tesseractLanguage);
            tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
            tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only
            logger.info("Tesseract OCR initialized successfully");
            ocrAvailable = true;
        } catch (Exception e) {
            logger.error("Failed to initialize Tesseract OCR: {}", e.getMessage());
            ocrAvailable = false;
        } catch (Error err) {
            logger.error("Critical error while initializing Tesseract OCR: {}", err.getMessage());
            ocrAvailable = false;
        }
    }
    
    /**
     * Extract text from uploaded file using OCR
     */
    @Async
    public CompletableFuture<OCRResult> extractTextAsync(MultipartFile file, Long documentId) {
        try {
            logger.info("Starting OCR processing for document: {}", documentId);
            
            OCRResult result = extractText(file);
            result.setDocumentId(documentId);
            
            int textLength = result.getExtractedText() != null ? result.getExtractedText().length() : 0;
            logger.info("OCR processing completed for document: {} - Text length: {}", 
                       documentId, textLength);
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logger.error("OCR processing failed for document: {} - Error: {}", documentId, e.getMessage());
            OCRResult errorResult = new OCRResult();
            errorResult.setDocumentId(documentId);
            errorResult.setSuccess(false);
            errorResult.setErrorMessage(e.getMessage());
            return CompletableFuture.completedFuture(errorResult);
        }
    }
    
    /**
     * Extract text from file synchronously
     */
    public OCRResult extractText(MultipartFile file) throws IOException, TesseractException {
        OCRResult result = new OCRResult();
        
        if (!ocrAvailable) {
            result.setSuccess(false);
            result.setErrorMessage("OCR is currently unavailable");
            return result;
        }
        
        // Detect file type
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        
        result.setFileName(fileName);
        result.setContentType(contentType);
        result.setFileSize(file.getSize());
        
        try {
            String extractedText;
            
            if (isImageFile(contentType)) {
                // Process image files with OCR
                extractedText = processImageWithOCR(file);
            } else if (isPDFFile(contentType)) {
                // Process PDF files
                extractedText = processPDFWithOCR(file);
            } else if (isOfficeDocument(contentType)) {
                // Process Office documents
                extractedText = processOfficeDocument(file);
            } else {
                // Process as plain text
                extractedText = processPlainText(file);
            }
            
            result.setExtractedText(extractedText);
            result.setConfidence(calculateConfidence(extractedText));
            result.setSuccess(true);
            
            // Extract metadata
            Map<String, String> metadata = extractMetadata(file);
            result.setMetadata(metadata);
            
            // Classify document type
            DocumentTypeClassification classification = classifyDocument(extractedText, fileName);
            result.setDocumentType(classification.getDocumentType());
            result.setClassificationConfidence(classification.getConfidence());
            
        } catch (Exception e) {
            logger.error("Error processing file {}: {}", fileName, e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    public boolean isOcrAvailable() {
        return ocrAvailable;
    }
    
    /**
     * Process image files with OCR
     */
    private String processImageWithOCR(MultipartFile file) throws IOException, TesseractException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IOException("Unable to read image file");
        }
        
        // Preprocess image for better OCR results
        BufferedImage processedImage = preprocessImage(image);
        
        try {
            return tesseract.doOCR(processedImage);
        } catch (Error err) {
            logger.error("Native OCR error: {}", err.getMessage());
            throw new TesseractException("Native OCR error: " + err.getMessage());
        }
    }
    
    /**
     * Process PDF files with OCR
     */
    private String processPDFWithOCR(MultipartFile file) throws IOException, TesseractException {
        // For PDF files, we'll use Tika to extract text first
        // If no text is found, we'll convert PDF to images and use OCR
        try {
            String text = tika.parseToString(file.getInputStream());
            if (text != null && !text.trim().isEmpty()) {
                return text;
            }
        } catch (TikaException e) {
            logger.warn("Tika failed to extract text from PDF, falling back to OCR: {}", e.getMessage());
        }
        
        // Fallback to OCR processing
        // This would require PDF to image conversion
        return "PDF OCR processing not fully implemented yet";
    }
    
    /**
     * Process Office documents
     */
    private String processOfficeDocument(MultipartFile file) throws IOException, TikaException {
        return tika.parseToString(file.getInputStream());
    }
    
    /**
     * Process plain text files
     */
    private String processPlainText(MultipartFile file) throws IOException {
        return new String(file.getBytes());
    }
    
    /**
     * Preprocess image for better OCR results
     */
    private BufferedImage preprocessImage(BufferedImage originalImage) {
        // Basic image preprocessing
        // In a production system, you might want to add:
        // - Noise reduction
        // - Contrast enhancement
        // - Deskewing
        // - Binarization
        
        return originalImage;
    }
    
    /**
     * Extract metadata from file
     */
    private Map<String, String> extractMetadata(MultipartFile file) {
        Map<String, String> metadata = new HashMap<>();
        
        try {
            metadata.put("fileName", file.getOriginalFilename());
            metadata.put("contentType", file.getContentType());
            metadata.put("fileSize", String.valueOf(file.getSize()));
            metadata.put("lastModified", String.valueOf(file.getInputStream().available()));
            
            // Extract additional metadata using Tika
            Map<String, String> tikaMetadata = new HashMap<>();
            try {
                tikaMetadata = tika.parseToString(file.getInputStream()).length() > 0 ? 
                    Map.of("hasText", "true") : Map.of("hasText", "false");
            } catch (Exception e) {
                logger.warn("Failed to extract Tika metadata: {}", e.getMessage());
            }
            
            metadata.putAll(tikaMetadata);
            
        } catch (Exception e) {
            logger.error("Error extracting metadata: {}", e.getMessage());
        }
        
        return metadata;
    }
    
    /**
     * Classify document type based on content
     */
    private DocumentTypeClassification classifyDocument(String text, String fileName) {
        DocumentTypeClassification classification = new DocumentTypeClassification();
        
        String lowerText = text.toLowerCase();
        String lowerFileName = fileName.toLowerCase();
        
        // Simple keyword-based classification
        if (lowerText.contains("tender") || lowerText.contains("bid") || lowerText.contains("proposal")) {
            classification.setDocumentType("TENDER");
            classification.setConfidence(0.8);
        } else if (lowerText.contains("purchase order") || lowerText.contains("po number")) {
            classification.setDocumentType("PURCHASE_ORDER");
            classification.setConfidence(0.9);
        } else if (lowerText.contains("letter of credit") || lowerText.contains("lc")) {
            classification.setDocumentType("LETTER_OF_CREDIT");
            classification.setConfidence(0.9);
        } else if (lowerText.contains("bank guarantee") || lowerText.contains("bg")) {
            classification.setDocumentType("BANK_GUARANTEE");
            classification.setConfidence(0.9);
        } else if (lowerText.contains("contract") || lowerText.contains("agreement")) {
            classification.setDocumentType("CONTRACT");
            classification.setConfidence(0.8);
        } else if (lowerText.contains("correspondence") || lowerText.contains("letter")) {
            classification.setDocumentType("CORRESPONDENCE");
            classification.setConfidence(0.7);
        } else {
            classification.setDocumentType("OTHER");
            classification.setConfidence(0.5);
        }
        
        return classification;
    }
    
    /**
     * Calculate confidence score for extracted text
     */
    private double calculateConfidence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        
        // Simple confidence calculation based on text length and character patterns
        double confidence = 0.5; // Base confidence
        
        // Increase confidence for longer text
        if (text.length() > 100) {
            confidence += 0.2;
        }
        if (text.length() > 500) {
            confidence += 0.1;
        }
        
        // Increase confidence for structured text (contains numbers, dates, etc.)
        if (text.matches(".*\\d+.*")) {
            confidence += 0.1;
        }
        
        return Math.min(confidence, 1.0);
    }
    
    /**
     * Check if file is an image
     */
    private boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
    
    /**
     * Check if file is a PDF
     */
    private boolean isPDFFile(String contentType) {
        return "application/pdf".equals(contentType);
    }
    
    /**
     * Check if file is an Office document
     */
    private boolean isOfficeDocument(String contentType) {
        return contentType != null && (
            contentType.contains("word") ||
            contentType.contains("excel") ||
            contentType.contains("powerpoint") ||
            contentType.contains("officedocument")
        );
    }
    
    /**
     * OCR Result class
     */
    public static class OCRResult {
        private Long documentId;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private String extractedText;
        private double confidence;
        private boolean success;
        private String errorMessage;
        private Map<String, String> metadata;
        private String documentType;
        private double classificationConfidence;
        
        // Getters and setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public String getExtractedText() { return extractedText; }
        public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public double getClassificationConfidence() { return classificationConfidence; }
        public void setClassificationConfidence(double classificationConfidence) { this.classificationConfidence = classificationConfidence; }
    }
    
    /**
     * Document Type Classification class
     */
    public static class DocumentTypeClassification {
        private String documentType;
        private double confidence;
        
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}
