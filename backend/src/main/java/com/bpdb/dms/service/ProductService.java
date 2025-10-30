package com.bpdb.dms.service;

import com.bpdb.dms.entity.Product;
import com.bpdb.dms.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public Page<Product> list(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Optional<Product> get(Long id) {
        return productRepository.findById(id);
    }

    public Product create(Product product) {
        return productRepository.save(product);
    }

    public Product update(Long id, Product updated) {
        updated.setId(id);
        return productRepository.save(updated);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
