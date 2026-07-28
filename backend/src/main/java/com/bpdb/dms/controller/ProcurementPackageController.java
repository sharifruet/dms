package com.bpdb.dms.controller;

import com.bpdb.dms.entity.ProcurementPackage;
import com.bpdb.dms.service.ProcurementPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/procurement-packages")
@CrossOrigin(origins = "*")
public class ProcurementPackageController {

    @Autowired
    private ProcurementPackageService procurementPackageService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<Page<ProcurementPackage>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(procurementPackageService.list(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<ProcurementPackage> get(@PathVariable Long id) {
        Optional<ProcurementPackage> procurementPackage = procurementPackageService.get(id);
        return procurementPackage.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<ProcurementPackage> create(@RequestBody ProcurementPackage procurementPackage) {
        return ResponseEntity.ok(procurementPackageService.create(procurementPackage));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<ProcurementPackage> update(
            @PathVariable Long id,
            @RequestBody ProcurementPackage procurementPackage
    ) {
        return ResponseEntity.ok(procurementPackageService.update(id, procurementPackage));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        procurementPackageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
