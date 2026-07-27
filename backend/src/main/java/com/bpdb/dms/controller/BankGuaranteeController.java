package com.bpdb.dms.controller;

import com.bpdb.dms.dto.BankGuaranteeDto;
import com.bpdb.dms.service.BankGuaranteeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Bank Guarantee type-specific records.
 */
@RestController
@RequestMapping("/api/bank-guarantees")
@CrossOrigin(origins = "*")
public class BankGuaranteeController {

    private final BankGuaranteeService bankGuaranteeService;

    public BankGuaranteeController(BankGuaranteeService bankGuaranteeService) {
        this.bankGuaranteeService = bankGuaranteeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<BankGuaranteeDto>> listAll() {
        return ResponseEntity.ok(bankGuaranteeService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<BankGuaranteeDto> getById(@PathVariable Long id) {
        return bankGuaranteeService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<BankGuaranteeDto> getByDocumentId(@PathVariable Long documentId) {
        return bankGuaranteeService.findByDocumentId(documentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<BankGuaranteeDto> create(@RequestBody BankGuaranteeDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(bankGuaranteeService.create(dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<BankGuaranteeDto> update(@PathVariable Long id, @RequestBody BankGuaranteeDto dto) {
        try {
            return ResponseEntity.ok(bankGuaranteeService.update(id, dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            bankGuaranteeService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}
