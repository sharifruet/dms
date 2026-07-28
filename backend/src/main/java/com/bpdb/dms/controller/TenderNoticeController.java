package com.bpdb.dms.controller;

import com.bpdb.dms.dto.TenderNoticeDto;
import com.bpdb.dms.service.TenderNoticeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Tender Notice type-specific records.
 */
@RestController
@RequestMapping("/api/tender-notices")
@CrossOrigin(origins = "*")
public class TenderNoticeController {

    private final TenderNoticeService tenderNoticeService;

    public TenderNoticeController(TenderNoticeService tenderNoticeService) {
        this.tenderNoticeService = tenderNoticeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<TenderNoticeDto>> listAll() {
        return ResponseEntity.ok(tenderNoticeService.findAll());
    }

    @GetMapping("/live/count")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Long> getLiveTenderCount() {
        return ResponseEntity.ok(tenderNoticeService.countLiveTenders());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<TenderNoticeDto> getById(@PathVariable Long id) {
        return tenderNoticeService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<TenderNoticeDto> getByDocumentId(@PathVariable Long documentId) {
        return tenderNoticeService.findByDocumentId(documentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<TenderNoticeDto> create(@RequestBody TenderNoticeDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(tenderNoticeService.create(dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<TenderNoticeDto> update(@PathVariable Long id, @RequestBody TenderNoticeDto dto) {
        try {
            return ResponseEntity.ok(tenderNoticeService.update(id, dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            tenderNoticeService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}
