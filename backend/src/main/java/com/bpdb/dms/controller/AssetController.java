package com.bpdb.dms.controller;

import com.bpdb.dms.entity.Asset;
import com.bpdb.dms.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "*")
public class AssetController {

    @Autowired
    private AssetService assetService;

    @GetMapping
    public ResponseEntity<Page<Asset>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(assetService.list(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Asset> get(@PathVariable Long id) {
        Optional<Asset> asset = assetService.get(id);
        return asset.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Asset> create(@RequestBody Asset asset) {
        return ResponseEntity.ok(assetService.create(asset));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Asset> update(@PathVariable Long id, @RequestBody Asset asset) {
        return ResponseEntity.ok(assetService.update(id, asset));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
