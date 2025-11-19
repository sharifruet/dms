package com.bpdb.dms.controller;

import com.bpdb.dms.entity.DocumentTypeField;
import com.bpdb.dms.service.DocumentTypeFieldService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/document-type-fields")
@CrossOrigin(origins = "*")
public class DocumentTypeFieldController {

    private final DocumentTypeFieldService fieldService;

    public DocumentTypeFieldController(DocumentTypeFieldService fieldService) {
        this.fieldService = fieldService;
    }

    /**
     * Get all fields for a document type
     */
    @GetMapping("/{documentType}")
    public ResponseEntity<List<DocumentTypeField>> getFieldsForDocumentType(
            @PathVariable String documentType) {
        List<DocumentTypeField> fields = fieldService.getFieldsForDocumentType(documentType);
        return ResponseEntity.ok(fields);
    }

    /**
     * Get all fields (including inactive) for a document type
     */
    @GetMapping("/{documentType}/all")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<List<DocumentTypeField>> getAllFieldsForDocumentType(
            @PathVariable String documentType) {
        List<DocumentTypeField> fields = fieldService.getAllFieldsForDocumentType(documentType);
        return ResponseEntity.ok(fields);
    }

    /**
     * Create a new field configuration
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<DocumentTypeField> createField(@RequestBody DocumentTypeField field) {
        DocumentTypeField saved = fieldService.saveField(field);
        return ResponseEntity.ok(saved);
    }

    /**
     * Update an existing field configuration
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<DocumentTypeField> updateField(
            @PathVariable Long id,
            @RequestBody DocumentTypeField field) {
        field.setId(id);
        DocumentTypeField updated = fieldService.saveField(field);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a field configuration
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<Void> deleteField(@PathVariable Long id) {
        fieldService.deleteField(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deactivate a field configuration
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<DocumentTypeField> deactivateField(@PathVariable Long id) {
        DocumentTypeField field = fieldService.deactivateField(id);
        return ResponseEntity.ok(field);
    }

    /**
     * Map OCR data to fields for a document type
     */
    @PostMapping("/{documentType}/map-ocr")
    public ResponseEntity<Map<String, String>> mapOcrData(
            @PathVariable String documentType,
            @RequestBody Map<String, String> request) {
        String ocrText = request.get("ocrText");
        Map<String, String> mapped = fieldService.mapOcrDataToFields(documentType, ocrText);
        return ResponseEntity.ok(mapped);
    }

    /**
     * Validate field values against field configurations
     */
    @PostMapping("/{documentType}/validate")
    public ResponseEntity<Map<String, Object>> validateFields(
            @PathVariable String documentType,
            @RequestBody Map<String, String> fieldValues) {
        try {
            fieldService.validateFieldValues(documentType, fieldValues);
            return ResponseEntity.ok(Map.of("valid", true, "message", "Validation passed"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("valid", false, "message", e.getMessage()));
        }
    }
}

