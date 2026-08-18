package com.bpdb.dms.service;

import com.bpdb.dms.entity.Folder;
import com.bpdb.dms.entity.User;
import com.bpdb.dms.repository.FolderRepository;
import com.bpdb.dms.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing folders and folder hierarchy
 */
@Service
@Transactional
public class FolderService {
    
    private static final Logger logger = LoggerFactory.getLogger(FolderService.class);
    
    @Autowired
    private FolderRepository folderRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Create a new folder
     */
    public Folder createFolder(String name, String description, Long parentFolderId, 
                               String department, User createdBy) {
        try {
            // Check if folder with same name exists in parent
            Folder parentFolder = null;
            if (parentFolderId != null) {
                parentFolder = folderRepository.findById(parentFolderId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found: " + parentFolderId));
                
                // Check for duplicate name
                Optional<Folder> existing = folderRepository.findByNameAndParentFolder(name, parentFolder);
                if (existing.isPresent()) {
                    throw new IllegalArgumentException("Folder with name '" + name + "' already exists in parent folder");
                }
            } else {
                // Check for duplicate root folder name
                List<Folder> rootFolders = folderRepository.findByParentFolderIsNull();
                boolean exists = rootFolders.stream()
                    .anyMatch(f -> f.getName().equalsIgnoreCase(name) && f.getIsActive());
                if (exists) {
                    throw new IllegalArgumentException("Root folder with name '" + name + "' already exists");
                }
            }
            
            Folder folder = new Folder(name, createdBy);
            folder.setDescription(description);
            folder.setParentFolder(parentFolder);
            folder.setDepartment(department);
            
            Folder saved = folderRepository.save(folder);
            
            // Compute and set folder path
            String folderPath = computeFolderPath(saved);
            saved.setFolderPath(folderPath);
            saved = folderRepository.save(saved);
            
            // Log the action
            auditService.logUserAction(createdBy, "CREATE_FOLDER", "FOLDER", 
                saved.getId(), "Created folder: " + name, null);
            
            logger.info("Created folder: {} with path: {}", name, folderPath);
            
            return saved;
            
        } catch (Exception e) {
            logger.error("Failed to create folder: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Update folder
     */
    public Folder updateFolder(Long folderId, String name, String description, 
                               String department, User updatedBy) {
        try {
            Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));
            
            // Check if it's a system folder
            if (folder.getIsSystemFolder()) {
                throw new IllegalArgumentException("Cannot modify system folder");
            }
            
            // Check for duplicate name if name changed
            if (!folder.getName().equals(name)) {
                Folder parent = folder.getParentFolder();
                Optional<Folder> existing = folderRepository.findByNameAndParentFolder(name, parent);
                if (existing.isPresent() && !existing.get().getId().equals(folderId)) {
                    throw new IllegalArgumentException("Folder with name '" + name + "' already exists in parent folder");
                }
            }
            
            folder.setName(name);
            folder.setDescription(description);
            folder.setDepartment(department);
            
            Folder saved = folderRepository.save(folder);
            
            // Update folder path
            String folderPath = computeFolderPath(saved);
            saved.setFolderPath(folderPath);
            saved = folderRepository.save(saved);
            
            // Update paths of all subfolders
            updateSubFolderPaths(saved);
            
            // Log the action
            auditService.logUserAction(updatedBy, "UPDATE_FOLDER", "FOLDER", 
                folderId, "Updated folder: " + name, null);
            
            logger.info("Updated folder: {}", folderId);
            
            return saved;
            
        } catch (Exception e) {
            logger.error("Failed to update folder: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Delete folder (soft delete)
     */
    public void deleteFolder(Long folderId, User deletedBy) {
        try {
            Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));
            
            // Check if it's a system folder
            if (folder.getIsSystemFolder()) {
                throw new IllegalArgumentException("Cannot delete system folder");
            }
            
            // Check if folder has subfolders
            List<Folder> subFolders = folderRepository.findByParentFolder(folder);
            if (!subFolders.isEmpty()) {
                throw new IllegalArgumentException("Cannot delete folder with subfolders. Please delete or move subfolders first.");
            }
            
            // Check if folder has documents
            long docCount = documentRepository.countByFolder(folder);
            if (docCount > 0) {
                throw new IllegalArgumentException("Cannot delete folder with documents. Please move or delete documents first.");
            }
            
            folder.setIsActive(false);
            folderRepository.save(folder);
            
            // Log the action
            auditService.logUserAction(deletedBy, "DELETE_FOLDER", "FOLDER", 
                folderId, "Deleted folder: " + folder.getName(), null);
            
            logger.info("Deleted folder: {}", folderId);
            
        } catch (Exception e) {
            logger.error("Failed to delete folder: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get folder by ID
     */
    public Folder getFolderById(Long folderId) {
        return folderRepository.findById(folderId)
            .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));
    }
    
    /**
     * Get root folders
     */
    public List<Folder> getRootFolders() {
        return folderRepository.findRootFolders();
    }
    
    /**
     * Get subfolders of a folder
     */
    public List<Folder> getSubFolders(Long folderId) {
        Folder folder = folderRepository.findById(folderId)
            .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));
        
        return folderRepository.findByParentFolder(folder).stream()
            .filter(Folder::getIsActive)
            .collect(Collectors.toList());
    }
    
    /**
     * Get folder tree (hierarchy)
     */
    public List<Folder> getFolderTree() {
        List<Folder> rootFolders = folderRepository.findRootFolders();
        return buildFolderTree(rootFolders);
    }
    
    /**
     * Build folder tree recursively
     */
    private List<Folder> buildFolderTree(List<Folder> folders) {
        List<Folder> result = new ArrayList<>();
        for (Folder folder : folders) {
            if (folder.getIsActive()) {
                List<Folder> subFolders = folderRepository.findByParentFolder(folder).stream()
                    .filter(Folder::getIsActive)
                    .collect(Collectors.toList());
                folder.setSubFolders(buildFolderTree(subFolders));
                result.add(folder);
            }
        }
        return result;
    }
    
    /**
     * Move folder to new parent
     */
    public Folder moveFolder(Long folderId, Long newParentFolderId, User movedBy) {
        try {
            Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));
            
            if (folder.getIsSystemFolder()) {
                throw new IllegalArgumentException("Cannot move system folder");
            }
            
            Folder newParent = null;
            if (newParentFolderId != null) {
                newParent = folderRepository.findById(newParentFolderId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found: " + newParentFolderId));
                
                // Prevent circular reference
                if (isAncestor(folder, newParent)) {
                    throw new IllegalArgumentException("Cannot move folder into its own subfolder");
                }
            }
            
            folder.setParentFolder(newParent);
            Folder saved = folderRepository.save(folder);
            
            // Update folder path
            String folderPath = computeFolderPath(saved);
            saved.setFolderPath(folderPath);
            saved = folderRepository.save(saved);
            
            // Update paths of all subfolders
            updateSubFolderPaths(saved);
            
            // Log the action
            auditService.logUserAction(movedBy, "MOVE_FOLDER", "FOLDER", 
                folderId, "Moved folder to: " + (newParent != null ? newParent.getName() : "root"), null);
            
            logger.info("Moved folder {} to parent {}", folderId, newParentFolderId);
            
            return saved;
            
        } catch (Exception e) {
            logger.error("Failed to move folder: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get folder summary (total files, uploaded files, remaining uploads)
     */
    public FolderSummary getFolderSummary(Long folderId) {
        Folder folder = folderRepository.findById(folderId)
            .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));
        
        long totalFiles = documentRepository.countByFolder(folder);
        long uploadedFiles = documentRepository.countByFolderAndIsActiveTrue(folder);
        long remainingUploads = 0; // Can be configured based on folder quota if needed
        
        return new FolderSummary(folderId, folder.getName(), totalFiles, uploadedFiles, remainingUploads);
    }
    
    /**
     * Compute folder path recursively
     */
    private String computeFolderPath(Folder folder) {
        List<String> pathParts = new ArrayList<>();
        Folder current = folder;
        
        while (current != null) {
            pathParts.add(0, current.getName());
            current = current.getParentFolder();
        }
        
        return "/" + String.join("/", pathParts);
    }
    
    /**
     * Update paths of all subfolders recursively
     */
    private void updateSubFolderPaths(Folder folder) {
        List<Folder> subFolders = folderRepository.findByParentFolder(folder);
        for (Folder subFolder : subFolders) {
            String path = computeFolderPath(subFolder);
            subFolder.setFolderPath(path);
            folderRepository.save(subFolder);
            updateSubFolderPaths(subFolder);
        }
    }
    
    /**
     * Check if folder is ancestor of another folder
     */
    private boolean isAncestor(Folder ancestor, Folder descendant) {
        Folder current = descendant.getParentFolder();
        while (current != null) {
            if (current.getId().equals(ancestor.getId())) {
                return true;
            }
            current = current.getParentFolder();
        }
        return false;
    }
    
    /**
     * DTO for folder summary
     */
    public static class FolderSummary {
        private Long folderId;
        private String folderName;
        private long totalFiles;
        private long uploadedFiles;
        private long remainingUploads;
        
        public FolderSummary(Long folderId, String folderName, long totalFiles, 
                           long uploadedFiles, long remainingUploads) {
            this.folderId = folderId;
            this.folderName = folderName;
            this.totalFiles = totalFiles;
            this.uploadedFiles = uploadedFiles;
            this.remainingUploads = remainingUploads;
        }
        
        // Getters and setters
        public Long getFolderId() { return folderId; }
        public void setFolderId(Long folderId) { this.folderId = folderId; }
        
        public String getFolderName() { return folderName; }
        public void setFolderName(String folderName) { this.folderName = folderName; }
        
        public long getTotalFiles() { return totalFiles; }
        public void setTotalFiles(long totalFiles) { this.totalFiles = totalFiles; }
        
        public long getUploadedFiles() { return uploadedFiles; }
        public void setUploadedFiles(long uploadedFiles) { this.uploadedFiles = uploadedFiles; }
        
        public long getRemainingUploads() { return remainingUploads; }
        public void setRemainingUploads(long remainingUploads) { this.remainingUploads = remainingUploads; }
    }
}

