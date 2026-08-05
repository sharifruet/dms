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

            fields = extractionService.extractFields(packageId, stageCode, docRole,
                    document.getId(), stored);
            response.put("ocrConfidence", ocr.getConfidence());
        } catch (Exception e) {
            extractionService.failJob(job.getId(), e.getMessage());
            response.put("ocrError",
                    "Text could not be read from this file - please enter the values by hand");
            log.warn("OCR failed for document {}: {}", document.getId(), e.getMessage());
        }
        response.put("fields", fields);
        return response;
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
