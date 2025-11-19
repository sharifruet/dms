package com.bpdb.dms.repository;

import com.bpdb.dms.entity.AppDocumentEntry;
import com.bpdb.dms.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppDocumentEntryRepository extends JpaRepository<AppDocumentEntry, Long> {

    List<AppDocumentEntry> findByDocument(Document document);

    void deleteByDocument(Document document);
}

