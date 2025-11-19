package com.bpdb.dms.controller;

import com.bpdb.dms.entity.DocumentCategory;
import com.bpdb.dms.service.DocumentCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/document-categories")
@CrossOrigin(origins = "*")
public class DocumentCategoryController {

    private final DocumentCategoryService documentCategoryService;

    public DocumentCategoryController(DocumentCategoryService documentCategoryService) {
        this.documentCategoryService = documentCategoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<DocumentCategory>> listActiveCategories() {
        return ResponseEntity.ok(documentCategoryService.getActiveCategories());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<DocumentCategory> createCategory(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        String displayName = (String) payload.get("displayName");
        String description = (String) payload.get("description");
        Boolean isActive = payload.containsKey("isActive") ? (Boolean) payload.get("isActive") : Boolean.TRUE;

        DocumentCategory category = documentCategoryService.createCategory(name, displayName, description, isActive);
        return ResponseEntity.ok(category);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGEMENT')")
    public ResponseEntity<DocumentCategory> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        DocumentCategory category = documentCategoryService.updateCategory(id, updates);
        return ResponseEntity.ok(category);
    }
}

