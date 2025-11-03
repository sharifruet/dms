package com.bpdb.dms.service;

import com.bpdb.dms.entity.ProductCategory;
import com.bpdb.dms.repository.ProductCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProductCategoryService {
    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    public Page<ProductCategory> list(Pageable pageable) {
        return productCategoryRepository.findAll(pageable);
    }

    public Optional<ProductCategory> get(Long id) {
        return productCategoryRepository.findById(id);
    }

    public ProductCategory create(ProductCategory category) {
        return productCategoryRepository.save(category);
    }

    public ProductCategory update(Long id, ProductCategory updated) {
        updated.setId(id);
        return productCategoryRepository.save(updated);
    }

    public void delete(Long id) {
        productCategoryRepository.deleteById(id);
    }
}
