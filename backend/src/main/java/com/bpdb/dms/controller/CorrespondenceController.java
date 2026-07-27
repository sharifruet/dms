package com.bpdb.dms.controller;

import com.bpdb.dms.dto.CorrespondenceDto;
import com.bpdb.dms.service.CorrespondenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Correspondence type-specific records.
 */
@RestController
@RequestMapping("/api/correspondences")
@CrossOrigin(origins = "*")
public class CorrespondenceController {

    private final CorrespondenceService correspondenceService;

    public CorrespondenceController(CorrespondenceService correspondenceService) {
        this.correspondenceService = correspondenceService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<CorrespondenceDto>> listAll() {
        return ResponseEntity.ok(correspondenceService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<CorrespondenceDto> getById(@PathVariable Long id) {
        return correspondenceService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<CorrespondenceDto> getByDocumentId(@PathVariable Long documentId) {
        return correspondenceService.findByDocumentId(documentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<CorrespondenceDto> create(@RequestBody CorrespondenceDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(correspondenceService.create(dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<CorrespondenceDto> update(@PathVariable Long id, @RequestBody CorrespondenceDto dto) {
        try {
            return ResponseEntity.ok(correspondenceService.update(id, dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            correspondenceService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}
