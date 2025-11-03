package com.bpdb.dms.controller;

import com.bpdb.dms.entity.AssetAssignment;
import com.bpdb.dms.service.AssetAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/asset-assignments")
@CrossOrigin(origins = "*")
public class AssetAssignmentController {

    @Autowired
    private AssetAssignmentService assetAssignmentService;

    @GetMapping
    public ResponseEntity<Page<AssetAssignment>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(assetAssignmentService.list(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetAssignment> get(@PathVariable Long id) {
        Optional<AssetAssignment> assignment = assetAssignmentService.get(id);
        return assignment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AssetAssignment> create(@RequestBody AssetAssignment assignment) {
        return ResponseEntity.ok(assetAssignmentService.create(assignment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetAssignment> update(@PathVariable Long id, @RequestBody AssetAssignment assignment) {
        return ResponseEntity.ok(assetAssignmentService.update(id, assignment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetAssignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
