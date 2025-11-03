package com.bpdb.dms.controller;

import com.bpdb.dms.entity.ProductCategory;
import com.bpdb.dms.service.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/product-categories")
@CrossOrigin(origins = "*")
public class ProductCategoryController {

    @Autowired
    private ProductCategoryService productCategoryService;

    @GetMapping
    public ResponseEntity<Page<ProductCategory>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(productCategoryService.list(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductCategory> get(@PathVariable Long id) {
        Optional<ProductCategory> category = productCategoryService.get(id);
        return category.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProductCategory> create(@RequestBody ProductCategory category) {
        return ResponseEntity.ok(productCategoryService.create(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductCategory> update(@PathVariable Long id, @RequestBody ProductCategory category) {
        return ResponseEntity.ok(productCategoryService.update(id, category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
