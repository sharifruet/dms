package com.bpdb.dms.procurement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.procurement.entity.ExtractedField;
import com.bpdb.dms.procurement.entity.ExtractedFieldHistory;
import com.bpdb.dms.procurement.repository.ExtractedFieldHistoryRepository;
import com.bpdb.dms.procurement.repository.ExtractedFieldRepository;

/**
 * The only component that writes captured values.
 *
 * Every value - OCR-read or hand-typed - lands here so that provenance, verification
 * state and history are recorded consistently. The raw text is never overwritten by a
 * correction; a correction updates the parsed value and appends to history
 * (REQ-P3, REQ-P4, REQ-P5).
 */
@Service
public class CaptureService {

    private static final Logger log = LoggerFactory.getLogger(CaptureService.class);

    /** Below this, an OCR value is never auto-accepted and must be reviewed (REQ-P7). */
    private static final BigDecimal CONFIDENCE_THRESHOLD = new BigDecimal("0.80");

    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("d MMMM yyyy"),
        DateTimeFormatter.ofPattern("dd MMM yyyy"),
    };

    private static final Pattern NUMBER = Pattern.compile("-?[0-9][0-9,]*(\\.[0-9]+)?");

    private final ExtractedFieldRepository fieldRepository;
    private final ExtractedFieldHistoryRepository historyRepository;
    private final StageDataService stageDataService;

    public CaptureService(ExtractedFieldRepository fieldRepository,
                          ExtractedFieldHistoryRepository historyRepository,
                          @Lazy StageDataService stageDataService) {
        this.fieldRepository = fieldRepository;
        this.historyRepository = historyRepository;
        this.stageDataService = stageDataService;
    }

    /**
     * Record a value read by OCR. Lands as OCR_SUGGESTED - it does not count towards
     * stage completion until a user confirms it.
     */
    @Transactional
    public ExtractedField captureFromOcr(CaptureRequest req) {
        ExtractedField field = findOrCreate(req);
        boolean ocrFoundSomething = req.rawValue != null && !req.rawValue.isBlank();

        // Two things a re-read must never do to a value that already exists.
        //
        // An OCR pass that found nothing must not erase what is there. Re-uploading a
        // document, or re-running OCR on a poor scan, would otherwise wipe good data and
        // report it as missing - which is precisely what happened the first time this was
        // driven end to end: four confirmed values were nulled by an extraction that read
        // nothing at all.
        if (!ocrFoundSomething && field.hasValue()) {
            field.setValidationMessage("A later OCR pass found no value for this field; "
                    + "the existing value has been kept");
            log.debug("OCR found nothing for {} on entity {} - keeping the existing value",
                    req.fieldKey, req.entityId);
            return fieldRepository.save(field);
        }

        // And a machine reading must not overrule a person. Once someone has confirmed a
        // value, OCR may disagree with it but may not replace it: the confirmed value
        // stands and the disagreement is flagged, so a misread cannot quietly undo a
        // review (REQ-X3 - the verify screen is where OCR becomes trustworthy data).
        if (isConfirmed(field)) {
            if (ocrFoundSomething && !sameAsCurrent(field, req.rawValue)) {
                field.setValidationState(ExtractedField.CONFLICT);
                field.setValidationMessage("OCR now reads '" + req.rawValue.trim()
                        + "' but this field was confirmed as '" + field.displayValue()
                        + "'. The confirmed value has been kept - check the document.");
                log.info("OCR conflict on {} (entity {}): confirmed '{}' vs OCR '{}'",
                        req.fieldKey, req.entityId, field.displayValue(), req.rawValue.trim());
            }
            return fieldRepository.save(field);
        }

        field.setCaptureSource(ExtractedField.SOURCE_OCR);
        field.setDocumentId(req.documentId);
        field.setOcrResultId(req.ocrResultId);
        field.setPageNo(req.pageNo);
        field.setBbox(req.bbox);
        field.setOcrConfidence(req.confidence);

        if (field.getRawValue() == null) {
            field.setRawValue(req.rawValue);
        }
        applyParsedValue(field, req.rawValue);

        if (!ocrFoundSomething) {
            // "missing" is a recorded fact, not an absent row (REQ-P8)
            field.setStatus(ExtractedField.OCR_SUGGESTED);
            field.setValidationState(ExtractedField.NOT_FOUND);
            field.setValidationMessage("OCR did not find a value for this field");
        } else if (req.confidence != null && req.confidence.compareTo(CONFIDENCE_THRESHOLD) < 0) {
            field.setStatus(ExtractedField.OCR_SUGGESTED);
            field.setValidationState(ExtractedField.NEEDS_REVIEW);
            field.setValidationMessage("Low OCR confidence - please check against the document");
        } else {
            field.setStatus(ExtractedField.OCR_SUGGESTED);
            field.setValidationState(ExtractedField.VALID);
            field.setValidationMessage(null);
        }
        return fieldRepository.save(field);
    }

    /** Has a person stood behind this value, by verifying it, correcting it or typing it? */
    private static boolean isConfirmed(ExtractedField field) {
        return ExtractedField.VERIFIED.equals(field.getStatus())
                || ExtractedField.MANUAL_OVERRIDE.equals(field.getStatus());
    }

    /** Compares a fresh reading with what the field holds, ignoring incidental spacing. */
    private static boolean sameAsCurrent(ExtractedField field, String rawValue) {
        String current = field.displayValue();
        if (current == null || rawValue == null) {
            return false;
        }
        return normalizeForComparison(current).equals(normalizeForComparison(rawValue));
    }

    private static String normalizeForComparison(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Record a value typed by a user. Manual entry is trusted, so it is confirmed on
     * arrival - there is nothing for the user to verify against.
     */
    @Transactional
    public ExtractedField captureManual(CaptureRequest req, Long userId) {
        ExtractedField field = findOrCreate(req);
        boolean existed = field.getId() != null;
        String before = existed ? field.displayValue() : null;
        String beforeStatus = field.getStatus();

        // Read the previous source before overwriting it, otherwise the check below is
        // asking whether MANUAL equals OCR and correcting an OCR reading is recorded as a
        // plain verification rather than an override
        boolean correctingAnOcrReading =
                existed && ExtractedField.SOURCE_OCR.equals(field.getCaptureSource());

        field.setCaptureSource(ExtractedField.SOURCE_MANUAL);
        if (field.getRawValue() == null) {
            field.setRawValue(req.rawValue);
        }
        applyParsedValue(field, req.rawValue);
        field.setStatus(correctingAnOcrReading
                ? ExtractedField.MANUAL_OVERRIDE : ExtractedField.VERIFIED);
        field.setValidationState(ExtractedField.VALID);
        field.setValidationMessage(null);
        field.setVerifiedBy(userId);
        field.setVerifiedAt(LocalDateTime.now());

        ExtractedField saved = fieldRepository.save(field);
        appendHistory(saved, before, saved.displayValue(), beforeStatus, saved.getStatus(),
                userId, req.reason == null ? "Manual entry" : req.reason);
        return saved;
    }

    /**
     * Record a value read out of an imported file — today, the APP workbook at Stage 1.
     *
     * Treated as confirmed on arrival, like manual entry and for the same reason: a
     * spreadsheet cell is read exactly, not guessed at, and the value is the client's own
     * approved plan rather than a machine's opinion of a scan. Asking someone to re-verify
     * 28 fields that were parsed deterministically would be busywork, and busywork is how
     * verification screens stop being read.
     *
     * The origin — which sheet and row it came from — is carried in the history reason, so
     * an imported value can still be traced back to the file it came from.
     */
    @Transactional
    public ExtractedField captureImported(CaptureRequest req, Long userId) {
        ExtractedField field = findOrCreate(req);
        boolean existed = field.getId() != null;
        String before = existed ? field.displayValue() : null;
        String beforeStatus = field.getStatus();

        field.setCaptureSource(ExtractedField.SOURCE_IMPORT);
        if (field.getRawValue() == null) {
            field.setRawValue(req.rawValue);
        }
        applyParsedValue(field, req.rawValue);
        field.setStatus(ExtractedField.VERIFIED);
        field.setValidationState(ExtractedField.VALID);
        field.setValidationMessage(null);
        field.setVerifiedBy(userId);
        field.setVerifiedAt(LocalDateTime.now());

        ExtractedField saved = fieldRepository.save(field);
        appendHistory(saved, before, saved.displayValue(), beforeStatus, saved.getStatus(),
                userId, req.reason == null ? "Imported" : req.reason);
        return saved;
    }

    /** Confirm an OCR suggestion as-is. */
    @Transactional
    public ExtractedField verify(Long fieldId, Long userId) {
        ExtractedField field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Field not found: " + fieldId));
        String beforeStatus = field.getStatus();
        field.setStatus(ExtractedField.VERIFIED);
        field.setValidationState(ExtractedField.VALID);
        field.setValidationMessage(null);
        field.setVerifiedBy(userId);
        field.setVerifiedAt(LocalDateTime.now());
        ExtractedField saved = fieldRepository.save(field);
        appendHistory(saved, saved.displayValue(), saved.displayValue(), beforeStatus,
                ExtractedField.VERIFIED, userId, "Verified against source document");
        return saved;
    }

    /**
     * Correct an OCR suggestion. The raw value stays exactly as it was read - only the
     * parsed value changes, and the change is appended to history (REQ-P5).
     */
    @Transactional
    public ExtractedField override(Long fieldId, String newValue, Long userId, String reason) {
        ExtractedField field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Field not found: " + fieldId));
        String before = field.displayValue();
        String beforeStatus = field.getStatus();

        applyParsedValue(field, newValue);
        field.setStatus(ExtractedField.MANUAL_OVERRIDE);
        field.setValidationState(ExtractedField.VALID);
        field.setValidationMessage(null);
        field.setVerifiedBy(userId);
        field.setVerifiedAt(LocalDateTime.now());

        ExtractedField saved = fieldRepository.save(field);
        appendHistory(saved, before, newValue, beforeStatus, ExtractedField.MANUAL_OVERRIDE,
                userId, reason == null ? "Corrected by user" : reason);
        // Keep the typed row (tender, evaluation, …) in step with the capture log.
        // Without this, a second edit on Captured values never reached Tender attempts.
        stageDataService.applyCapturedValueToEntity(saved, newValue);
        return saved;
    }

    @Transactional
    public ExtractedField reject(Long fieldId, Long userId, String reason) {
        ExtractedField field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Field not found: " + fieldId));
        String beforeStatus = field.getStatus();
        field.setStatus(ExtractedField.REJECTED);
        field.setValidationState(ExtractedField.INVALID);
        field.setValidationMessage(reason);
        ExtractedField saved = fieldRepository.save(field);
        appendHistory(saved, saved.displayValue(), null, beforeStatus, ExtractedField.REJECTED,
                userId, reason);
        return saved;
    }

    @Transactional
    public int verifyAll(Long packageId, Short stageCode, Long userId) {
        List<ExtractedField> pending = fieldRepository.findByPackageIdAndStageCode(packageId, stageCode);
        int count = 0;
        for (ExtractedField f : pending) {
            if (ExtractedField.OCR_SUGGESTED.equals(f.getStatus()) && f.hasValue()) {
                verify(f.getId(), userId);
                count++;
            }
        }
        return count;
    }

    public List<ExtractedFieldHistory> history(Long fieldId) {
        return historyRepository.findByExtractedFieldIdOrderByVersionDesc(fieldId);
    }

    // ---------------------------------------------------------------- internals

    private ExtractedField findOrCreate(CaptureRequest req) {
        Optional<ExtractedField> existing = req.entityId == null
                ? Optional.empty()
                : fieldRepository.findByEntityTypeAndEntityIdAndFieldKey(
                        req.entityType, req.entityId, req.fieldKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        ExtractedField f = new ExtractedField();
        f.setEntityType(req.entityType);
        f.setEntityId(req.entityId);
        f.setPackageId(req.packageId);
        f.setStageCode(req.stageCode);
        f.setFieldKey(req.fieldKey);
        f.setFieldLabel(req.fieldLabel);
        f.setDataType(req.dataType == null ? "TEXT" : req.dataType);
        f.setIsMandatory(req.mandatory);
        f.setCurrency(req.currency);
        f.setUnit(req.unit);
        return f;
    }

    /** Parse the text into the typed column that matches the field's data type (REQ-P4). */
    private void applyParsedValue(ExtractedField field, String value) {
        field.setTextValue(null);
        field.setNumericValue(null);
        field.setDateValue(null);
        field.setBoolValue(null);
        if (value == null || value.isBlank()) {
            return;
        }
        String trimmed = value.trim();
        switch (field.getDataType() == null ? "TEXT" : field.getDataType().toUpperCase()) {
            case "NUMBER":
            case "CURRENCY":
                BigDecimal n = parseNumber(trimmed);
                if (n != null) {
                    field.setNumericValue(n);
                    field.setTextValue(trimmed);
                } else {
                    field.setTextValue(trimmed);
                    field.setValidationState(ExtractedField.NEEDS_REVIEW);
                    field.setValidationMessage("Could not read a number from \"" + trimmed + "\"");
                }
                break;
            case "DATE":
                LocalDate d = parseDate(trimmed);
                if (d != null) {
                    field.setDateValue(d);
                    field.setTextValue(trimmed);
                } else {
                    field.setTextValue(trimmed);
                    field.setValidationState(ExtractedField.NEEDS_REVIEW);
                    field.setValidationMessage("Could not read a date from \"" + trimmed + "\"");
                }
                break;
            case "BOOL":
                field.setBoolValue(parseBool(trimmed));
                field.setTextValue(trimmed);
                break;
            default:
                field.setTextValue(trimmed);
        }
    }

    private BigDecimal parseNumber(String s) {
        Matcher m = NUMBER.matcher(s.replace(" ", ""));
        if (!m.find()) {
            return null;
        }
        try {
            return new BigDecimal(m.group().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(s, fmt);
            } catch (Exception ignored) {
                // try the next pattern
            }
        }
        return null;
    }

    private Boolean parseBool(String s) {
        String v = s.trim().toLowerCase();
        return v.startsWith("y") || v.startsWith("t") || v.equals("1") || v.startsWith("responsive");
    }

    private void appendHistory(ExtractedField field, String oldValue, String newValue,
                               String oldStatus, String newStatus, Long userId, String reason) {
        long version = historyRepository.countByExtractedFieldId(field.getId()) + 1;
        ExtractedFieldHistory h = new ExtractedFieldHistory();
        h.setExtractedFieldId(field.getId());
        h.setVersion((int) version);
        h.setOldValue(oldValue);
        h.setNewValue(newValue);
        h.setOldStatus(oldStatus);
        h.setNewStatus(newStatus);
        h.setChangedBy(userId);
        h.setChangedAt(LocalDateTime.now());
        h.setChangeReason(reason);
        historyRepository.save(h);
        log.debug("Field {} {} -> {} by user {}", field.getFieldKey(), oldStatus, newStatus, userId);
    }

    /** Everything needed to record one captured value. */
    public static class CaptureRequest {
        public String entityType;
        public Long entityId;
        public Long packageId;
        public Short stageCode;
        public String fieldKey;
        public String fieldLabel;
        public String dataType;
        public String rawValue;
        public String currency;
        public String unit;
        public Boolean mandatory = Boolean.FALSE;
        public Long documentId;
        public Long ocrResultId;
        public Integer pageNo;
        public String bbox;
        public BigDecimal confidence;
        public String reason;
    }
}
