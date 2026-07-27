package com.bpdb.dms.controller;

import com.bpdb.dms.dto.BillDocumentDto;
import com.bpdb.dms.service.BillDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Bill document type-specific records.
 * Separate from finance billing endpoints in {@link FinanceController}.
 */
@RestController
@RequestMapping("/api/bill-documents")
@CrossOrigin(origins = "*")
public class BillDocumentController {

    private final BillDocumentService billDocumentService;

    public BillDocumentController(BillDocumentService billDocumentService) {
        this.billDocumentService = billDocumentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<BillDocumentDto>> listAll() {
        return ResponseEntity.ok(billDocumentService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<BillDocumentDto> getById(@PathVariable Long id) {
        return billDocumentService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<BillDocumentDto> getByDocumentId(@PathVariable Long documentId) {
        return billDocumentService.findByDocumentId(documentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<BillDocumentDto> create(@RequestBody BillDocumentDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(billDocumentService.create(dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<BillDocumentDto> update(@PathVariable Long id, @RequestBody BillDocumentDto dto) {
        try {
            return ResponseEntity.ok(billDocumentService.update(id, dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            billDocumentService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}
