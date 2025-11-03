package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
