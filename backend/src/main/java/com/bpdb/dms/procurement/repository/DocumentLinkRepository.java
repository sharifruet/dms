package com.bpdb.dms.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bpdb.dms.procurement.entity.DocumentLink;

@Repository
public interface DocumentLinkRepository extends JpaRepository<DocumentLink, Long> {

    List<DocumentLink> findByPackageId(Long packageId);

    List<DocumentLink> findByPackageIdAndStageCode(Long packageId, Short stageCode);

    List<DocumentLink> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<DocumentLink> findByDocumentId(Long documentId);
}
