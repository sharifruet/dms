package com.bpdb.dms.repository;

import com.bpdb.dms.entity.DocumentTypeField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentTypeFieldRepository extends JpaRepository<DocumentTypeField, Long> {
    
    List<DocumentTypeField> findByDocumentTypeAndIsActiveTrueOrderByDisplayOrderAsc(String documentType);
    
    List<DocumentTypeField> findByDocumentTypeOrderByDisplayOrderAsc(String documentType);
    
    Optional<DocumentTypeField> findByDocumentTypeAndFieldKey(String documentType, String fieldKey);
    
    List<DocumentTypeField> findByIsActiveTrueOrderByDocumentTypeAscDisplayOrderAsc();
    
    boolean existsByDocumentTypeAndFieldKey(String documentType, String fieldKey);
}

