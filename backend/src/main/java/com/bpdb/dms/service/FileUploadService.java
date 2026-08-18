package com.bpdb.dms.service;

import com.bpdb.dms.dto.FileUploadResponse;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.Folder;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.model.DocumentType;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.FolderRepository;
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
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private FolderRepository folderRepository;
    
    @Autowired
    private OCRService ocrService;
    
    @Autowired
    private DocumentIndexingService documentIndexingService;
    
    @Autowired
    private DocumentMetadataService documentMetadataService;

    @Autowired
    private DocumentVersioningService documentVersioningService;

    @Autowired(required = false)
    private DocumentClassificationService documentClassificationService;
    
    @Autowired(required = false)
    private BillOCRService billOCRService;
    
    @Autowired(required = false)
    private DatabaseMetadataExtractionService databaseMetadataExtractionService;

    /**
     * Upload a single file
     */
    public FileUploadResponse uploadFile(MultipartFile file, User user, String documentType, String description,
                                         Map<String, String> manualMetadata, Long folderId) {
        try {
            // Validate file
            String validationError = validateFile(file);
            if (validationError != null) {
                return FileUploadResponse.error(validationError);
            }

            // Validate and normalize document type
            DocumentType resolvedType = DocumentType.resolve(documentType).orElse(null);
            if (resolvedType == null) {
                String message = "Invalid document type. Allowed types are: " + DocumentType.allowedTypesList();
                logger.warn("Document type validation failed for value '{}': {}", documentType, message);
                return FileUploadResponse.error(message);
            }

            // The folder-based tender workflow that used to gate this upload is gone with the
            // generic workflow engine (Q-16). Uploading is no longer conditional on a folder
            // carrying a workflow: sequencing is the procurement StageEngine's job, and a
            // document reaches a package through document_link, not through folder identity.

            // Calculate file hash for duplicate detection
            String fileHash = calculateFileHash(file);
            
            // Check for duplicate files
            Optional<Document> existingDocument = documentRepository.findFirstByFileHashAndIsActiveTrue(fileHash);
            if (existingDocument.isPresent()) {
                // Return duplicate information - frontend will handle the decision
                Document duplicate = existingDocument.get();
                return FileUploadResponse.duplicate(
                    duplicate.getId(),
                    duplicate.getFileName(),
                    duplicate.getOriginalName(),
                    duplicate.getFileSize(),
                    duplicate.getMimeType(),
                    duplicate.getDocumentType(),
                    duplicate.getCreatedAt(),
                    duplicate.getUploadedBy() != null ? duplicate.getUploadedBy().getUsername() : "Unknown",
                    "A file with identical content already exists in the system."
                );
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
            document.setDocumentType(resolvedType.name());
            document.setDescription(description);
            document.setUploadedBy(user);
            document.setDepartment(user.getDepartment());
            document.setIsActive(true);
            document.setFileHash(fileHash);
            
            // Set folder if provided
            if (folderId != null) {
                folderRepository.findById(folderId).ifPresent(document::setFolder);
            }
            
            // Save to database
            Document savedDocument = documentRepository.save(document);

            Map<String, String> combinedMetadata = new HashMap<>();
            if (manualMetadata != null && !manualMetadata.isEmpty()) {
                combinedMetadata.putAll(documentMetadataService.applyManualMetadata(savedDocument, manualMetadata));
            }

            Map<String, String> additionalMetadata = new HashMap<>();
            // The legacy APP-Excel side channel that used to run here is gone with
            // AppDocumentService. APP workbooks are imported through the procurement Stage 1
            // path (AppWorkbookParser -> AppPackageImportService), which creates packages
            // rather than scattering entries across document metadata.
            combinedMetadata.putAll(additionalMetadata);
            
            // High Priority Feature: Bill OCR Extraction for BILL document type
            // Extract bill data when a BILL document is uploaded and store in document metadata
            // Bills are now stored as documents with custom fields (no separate bill tables)
            if (resolvedType == DocumentType.BILL && billOCRService != null) {
                try {
                    logger.info("Starting bill OCR extraction for document: {}", savedDocument.getId());
                    Map<String, String> billMetadata = billOCRService.extractBillDataAsMetadata(file);
                    
                    // Separate bill field values from confidence scores
                    Map<String, String> billFields = new HashMap<>();
                    Map<String, String> confidenceFields = new HashMap<>();
                    
                    for (Map.Entry<String, String> entry : billMetadata.entrySet()) {
                        String key = entry.getKey();
                        if (key.endsWith("_confidence")) {
                            confidenceFields.put(key, entry.getValue());
                        } else if (!key.equals("bill_ocr_status") && !key.equals("bill_ocr_error") && !key.equals("bill_ocr_overall_confidence")) {
                            billFields.put(key, entry.getValue());
                        }
                    }
                    
                    // Store extracted bill data in document metadata using DocumentMetadataService
                    // This uses the document type fields defined for BILL document type
                    // Use AUTO_OCR source since data is extracted from OCR
                    documentMetadataService.applyAutoMetadata(savedDocument, billFields);
                    
                    // Store confidence scores as separate metadata entries
                    documentMetadataService.applyManualMetadata(savedDocument, confidenceFields);
                    
                    // Also add status/error to combinedMetadata for indexing
                    if (billMetadata.containsKey("bill_ocr_status")) {
                        combinedMetadata.put("bill_ocr_status", billMetadata.get("bill_ocr_status"));
                    }
                    if (billMetadata.containsKey("bill_ocr_error")) {
                        combinedMetadata.put("bill_ocr_error", billMetadata.get("bill_ocr_error"));
                    }
                    if (billMetadata.containsKey("bill_ocr_overall_confidence")) {
                        combinedMetadata.put("bill_ocr_overall_confidence", billMetadata.get("bill_ocr_overall_confidence"));
                    }
                    
                    logger.info("Bill OCR extraction completed for document: {} with status: {}", 
                               savedDocument.getId(), billMetadata.get("bill_ocr_status"));
                } catch (Exception e) {
                    logger.error("Error during bill OCR extraction for document {}: {}", 
                                savedDocument.getId(), e.getMessage(), e);
                    combinedMetadata.put("bill_ocr_status", "error");
                    combinedMetadata.put("bill_ocr_error", e.getMessage());
                    // Don't fail the upload if OCR extraction fails
                }
            }
            
            // Process OCR and indexing asynchronously
            processDocumentAsync(savedDocument, file, combinedMetadata);
            
            logger.info("File uploaded successfully: {} by user: {}", originalFilename, user.getUsername());
            
            // Perform quick classification based on filename for immediate feedback
            FileUploadResponse response = FileUploadResponse.success(
                savedDocument.getId(),
                savedDocument.getFileName(),
                savedDocument.getOriginalName(),
                savedDocument.getFileSize(),
                savedDocument.getMimeType(),
                savedDocument.getDocumentType()
            );
            
            // Add auto-detected document type if classification service is available
            if (documentClassificationService != null) {
                try {
                    DocumentClassificationService.ClassificationResult classification = 
                        documentClassificationService.classify(null, originalFilename);
                    if (classification.getConfidence() >= 0.3) {
                        response.setDetectedDocumentType(classification.getDocumentType().name());
                        response.setDetectionConfidence(classification.getConfidence());
                    }
                } catch (Exception e) {
                    logger.debug("Quick classification failed: {}", e.getMessage());
                }
            }
            
            return response;
            
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

        // Allow any file type for uploads (including APP entry attachments and
        // other supporting documents). Downstream OCR/processing components
        // will handle unsupported formats as needed.

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
    public void processDocumentAsync(Document document, MultipartFile file, Map<String, String> additionalMetadata) {
        final Long documentId = document.getId();
        
        try {
            logger.info("Starting async processing for document: {}", documentId);

            Document managedDocument = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found for OCR processing: " + documentId));
            
            // Ensure user is loaded
            if (managedDocument.getUploadedBy() != null) {
                managedDocument.getUploadedBy().getId();
            }
            
            // Perform OCR processing
            Map<String, String> combinedMetadata = new HashMap<>(documentMetadataService.getMetadataMap(managedDocument));
            if (additionalMetadata != null) {
                combinedMetadata.putAll(additionalMetadata);
            }
            
            if (!ocrService.isOcrAvailable()) {
                logger.warn("Skipping OCR processing for document {} because OCR service is unavailable", documentId);
                combinedMetadata.put("ocrStatus", "unavailable");
                documentIndexingService.indexDocument(
                    managedDocument,
                    "",
                    combinedMetadata,
                    0.0,
                    0.0
                );
                return;
            }
            
            OCRService.OCRResult ocrResult;
            try {
                ocrResult = ocrService.extractText(file);
                managedDocument.setExtractedText(ocrResult.getExtractedText());
                documentRepository.save(managedDocument);
                
                // Extract metadata using database regex after extracted_text is saved
                if (ocrResult.getExtractedText() != null && !ocrResult.getExtractedText().trim().isEmpty()) {
                    try {
                        databaseMetadataExtractionService.extractMetadataForDocument(documentId);
                    } catch (Exception dbExtractError) {
                        logger.warn("Database metadata extraction failed for document {}: {}", 
                            documentId, dbExtractError.getMessage());
                        // Continue processing even if DB extraction fails
                    }
                }
            } catch (Throwable ocrError) {
                logger.error("OCR extraction threw an error for document {}: {}", documentId, ocrError.getMessage());
                combinedMetadata.put("ocrStatus", "failed");
                String errorMsg = ocrError.getMessage();
                if (errorMsg != null && (errorMsg.contains("TessAPI") || errorMsg.contains("Could not initialize"))) {
                    errorMsg = "Tesseract native library not available. Please install Tesseract on the system. " +
                              "On macOS: brew install tesseract. " +
                              "On Linux: apt-get install tesseract-ocr. " +
                              "Error: " + errorMsg;
                }
                combinedMetadata.put("error", errorMsg != null ? errorMsg : "unknown");
                documentIndexingService.indexDocument(
                    managedDocument,
                    "",
                    combinedMetadata,
                    0.0,
                    0.0
                );
                return;
            }
            
            if (ocrResult != null && ocrResult.isSuccess()) {
                // Update document with OCR results if needed
                boolean documentTypeChanged = false;
                if (ocrResult.getDocumentType() != null && managedDocument.getDocumentType() == null) {
                    try {
                        managedDocument.setDocumentType(ocrResult.getDocumentType());
                        documentRepository.save(managedDocument);
                        documentTypeChanged = true;
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid document type from OCR: {}", ocrResult.getDocumentType());
                    }
                }

                if (ocrResult.getMetadata() != null) {
                    combinedMetadata.putAll(ocrResult.getMetadata());
                }

                combinedMetadata.putAll(documentMetadataService.extractMetadataFromText(managedDocument, ocrResult.getExtractedText()));
                
                // Re-run database metadata extraction if document type was changed or if it's a TENDER_NOTICE
                // This ensures procurementDescription and other fields are extracted correctly
                if (documentTypeChanged || DocumentType.TENDER_NOTICE.name().equals(managedDocument.getDocumentType())) {
                    if (databaseMetadataExtractionService != null && 
                        ocrResult.getExtractedText() != null && !ocrResult.getExtractedText().trim().isEmpty()) {
                        try {
                            logger.info("Re-running database metadata extraction for document {} (type: {})", 
                                documentId, managedDocument.getDocumentType());
                            databaseMetadataExtractionService.extractMetadataForDocument(documentId);
                        } catch (Exception dbExtractError) {
                            logger.warn("Database metadata extraction failed for document {}: {}", 
                                documentId, dbExtractError.getMessage());
                            // Continue processing even if DB extraction fails
                        }
                    }
                }
                
                // Index document for search
                documentIndexingService.indexDocument(
                    managedDocument,
                    ocrResult.getExtractedText(),
                    combinedMetadata,
                    ocrResult.getConfidence(),
                    ocrResult.getClassificationConfidence()
                );
                
                logger.info("Async processing completed for document: {} - OCR confidence: {}", 
                           documentId, ocrResult.getConfidence());
                
            } else {
                String errorMsg = ocrResult != null ? ocrResult.getErrorMessage() : "Unknown error";
                logger.error("OCR processing failed for document: {} - Error: {}", 
                           documentId, errorMsg);
                
                // Still index the document without OCR text
                if (ocrResult != null && ocrResult.getMetadata() != null) {
                    combinedMetadata.putAll(ocrResult.getMetadata());
                }
                combinedMetadata.put("ocrStatus", "failed");
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    combinedMetadata.put("ocrError", errorMsg);
                }
                documentIndexingService.indexDocument(
                    managedDocument,
                    "",
                    combinedMetadata,
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
                    new HashMap<>(),
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
            processDocumentAsync(document, multipartFile, new HashMap<>());
            
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
    
    /**
     * Handle duplicate file upload with user's choice
     * @param file The file being uploaded
     * @param user The user uploading
     * @param documentType Document type
     * @param description Description
     * @param manualMetadata Manual metadata
     * @param folderId Folder ID
     * @param duplicateDocumentId The ID of the duplicate document
     * @param action The action to take: "skip", "version", or "replace"
     * @return FileUploadResponse
     */
    public FileUploadResponse handleDuplicateUpload(MultipartFile file, User user, String documentType,
                                                    String description, Map<String, String> manualMetadata,
                                                    Long folderId, Long duplicateDocumentId, String action) {
        try {
            // Verify duplicate document exists
            if (!documentRepository.existsById(duplicateDocumentId)) {
                return FileUploadResponse.error("Duplicate document not found");
            }
            
            switch (action.toLowerCase()) {
                case "skip":
                    return FileUploadResponse.error("Upload skipped - duplicate file already exists");
                    
                case "version":
                    // Upload as a new version of the duplicate document
                    return uploadAsVersion(file, user, duplicateDocumentId, description, manualMetadata);
                    
                case "replace":
                    // Replace the duplicate document
                    return replaceDocument(file, user, duplicateDocumentId, documentType, description, manualMetadata, folderId);
                    
                default:
                    return FileUploadResponse.error("Invalid action. Must be 'skip', 'version', or 'replace'");
            }
        } catch (Exception e) {
            logger.error("Error handling duplicate upload: {}", e.getMessage());
            return FileUploadResponse.error("Failed to handle duplicate upload: " + e.getMessage());
        }
    }
    
    /**
     * Upload file as a new version of an existing document
     */
    private FileUploadResponse uploadAsVersion(MultipartFile file, User user, Long documentId,
                                              String description, Map<String, String> manualMetadata) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            // Create a new version
            com.bpdb.dms.entity.VersionType versionType = com.bpdb.dms.entity.VersionType.PATCH;
            String changeDescription = description != null ? description : "New version uploaded";
            
            documentVersioningService.createDocumentVersion(
                documentId,
                file,
                changeDescription,
                versionType,
                user
            );
            
            logger.info("File uploaded as new version of document: {} by user: {}", 
                      document.getOriginalName(), user.getUsername());
            
            return FileUploadResponse.success(
                documentId,
                document.getFileName(),
                document.getOriginalName(),
                document.getFileSize(),
                document.getMimeType(),
                document.getDocumentType()
            );
        } catch (Exception e) {
            logger.error("Error uploading as version: {}", e.getMessage());
            return FileUploadResponse.error("Failed to upload as version: " + e.getMessage());
        }
    }
    
    /**
     * Replace an existing document with a new file
     */
    private FileUploadResponse replaceDocument(MultipartFile file, User user, Long documentId,
                                              String documentType, String description,
                                              Map<String, String> manualMetadata, Long folderId) {
        try {
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
            
            // Validate file
            String validationError = validateFile(file);
            if (validationError != null) {
                return FileUploadResponse.error(validationError);
            }
            
            // Validate and normalize document type
            DocumentType resolvedType = DocumentType.resolve(documentType).orElse(null);
            if (resolvedType == null) {
                String message = "Invalid document type. Allowed types are: " + DocumentType.allowedTypesList();
                return FileUploadResponse.error(message);
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
            
            // Save new file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Calculate new file hash
            String fileHash = calculateFileHash(file);
            
            // Update document
            document.setFileName(uniqueFilename);
            if (originalFilename != null && !originalFilename.equals(document.getOriginalName())) {
                document.setOriginalName(originalFilename);
            }
            document.setFilePath(filePath.toString());
            document.setFileSize(file.getSize());
            document.setMimeType(file.getContentType());
            document.setDocumentType(resolvedType.name());
            if (description != null) {
                document.setDescription(description);
            }
            document.setFileHash(fileHash);
            
            // Set folder if provided
            if (folderId != null) {
                folderRepository.findById(folderId).ifPresent(document::setFolder);
            }
            
            Document savedDocument = documentRepository.save(document);
            
            // Apply metadata
            Map<String, String> combinedMetadata = new HashMap<>();
            if (manualMetadata != null && !manualMetadata.isEmpty()) {
                combinedMetadata.putAll(documentMetadataService.applyManualMetadata(savedDocument, manualMetadata));
            }
            
            // Process OCR and indexing asynchronously
            processDocumentAsync(savedDocument, file, combinedMetadata);
            
            logger.info("Document replaced successfully: {} by user: {}", 
                      savedDocument.getOriginalName(), user.getUsername());
            
            return FileUploadResponse.success(
                savedDocument.getId(),
                savedDocument.getFileName(),
                savedDocument.getOriginalName(),
                savedDocument.getFileSize(),
                savedDocument.getMimeType(),
                savedDocument.getDocumentType()
            );
        } catch (Exception e) {
            logger.error("Error replacing document: {}", e.getMessage());
            return FileUploadResponse.error("Failed to replace document: " + e.getMessage());
        }
    }
    
    /**
     * Calculate SHA-256 hash of file content for duplicate detection
     */
    private String calculateFileHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(file.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("Failed to calculate file hash: {}", e.getMessage());
            return "";
        }
    }
}
