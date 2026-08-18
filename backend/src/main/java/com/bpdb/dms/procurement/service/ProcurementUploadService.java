package com.bpdb.dms.procurement.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bpdb.dms.entity.Document;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.procurement.entity.DocumentLink;
import com.bpdb.dms.procurement.entity.ExtractedField;
import com.bpdb.dms.procurement.entity.OcrJob;
import com.bpdb.dms.procurement.entity.OcrResult;
import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.repository.DocumentRepository;
import com.bpdb.dms.repository.UserRepository;
import com.bpdb.dms.service.OCRService;

/**
 * Upload path for lifecycle documents: store the file, link it to the stage that
 * needs it, OCR it, and turn the text into candidate values for the verify screen.
 *
 * A document uploaded here is never unattached - the stage the user is working in
 * supplies the package context (REQ-L4, REQ-P1).
 */
@Service
public class ProcurementUploadService {

    private static final Logger log = LoggerFactory.getLogger(ProcurementUploadService.class);

    /** Same ceiling the ordinary document upload uses, so the two doors agree. */
    @Value("${app.max.file.size:104857600}")
    private long maxFileSize;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ProcurementPackageRepository packageRepository;
    private final LinkageService linkageService;
    private final ExtractionService extractionService;
    private final OCRService ocrService;

    public ProcurementUploadService(DocumentRepository documentRepository,
                                    UserRepository userRepository,
                                    ProcurementPackageRepository packageRepository,
                                    LinkageService linkageService,
                                    ExtractionService extractionService,
                                    OCRService ocrService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.packageRepository = packageRepository;
        this.linkageService = linkageService;
        this.extractionService = extractionService;
        this.ocrService = ocrService;
    }

    /**
     * Store an uploaded file against a stage and extract what it can.
     * OCR failure does not fail the upload - the document is still filed, and the user
     * enters the values by hand (REQ-P12).
     */
    @Transactional
    public Map<String, Object> upload(MultipartFile file, Long packageId, short stageCode,
                                      String docRole, Long userId) throws IOException {
        ProcurementPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Package not found: " + packageId));

        reject(file);
        Document document = storeFile(file, docRole, userId);

        linkageService.link(document.getId(), entityTypeForStage(stageCode), null,
                packageId, null, stageCode, docRole,
                com.bpdb.dms.procurement.entity.DocumentLink.STAGE_CONTEXT, userId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("document", document);
        response.put("packageNumber", pkg.getPackageNumber());

        List<ExtractedField> fields = new ArrayList<>();
        OcrJob job = extractionService.startJob(document.getId(), "tesseract", "5.x");
        try {
            OCRService.OCRResult ocr = ocrService.extractText(file);
            OcrResult stored = extractionService.storeResult(
                    job.getId(), document.getId(), ocr.getExtractedText(),
                    ocr.getExtractedText() == null ? List.of() : List.of(ocr.getExtractedText()),
                    ocr.getConfidence(), null);

            document.setExtractedText(ocr.getExtractedText());
            documentRepository.save(document);

            // Where each word sat, so a captured value can point at its source (REQ-P6)
            List<OCRService.WordBox> words = ocrService.extractWords(file);

            fields = extractionService.extractFields(packageId, stageCode, docRole,
                    document.getId(), stored, words);
            response.put("ocrConfidence", ocr.getConfidence());
            response.put("sourceRegionsAvailable", !words.isEmpty());

            // A scan that yields nothing usable is flagged for manual entry rather than
            // being left looking like a successful read of an empty document (REQ-P12)
            if (usableText(ocr.getExtractedText())) {
                response.put("manualEntryRequired", false);
            } else {
                response.put("manualEntryRequired", true);
                response.put("ocrError", "No usable text could be read from this file"
                        + " - please enter the values by hand (REQ-P12)");
                log.info("Document {} produced no usable OCR text; flagged for manual entry",
                        document.getId());
            }
        } catch (Exception e) {
            extractionService.failJob(job.getId(), e.getMessage());
            response.put("ocrError",
                    "Text could not be read from this file - please enter the values by hand");
            response.put("manualEntryRequired", true);
            log.warn("OCR failed for document {}: {}", document.getId(), e.getMessage());
        }
        response.put("fields", fields);
        return response;
    }

    /**
     * Re-run OCR over a document already filed (REQ-P10).
     *
     * <p>Additive: the previous result keeps its version and the fields derived from it
     * stay retrievable; this one becomes current. Worth having because the reasons a first
     * pass fails are usually fixable — Tesseract not installed, the wrong language, a scan
     * that has since been replaced — and until now the only way to try again was to upload
     * the document a second time, which left two documents where there is one.
     */
    @Transactional
    public Map<String, Object> reOcr(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        DocumentLink link = linkageService.linkFor(documentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Document " + documentId + " is not linked to a package, so there is "
                        + "no stage to extract fields for"));

        Map<String, Object> response = new LinkedHashMap<>();
        OcrJob job = extractionService.startJob(documentId, "tesseract", "5.x");
        response.put("attempt", job.getAttemptNo());
        try {
            MultipartFile stored = storedFileOf(document);
            OCRService.OCRResult ocr = ocrService.extractText(stored);
            OcrResult result = extractionService.storeResult(job.getId(), documentId,
                    ocr.getExtractedText(),
                    ocr.getExtractedText() == null ? List.of() : List.of(ocr.getExtractedText()),
                    ocr.getConfidence(), null);
            document.setExtractedText(ocr.getExtractedText());
            documentRepository.save(document);

            List<ExtractedField> fields = extractionService.extractFields(
                    link.getPackageId(), link.getStageCode(), link.getDocRole(),
                    documentId, result, ocrService.extractWords(stored));

            response.put("version", result.getVersion());
            response.put("fields", fields);
            response.put("manualEntryRequired", !usableText(ocr.getExtractedText()));
            log.info("Document {} re-OCRed as version {} by user {}",
                    documentId, result.getVersion(), userId);
        } catch (Exception e) {
            extractionService.failJob(job.getId(), e.getMessage());
            response.put("ocrError", e.getMessage());
            response.put("manualEntryRequired", true);
        }
        return response;
    }

