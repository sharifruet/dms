package com.bpdb.dms.service;

import com.bpdb.dms.dto.FileUploadResponse;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentType;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling file upload operations
 */
@Service
public class FileUploadService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${app.max.file.size:104857600}") // 100MB default
    private long maxFileSize;
    
    // Allowed file types
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "image/jpeg",
        "image/png",
        "image/tiff",
        "text/plain"
    );
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private OCRService ocrService;
    
    @Autowired
    private DocumentIndexingService documentIndexingService;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Upload a single file
     */
    public FileUploadResponse uploadFile(MultipartFile file, User user, DocumentType documentType, String description) {
        try {
            // Validate file
            String validationError = validateFile(file);
            if (validationError != null) {
                return FileUploadResponse.error(validationError);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(fileExtension);
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Save file to disk
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create document record
            Document document = new Document();
            document.setFileName(uniqueFilename);
            document.setOriginalName(originalFilename);
            document.setFilePath(filePath.toString());
            document.setFileSize(file.getSize());
            document.setMimeType(file.getContentType());
            document.setDocumentType(documentType);
            document.setDescription(description);
            document.setUploadedBy(user);
            document.setDepartment(user.getDepartment());
            document.setIsActive(true);
            
            // Save to database
            Document savedDocument = documentRepository.save(document);
            
            // Process OCR and indexing asynchronously
            processDocumentAsync(savedDocument, file);
            
            logger.info("File uploaded successfully: {} by user: {}", originalFilename, user.getUsername());
            
            return FileUploadResponse.success(
                savedDocument.getId(),
                savedDocument.getFileName(),
                savedDocument.getOriginalName(),
                savedDocument.getFileSize(),
                savedDocument.getMimeType(),
                savedDocument.getDocumentType()
            );
            
        } catch (IOException e) {
            logger.error("Error uploading file: {}", e.getMessage());
            return FileUploadResponse.error("Failed to upload file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during file upload: {}", e.getMessage());
            return FileUploadResponse.error("Unexpected error occurred during upload");
        }
    }
    
    /**
     * Validate uploaded file
     */
    private String validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            return "File is empty";
        }
        
        if (file.getSize() > maxFileSize) {
            return "File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB";
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            return "File type not supported. Allowed types: PDF, DOC, DOCX, XLS, XLSX, JPEG, PNG, TIFF, TXT";
        }
        
        return null;
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * Generate unique filename
     */
    private String generateUniqueFilename(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + uuid + extension;
    }
    
    /**
     * Get file path for download
     */
    public Path getFilePath(Long documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        return Paths.get(document.getFilePath());
    }
    
    /**
     * Delete file from storage and database
     */
    public boolean deleteFile(Long documentId) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            // Delete file from storage
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            
            // Mark as inactive in database (soft delete)
            document.setIsActive(false);
            documentRepository.save(document);
            
            logger.info("File deleted successfully: {}", document.getOriginalName());
            return true;
            
        } catch (Exception e) {
            logger.error("Error deleting file: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Process document with OCR and indexing asynchronously
     */
    @Async
    public void processDocumentAsync(Document document, MultipartFile file) {
        OCRService.OCRResult ocrResult = null;
        final Long documentId = document.getId();
        
        try {
            logger.info("Starting async processing for document: {}", documentId);
            
            // Refetch document in async context to ensure user is loaded
            final Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
            // Ensure user is loaded
            if (doc.getUploadedBy() != null) {
                doc.getUploadedBy().getId();
            }
            
            // Perform OCR processing with error handling for native crashes
            try {
                ocrResult = ocrService.extractText(file);
            } catch (Throwable t) {
                // Catch all errors including native crashes (Error, Exception, etc.)
                logger.error("OCR processing failed for document: {} - Error type: {}, Message: {}", 
                           doc.getId(), t.getClass().getName(), t.getMessage());
                // Create a failed result to continue processing
                ocrResult = new OCRService.OCRResult();
                ocrResult.setSuccess(false);
                String errorMsg = t.getMessage();
                if (errorMsg != null && (errorMsg.contains("TessAPI") || errorMsg.contains("Could not initialize"))) {
                    errorMsg = "Tesseract native library not available. Please install Tesseract on the system. " +
                              "On macOS: brew install tesseract. " +
                              "On Linux: apt-get install tesseract-ocr. " +
                              "Error: " + errorMsg;
                }
                ocrResult.setErrorMessage(errorMsg != null ? errorMsg : "OCR processing failed");
            }
            
            if (ocrResult != null && ocrResult.isSuccess()) {
                // Update document with OCR results if needed
                if (ocrResult.getDocumentType() != null && doc.getDocumentType() == null) {
                    try {
                        doc.setDocumentType(DocumentType.valueOf(ocrResult.getDocumentType()));
                        documentRepository.save(doc);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid document type from OCR: {}", ocrResult.getDocumentType());
                    }
                }
                
                // Index document for search
                documentIndexingService.indexDocument(
                    doc,
                    ocrResult.getExtractedText(),
                    ocrResult.getMetadata(),
                    ocrResult.getConfidence(),
                    ocrResult.getClassificationConfidence()
                );
                
                logger.info("Async processing completed for document: {} - OCR confidence: {}", 
                           doc.getId(), ocrResult.getConfidence());
                
            } else {
                String errorMsg = ocrResult != null ? ocrResult.getErrorMessage() : "Unknown error";
                logger.error("OCR processing failed for document: {} - Error: {}", 
                           doc.getId(), errorMsg);
                
                // Still index the document without OCR text
                // Store error message in metadata for debugging
                Map<String, String> metadata = ocrResult != null && ocrResult.getMetadata() != null 
                    ? new HashMap<>(ocrResult.getMetadata()) 
                    : new HashMap<>();
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    metadata.put("ocrError", errorMsg);
                }
                
                documentIndexingService.indexDocument(
                    doc,
                    "",
                    metadata,
                    0.0,
                    0.0
                );
            }
            
        } catch (Throwable e) {
            // Catch any remaining errors to prevent async task from crashing
            logger.error("Async processing failed for document: {} - Error type: {}, Message: {}", 
                        documentId, e.getClass().getName(), e.getMessage());
            
            // Still try to index the document even if OCR failed
            try {
                // Refetch document for indexing in case of error
                Document docForIndex = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
                if (docForIndex.getUploadedBy() != null) {
                    docForIndex.getUploadedBy().getId();
                }
                documentIndexingService.indexDocument(
                    docForIndex,
                    "",
                    null,
                    0.0,
                    0.0
                );
            } catch (Exception indexError) {
                logger.error("Failed to index document {} after OCR failure: {}", 
                            documentId, indexError.getMessage());
            }
        }
    }
    
    /**
     * Re-process OCR for an existing document
     */
    @Async
    public void reprocessOCR(Long documentId) {
        try {
            logger.info("Starting OCR re-processing for document: {}", documentId);
            
            // Note: We fetch document here but processDocumentAsync will refetch it 
            // in its own async context to ensure user is loaded
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
            
            // Check if file exists
            Path filePath = Paths.get(document.getFilePath());
            if (!Files.exists(filePath)) {
                logger.error("File not found for document {}: {}", documentId, document.getFilePath());
                return;
            }
            
            // Create a MultipartFile wrapper from the existing file
            File file = filePath.toFile();
            byte[] fileBytes = Files.readAllBytes(filePath);
            
            MultipartFile multipartFile = new MultipartFile() {
                @Override
                public String getName() {
                    return "file";
                }

                @Override
                public String getOriginalFilename() {
                    return document.getOriginalName();
                }

                @Override
                public String getContentType() {
                    return document.getMimeType();
                }

                @Override
                public boolean isEmpty() {
                    return fileBytes.length == 0;
                }

                @Override
                public long getSize() {
                    return fileBytes.length;
                }

                @Override
                public byte[] getBytes() throws IOException {
                    return fileBytes;
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new FileInputStream(file);
                }

                @Override
                public void transferTo(File dest) throws IOException, IllegalStateException {
                    Files.copy(filePath, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            };
            
            // Process with OCR
            processDocumentAsync(document, multipartFile);
            
            logger.info("OCR re-processing triggered for document: {}", documentId);
            
        } catch (Exception e) {
            logger.error("Error re-processing OCR for document {}: {}", documentId, e.getMessage(), e);
        }
    }
    
    /**
     * Re-process OCR for all documents (or documents without OCR text)
     */
    @Async
    public void reprocessAllDocumentsOCR() {
        try {
            logger.info("Starting OCR re-processing for all documents");
            
            List<Document> documents = documentRepository.findByIsActiveTrue(
                org.springframework.data.domain.PageRequest.of(0, 1000)
            ).getContent();
            
            int processed = 0;
            int failed = 0;
            
            for (Document document : documents) {
                try {
                    reprocessOCR(document.getId());
                    processed++;
                } catch (Exception e) {
                    logger.error("Failed to re-process OCR for document {}: {}", 
                               document.getId(), e.getMessage());
                    failed++;
                }
            }
            
            logger.info("OCR re-processing completed. Processed: {}, Failed: {}", processed, failed);
            
        } catch (Exception e) {
            logger.error("Error re-processing OCR for all documents: {}", e.getMessage(), e);
        }
    }
}
