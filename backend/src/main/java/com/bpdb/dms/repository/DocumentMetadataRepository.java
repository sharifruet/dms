package com.bpdb.dms.repository;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Long> {

    List<DocumentMetadata> findByDocument(Document document);

    void deleteByDocument(Document document);
}