    /**
     * The file as it sits on disk, wrapped so the OCR engine can read it the same way it
     * reads an upload.
     */
    private MultipartFile storedFileOf(Document document) throws IOException {
        Path path = Paths.get(document.getFilePath());
        if (!Files.exists(path)) {
            throw new IllegalStateException("The stored file for document " + document.getId()
                    + " is missing from " + document.getFilePath());
        }
        byte[] content = Files.readAllBytes(path);
        return new StoredFile(document.getOriginalName(), document.getMimeType(), content);
    }

    /** A file already on disk, presented as an upload. */
    private static class StoredFile implements MultipartFile {
        private final String name;
        private final String contentType;
        private final byte[] content;

        StoredFile(String name, String contentType, byte[] content) {
            this.name = name == null ? "document" : name;
            this.contentType = contentType;
            this.content = content;
        }

        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }
        @Override public void transferTo(java.io.File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }

    /**
     * Whether a read produced anything worth reviewing.
     *
     * <p>A handful of stray characters from a bad scan is not text; treating it as a
     * successful read leaves the user staring at empty fields with no explanation.
     */
    public static boolean usableText(String text) {
        if (text == null) {
            return false;
        }
        String letters = text.replaceAll("[^\\p{L}\\p{N}]", "");
        return letters.length() >= 20;
    }

    /** Extensions a procurement document can legitimately have. */
    private static final java.util.Set<String> ACCEPTED_EXTENSIONS = java.util.Set.of(
            "pdf", "jpg", "jpeg", "png", "tif", "tiff", "doc", "docx", "xls", "xlsx");

    /**
     * Refuse what should not be stored.
     *
     * <p>This path had no validation at all — no size limit, no type check, not even an
     * empty-file check — while the ordinary document upload validated all three. The same
     * file rejected at one door was accepted at the other, which is the kind of gap that
     * only shows up when somebody goes looking for it.
     */
    private void reject(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The file is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File exceeds the maximum size of "
                    + (maxFileSize / 1024 / 1024) + "MB");
        }
        String name = file.getOriginalFilename();
        String extension = name != null && name.contains(".")
                ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT)
                : "";
        if (!ACCEPTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("'" + extension
                    + "' is not a document type this system accepts. Allowed: "
                    + String.join(", ", new java.util.TreeSet<>(ACCEPTED_EXTENSIONS)));
        }
    }

    private Document storeFile(MultipartFile file, String docRole, Long userId) throws IOException {
        Path uploadPath = Paths.get(uploadDir, "procurement");
        Files.createDirectories(uploadPath);

        String original = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String extension = original.contains(".")
                ? original.substring(original.lastIndexOf('.')) : "";
        String stored = System.currentTimeMillis() + "_" + Math.abs(original.hashCode()) + extension;
        Path target = uploadPath.resolve(stored);
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        Document document = new Document();
        document.setFileName(stored);
        document.setOriginalName(original);
        document.setFilePath(target.toString());
        document.setFileSize(file.getSize());
        document.setMimeType(file.getContentType());
        document.setFileHash(hash(file));
        document.setDocumentType(docRole);
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            document.setUploadedBy(user);
        }
        return documentRepository.save(document);
    }

    private String hash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(file.getBytes()));
        } catch (Exception e) {
            return null;
        }
    }

    /** The record a stage's documents attach to (see the graph in requirements 2.1). */
    private String entityTypeForStage(short stageCode) {
        return switch (stageCode) {
            case 1 -> "PACKAGE";
            case 2 -> "TENDER";
            case 3 -> "TENDER_OPENING";
            case 4 -> "EVALUATION";
            case 5 -> "CONTRACT_APPROVAL";
            case 6 -> "NOA";
            case 7 -> "PERFORMANCE_SECURITY";
            case 8 -> "CONTRACT";
            case 9 -> "LETTER_OF_CREDIT";
            case 10 -> "PRODUCTION_SCHEDULE";
            case 11 -> "INSPECTION_EVENT";
            case 12 -> "DELIVERY";
            case 13 -> "INVOICE";
            case 14 -> "PAYMENT";
            case 15 -> "WARRANTY";
            case 16 -> "CONTRACT_CLOSURE";
            default -> "PACKAGE";
        };
    }
}
