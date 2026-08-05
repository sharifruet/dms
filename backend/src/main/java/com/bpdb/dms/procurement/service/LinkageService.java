package com.bpdb.dms.procurement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.procurement.entity.DocumentLink;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.DocumentLinkRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.repository.DocumentRepository;

/**
 * Keeps the document graph connected: attaches uploads to the record that owns them,
 * resolves parents from OCR keys where it can, and reports anything that ended up
 * orphaned (REQ-L1, L3, L4, L10, L12).
 */
@Service
public class LinkageService {

    private static final Logger log = LoggerFactory.getLogger(LinkageService.class);

    private final DocumentLinkRepository linkRepository;
    private final DocumentRepository documentRepository;
    private final ProcurementPackageRepository packageRepository;

    public LinkageService(DocumentLinkRepository linkRepository,
                          DocumentRepository documentRepository,
                          ProcurementPackageRepository packageRepository) {
        this.linkRepository = linkRepository;
        this.documentRepository = documentRepository;
        this.packageRepository = packageRepository;
    }

    /**
     * Link a document to the record it belongs to. The package context is also copied
     * onto the document row so search can return it without a join (REQ-L9).
     */
    @Transactional
    public DocumentLink link(Long documentId, String entityType, Long entityId, Long packageId,
                             Long contractId, short stageCode, String docRole,
                             String origin, Long userId) {
        List<DocumentLink> existing = linkRepository.findByDocumentId(documentId);
        for (DocumentLink l : existing) {
            if (l.getEntityType().equals(entityType)
                    && java.util.Objects.equals(l.getEntityId(), entityId)
                    && l.getDocRole().equals(docRole)) {
                return l;
            }
        }

        DocumentLink link = new DocumentLink();
        link.setDocumentId(documentId);
        link.setEntityType(entityType);
        link.setEntityId(entityId);
        link.setPackageId(packageId);
        link.setContractId(contractId);
        link.setStageCode(stageCode);
        link.setDocRole(docRole);
        link.setLinkOrigin(origin == null ? DocumentLink.STAGE_CONTEXT : origin);
        link.setCreatedBy(userId);
        DocumentLink saved = linkRepository.save(link);

        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setPackageId(packageId);
            doc.setContractId(contractId);
            doc.setStageCode(stageCode);
            documentRepository.save(doc);
        });

        log.debug("Linked document {} to {}#{} as {} ({})",
                documentId, entityType, entityId, docRole, link.getLinkOrigin());
        return saved;
    }

    /**
     * Resolve a package from a key read off a document. Returns empty rather than
     * inventing a package, so an unmatched key surfaces as an exception instead of
     * quietly creating a duplicate (REQ-L3).
     */
    public Optional<ProcurementPackage> resolvePackageByKey(String packageNumber) {
        if (packageNumber == null || packageNumber.isBlank()) {
            return Optional.empty();
        }
        return packageRepository.findByPackageNumber(packageNumber.trim());
    }

    public List<DocumentLink> documentsFor(Long packageId) {
        return linkRepository.findByPackageId(packageId);
    }

    public List<DocumentLink> documentsForStage(Long packageId, short stageCode) {
        return linkRepository.findByPackageIdAndStageCode(packageId, stageCode);
    }

    public List<DocumentLink> documentsForEntity(String entityType, Long entityId) {
        return linkRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Documents that never got attached to a package. These are not errors to hide -
     * they are work for someone to do, so they go on the exceptions dashboard.
     */
    public List<Document> orphanedDocuments() {
        List<Document> orphans = new ArrayList<>();
        for (Document doc : documentRepository.findAll()) {
            if (doc.getPackageId() == null && linkRepository.findByDocumentId(doc.getId()).isEmpty()) {
                orphans.add(doc);
            }
        }
        return orphans;
    }

    /** Links whose owning record no longer resolves - a broken edge in the graph. */
    public List<DocumentLink> brokenLinks() {
        List<DocumentLink> broken = new ArrayList<>();
        for (DocumentLink link : linkRepository.findAll()) {
            if (!packageRepository.existsById(link.getPackageId())
                    || documentRepository.findById(link.getDocumentId()).isEmpty()) {
                broken.add(link);
            }
        }
        return broken;
    }

    @Transactional
    public void unlink(Long linkId) {
        linkRepository.deleteById(linkId);
    }
}
