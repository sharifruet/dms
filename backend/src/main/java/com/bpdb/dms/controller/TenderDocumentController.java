package com.bpdb.dms.controller;

import com.bpdb.dms.dto.TenderDocumentDto;
import com.bpdb.dms.service.TenderDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Tender Document type-specific records.
 */
@RestController
@RequestMapping("/api/tender-documents")
@CrossOrigin(origins = "*")
public class TenderDocumentController {

    private final TenderDocumentService tenderDocumentService;

    public TenderDocumentController(TenderDocumentService tenderDocumentService) {
        this.tenderDocumentService = tenderDocumentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<TenderDocumentDto>> listAll() {
        return ResponseEntity.ok(tenderDocumentService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<TenderDocumentDto> getById(@PathVariable Long id) {
        return tenderDocumentService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<TenderDocumentDto> getByDocumentId(@PathVariable Long documentId) {
        return tenderDocumentService.findByDocumentId(documentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<TenderDocumentDto> create(@RequestBody TenderDocumentDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(tenderDocumentService.create(dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<TenderDocumentDto> update(@PathVariable Long id, @RequestBody TenderDocumentDto dto) {
        try {
            return ResponseEntity.ok(tenderDocumentService.update(id, dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            tenderDocumentService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}
