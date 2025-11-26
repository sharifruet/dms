package com.bpdb.dms.service;

import com.bpdb.dms.entity.*;
import com.bpdb.dms.model.DocumentType;
import com.bpdb.dms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing tender workflows and folder-workflow mapping
 * Handles automatic workflow creation on Tender Notice upload and folder-based workflow association
 */
@Service
@Transactional
public class TenderWorkflowService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenderWorkflowService.class);
    
    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;
    
    @Autowired
    private FolderRepository folderRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private AppHeaderRepository appHeaderRepository;
    
    /**
     * Create or get workflow for a folder
     * If folder already has a workflow, returns it; otherwise creates a new one
     * 
     * @param folderId The folder ID
     * @param workflowName The workflow name (typically the folder name)
     * @param createdBy The user creating the workflow
     * @param appEntryId Optional APP entry ID to link to the workflow
     * @return The workflow for the folder
     * @throws IllegalArgumentException if folder doesn't exist or already has a Tender Notice
     */
    public Workflow createOrGetWorkflowForFolder(Long folderId, String workflowName, User createdBy, Long appEntryId) {
        Folder folder = folderRepository.findById(folderId)
            .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));
        
        // Check if folder already has a workflow
        Optional<Workflow> existingWorkflow = workflowRepository.findByFolder(folder);
        if (existingWorkflow.isPresent()) {
            Workflow workflow = existingWorkflow.get();
            
            // If APP entry is provided and workflow doesn't have one, link it
            if (appEntryId != null && workflow.getAppEntry() == null) {
                try {
                    linkWorkflowToAppEntry(workflow.getId(), appEntryId);
                    logger.info("Linked APP entry {} to existing workflow {}", appEntryId, workflow.getId());
                } catch (Exception e) {
                    logger.warn("Failed to link APP entry {} to existing workflow {}: {}", 
                               appEntryId, workflow.getId(), e.getMessage());
                }
            }
            
            logger.info("Folder {} already has workflow {}", folderId, workflow.getId());
            return workflow;
        }
        
        // Validate that folder doesn't already have a Tender Notice document
        // Only check if creating new workflow (not if reusing existing)
        validateNoExistingTenderNotice(folder);
        
        // Create new workflow for this folder
        Workflow workflow = new Workflow(
            workflowName,
            "Tender workflow for folder: " + folder.getName(),
            WorkflowType.CUSTOM_WORKFLOW,
            createdBy
        );
        
        // Set workflow definition for tender process
        String definition = "{\"steps\":[{\"order\":1,\"name\":\"Collect Tender Documents (2–7)\",\"type\":\"SEQUENTIAL\"},{\"order\":2,\"name\":\"Review & Finalize\",\"type\":\"APPROVAL\"}]}";
        workflow.setDefinition(definition);
        workflow.setFolder(folder);
        workflow.setStatus(WorkflowStatus.ACTIVE);
        
        // Link APP entry if provided
        if (appEntryId != null) {
            try {
                AppHeader appEntry = appHeaderRepository.findById(appEntryId)
                    .orElseThrow(() -> new IllegalArgumentException("APP entry not found: " + appEntryId));
                workflow.setAppEntry(appEntry);
                logger.info("Linking APP entry {} to new workflow for folder {}", appEntryId, folderId);
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to link APP entry {} to workflow: {}", appEntryId, e.getMessage());
                // Don't fail workflow creation if APP entry linking fails
            }
        }
        
        Workflow savedWorkflow = workflowRepository.save(workflow);
        
        auditService.logActivity(createdBy.getUsername(), "TENDER_WORKFLOW_CREATED", 
            "Tender workflow created for folder: " + folder.getName(), null);
        
        logger.info("Created tender workflow {} for folder {} with APP entry {}", 
                   savedWorkflow.getId(), folderId, appEntryId != null ? appEntryId : "none");
        
        return savedWorkflow;
    }
    
    /**
     * Overloaded method without appEntryId for backward compatibility
     */
    public Workflow createOrGetWorkflowForFolder(Long folderId, String workflowName, User createdBy) {
        return createOrGetWorkflowForFolder(folderId, workflowName, createdBy, null);
    }
    
    /**
     * Get workflow for a folder
     * 
     * @param folderId The folder ID
     * @return The workflow if exists, empty otherwise
     */
    public Optional<Workflow> getWorkflowByFolder(Long folderId) {
        Folder folder = folderRepository.findById(folderId)
            .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));
        
        return workflowRepository.findByFolder(folder);
    }
    
    /**
     * Check if folder has a workflow
     * 
     * @param folderId The folder ID
     * @return true if folder has a workflow, false otherwise
     */
    public boolean folderHasWorkflow(Long folderId) {
        Folder folder = folderRepository.findById(folderId).orElse(null);
        if (folder == null) {
            return false;
        }
        return workflowRepository.existsByFolder(folder);
    }
    
    /**
     * Validate that folder doesn't already have a Tender Notice document.
     * 
     * NOTE: Business requirement has changed – multiple Tender Notices
     * are now allowed per folder/workflow. This method is kept for
     * backward compatibility but no longer enforces a restriction.
     */
    public void validateNoExistingTenderNotice(Folder folder) {
        // Intentionally no-op: multiple Tender Notices per folder are allowed.
    }
    
    /**
     * Validate folder for Tender Notice upload
     * Ensures folder is selected and doesn't already have a Tender Notice
     * 
     * @param folderId The folder ID (can be null)
     * @throws IllegalArgumentException if validation fails
     */
    public void validateFolderForTenderNotice(Long folderId) {
        if (folderId == null) {
            throw new IllegalArgumentException("Folder selection is required for Tender Notice uploads");
        }
        
        Folder folder = folderRepository.findById(folderId)
            .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));
        
        validateNoExistingTenderNotice(folder);
    }
    
    /**
     * Validate folder for workflow-based document upload
     * Ensures folder has an associated workflow
     * 
     * @param folderId The folder ID
     * @throws IllegalArgumentException if folder doesn't have a workflow
     */
    public void validateFolderHasWorkflow(Long folderId) {
        if (folderId == null) {
            throw new IllegalArgumentException("Folder selection is required");
        }
        
        if (!folderHasWorkflow(folderId)) {
            throw new IllegalArgumentException(
                "Selected folder does not have an associated workflow. " +
                "Please select a folder that was used for a Tender Notice upload."
            );
        }
    }
    
    /**
     * Get workflow instance for a document
     * Returns the workflow instance associated with the document's folder's workflow
     * 
     * @param documentId The document ID
     * @return The workflow instance if found, empty otherwise
     */
    public Optional<WorkflowInstance> getWorkflowInstanceForDocument(Long documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null || document.getFolder() == null) {
            return Optional.empty();
        }
        
        Optional<Workflow> workflow = workflowRepository.findByFolder(document.getFolder());
        if (workflow.isEmpty()) {
            return Optional.empty();
        }
        
        // Find workflow instance for this workflow and document
        List<WorkflowInstance> instances = workflowInstanceRepository.findByWorkflow(workflow.get(), null).getContent();
        return instances.stream()
            .filter(instance -> document.equals(instance.getDocument()))
            .findFirst();
    }
    
    /**
     * Create workflow instance for Tender Notice document
     * 
     * @param workflow The workflow
     * @param document The Tender Notice document
     * @param initiatedBy The user initiating the workflow
     * @return The created workflow instance
     */
    public WorkflowInstance createWorkflowInstanceForTenderNotice(
            Workflow workflow, Document document, User initiatedBy) {
        
        WorkflowInstance instance = new WorkflowInstance(workflow, document, initiatedBy);
        instance.setStatus(WorkflowInstanceStatus.IN_PROGRESS);
        instance.setStartedAt(LocalDateTime.now());
        
        WorkflowInstance savedInstance = workflowInstanceRepository.save(instance);
        
        logger.info("Created workflow instance {} for Tender Notice document {}", 
            savedInstance.getId(), document.getId());
        
        return savedInstance;
    }
    
    /**
     * Get all documents for a workflow (by folder)
     * 
     * @param folderId The folder ID
     * @return List of documents in the folder's workflow
     */
    public List<Document> getDocumentsForWorkflow(Long folderId) {
        // Get all documents in the folder (using pagination with large page size)
        Page<Document> documentsPage = documentRepository.findByFolderId(
            folderId, 
            org.springframework.data.domain.PageRequest.of(0, 1000)
        );
        return documentsPage.getContent();
    }
    
    /**
     * Check if multiple documents of a type are allowed in the workflow
     * PS, PG, Bills, and Correspondence can be multiple
     * 
     * @param documentType The document type
     * @return true if multiple documents are allowed
     */
    public boolean allowsMultipleDocuments(DocumentType documentType) {
        return documentType == DocumentType.PERFORMANCE_SECURITY_PS ||
               documentType == DocumentType.PERFORMANCE_GUARANTEE_PG ||
               documentType == DocumentType.BILL ||
               documentType == DocumentType.CORRESPONDENCE;
    }
    
    /**
     * Link a workflow to an APP entry (budget entry)
     * 
     * @param workflowId The workflow ID
     * @param appEntryId The APP entry ID
     * @return The updated workflow
     * @throws IllegalArgumentException if workflow or APP entry not found
     */
    public Workflow linkWorkflowToAppEntry(Long workflowId, Long appEntryId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
        
        AppHeader appEntry = appHeaderRepository.findById(appEntryId)
            .orElseThrow(() -> new IllegalArgumentException("APP entry not found: " + appEntryId));
        
        workflow.setAppEntry(appEntry);
        Workflow saved = workflowRepository.save(workflow);
        
        logger.info("Linked workflow {} to APP entry {} (Fiscal Year: {}, Installment: {})", 
                   workflowId, appEntryId, appEntry.getFiscalYear(), appEntry.getReleaseInstallmentNo());
        
        return saved;
    }
    
    /**
     * Get APP entry for a workflow
     * 
     * @param workflowId The workflow ID
     * @return The APP entry if linked, empty otherwise
     */
    public Optional<AppHeader> getAppEntryForWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
        
        AppHeader appEntry = workflow.getAppEntry();
        return Optional.ofNullable(appEntry);
    }
    
    /**
     * Unlink workflow from APP entry
     * 
     * @param workflowId The workflow ID
     * @return The updated workflow
     */
    public Workflow unlinkWorkflowFromAppEntry(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
        
        workflow.setAppEntry(null);
        Workflow saved = workflowRepository.save(workflow);
        
        logger.info("Unlinked workflow {} from APP entry", workflowId);
        
        return saved;
    }
}

