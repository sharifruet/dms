package com.bpdb.dms.procurement.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bpdb.dms.entity.DocumentTypeField;
import com.bpdb.dms.procurement.entity.ExtractedField;
import com.bpdb.dms.procurement.entity.OcrJob;
import com.bpdb.dms.procurement.entity.OcrPage;
import com.bpdb.dms.procurement.entity.OcrResult;
import com.bpdb.dms.service.OCRService;
import com.bpdb.dms.procurement.repository.OcrJobRepository;
import com.bpdb.dms.procurement.repository.OcrPageRepository;
import com.bpdb.dms.procurement.repository.OcrResultRepository;

/**
 * Turns OCR text into reviewable candidate values.
 *
 * Two things matter here. First, the OCR output is persisted in full and versioned, so
 * re-running extraction never destroys what a previous run found or what a user
 * corrected (REQ-P9, P10). Second, the patterns live in the field catalogue rather than
 * in this class, so a new document type is configuration (REQ-P13).
 */
@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private final OcrJobRepository jobRepository;
    private final OcrResultRepository resultRepository;
    private final OcrPageRepository pageRepository;
    private final CaptureService captureService;
    private final StageDefinitionService definitions;
    private final StageDataService stageDataService;

    public ExtractionService(OcrJobRepository jobRepository,
                             OcrResultRepository resultRepository,
                             OcrPageRepository pageRepository,
                             CaptureService captureService,
                             StageDefinitionService definitions,
                             StageDataService stageDataService) {
        this.jobRepository = jobRepository;
        this.resultRepository = resultRepository;
        this.pageRepository = pageRepository;
        this.captureService = captureService;
        this.definitions = definitions;
        this.stageDataService = stageDataService;
    }

    @Transactional
    public OcrJob startJob(Long documentId, String engine, String engineVersion) {
        int attempt = jobRepository.findByDocumentIdOrderByIdDesc(documentId).size() + 1;
        OcrJob job = new OcrJob();
        job.setDocumentId(documentId);
        job.setStatus("RUNNING");
        job.setEngine(engine);
        job.setEngineVersion(engineVersion);
        job.setAttemptNo(attempt);
        job.setStartedAt(LocalDateTime.now());
        return jobRepository.save(job);
    }

    @Transactional
    public OcrJob failJob(Long jobId, String error) {
        OcrJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("OCR job not found: " + jobId));
        job.setStatus("FAILED");
        job.setErrorMessage(error);
        job.setFinishedAt(LocalDateTime.now());
        // Failures stay visible on the exceptions dashboard rather than disappearing (REQ-P11)
        log.warn("OCR job {} for document {} failed: {}", jobId, job.getDocumentId(), error);
        return jobRepository.save(job);
    }

    /**
     * Store an OCR run. Previous results for the document stay, but stop being current,
     * so earlier extractions remain retrievable (REQ-P10).
     */
    @Transactional
    public OcrResult storeResult(Long jobId, Long documentId, String fullText,
                                 List<String> pageTexts, Double avgConfidence, String language) {
        List<OcrResult> previous = resultRepository.findByDocumentIdOrderByVersionDesc(documentId);
        for (OcrResult old : previous) {
            if (Boolean.TRUE.equals(old.getIsCurrent())) {
                old.setIsCurrent(Boolean.FALSE);
                resultRepository.save(old);
            }
        }
        int version = previous.isEmpty() ? 1
                : (previous.get(0).getVersion() == null ? 1 : previous.get(0).getVersion() + 1);

        OcrResult result = new OcrResult();
        result.setJobId(jobId);
        result.setDocumentId(documentId);
        result.setVersion(version);
        result.setFullText(fullText);
        result.setPageCount(pageTexts == null ? null : pageTexts.size());
        result.setAvgConfidence(avgConfidence == null ? null : BigDecimal.valueOf(avgConfidence));
        result.setLanguage(language);
        result.setIsCurrent(Boolean.TRUE);
        OcrResult saved = resultRepository.save(result);

        if (pageTexts != null) {
            for (int i = 0; i < pageTexts.size(); i++) {
                OcrPage page = new OcrPage();
                page.setResultId(saved.getId());
                page.setPageNo(i + 1);
                page.setPageText(pageTexts.get(i));
                pageRepository.save(page);
            }
        }

        if (jobId != null) {
            jobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus("SUCCESS");
                job.setFinishedAt(LocalDateTime.now());
                if (job.getStartedAt() != null) {
                    job.setDurationMs((int) java.time.Duration
                            .between(job.getStartedAt(), job.getFinishedAt()).toMillis());
                }
                jobRepository.save(job);
            });
        }
        return saved;
    }

    /**
     * Run the catalogue's patterns for a document type over OCR text and record what
     * each one found - including the ones that found nothing, so "missing" is a
     * recorded fact rather than an absent row (REQ-P8).
     */
    @Transactional
    public List<ExtractedField> extractFields(Long packageId, short stageCode, String documentType,
                                              Long documentId, OcrResult result) {
        return extractFields(packageId, stageCode, documentType, documentId, result, List.of());
    }

    /**
     * As above, but with the word boxes the OCR engine produced, so each value can record
     * where on the page it was read from (REQ-P6).
     */
    @Transactional
    public List<ExtractedField> extractFields(Long packageId, short stageCode, String documentType,
                                              Long documentId, OcrResult result,
                                              List<OCRService.WordBox> words) {
        List<ExtractedField> captured = new ArrayList<>();
        List<DocumentTypeField> catalogue = definitions.fieldsForDocumentType(documentType);
        if (catalogue.isEmpty()) {
            log.info("No catalogue fields configured for document type {}", documentType);
            return captured;
        }
        String text = result == null ? null : result.getFullText();

        for (DocumentTypeField def : catalogue) {
            if (!Boolean.TRUE.equals(def.getIsOcrMappable())) {
                continue;
            }
            // extracted_field.entity_type is NOT NULL. Legacy document_type_fields have
            // none; writing them fails the insert (tenderId and friends from 018/030).
            if (def.getEntityType() == null || def.getEntityType().isBlank()) {
                continue;
            }
            Match match = findValue(text, def.getOcrPattern());

            Object entity = stageDataService.ensureEntity(def.getEntityType(), packageId);
            CaptureService.CaptureRequest req = new CaptureService.CaptureRequest();
            req.entityType = def.getEntityType();
            req.entityId = entity instanceof com.bpdb.dms.procurement.entity.BaseProcurementEntity be
                    ? be.getId() : null;
            req.packageId = packageId;
            req.stageCode = stageCode;
            req.fieldKey = def.getFieldKey();
            req.fieldLabel = def.getFieldLabel();
            req.dataType = def.getFieldType();
            req.mandatory = def.getIsMandatory();
            req.documentId = documentId;
            req.ocrResultId = result == null ? null : result.getId();
            req.rawValue = match == null ? null : match.value;
            req.confidence = match == null ? null : match.confidence;
            // Where on the page this reading came from, where the engine could say (REQ-P6)
            OCRService.WordBox where = match == null ? null : locate(match.value, words);
            if (where != null) {
                req.pageNo = where.getPageNo();
                req.bbox = where.asBbox();
            }
            captured.add(captureService.captureFromOcr(req));
        }
        log.info("Extracted {} candidate fields from document {} ({})",
                captured.size(), documentId, documentType);
        return captured;
    }

    public Optional<OcrResult> currentResult(Long documentId) {
        return resultRepository.findByDocumentIdAndIsCurrentTrue(documentId);
    }

    public List<OcrPage> pages(Long resultId) {
        return pageRepository.findByResultIdOrderByPageNoAsc(resultId);
    }

    public List<OcrJob> failedJobs() {
        return jobRepository.findByStatus("FAILED");
    }

    /**
     * Find the words that make up an extracted value, and return the box around them
     * (REQ-P6).
     *
     * <p>The value came out of a regex over the full text, so it has no position of its
     * own; the words do. Matching is on the word sequence, and multi-word values return
     * the union of their boxes — a date read as "12 March 2025" should highlight the whole
     * date, not the "12".
     *
     * <p>Returns null rather than guessing when the words cannot be found: a highlight in
     * the wrong place is worse than none, because it invites a person to confirm the wrong
     * region of the page.
     */
    public static OCRService.WordBox locate(String value, List<OCRService.WordBox> words) {
        if (value == null || value.isBlank() || words == null || words.isEmpty()) {
            return null;
        }
        String[] parts = value.trim().split("\\s+");
        for (int i = 0; i + parts.length <= words.size(); i++) {
            OCRService.WordBox box = null;
            boolean matched = true;
            for (int j = 0; j < parts.length; j++) {
                OCRService.WordBox word = words.get(i + j);
                if (word.getPageNo() != words.get(i).getPageNo()
                        || !sameToken(word.getText(), parts[j])) {
                    matched = false;
                    break;
                }
                box = box == null ? word : box.union(word);
            }
            if (matched && box != null) {
                return box;
            }
        }
        return null;
    }

    /**
     * Words as OCR returns them carry trailing punctuation and inconsistent case, neither
     * of which means the word is a different word.
     */
    private static boolean sameToken(String a, String b) {
        return strip(a).equalsIgnoreCase(strip(b));
    }

    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
    }

    private Match findValue(String text, String patternSource) {
        if (text == null || text.isBlank() || patternSource == null || patternSource.isBlank()) {
            return null;
        }
        try {
            Pattern pattern = Pattern.compile(patternSource, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(text);
            if (!matcher.find()) {
                return null;
            }
            String value = matcher.groupCount() >= 1 ? matcher.group(1) : matcher.group();
            Match m = new Match();
            m.value = value == null ? null : value.trim();
            // A pattern hit is good evidence but not proof; a person still confirms it.
            m.confidence = new BigDecimal("0.85");
            return m;
        } catch (PatternSyntaxException e) {
            log.warn("Invalid OCR pattern in catalogue: {}", patternSource);
            return null;
        }
    }

    private static class Match {
        String value;
        BigDecimal confidence;
    }
}
