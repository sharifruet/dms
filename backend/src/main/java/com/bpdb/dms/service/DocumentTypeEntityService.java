package com.bpdb.dms.service;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.model.DocumentType;
import com.bpdb.dms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Populates type-specific entity tables from document metadata after OCR extraction.
 * CORRESPONDENCE and STATIONERY_RECORD are created with documentId only (fields later).
 */
@Service
@Transactional
public class DocumentTypeEntityService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTypeEntityService.class);

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("d/M/yy"),
        DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d-MMMM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy-M-d")
    );

    @Autowired
    private DocumentMetadataService documentMetadataService;

    @Autowired
    private TenderNoticeRepository tenderNoticeRepository;

    @Autowired
    private TenderDocumentRepository tenderDocumentRepository;

    @Autowired
    private ContractAgreementRepository contractAgreementRepository;

    @Autowired
    private BankGuaranteeRepository bankGuaranteeRepository;

    @Autowired
    private PerformanceSecurityRepository performanceSecurityRepository;

    @Autowired
    private PerformanceGuaranteeRepository performanceGuaranteeRepository;

    @Autowired
    private BillDocumentRepository billDocumentRepository;

    @Autowired
    private CorrespondenceRepository correspondenceRepository;

    @Autowired
    private StationeryRecordRepository stationeryRecordRepository;

    /**
     * Populate the type-specific entity for the given document using present metadata.
     * Missing metadata fields are left unset for later implementation.
     */
    public void populateFromDocument(Document document) {
        if (document == null || document.getId() == null) {
            return;
        }

        try {
            Map<String, String> meta = documentMetadataService.getMetadataMap(document);
            Optional<DocumentType> typeOpt = DocumentType.resolve(document.getDocumentType());
            if (typeOpt.isEmpty()) {
                logger.debug("Skipping entity populate for document {}: unknown type {}",
                    document.getId(), document.getDocumentType());
                return;
            }

            DocumentType type = typeOpt.get();
            Long documentId = document.getId();

            switch (type) {
                case TENDER_NOTICE -> populateTenderNotice(documentId, meta);
                case TENDER_DOCUMENT -> populateTenderDocument(documentId, meta);
                case CONTRACT_AGREEMENT -> populateContractAgreement(documentId, meta);
                case BANK_GUARANTEE_BG -> populateBankGuarantee(documentId, meta);
                case PERFORMANCE_SECURITY_PS -> populatePerformanceSecurity(documentId, meta);
                case PERFORMANCE_GUARANTEE_PG -> populatePerformanceGuarantee(documentId, meta);
                case BILL -> populateBillDocument(documentId, meta);
                case CORRESPONDENCE -> populateCorrespondence(documentId);
                case STATIONERY_RECORD -> populateStationeryRecord(documentId);
                default -> logger.debug("No type entity for document type {} (document {})",
                    type, documentId);
            }
        } catch (Exception e) {
            logger.warn("Failed to populate type entity for document {}: {}",
                document.getId(), e.getMessage(), e);
        }
    }

    private void populateTenderNotice(Long documentId, Map<String, String> meta) {
        TenderNotice entity = tenderNoticeRepository.findByDocumentId(documentId)
            .orElseGet(() -> {
                TenderNotice t = new TenderNotice();
                t.setDocumentId(documentId);
                return t;
            });

        firstPresent(meta, "tenderId", "proposalId").ifPresent(entity::setTenderId);
        get(meta, "tenderType").ifPresent(entity::setTenderType);
        parseDate(meta, "tenderDate").ifPresent(entity::setTenderDate);
        parseDate(meta, "closingDate").ifPresent(entity::setClosingDate);
        parseDate(meta, "publicationDate").ifPresent(entity::setPublicationDate);
        get(meta, "procurementPackageNo").ifPresent(entity::setProcurementPackageNo);
        parseDate(meta, "tenderValidUpTo").ifPresent(entity::setTenderValidUpTo);
        get(meta, "procurementNature").ifPresent(entity::setProcurementNature);
        get(meta, "procurementType").ifPresent(entity::setProcurementType);
        get(meta, "procurementDescription").ifPresent(entity::setProcurementDescription);

        tenderNoticeRepository.save(entity);
        logger.info("Populated TenderNotice for document {}", documentId);
    }

    private void populateTenderDocument(Long documentId, Map<String, String> meta) {
        TenderDocument entity = tenderDocumentRepository.findByDocumentId(documentId)
            .orElseGet(() -> {
                TenderDocument t = new TenderDocument();
                t.setDocumentId(documentId);
                return t;
            });

        firstPresent(meta, "tenderId", "proposalId").ifPresent(entity::setTenderId);
        get(meta, "documentCategory").ifPresent(entity::setDocumentCategory);
        get(meta, "tenderType").ifPresent(entity::setTenderType);

        tenderDocumentRepository.save(entity);
        logger.info("Populated TenderDocument for document {}", documentId);
    }

    private void populateContractAgreement(Long documentId, Map<String, String> meta) {
        ContractAgreement entity = contractAgreementRepository.findByDocumentId(documentId)
            .orElseGet(() -> {
                ContractAgreement c = new ContractAgreement();
                c.setDocumentId(documentId);
                return c;
            });

        firstPresent(meta, "contractNumber", "contractNo").ifPresent(entity::setContractNumber);
        get(meta, "vendorName").ifPresent(entity::setVendorName);
        parseDate(meta, "contractDate", "date").ifPresent(entity::setContractDate);
        parseDecimal(meta, "contractAmount", "amount").ifPresent(entity::setContractAmount);
        get(meta, "validityPeriod").ifPresent(entity::setValidityPeriod);
        parseDate(meta, "expiryDate").ifPresent(entity::setExpiryDate);

        contractAgreementRepository.save(entity);
        logger.info("Populated ContractAgreement for document {}", documentId);
    }

    private void populateBankGuarantee(Long documentId, Map<String, String> meta) {
        BankGuarantee entity = bankGuaranteeRepository.findByDocumentId(documentId)
            .orElseGet(() -> {
                BankGuarantee b = new BankGuarantee();
                b.setDocumentId(documentId);
                return b;
            });

        get(meta, "bgNumber").ifPresent(entity::setBgNumber);
        get(meta, "bankName").ifPresent(entity::setBankName);
        parseDecimal(meta, "bgAmount", "amount").ifPresent(entity::setBgAmount);
        parseDate(meta, "issueDate").ifPresent(entity::setIssueDate);
        parseDate(meta, "expiryDate").ifPresent(entity::setExpiryDate);

        bankGuaranteeRepository.save(entity);
        logger.info("Populated BankGuarantee for document {}", documentId);
    }

    private void populatePerformanceSecurity(Long documentId, Map<String, String> meta) {
        PerformanceSecurity entity = performanceSecurityRepository.findByDocumentId(documentId)
            .orElseGet(() -> {
                PerformanceSecurity p = new PerformanceSecurity();
                p.setDocumentId(documentId);
                return p;
            });

        get(meta, "psNumber").ifPresent(entity::setPsNumber);
        parseDecimal(meta, "psAmount", "amount").ifPresent(entity::setPsAmount);
        parseDate(meta, "expiryDate").ifPresent(entity::setExpiryDate);
        get(meta, "bankName").ifPresent(entity::setBankName);
        parseDate(meta, "issueDate").ifPresent(entity::setIssueDate);
        firstPresent(meta, "contractNumber", "contractNo").ifPresent(entity::setContractNumber);

        performanceSecurityRepository.save(entity);
        logger.info("Populated PerformanceSecurity for document {}", documentId);
    }

    private void populatePerformanceGuarantee(Long documentId, Map<String, String> meta) {
        PerformanceGuarantee entity = performanceGuaranteeRepository.findByDocumentId(documentId)
            .orElseGet(() -> {
                PerformanceGuarantee p = new PerformanceGuarantee();
                p.setDocumentId(documentId);
                return p;
            });

        get(meta, "pgNumber").ifPresent(entity::setPgNumber);
        parseDecimal(meta, "pgAmount", "amount").ifPresent(entity::setPgAmount);
        parseDate(meta, "expiryDate").ifPresent(entity::setExpiryDate);
        get(meta, "bankName").ifPresent(entity::setBankName);
        parseDate(meta, "issueDate").ifPresent(entity::setIssueDate);
        firstPresent(meta, "contractNumber", "contractNo").ifPresent(entity::setContractNumber);

        performanceGuaranteeRepository.save(entity);
        logger.info("Populated PerformanceGuarantee for document {}", documentId);
    }

    private void populateBillDocument(Long documentId, Map<String, String> meta) {
        BillDocument entity = billDocumentRepository.findByDocumentId(documentId)
            .orElseGet(() -> {
                BillDocument b = new BillDocument();
                b.setDocumentId(documentId);
                return b;
            });

        get(meta, "vendorName").ifPresent(entity::setVendorName);
        get(meta, "invoiceNumber").ifPresent(entity::setInvoiceNumber);
        parseDate(meta, "invoiceDate", "date").ifPresent(entity::setInvoiceDate);
        get(meta, "fiscalYear").ifPresent(entity::setFiscalYear);
        parseDecimal(meta, "totalAmount", "amount").ifPresent(entity::setTotalAmount);
        parseDecimal(meta, "taxAmount").ifPresent(entity::setTaxAmount);
        parseDecimal(meta, "netAmount").ifPresent(entity::setNetAmount);
        get(meta, "description").ifPresent(entity::setDescription);

        billDocumentRepository.save(entity);
        logger.info("Populated BillDocument for document {}", documentId);
    }

    /** Stub: only documentId for now */
    private void populateCorrespondence(Long documentId) {
        if (correspondenceRepository.existsByDocumentId(documentId)) {
            return;
        }
        Correspondence entity = new Correspondence();
        entity.setDocumentId(documentId);
        correspondenceRepository.save(entity);
        logger.info("Populated Correspondence (documentId only) for document {}", documentId);
    }

    /** Stub: only documentId for now */
    private void populateStationeryRecord(Long documentId) {
        if (stationeryRecordRepository.existsByDocumentId(documentId)) {
            return;
        }
        StationeryRecord entity = new StationeryRecord();
        entity.setDocumentId(documentId);
        stationeryRecordRepository.save(entity);
        logger.info("Populated StationeryRecord (documentId only) for document {}", documentId);
    }

    private Optional<String> get(Map<String, String> meta, String key) {
        if (meta == null || key == null) {
            return Optional.empty();
        }
        String value = meta.get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private Optional<String> firstPresent(Map<String, String> meta, String... keys) {
        for (String key : keys) {
            Optional<String> value = get(meta, key);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private Optional<LocalDate> parseDate(Map<String, String> meta, String... keys) {
        return firstPresent(meta, keys).flatMap(this::parseDateValue);
    }

    private Optional<LocalDate> parseDateValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String cleaned = raw.trim();
        // Drop time portion if present (e.g. "12-Jan-2024 14:30")
        if (cleaned.contains(" ")) {
            cleaned = cleaned.substring(0, cleaned.indexOf(' ')).trim();
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return Optional.of(LocalDate.parse(cleaned, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }
        // Try ISO after normalizing separators
        try {
            return Optional.of(LocalDate.parse(cleaned.replace('/', '-')));
        } catch (DateTimeParseException ignored) {
            logger.debug("Could not parse date value: {}", raw);
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> parseDecimal(Map<String, String> meta, String... keys) {
        return firstPresent(meta, keys).flatMap(this::parseDecimalValue);
    }

    private Optional<BigDecimal> parseDecimalValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            String cleaned = raw.replaceAll("[^0-9.,-]", "").trim();
            if (cleaned.isEmpty()) {
                return Optional.empty();
            }
            // Handle European-style 1.234,56 vs 1,234.56
            if (cleaned.contains(",") && cleaned.contains(".")) {
                if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                    cleaned = cleaned.replace(".", "").replace(",", ".");
                } else {
                    cleaned = cleaned.replace(",", "");
                }
            } else if (cleaned.contains(",")) {
                cleaned = cleaned.replace(",", ".");
            }
            return Optional.of(new BigDecimal(cleaned));
        } catch (NumberFormatException e) {
            logger.debug("Could not parse decimal value: {}", raw);
            return Optional.empty();
        }
    }
}
