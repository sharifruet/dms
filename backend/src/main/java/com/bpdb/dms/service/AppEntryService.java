package com.bpdb.dms.service;

import com.bpdb.dms.entity.AppHeader;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.AppHeaderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing APP (Annual Project Plan) manual entries
 */
@Service
@Transactional
public class AppEntryService {

    private static final Logger logger = LoggerFactory.getLogger(AppEntryService.class);

    @Autowired
    private AppHeaderRepository appHeaderRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Create a new APP entry manually
     */
    public AppHeader createAppEntry(CreateAppEntryRequest request, User user) {
        // Validate required fields
        validateRequiredFields(request);

        // Check for duplicate fiscal year + installment combination
        Optional<AppHeader> existing = appHeaderRepository.findByFiscalYearAndReleaseInstallmentNo(
            request.getFiscalYear(), 
            request.getReleaseInstallmentNo()
        );

        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                String.format(
                    "An APP entry already exists for Fiscal Year %d and Installment No %d. " +
                    "Please use a different installment number or update the existing entry.",
                    request.getFiscalYear(),
                    request.getReleaseInstallmentNo()
                )
            );
        }

        // Create new APP entry
        AppHeader header = new AppHeader();
        header.setFiscalYear(request.getFiscalYear());
        header.setAllocationType(request.getAllocationType());
        header.setBudgetReleaseDate(request.getBudgetReleaseDate());
        header.setAllocationAmount(request.getAllocationAmount());
        header.setReleaseInstallmentNo(request.getReleaseInstallmentNo());
        header.setReferenceMemoNumber(request.getReferenceMemoNumber());
        header.setDepartment(request.getDepartment());
        header.setCreatedBy(user);

        // Handle attachment upload if provided
        if (request.getAttachment() != null && !request.getAttachment().isEmpty()) {
            try {
                String filePath = storeAttachmentFile(request.getAttachment(), user.getId());
                header.setAttachmentFilePath(filePath);
            } catch (Exception e) {
                logger.error("Failed to store APP attachment", e);
                throw new RuntimeException("Failed to upload attachment: " + e.getMessage());
            }
        }

        AppHeader saved = appHeaderRepository.save(header);
        logger.info("Created APP entry ID {} for fiscal year {} installment {}", 
            saved.getId(), saved.getFiscalYear(), saved.getReleaseInstallmentNo());
        
        return saved;
    }

    /**
     * Get next installment number for a fiscal year
     */
    public Integer getNextInstallmentNo(Integer fiscalYear) {
        Optional<Integer> maxInstallment = appHeaderRepository.findMaxInstallmentNoByFiscalYear(fiscalYear);
        return maxInstallment.map(n -> n + 1).orElse(1);
    }

    /**
     * Check if a duplicate entry exists
     */
    public boolean isDuplicate(Integer fiscalYear, Integer installmentNo) {
        return appHeaderRepository.findByFiscalYearAndReleaseInstallmentNo(fiscalYear, installmentNo).isPresent();
    }

    /**
     * Get all APP entries
     */
    public List<AppHeader> getAllAppEntries() {
        return appHeaderRepository.findAll();
    }

    /**
     * Get APP entries by fiscal year
     */
    public List<AppHeader> getAppEntriesByFiscalYear(Integer fiscalYear) {
        return appHeaderRepository.findByFiscalYearOrderByReleaseInstallmentNoAsc(fiscalYear);
    }

    /**
     * Get distinct fiscal years
     */
    public List<Integer> getDistinctFiscalYears() {
        return appHeaderRepository.findDistinctFiscalYears();
    }

    /**
     * Get APP entry by ID
     */
    public Optional<AppHeader> getAppEntryById(Long id) {
        return appHeaderRepository.findById(id);
    }

    /**
     * Update an existing APP entry
     */
    public AppHeader updateAppEntry(Long id, CreateAppEntryRequest request, User user) {
        AppHeader header = appHeaderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("APP entry not found: " + id));

        // Check for duplicate if fiscal year or installment changed
        if (!header.getFiscalYear().equals(request.getFiscalYear()) || 
            !header.getReleaseInstallmentNo().equals(request.getReleaseInstallmentNo())) {
            Optional<AppHeader> existing = appHeaderRepository.findByFiscalYearAndReleaseInstallmentNo(
                request.getFiscalYear(), 
                request.getReleaseInstallmentNo()
            );
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("Duplicate fiscal year/installment combination");
            }
        }

        // Update fields
        header.setFiscalYear(request.getFiscalYear());
        header.setAllocationType(request.getAllocationType());
        header.setBudgetReleaseDate(request.getBudgetReleaseDate());
        header.setAllocationAmount(request.getAllocationAmount());
        header.setReleaseInstallmentNo(request.getReleaseInstallmentNo());
        header.setReferenceMemoNumber(request.getReferenceMemoNumber());
        header.setDepartment(request.getDepartment());

        // Handle attachment update if provided
        if (request.getAttachment() != null && !request.getAttachment().isEmpty()) {
            try {
                // Delete old attachment if exists
                if (header.getAttachmentFilePath() != null) {
                    deleteAttachmentFile(header.getAttachmentFilePath());
                }
                String filePath = storeAttachmentFile(request.getAttachment(), user.getId());
                header.setAttachmentFilePath(filePath);
            } catch (Exception e) {
                logger.error("Failed to update APP attachment", e);
                throw new RuntimeException("Failed to upload attachment: " + e.getMessage());
            }
        }

        return appHeaderRepository.save(header);
    }

    /**
     * Delete an APP entry
     */
    public void deleteAppEntry(Long id) {
        AppHeader header = appHeaderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("APP entry not found: " + id));

        // Delete attachment if exists
        if (header.getAttachmentFilePath() != null) {
            try {
                deleteAttachmentFile(header.getAttachmentFilePath());
            } catch (Exception e) {
                logger.warn("Failed to delete attachment file", e);
            }
        }

        appHeaderRepository.delete(header);
    }

    /**
     * Validate required fields
     */
    private void validateRequiredFields(CreateAppEntryRequest request) {
        if (request.getFiscalYear() == null) {
            throw new IllegalArgumentException("Fiscal Year is required");
        }
        if (request.getAllocationType() == null || request.getAllocationType().trim().isEmpty()) {
            throw new IllegalArgumentException("Allocation Type is required");
        }
        if (request.getBudgetReleaseDate() == null) {
            throw new IllegalArgumentException("Budget Release Date is required");
        }
        if (request.getAllocationAmount() == null || request.getAllocationAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Allocation Amount must be greater than zero");
        }
        if (request.getReleaseInstallmentNo() == null || request.getReleaseInstallmentNo() < 1) {
            throw new IllegalArgumentException("Release Installment No must be at least 1");
        }
    }

    /**
     * Store attachment file
     */
    private String storeAttachmentFile(MultipartFile file, Long userId) throws IOException {
        String attachmentDir = uploadDir + "/app-attachments";
        Path attachmentPath = Paths.get(attachmentDir);
        
        if (!Files.exists(attachmentPath)) {
            Files.createDirectories(attachmentPath);
        }
        
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
            : "";
        String uniqueFilename = "app_" + userId + "_" + UUID.randomUUID().toString() + fileExtension;
        
        Path filePath = attachmentPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return filePath.toString();
    }

    /**
     * Delete attachment file
     */
    private void deleteAttachmentFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    /**
     * Request DTO for creating/updating APP entries
     */
    public static class CreateAppEntryRequest {
        private Integer fiscalYear;
        private String allocationType;
        private LocalDate budgetReleaseDate;
        private BigDecimal allocationAmount;
        private Integer releaseInstallmentNo;
        private String referenceMemoNumber;
        private String department;
        private MultipartFile attachment;

        // Getters and setters
        public Integer getFiscalYear() { return fiscalYear; }
        public void setFiscalYear(Integer fiscalYear) { this.fiscalYear = fiscalYear; }
        
        public String getAllocationType() { return allocationType; }
        public void setAllocationType(String allocationType) { this.allocationType = allocationType; }
        
        public LocalDate getBudgetReleaseDate() { return budgetReleaseDate; }
        public void setBudgetReleaseDate(LocalDate budgetReleaseDate) { this.budgetReleaseDate = budgetReleaseDate; }
        
        public BigDecimal getAllocationAmount() { return allocationAmount; }
        public void setAllocationAmount(BigDecimal allocationAmount) { this.allocationAmount = allocationAmount; }
        
        public Integer getReleaseInstallmentNo() { return releaseInstallmentNo; }
        public void setReleaseInstallmentNo(Integer releaseInstallmentNo) { this.releaseInstallmentNo = releaseInstallmentNo; }
        
        public String getReferenceMemoNumber() { return referenceMemoNumber; }
        public void setReferenceMemoNumber(String referenceMemoNumber) { this.referenceMemoNumber = referenceMemoNumber; }
        
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        
        public MultipartFile getAttachment() { return attachment; }
        public void setAttachment(MultipartFile attachment) { this.attachment = attachment; }
    }
}

