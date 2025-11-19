package com.bpdb.dms.controller;

import com.bpdb.dms.dto.FileUploadResponse;
import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentIndex;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.DocumentIndexRepository;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.DocumentCategoryService;
import com.bpdb.dms.service.FileUploadService;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

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
    private ObjectMapper objectMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Page<Document>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long folderId
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Document> result;
        if (folderId != null) {
            result = documentRepository.findByFolderId(folderId, pageable);
        } else {
            result = documentRepository.findAll(pageable);
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

            // Get OCR text from Elasticsearch (with error handling)
            try {
                Optional<DocumentIndex> indexOpt = documentIndexRepository.findById(id.toString());
                if (indexOpt.isPresent()) {
                    DocumentIndex index = indexOpt.get();
                    String ocrText = index.getExtractedText() != null ? index.getExtractedText() : "";
                    response.put("ocrText", ocrText);
                    response.put("ocrConfidence", index.getOcrConfidence() != null ? index.getOcrConfidence() : 0.0);
                    
                    // Check if there's an OCR error in metadata
                    if (ocrText.isEmpty() && index.getMetadata() != null) {
                        String ocrError = index.getMetadata().get("ocrError");
                        if (ocrError != null && !ocrError.isEmpty()) {
                            response.put("ocrError", ocrError);
                            response.put("ocrProcessing", false); // Not processing, it failed
                        } else {
                            response.put("ocrProcessing", true); // Still processing or no text
                        }
                    } else if (ocrText.isEmpty()) {
                        response.put("ocrProcessing", true);
                    } else {
                        response.put("ocrProcessing", false);
                    }
                } else {
                    // Document not yet indexed (OCR may still be processing)
                    response.put("ocrText", "");
                    response.put("ocrConfidence", 0.0);
                    response.put("ocrProcessing", true);
                }
            } catch (Exception e) {
                // Elasticsearch might be unavailable or document not indexed yet
                response.put("ocrText", "");
                response.put("ocrConfidence", 0.0);
                response.put("ocrProcessing", true);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
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
}
