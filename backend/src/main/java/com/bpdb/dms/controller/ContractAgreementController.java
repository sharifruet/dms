package com.bpdb.dms.controller;

import com.bpdb.dms.dto.ContractAgreementDto;
import com.bpdb.dms.service.ContractAgreementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Contract Agreement type-specific records.
 */
@RestController
@RequestMapping("/api/contract-agreements")
@CrossOrigin(origins = "*")
public class ContractAgreementController {

    private final ContractAgreementService contractAgreementService;

    public ContractAgreementController(ContractAgreementService contractAgreementService) {
        this.contractAgreementService = contractAgreementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<ContractAgreementDto>> listAll() {
        return ResponseEntity.ok(contractAgreementService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<ContractAgreementDto> getById(@PathVariable Long id) {
        return contractAgreementService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<ContractAgreementDto> getByDocumentId(@PathVariable Long documentId) {
        return contractAgreementService.findByDocumentId(documentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<ContractAgreementDto> create(@RequestBody ContractAgreementDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(contractAgreementService.create(dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<ContractAgreementDto> update(@PathVariable Long id, @RequestBody ContractAgreementDto dto) {
        try {
            return ResponseEntity.ok(contractAgreementService.update(id, dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            contractAgreementService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}
