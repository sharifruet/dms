package com.bpdb.dms.controller;

import com.bpdb.dms.dto.PerformanceGuaranteeDto;
import com.bpdb.dms.service.PerformanceGuaranteeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Performance Guarantee type-specific records.
 */
@RestController
@RequestMapping("/api/performance-guarantees")
@CrossOrigin(origins = "*")
public class PerformanceGuaranteeController {

    private final PerformanceGuaranteeService performanceGuaranteeService;

    public PerformanceGuaranteeController(PerformanceGuaranteeService performanceGuaranteeService) {
        this.performanceGuaranteeService = performanceGuaranteeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<PerformanceGuaranteeDto>> listAll() {
        return ResponseEntity.ok(performanceGuaranteeService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<PerformanceGuaranteeDto> getById(@PathVariable Long id) {
        return performanceGuaranteeService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<PerformanceGuaranteeDto> getByDocumentId(@PathVariable Long documentId) {
        return performanceGuaranteeService.findByDocumentId(documentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<PerformanceGuaranteeDto> create(@RequestBody PerformanceGuaranteeDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(performanceGuaranteeService.create(dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<PerformanceGuaranteeDto> update(@PathVariable Long id, @RequestBody PerformanceGuaranteeDto dto) {
        try {
            return ResponseEntity.ok(performanceGuaranteeService.update(id, dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            performanceGuaranteeService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}
