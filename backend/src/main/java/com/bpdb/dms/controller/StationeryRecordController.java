package com.bpdb.dms.controller;

import com.bpdb.dms.dto.StationeryRecordDto;
import com.bpdb.dms.service.StationeryRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Stationery Record type-specific records.
 */
@RestController
@RequestMapping("/api/stationery-records")
@CrossOrigin(origins = "*")
public class StationeryRecordController {

    private final StationeryRecordService stationeryRecordService;

    public StationeryRecordController(StationeryRecordService stationeryRecordService) {
        this.stationeryRecordService = stationeryRecordService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<StationeryRecordDto>> listAll() {
        return ResponseEntity.ok(stationeryRecordService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<StationeryRecordDto> getById(@PathVariable Long id) {
        return stationeryRecordService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<StationeryRecordDto> getByDocumentId(@PathVariable Long documentId) {
        return stationeryRecordService.findByDocumentId(documentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-employee/{employeeId}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_VIEW')")
    public ResponseEntity<List<StationeryRecordDto>> getByEmployeeId(@PathVariable Long employeeId) {
        return ResponseEntity.ok(stationeryRecordService.findByEmployeeId(employeeId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<StationeryRecordDto> create(@RequestBody StationeryRecordDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(stationeryRecordService.create(dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<StationeryRecordDto> update(@PathVariable Long id, @RequestBody StationeryRecordDto dto) {
        try {
            return ResponseEntity.ok(stationeryRecordService.update(id, dto));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DOCUMENT_UPLOAD')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            stationeryRecordService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}
