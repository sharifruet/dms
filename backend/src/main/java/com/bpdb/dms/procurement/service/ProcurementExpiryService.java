package com.bpdb.dms.procurement.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.ExpiryStatus;
import com.bpdb.dms.entity.ExpiryTracking;
import com.bpdb.dms.entity.ExpiryType;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.ExpiryTrackingRepository;

/**
 * Registers and maintains the expiry trackers listed in requirements section 6.
 *
 * A tracker is created automatically when the field that carries the date is verified
 * (REQ-E1). Extensions and amendments supersede the old tracker rather than editing it,
 * so the previous expiry stays visible in history (REQ-E5).
 */
@Service
public class ProcurementExpiryService {

    private static final Logger log = LoggerFactory.getLogger(ProcurementExpiryService.class);

    /** Entity types that carry a tracked expiry, mapped to the matrix rows E-1..E-6. */
    public static final String TENDER = "TENDER";
    public static final String BID_SECURITY = "BID_SECURITY";
    public static final String PERFORMANCE_SECURITY = "PERFORMANCE_SECURITY";
    public static final String LETTER_OF_CREDIT = "LETTER_OF_CREDIT";
    public static final String CONTRACT = "CONTRACT";
    public static final String WARRANTY = "WARRANTY";

    private final ExpiryTrackingRepository expiryRepository;
    private final DocumentRepository documentRepository;

    public ProcurementExpiryService(ExpiryTrackingRepository expiryRepository,
                                    DocumentRepository documentRepository) {
        this.expiryRepository = expiryRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Register (or refresh) the tracker for an instrument. Idempotent per entity, so
     * re-verifying a field does not create a second tracker for the same instrument.
     */
    @Transactional
    public ExpiryTracking register(String entityType, Long entityId, Long packageId,
                                   Long documentId, LocalDate expiryDate, String note) {
        if (expiryDate == null) {
            return null;
        }
        ExpiryTracking existing = activeFor(entityType, entityId);
        ExpiryTracking tracker = existing == null ? new ExpiryTracking() : existing;

        tracker.setEntityType(entityType);
        tracker.setEntityId(entityId);
        tracker.setPackageId(packageId);
        tracker.setExpiryType(mapType(entityType));
        tracker.setExpiryDate(expiryDate.atStartOfDay());
        tracker.setStatus(ExpiryStatus.ACTIVE);
        tracker.setNotes(note);

        if (documentId != null) {
            Document doc = documentRepository.findById(documentId).orElse(null);
            if (doc != null) {
                tracker.setDocument(doc);
            }
        }
        if (tracker.getDocument() == null) {
            // the tracker table requires a document; without one there is nothing to guard
            log.warn("Skipping expiry tracker for {}#{} - no source document", entityType, entityId);
            return null;
        }
        return expiryRepository.save(tracker);
    }

    /**
     * Replace a tracker with a later one - an LC amendment, a PG extension, a tender
     * validity extension. The old row is kept and marked, never edited in place.
     */
    @Transactional
    public ExpiryTracking supersede(Long oldTrackerId, LocalDate newExpiryDate, String reason) {
        ExpiryTracking old = expiryRepository.findById(oldTrackerId)
                .orElseThrow(() -> new IllegalArgumentException("Tracker not found: " + oldTrackerId));

        ExpiryTracking replacement = new ExpiryTracking();
        replacement.setEntityType(old.getEntityType());
        replacement.setEntityId(old.getEntityId());
        replacement.setPackageId(old.getPackageId());
        replacement.setDocument(old.getDocument());
        replacement.setExpiryType(old.getExpiryType());
        replacement.setExpiryDate(newExpiryDate.atStartOfDay());
        replacement.setStatus(ExpiryStatus.ACTIVE);
        replacement.setNotes(reason);
        ExpiryTracking saved = expiryRepository.save(replacement);

        old.setStatus(ExpiryStatus.RENEWED);
        old.setSupersededById(saved.getId());
        old.setRenewalDate(LocalDateTime.now());
        expiryRepository.save(old);
        return saved;
    }

    /** Close every open tracker for a package - called at contract close (REQ-16.2). */
    @Transactional
    public int closeAllForPackage(Long packageId) {
        int closed = 0;
        for (ExpiryTracking t : expiryRepository.findAll()) {
            if (packageId.equals(t.getPackageId()) && t.getStatus() == ExpiryStatus.ACTIVE) {
                t.setStatus(ExpiryStatus.CANCELLED);
                t.setNotes("Closed automatically at contract close");
                expiryRepository.save(t);
                closed++;
            }
        }
        return closed;
    }

    public List<ExpiryTracking> forPackage(Long packageId) {
        List<ExpiryTracking> result = new ArrayList<>();
        for (ExpiryTracking t : expiryRepository.findAll()) {
            if (packageId.equals(t.getPackageId())) {
                result.add(t);
            }
        }
        return result;
    }

    /** Trackers expiring within the window, soonest first - the dashboard query. */
    public List<ExpiryTracking> expiringWithin(int days) {
        LocalDateTime limit = LocalDateTime.now().plusDays(days);
        List<ExpiryTracking> result = new ArrayList<>();
        for (ExpiryTracking t : expiryRepository.findAll()) {
            if (t.getStatus() == ExpiryStatus.ACTIVE && t.getExpiryDate() != null
                    && t.getExpiryDate().isBefore(limit)) {
                result.add(t);
            }
        }
        result.sort((a, b) -> a.getExpiryDate().compareTo(b.getExpiryDate()));
        return result;
    }

    private ExpiryTracking activeFor(String entityType, Long entityId) {
        for (ExpiryTracking t : expiryRepository.findAll()) {
            if (entityType.equals(t.getEntityType()) && entityId.equals(t.getEntityId())
                    && t.getStatus() == ExpiryStatus.ACTIVE) {
                return t;
            }
        }
        return null;
    }

    private ExpiryType mapType(String entityType) {
        return switch (entityType) {
            case TENDER -> ExpiryType.OTHER;
            case BID_SECURITY, PERFORMANCE_SECURITY -> ExpiryType.BANK_GUARANTEE;
            case LETTER_OF_CREDIT -> ExpiryType.LETTER_OF_CREDIT;
            case CONTRACT -> ExpiryType.CONTRACT;
            case WARRANTY -> ExpiryType.WARRANTY;
            default -> ExpiryType.OTHER;
        };
    }
}
