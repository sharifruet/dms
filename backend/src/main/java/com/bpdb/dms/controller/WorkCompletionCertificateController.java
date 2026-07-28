package com.bpdb.dms.controller;

import com.bpdb.dms.dto.WorkCompletionCertificateDto;
import com.bpdb.dms.service.WorkCompletionCertificateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Work Completion Certificate type-specific records.
 */
@RestController
@RequestMapping("/api/work-completion-certificates")
@CrossOrigin(origins = "*")
public class WorkCompletionCertificateController {

    private final WorkCompletionCertificateService workCompletionCertificateService;

    public WorkCompletionCertificateController(WorkCompletionCertificateService workCompletionCertificateService) {
        this.workCompletionCertificateService = workCompletionCertificateService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<WorkCompletionCertificateDto>> listAll() {
        return ResponseEntity.ok(workCompletionCertificateService.findAll());
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Long> getTotalCount() {
        return ResponseEntity.ok(workCompletionCertificateService.count());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<WorkCompletionCertificateDto> getById(@PathVariable Long id) {
        return workCompletionCertificateService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<WorkCompletionCertificateDto> getByDocumentId(@PathVariable Long documentId) {
        return workCompletionCertificateService.findByDocumentId(documentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<WorkCompletionCertificateDto> create(@RequestBody WorkCompletionCertificateDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(workCompletionCertificateService.create(dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<WorkCompletionCertificateDto> update(@PathVariable Long id, @RequestBody WorkCompletionCertificateDto dto) {
        try {
            return ResponseEntity.ok(workCompletionCertificateService.update(id, dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            workCompletionCertificateService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}
