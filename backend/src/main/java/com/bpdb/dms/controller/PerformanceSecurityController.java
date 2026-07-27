package com.bpdb.dms.controller;

import com.bpdb.dms.dto.PerformanceSecurityDto;
import com.bpdb.dms.service.PerformanceSecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Performance Security type-specific records.
 */
@RestController
@RequestMapping("/api/performance-securities")
@CrossOrigin(origins = "*")
public class PerformanceSecurityController {

    private final PerformanceSecurityService performanceSecurityService;

    public PerformanceSecurityController(PerformanceSecurityService performanceSecurityService) {
        this.performanceSecurityService = performanceSecurityService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<PerformanceSecurityDto>> listAll() {
        return ResponseEntity.ok(performanceSecurityService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<PerformanceSecurityDto> getById(@PathVariable Long id) {
        return performanceSecurityService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<PerformanceSecurityDto> getByDocumentId(@PathVariable Long documentId) {
        return performanceSecurityService.findByDocumentId(documentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<PerformanceSecurityDto> create(@RequestBody PerformanceSecurityDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(performanceSecurityService.create(dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<PerformanceSecurityDto> update(@PathVariable Long id, @RequestBody PerformanceSecurityDto dto) {
        try {
            return ResponseEntity.ok(performanceSecurityService.update(id, dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            performanceSecurityService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}
