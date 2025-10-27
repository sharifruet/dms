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
import java.util.Arrays;
import java.util.List;
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
        try {
            logger.info("Starting async processing for document: {}", document.getId());
            
            // Perform OCR processing
            OCRService.OCRResult ocrResult = ocrService.extractText(file);
            
            if (ocrResult.isSuccess()) {
                // Update document with OCR results if needed
                if (ocrResult.getDocumentType() != null && document.getDocumentType() == null) {
                    try {
                        document.setDocumentType(DocumentType.valueOf(ocrResult.getDocumentType()));
                        documentRepository.save(document);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid document type from OCR: {}", ocrResult.getDocumentType());
                    }
                }
                
                // Index document for search
                documentIndexingService.indexDocument(
                    document,
                    ocrResult.getExtractedText(),
                    ocrResult.getMetadata(),
                    ocrResult.getConfidence(),
                    ocrResult.getClassificationConfidence()
                );
                
                logger.info("Async processing completed for document: {} - OCR confidence: {}", 
                           document.getId(), ocrResult.getConfidence());
                
            } else {
                logger.error("OCR processing failed for document: {} - Error: {}", 
                           document.getId(), ocrResult.getErrorMessage());
                
                // Still index the document without OCR text
                documentIndexingService.indexDocument(
                    document,
                    "",
                    ocrResult.getMetadata(),
                    0.0,
                    0.0
                );
            }
            
        } catch (Exception e) {
            logger.error("Async processing failed for document: {} - Error: {}", 
                        document.getId(), e.getMessage());
        }
    }
}
