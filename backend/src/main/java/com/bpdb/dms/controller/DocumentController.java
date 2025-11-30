package com.bpdb.dms.controller;

import com.bpdb.dms.dto.FileUploadResponse;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentIndex;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentIndexRepository;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.FolderRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.entity.Folder;
import com.bpdb.dms.service.DocumentArchiveService;
import com.bpdb.dms.service.DocumentCategoryService;
import com.bpdb.dms.service.DocumentMetadataService;
import com.bpdb.dms.service.DocumentTypeFieldService;
import com.bpdb.dms.entity.DocumentTypeField;
import com.bpdb.dms.service.FileUploadService;
import com.bpdb.dms.service.DatabaseMetadataExtractionService;
import com.bpdb.dms.service.StationeryTrackingService;
import com.bpdb.dms.entity.AppDocumentEntry;
import com.bpdb.dms.repository.AppDocumentEntryRepository;
import com.bpdb.dms.model.DocumentType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private DocumentIndexRepository documentIndexRepository;

    @Autowired
    private DocumentCategoryService documentCategoryService;

    @Autowired
    private DocumentArchiveService documentArchiveService;

    @Autowired
    private StationeryTrackingService stationeryTrackingService;

    @Autowired
    private AppDocumentEntryRepository appDocumentEntryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentTypeFieldService documentTypeFieldService;

    @Autowired
    private DocumentMetadataService documentMetadataService;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired(required = false)
    private DatabaseMetadataExtractionService databaseMetadataExtractionService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Page<Document>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String documentType
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Document> result;
        
        // Filter by folder and/or document type
        if (folderId != null && documentType != null && !documentType.isBlank()) {
            // Both folder and document type filters
            result = documentRepository.findByFolderIdAndDocumentType(folderId, documentType, pageable);
        } else if (folderId != null) {
            // Only folder filter
            result = documentRepository.findByFolderId(folderId, pageable);
        } else if (documentType != null && !documentType.isBlank()) {
            // Only document type filter - filter by active and non-deleted documents
            result = documentRepository.findByDocumentTypeAndIsActiveTrueAndDeletedAtIsNull(documentType, pageable);
        } else {
            // No filters - get all active, non-deleted documents
            result = documentRepository.findActiveNonArchivedDocuments(pageable);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable Long id) {
        try {
            Optional<Document> documentOpt = documentRepository.findById(id);
            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Document document = documentOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("document", document);

            // Get OCR text - prioritize database extractedText over Elasticsearch
            String ocrText = null;
            Double ocrConfidence = 0.0;
            
            // 1. First, try to get extractedText from database (primary source)
            String dbExtractedText = document.getExtractedText();
            if (dbExtractedText != null && !dbExtractedText.trim().isEmpty()) {
                ocrText = dbExtractedText;
                // Set OCR as completed since we have text from database
                response.put("ocrProcessing", false);
                logger.debug("Found extracted text in database for document {}", id);
            }
            
            // 2. Fallback to Elasticsearch if database doesn't have text
            if (ocrText == null || ocrText.trim().isEmpty()) {
                try {
                    Optional<DocumentIndex> indexOpt = documentIndexRepository.findById(id.toString());
                    if (indexOpt.isPresent()) {
                        DocumentIndex index = indexOpt.get();
                        String esOcrText = index.getExtractedText() != null ? index.getExtractedText() : "";
                        if (!esOcrText.trim().isEmpty()) {
                            ocrText = esOcrText;
                            ocrConfidence = index.getOcrConfidence() != null ? index.getOcrConfidence() : 0.0;
                            logger.debug("Found extracted text in Elasticsearch for document {}", id);
                        }
                        
                        // Check if there's an OCR error in metadata
                        if ((ocrText == null || ocrText.isEmpty()) && index.getMetadata() != null) {
                            String ocrError = index.getMetadata().get("ocrError");
                            if (ocrError != null && !ocrError.isEmpty()) {
                                response.put("ocrError", ocrError);
                                response.put("ocrProcessing", false); // Not processing, it failed
                            } else {
                                response.put("ocrProcessing", true); // Still processing or no text
                            }
                        } else if (ocrText != null && !ocrText.isEmpty()) {
                            response.put("ocrProcessing", false);
                        } else {
                            response.put("ocrProcessing", true);
                        }
                    } else {
                        // Document not yet indexed (OCR may still be processing)
                        response.put("ocrProcessing", true);
                    }
                } catch (Exception e) {
                    // Elasticsearch might be unavailable or document not indexed yet
                    logger.debug("Elasticsearch unavailable or document not indexed for document {}: {}", id, e.getMessage());
                    if (ocrText == null || ocrText.trim().isEmpty()) {
                        response.put("ocrProcessing", true);
                    }
                }
            }
            
            // Set OCR text and confidence in response
            response.put("ocrText", ocrText != null ? ocrText : "");
            response.put("ocrConfidence", ocrConfidence);
            
            // Add extractedText to the response for frontend compatibility
            // Since @JsonIgnore on Document entity excludes it, we add it separately
            response.put("extractedText", dbExtractedText != null ? dbExtractedText : "");

            // Get document type fields and their values
            if (document.getDocumentType() != null) {
                try {
                    List<DocumentTypeField> typeFields = documentTypeFieldService.getFieldsForDocumentType(document.getDocumentType());
                    Map<String, String> metadataMap = documentMetadataService.getMetadataMap(document);
                    
                    // Create a list of fields with their current values
                    List<Map<String, Object>> fieldsWithValues = new ArrayList<>();
                    for (DocumentTypeField field : typeFields) {
                        Map<String, Object> fieldData = new HashMap<>();
                        fieldData.put("id", field.getId());
                        fieldData.put("fieldKey", field.getFieldKey());
                        fieldData.put("fieldLabel", field.getFieldLabel());
                        fieldData.put("fieldType", field.getFieldType());
                        fieldData.put("isRequired", field.getIsRequired());
                        fieldData.put("defaultValue", field.getDefaultValue());
                        fieldData.put("fieldOptions", field.getFieldOptions());
                        fieldData.put("displayOrder", field.getDisplayOrder());
                        fieldData.put("description", field.getDescription());
                        // Get current value from metadata
                        fieldData.put("value", metadataMap.getOrDefault(field.getFieldKey(), field.getDefaultValue()));
                        fieldsWithValues.add(fieldData);
                    }
                    response.put("typeFields", fieldsWithValues);
                    // Also include raw metadata map for direct access (useful for bill fields with confidence scores)
                    response.put("metadata", metadataMap);
                } catch (Exception e) {
                    // If service is not available, just continue without type fields
                    response.put("typeFields", Collections.emptyList());
                    response.put("metadata", Collections.emptyMap());
                }
            } else {
                response.put("typeFields", Collections.emptyList());
                try {
                    Map<String, String> metadataMap = documentMetadataService.getMetadataMap(document);
                    response.put("metadata", metadataMap);
                } catch (Exception e) {
                    response.put("metadata", Collections.emptyMap());
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}/metadata")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Map<String, Object>> updateDocumentMetadata(
            @PathVariable Long id,
            @RequestBody Map<String, String> metadata) {
        try {
            Optional<Document> documentOpt = documentRepository.findById(id);
            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Document document = documentOpt.get();
            Map<String, String> updatedMetadata = documentMetadataService.applyManualMetadata(document, metadata);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("metadata", updatedMetadata);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}/folder")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Map<String, Object>> updateDocumentFolder(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Optional<Document> documentOpt = documentRepository.findById(id);
            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Document document = documentOpt.get();
            Long folderId = request.get("folderId") != null ? 
                (request.get("folderId") instanceof Number ? 
                    ((Number) request.get("folderId")).longValue() : 
                    Long.parseLong(request.get("folderId").toString())) : null;

            if (folderId != null) {
                Optional<Folder> folderOpt = folderRepository.findById(folderId);
                if (folderOpt.isEmpty()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("error", "Folder not found");
                    return ResponseEntity.badRequest().body(response);
                }
                document.setFolder(folderOpt.get());
            } else {
                document.setFolder(null);
            }

            documentRepository.save(document);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document folder updated successfully");
            response.put("folderId", folderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}/ocr")
    public ResponseEntity<Map<String, Object>> getDocumentOCR(@PathVariable Long id) {
        try {
            Optional<DocumentIndex> indexOpt = documentIndexRepository.findById(id.toString());
            if (indexOpt.isEmpty()) {
                // Document not yet indexed (OCR may still be processing)
                Map<String, Object> response = new HashMap<>();
                response.put("ocrText", "");
                response.put("ocrConfidence", 0.0);
                response.put("documentId", id);
                response.put("ocrProcessing", true);
                return ResponseEntity.ok(response);
            }

            DocumentIndex index = indexOpt.get();
            Map<String, Object> response = new HashMap<>();
            String ocrText = index.getExtractedText() != null ? index.getExtractedText() : "";
            response.put("ocrText", ocrText);
            response.put("ocrConfidence", index.getOcrConfidence() != null ? index.getOcrConfidence() : 0.0);
            response.put("documentId", index.getDocumentId());
            
            // Check if there's an OCR error in metadata
            if (ocrText.isEmpty() && index.getMetadata() != null) {
                String ocrError = index.getMetadata().get("ocrError");
                if (ocrError != null && !ocrError.isEmpty()) {
                    response.put("ocrError", ocrError);
                    response.put("ocrProcessing", false); // Not processing, it failed
                } else {
                    response.put("ocrProcessing", true); // Still processing
                }
            } else if (ocrText.isEmpty()) {
                response.put("ocrProcessing", true);
            } else {
                response.put("ocrProcessing", false);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Elasticsearch might be unavailable
            Map<String, Object> response = new HashMap<>();
            response.put("ocrText", "");
            response.put("ocrConfidence", 0.0);
            response.put("documentId", id);
            response.put("ocrProcessing", true);
            response.put("error", "Failed to retrieve OCR data");
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<FileUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "metadata", required = false) String metadataJson,
            @RequestParam(value = "folderId", required = false) Long folderId,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            documentCategoryService.ensureCategoryExists(documentType);

            Map<String, String> metadata = parseMetadata(metadataJson);

            FileUploadResponse resp = fileUploadService.uploadFile(file, user, documentType, description, metadata, folderId);
            if (!resp.isSuccess()) {
                return ResponseEntity.badRequest().body(resp);
            }
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(FileUploadResponse.error(ex.getMessage()));
        }
    }

    private Map<String, String> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
            Map<String, String> normalized = new HashMap<>();
            raw.forEach((key, value) -> {
                if (key != null && value != null) {
                    normalized.put(key, value.toString());
                }
            });
            return normalized;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid metadata payload: " + ex.getMessage());
        }
    }

    @GetMapping("/types")
    public ResponseEntity<List<Map<String, String>>> getDocumentTypes() {
        List<Map<String, String>> types = java.util.Arrays.stream(DocumentType.values())
                .map(dt -> java.util.Map.of("value", dt.name(), "label", dt.getLabel()))
                .toList();
        return ResponseEntity.ok(types);
    }

    @PostMapping("/{id}/extract-metadata")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_EDIT')")
    public ResponseEntity<Map<String, Object>> extractMetadata(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            Optional<Document> documentOpt = documentRepository.findById(id);
            if (documentOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Document not found");
                return ResponseEntity.notFound().build();
            }

            if (databaseMetadataExtractionService == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Database metadata extraction service not available");
                return ResponseEntity.internalServerError().body(error);
            }

            Document document = documentOpt.get();
            if (document.getExtractedText() == null || document.getExtractedText().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No extracted text found for document. Please run OCR first.");
                return ResponseEntity.badRequest().body(error);
            }

            // Get metadata before extraction
            Map<String, String> metadataBefore = documentMetadataService.getMetadataMap(document);
            String procurementDescBefore = metadataBefore.get("procurementDescription");

            // Trigger metadata extraction
            logger.info("Manual metadata extraction triggered for document {}", id);
            databaseMetadataExtractionService.extractMetadataForDocument(id);

            // Reload document to get updated metadata
            document = documentRepository.findById(id).orElse(null);
            if (document == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Document not found after extraction");
                return ResponseEntity.internalServerError().body(error);
            }

            // Get updated metadata
            Map<String, String> metadataAfter = documentMetadataService.getMetadataMap(document);
            String procurementDescAfter = metadataAfter.get("procurementDescription");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Metadata extraction completed");
            response.put("metadata", metadataAfter);
            response.put("documentId", id);
            response.put("documentType", document.getDocumentType());
            
            // Add diagnostic information
            Map<String, Object> diagnostics = new HashMap<>();
            diagnostics.put("extractedTextLength", document.getExtractedText() != null ? document.getExtractedText().length() : 0);
            diagnostics.put("procurementDescriptionBefore", procurementDescBefore != null ? "EXISTS (" + procurementDescBefore.length() + " chars)" : "NULL");
            diagnostics.put("procurementDescriptionAfter", procurementDescAfter != null ? "EXISTS (" + procurementDescAfter.length() + " chars)" : "NULL");
            diagnostics.put("procurementDescriptionExtracted", procurementDescAfter != null && !procurementDescAfter.trim().isEmpty());
            if (procurementDescAfter != null && !procurementDescAfter.trim().isEmpty()) {
                diagnostics.put("procurementDescriptionPreview", 
                    procurementDescAfter.length() > 300 ? procurementDescAfter.substring(0, 300) + "..." : procurementDescAfter);
            }
            response.put("diagnostics", diagnostics);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error extracting metadata for document {}: {}", id, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("stackTrace", e.getStackTrace());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/{id}/reprocess-ocr")
    public ResponseEntity<Map<String, Object>> reprocessOCR(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            Optional<Document> documentOpt = documentRepository.findById(id);
            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Trigger OCR re-processing asynchronously
            fileUploadService.reprocessOCR(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "OCR re-processing started for document: " + id);
            response.put("documentId", id);
            response.put("status", "processing");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to start OCR re-processing: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/reprocess-ocr/all")
    public ResponseEntity<Map<String, Object>> reprocessAllDocumentsOCR(
            Authentication authentication
    ) {
        try {
            // Trigger OCR re-processing for all documents asynchronously
            fileUploadService.reprocessAllDocumentsOCR();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "OCR re-processing started for all documents");
            response.put("status", "processing");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to start OCR re-processing: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/upload-duplicate")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<FileUploadResponse> handleDuplicateUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam("duplicateDocumentId") Long duplicateDocumentId,
            @RequestParam("action") String action, // "skip", "version", or "replace"
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "metadata", required = false) String metadataJson,
            @RequestParam(value = "folderId", required = false) Long folderId,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            Map<String, String> metadata = parseMetadata(metadataJson);

            FileUploadResponse resp = fileUploadService.handleDuplicateUpload(
                file, user, documentType, description, metadata, folderId, duplicateDocumentId, action
            );
            
            if (!resp.isSuccess()) {
                return ResponseEntity.badRequest().body(resp);
            }
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FileUploadResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_ARCHIVE')")
    public ResponseEntity<Document> archiveDocument(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            Document archived = documentArchiveService.archiveDocument(id, user);
            return ResponseEntity.ok(archived);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/restore-archive")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_ARCHIVE')")
    public ResponseEntity<Document> restoreArchivedDocument(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            Document restored = documentArchiveService.restoreArchivedDocument(id, user);
            return ResponseEntity.ok(restored);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_DELETE')")
    public ResponseEntity<Document> deleteDocument(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            Document deleted = documentArchiveService.deleteDocument(id, user);
            return ResponseEntity.ok(deleted);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/restore-delete")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_DELETE')")
    public ResponseEntity<Document> restoreDeletedDocument(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            Document restored = documentArchiveService.restoreDeletedDocument(id, user);
            return ResponseEntity.ok(restored);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/archived")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Page<Document>> getArchivedDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "archivedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(documentArchiveService.getArchivedDocuments(pageable));
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Page<Document>> getDeletedDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "deletedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(documentArchiveService.getDeletedDocuments(pageable));
    }

    @GetMapping("/archive/statistics")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<DocumentArchiveService.ArchiveStatistics> getArchiveStatistics() {
        return ResponseEntity.ok(documentArchiveService.getArchiveStatistics());
    }

    @PostMapping("/archive/batch")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_ARCHIVE')")
    public ResponseEntity<List<Document>> archiveDocuments(
            @RequestBody List<Long> documentIds,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            List<Document> archived = documentArchiveService.archiveDocuments(documentIds, user);
            return ResponseEntity.ok(archived);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/restore-archive/batch")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_ARCHIVE')")
    public ResponseEntity<List<Document>> restoreArchivedDocuments(
            @RequestBody List<Long> documentIds,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            List<Document> restored = documentArchiveService.restoreArchivedDocuments(documentIds, user);
            return ResponseEntity.ok(restored);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/restore-delete/batch")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_DELETE')")
    public ResponseEntity<List<Document>> restoreDeletedDocuments(
            @RequestBody List<Long> documentIds,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            List<Document> restored = documentArchiveService.restoreDeletedDocuments(documentIds, user);
            return ResponseEntity.ok(restored);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/assign-stationery")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_EDIT')")
    public ResponseEntity<Document> assignStationeryToEmployee(
            @PathVariable Long id,
            @RequestParam("employeeId") Long employeeId,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            Document assigned = stationeryTrackingService.assignStationeryToEmployee(id, employeeId, user);
            return ResponseEntity.ok(assigned);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/unassign-stationery")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_EDIT')")
    public ResponseEntity<Document> unassignStationeryFromEmployee(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            Document unassigned = stationeryTrackingService.unassignStationeryFromEmployee(id, user);
            return ResponseEntity.ok(unassigned);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stationery")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Page<Document>> getStationeryRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(stationeryTrackingService.getAllStationeryRecords(pageable));
    }

    @GetMapping("/stationery/employee/{employeeId}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<Document>> getStationeryRecordsByEmployee(
            @PathVariable Long employeeId
    ) {
        return ResponseEntity.ok(stationeryTrackingService.getStationeryRecordsByEmployee(employeeId));
    }

    @GetMapping("/stationery/statistics")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<StationeryTrackingService.StationeryStatisticsSummary> getStationeryStatistics() {
        return ResponseEntity.ok(stationeryTrackingService.getStationeryStatisticsSummary());
    }

    @GetMapping("/stationery/statistics/employee")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<StationeryTrackingService.EmployeeStationeryStats>> getStationeryStatisticsPerEmployee() {
        return ResponseEntity.ok(stationeryTrackingService.getStationeryStatisticsPerEmployee());
    }

    @GetMapping("/{id}/app-entries")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<AppDocumentEntry>> getAppEntries(@PathVariable Long id) {
        try {
            Optional<Document> documentOpt = documentRepository.findById(id);
            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Document document = documentOpt.get();
            // Note: APP is no longer a document type. This endpoint supports legacy APP documents only.
            // For new APP entries, use the manual entry form (see AppEntryController).
            if (!"APP".equals(document.getDocumentType()) && !"OTHER".equals(document.getDocumentType())) {
                // Allow OTHER type as well for backward compatibility with migrated APP documents
                return ResponseEntity.badRequest().body(null);
            }

            List<AppDocumentEntry> entries = appDocumentEntryRepository.findByDocument(document);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Map<String, Object>> getDocumentStatistics() {
        try {
            long totalDocuments = documentRepository.count();
            long activeDocuments = documentRepository.countByIsActiveTrue();
            long archivedDocuments = documentRepository.countArchivedDocuments();
            long deletedDocuments = documentRepository.countDeletedDocuments();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDocuments", totalDocuments);
            stats.put("activeDocuments", activeDocuments);
            stats.put("archivedDocuments", archivedDocuments);
            stats.put("deletedDocuments", deletedDocuments);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/statistics/by-type")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Map<String, Long>> getDocumentStatisticsByType() {
        try {
            Map<String, Long> typeCounts = new HashMap<>();
            
            // Get all document types from categories
            List<String> documentTypes = documentCategoryService.getActiveCategoryNames();
            
            // Count documents for each type
            for (String documentType : documentTypes) {
                long count = documentRepository.countByDocumentType(documentType);
                if (count > 0) {
                    typeCounts.put(documentType, count);
                }
            }
            
            // Also check for any other document types that might exist in the database
            // but not in categories (to catch edge cases)
            List<Document> allDocuments = documentRepository.findAll();
            for (Document doc : allDocuments) {
                String docType = doc.getDocumentType();
                if (docType != null && !typeCounts.containsKey(docType) && !documentTypes.contains(docType)) {
                    long count = documentRepository.countByDocumentType(docType);
                    if (count > 0) {
                        typeCounts.put(docType, count);
                    }
                }
            }
            
            return ResponseEntity.ok(typeCounts);
        } catch (Exception e) {
            Map<String, Long> error = new HashMap<>();
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/statistics/tenders")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Map<String, Object>> getTenderStatistics() {
        try {
            // Get all tender documents
            List<Document> tenderNotices = documentRepository.findByDocumentType("TENDER_NOTICE", 
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();
            List<Document> tenderDocuments = documentRepository.findByDocumentType("TENDER_DOCUMENT", 
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();
            
            long totalTenders = tenderNotices.size() + tenderDocuments.size();
            
            // Count live and closed tenders based on closingDate metadata
            long liveTenders = 0;
            long closedTenders = 0;
            java.time.LocalDate today = java.time.LocalDate.now();
            
            for (Document doc : tenderNotices) {
                Map<String, String> metadata = documentMetadataService.getMetadataMap(doc);
                String closingDateStr = metadata.get("closingDate");
                if (closingDateStr != null && !closingDateStr.isEmpty()) {
                    try {
                        java.time.LocalDate closingDate = java.time.LocalDate.parse(closingDateStr);
                        if (closingDate.isAfter(today) || closingDate.isEqual(today)) {
                            liveTenders++;
                        } else {
                            closedTenders++;
                        }
                    } catch (Exception e) {
                        // If date parsing fails, count as live (assume it's active)
                        liveTenders++;
                    }
                } else {
                    // If no closing date, count as live
                    liveTenders++;
                }
            }
            
            for (Document doc : tenderDocuments) {
                Map<String, String> metadata = documentMetadataService.getMetadataMap(doc);
                String closingDateStr = metadata.get("closingDate");
                if (closingDateStr != null && !closingDateStr.isEmpty()) {
                    try {
                        java.time.LocalDate closingDate = java.time.LocalDate.parse(closingDateStr);
                        if (closingDate.isAfter(today) || closingDate.isEqual(today)) {
                            liveTenders++;
                        } else {
                            closedTenders++;
                        }
                    } catch (Exception e) {
                        // If date parsing fails, count as live
                        liveTenders++;
                    }
                } else {
                    // If no closing date, count as live
                    liveTenders++;
                }
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTenders", totalTenders);
            stats.put("liveTenders", liveTenders);
            stats.put("closedTenders", closedTenders);
            stats.put("draftTenders", Math.max(0, totalTenders - liveTenders - closedTenders));
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
