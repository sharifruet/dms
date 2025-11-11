package com.bpdb.dms.repository;

import com.bpdb.dms.entity.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Long> {

    Optional<DocumentCategory> findByNameIgnoreCase(String name);

    List<DocumentCategory> findAllByIsActiveTrueOrderByDisplayNameAsc();
}

