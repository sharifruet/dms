# TODO - DMS Implementation Status & Remaining Tasks

**Last Updated**: [Current Date]  
**Based on**: Requirements Specification v1.2 (Updated with APP/Bill changes and workflow requirements)  
**Status**: Comprehensive gap analysis between requirements and current implementation  
**Implementation Phases**: See [IMPLEMENTATION_PHASES.md](./IMPLEMENTATION_PHASES.md) for phased implementation plan

**Recent Changes**:
- Updated workflow requirements: Folder-based workflow selection, mandatory folder selection for Tender Notice
- APP removed as document type, now manual entry only
- BILL added as document type with OCR extraction
- Workflow supports multiple PS/PG/Bills per workflow

**Refinements Made**:
- Clarified one-to-one folder-to-workflow relationship (each folder can only have one workflow)
- Added validation to prevent multiple Tender Notices in same folder
- Clarified which document types support multiple instances (PS, PG, Bills, Correspondence) vs single instances
- Added error handling requirements for edge cases (folder without workflow, OCR failures)
- Enhanced APP entry validation (duplicate warning with confirmation option)
- Enhanced Bill OCR failure handling (allow full manual entry if OCR fails)
- Added OCR confidence tracking and accuracy metrics requirements
- Clarified Purchase Order (PO) and Letter of Credit (LC) may be classified as "Other" document type

---

## 📊 Implementation Status Summary

| Category | Implemented | Partial | Not Started | Total |
|----------|------------|---------|-------------|-------|
| Document Upload & Classification | 8 | 2 | 2 | 12 |
| OCR & Metadata Management | 3 | 1 | 1 | 5 |
| Document Repository & Storage | 6 | 3 | 2 | 11 |
| Search, Filter & Retrieval | 4 | 3 | 2 | 9 |
| Access Control & Role Management | 4 | 1 | 1 | 6 |
| Notification & Alert System | 5 | 2 | 1 | 8 |
| Dashboard & Reporting | 4 | 2 | 2 | 8 |
| Audit Trail & Compliance | 3 | 1 | 1 | 5 |
| Document Linkage & Lifecycle | 2 | 2 | 1 | 5 |
| Security & Backup | 2 | 1 | 1 | 4 |
| System Administration | 3 | 1 | 1 | 5 |
| Finance & Billing | 3 | 1 | 2 | 6 |
| Advanced AI Features | 0 | 0 | 18 | 18 |
| Mobile & Offline | 0 | 0 | 15 | 15 |
| Collaboration Features | 0 | 0 | 20 | 20 |

**Total**: 47 Implemented | 20 Partial | 66 Not Started | **133 Requirements**

---

## 🔴 CRITICAL PRIORITY (Core Features - Must Complete)

### 1. Auto Document Type Detection (FR-007)
**Status**: ⚠️ **PARTIAL**  
**Priority**: CRITICAL  
**Requirements**: FR-007, FR-008

**Current State**:
- ✅ Document types defined in enum
- ✅ Manual document type selection available
- ❌ Automatic detection not implemented
- ❌ AI/ML-based classification missing

**Tasks**:
- [ ] Remove APP from document types list (no longer a document type)
- [ ] Add BILL to document types list (new document type)
- [ ] Note: Purchase Order (PO) and Letter of Credit (LC) may be classified as "Other" if not separately defined in DocumentType enum
- [ ] Implement document type auto-detection using OCR content analysis
- [ ] Create classification rules engine based on keywords and patterns
- [ ] Integrate ML model for document classification (FR-128)
- [ ] Add confidence scoring for auto-detected types
- [ ] Implement manual override UI (partially done, needs enhancement)
- [ ] Add classification accuracy metrics tracking

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/DocumentClassificationService.java` (new)
- `backend/src/main/java/com/bpdb/dms/service/FileUploadService.java` (modify)
- `frontend/src/components/DocumentTypeSelector.tsx` (enhance)

---

### 2. Tender Workflow Auto-Creation (FR-009, FR-006a-d)
**Status**: ❌ **NOT STARTED**  
**Priority**: CRITICAL  
**Requirements**: FR-009 (Workflow Rule), FR-006a-d (Folder-Based Workflow Selection)

**Current State**:
- ✅ Workflow entities exist
- ✅ WorkflowService exists
- ✅ Folder entity and FolderService exist
- ❌ Auto-workflow creation on Tender Notice upload not implemented
- ❌ Folder selection mandatory enforcement missing
- ❌ Folder-to-workflow mapping not implemented
- ❌ Multiple PS/PG/Bills per workflow not supported

**Tasks**:
- [ ] Make folder selection mandatory for Tender Notice uploads (FR-006a)
- [ ] Add validation to prevent Tender Notice upload if folder is not selected
- [ ] Implement automatic workflow creation when Tender Notice is uploaded
- [ ] Use folder name as workflow name (FR-006b)
- [ ] Create one-to-one folder-to-workflow mapping system (FR-006c, FR-006d)
- [ ] Prevent multiple Tender Notices in the same folder (one workflow per folder)
- [ ] Validate that selected folder has associated workflow before allowing related document uploads
- [ ] Implement folder-based workflow association for subsequent documents (FR-006d)
- [ ] Remove separate workflow selection UI - use folder selection instead
- [ ] Support workflow document types: Tender Document, Contract, PS (multiple), PG (multiple), Bills (multiple), Correspondence (multiple), and others
- [ ] Clarify which document types are single vs multiple per workflow (FR-006f)
- [ ] Implement multiple documents of same type (PS, PG, Bills, Correspondence) per workflow
- [ ] Add workflow tracking and status dashboard
- [ ] Add alerts for missing workflow documents
- [ ] Create workflow completion tracking (based on key documents uploaded)
- [ ] Add error handling for edge cases (folder without workflow, duplicate Tender Notice attempts)

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/TenderWorkflowService.java` (new)
- `backend/src/main/java/com/bpdb/dms/service/FileUploadService.java` (modify - add folder validation, workflow creation, duplicate prevention)
- `backend/src/main/java/com/bpdb/dms/entity/Folder.java` (add one-to-one workflow relationship)
- `backend/src/main/java/com/bpdb/dms/entity/Workflow.java` (add folder relationship)
- `backend/src/main/java/com/bpdb/dms/service/FolderService.java` (add workflow validation methods)
- `backend/src/main/java/com/bpdb/dms/controller/FolderController.java` (add workflow mapping endpoints, validation)
- `backend/src/main/resources/db/changelog/XXX-add-folder-workflow-mapping.xml` (new - add foreign key)
- `frontend/src/pages/TenderWorkflows.tsx` (new)
- `frontend/src/components/DocumentUpload.tsx` (modify - enforce folder selection for Tender Notice, remove workflow selector, add error handling)
- `frontend/src/components/FolderSelector.tsx` (new or modify - add validation messages)

---

### 3. Full-Text OCR-Based Search Enhancement (FR-042)
**Status**: ⚠️ **PARTIAL**  
**Priority**: CRITICAL  
**Requirements**: FR-042, FR-043, FR-044

**Current State**:
- ✅ Elasticsearch integration exists
- ✅ DocumentIndex entity created
- ✅ Basic search implemented
- ❌ Boolean search operators not fully implemented
- ❌ Advanced metadata search filters incomplete
- ❌ Search suggestions/auto-complete missing

**Tasks**:
- [ ] Implement Boolean search operators (AND, OR, NOT)
- [ ] Enhance metadata search with specific field targeting (Tender No, PO No, Vendor, Date)
- [ ] Add search suggestions and auto-complete functionality
- [ ] Implement search result highlighting
- [ ] Add saved searches functionality (FR-045a)
- [ ] Create search analytics and optimization

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/SearchService.java` (enhance)
- `backend/src/main/java/com/bpdb/dms/controller/SearchController.java` (enhance)
- `frontend/src/pages/Search.tsx` (enhance)
- `frontend/src/components/SearchSuggestions.tsx` (new)

---

### 4. Two-Factor Authentication (2FA) (FR-059)
**Status**: ❌ **NOT STARTED**  
**Priority**: CRITICAL  
**Requirements**: FR-059

**Current State**:
- ✅ Basic authentication implemented
- ✅ JWT tokens working
- ❌ 2FA not implemented
- ❌ TOTP support missing
- ❌ SMS/Email 2FA missing

**Tasks**:
- [ ] Implement TOTP (Time-based One-Time Password) support
- [ ] Add 2FA configuration in user settings
- [ ] Create QR code generation for authenticator apps
- [ ] Implement SMS-based 2FA option
- [ ] Implement Email-based 2FA option
- [ ] Add 2FA backup codes
- [ ] Create 2FA setup and verification UI

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/TwoFactorAuthService.java` (new)
- `backend/src/main/java/com/bpdb/dms/entity/User.java` (add 2FA fields)
- `backend/src/main/java/com/bpdb/dms/controller/AuthController.java` (enhance)
- `frontend/src/components/TwoFactorAuthSetup.tsx` (new)
- `frontend/src/pages/Settings.tsx` (add 2FA section)

---

### 5. Document Watermarking for Downloads (FR-050)
**Status**: ❌ **NOT STARTED**  
**Priority**: CRITICAL  
**Requirements**: FR-050

**Current State**:
- ✅ Secure download implemented
- ✅ Access control verification working
- ✅ Download tracking exists
- ❌ Watermarking not implemented

**Tasks**:
- [ ] Implement PDF watermarking library integration
- [ ] Add watermark configuration (text, image, position)
- [ ] Create watermark templates (user info, timestamp, document ID)
- [ ] Add watermarking option in download settings
- [ ] Implement image watermarking for image files
- [ ] Add watermark preview functionality

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/WatermarkService.java` (new)
- `backend/src/main/java/com/bpdb/dms/controller/DocumentController.java` (enhance)
- `frontend/src/components/WatermarkSettings.tsx` (new)

---

### 2a. APP Manual Entry Form (FR-241-243)
**Status**: ❌ **NOT STARTED**  
**Priority**: CRITICAL  
**Requirements**: FR-241, FR-242, FR-243

**Current State**:
- ✅ APP entities exist (app_headers, app_lines tables)
- ✅ AppDocumentService exists
- ❌ APP Excel upload currently implemented (needs to be removed)
- ❌ Manual entry form not implemented
- ❌ New APP fields not in database schema

**Tasks**:
- [ ] Remove APP Excel upload functionality
- [ ] Remove APP as document type from DocumentType enum
- [ ] Update database schema to support new APP fields:
  - Fiscal Year (dropdown)
  - Allocation/Release Type (Annual/Revised/Additional/Emergency)
  - Budget Release Date
  - Allocation Amount (BDT)
  - Release Installment No. (auto/manual)
  - Reference/Memo Number
  - Attachment (PDF upload)
- [ ] Create APP manual entry form in frontend
- [ ] Implement fiscal year dropdown with predefined values (e.g., 2024-25, 2025-26, etc.)
- [ ] Implement allocation type dropdown (Annual/Revised/Additional/Emergency)
- [ ] Add date picker for budget release date
- [ ] Add installment number auto-increment logic (based on existing entries for same fiscal year)
- [ ] Allow manual override of installment number
- [ ] Implement PDF attachment upload for budget release letter
- [ ] Add validation for duplicate fiscal year/installment combinations (warn user, allow confirmation to proceed)
- [ ] Add required field validation (Fiscal Year, Allocation Type, Budget Release Date, Allocation Amount)
- [ ] Update APP service to handle manual entry instead of Excel parsing
- [ ] Add form validation error messages and user feedback

**Files to Create/Modify**:
- `backend/src/main/resources/db/changelog/XXX-update-app-entries-schema.xml` (new - update schema)
- `backend/src/main/java/com/bpdb/dms/entity/AppEntry.java` (new or modify - update fields)
- `backend/src/main/java/com/bpdb/dms/service/AppEntryService.java` (new - manual entry service)
- `backend/src/main/java/com/bpdb/dms/controller/AppEntryController.java` (new or modify)
- `backend/src/main/java/com/bpdb/dms/service/AppExcelImportService.java` (remove or deprecate)
- `backend/src/main/java/com/bpdb/dms/model/DocumentType.java` (remove APP enum value)
- `frontend/src/pages/AppEntries.tsx` (complete rewrite - manual form instead of Excel display)
- `frontend/src/components/AppEntryForm.tsx` (new)
- `frontend/src/constants/documentTypes.ts` (remove APP from DocumentType enum)

---

### 2b. Bill Document Type and OCR Extraction (FR-244-251)
**Status**: ❌ **NOT STARTED**  
**Priority**: CRITICAL  
**Requirements**: FR-244, FR-245, FR-246, FR-247, FR-248, FR-249, FR-250, FR-251

**Current State**:
- ✅ BillHeader and BillLine entities exist
- ✅ BillService exists for manual entry
- ✅ OCRService exists
- ❌ Bill not defined as document type (currently mapped to OTHER)
- ❌ Manual bill entry currently implemented (needs to be changed to upload only)
- ❌ OCR extraction for bills not implemented
- ❌ Bill verification interface not implemented

**Tasks**:
- [ ] Add BILL as document type in DocumentType enum
- [ ] Remove manual bill entry form (replace with upload-only interface)
- [ ] Implement bill upload via image/PDF files only (JPEG, PNG, TIFF, PDF formats)
- [ ] Create OCR extraction service for bills:
  - Extract vendor name
  - Extract invoice number
  - Extract invoice date
  - Extract fiscal year
  - Extract line items with amounts
  - Extract tax amounts
  - Extract total amounts
- [ ] Track OCR confidence scores for each extracted field
- [ ] Handle OCR extraction failures gracefully (allow full manual entry if OCR fails)
- [ ] Implement bill verification interface for reviewing OCR-extracted data
- [ ] Highlight fields with low OCR confidence scores in verification interface
- [ ] Add manual correction capability for all OCR fields
- [ ] Store original OCR-extracted values separately from corrected values (for accuracy tracking)
- [ ] Link bills to fiscal year and APP lines
- [ ] Validate bill year matches APP year when linked
- [ ] Auto-calculate totals from extracted line items
- [ ] Validate arithmetic consistency (sum of line items + tax should equal total)
- [ ] Store original uploaded bill file as attachment and document
- [ ] Index bill files for search (document search, not just bill entries)
- [ ] Add Bill to tender workflow document types (supports multiple bills per workflow)
- [ ] Track OCR accuracy metrics for continuous improvement

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/model/DocumentType.java` (add BILL enum value)
- `backend/src/main/java/com/bpdb/dms/service/BillOCRService.java` (new)
- `backend/src/main/java/com/bpdb/dms/service/OCRService.java` (enhance - add bill-specific extraction)
- `backend/src/main/java/com/bpdb/dms/service/BillService.java` (modify - remove manual entry, add OCR processing)
- `backend/src/main/java/com/bpdb/dms/controller/FinanceController.java` (modify - add bill upload endpoint)
- `backend/src/main/java/com/bpdb/dms/dto/BillOCRResult.java` (new)
- `frontend/src/pages/BillEntries.tsx` (complete rewrite - file upload with OCR verification)
- `frontend/src/components/BillUploadForm.tsx` (new)
- `frontend/src/components/BillVerificationForm.tsx` (new)
- `frontend/src/constants/documentTypes.ts` (add BILL to DocumentType enum)

---

## 🟡 HIGH PRIORITY (Important Features)

### 6. Smart Folders (DMC) - Predefined Templates (FR-033e)
**Status**: ⚠️ **PARTIAL**  
**Priority**: HIGH  
**Requirements**: FR-033e, FR-033f, FR-033g, FR-033h

**Current State**:
- ✅ SmartFolderEvaluationService exists
- ✅ SmartFolderDefinition entity created
- ✅ Basic rule evaluation working
- ❌ Predefined Smart Folders not created
- ❌ User personalization missing
- ❌ Dashboard embedding not implemented

**Tasks**:
- [ ] Create predefined Smart Folder templates:
  - "My Pending Approvals"
  - "Upcoming Expiries (30/15/7 days)"
  - "Unclassified/Needs Review"
  - "Recent Uploads"
  - "By Vendor/PO/LC/BG/PS/PG"
  - "By Tender"
- [ ] Implement user personalization (column sets, sort, pinned filters)
- [ ] Add quick actions (bulk operations)
- [ ] Implement drill-through to linked document sets
- [ ] Add relationship badges display
- [ ] Create Smart Folder export functionality
- [ ] Implement dashboard widget embedding

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/SmartFolderTemplateService.java` (new)
- `backend/src/main/resources/db/changelog/XXX-create-smart-folder-templates.xml` (new)
- `frontend/src/components/SmartFolderTemplates.tsx` (new)
- `frontend/src/components/SmartFolderPersonalization.tsx` (new)

---

### 7. Folder Summary Views (FR-039)
**Status**: ❌ **NOT STARTED**  
**Priority**: HIGH  
**Requirements**: FR-039

**Current State**:
- ✅ Folder entity exists
- ✅ FolderService implemented
- ✅ Folder navigation working
- ❌ Folder summary statistics not implemented

**Tasks**:
- [ ] Implement folder summary statistics:
  - Total files count
  - Uploaded files count
  - Remaining uploads (for workflow folders)
- [ ] Create folder summary API endpoint
- [ ] Add folder summary UI component
- [ ] Implement real-time summary updates
- [ ] Add summary to folder explorer view

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/FolderService.java` (enhance)
- `backend/src/main/java/com/bpdb/dms/controller/FolderController.java` (add summary endpoint)
- `frontend/src/components/FolderSummary.tsx` (enhance existing)

---

### 8. Export Search Results (FR-052)
**Status**: ⚠️ **PARTIAL**  
**Priority**: HIGH  
**Requirements**: FR-052

**Current State**:
- ✅ PDF report generation exists (PdfReportService)
- ✅ Excel report generation exists (ExcelReportService)
- ❌ Search results export not implemented
- ❌ Export with search criteria missing

**Tasks**:
- [ ] Implement search results export to PDF
- [ ] Implement search results export to Excel
- [ ] Include search criteria in exported reports
- [ ] Add metadata and document references to export
- [ ] Create export options UI (format selection, fields selection)
- [ ] Add bulk export functionality

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/SearchExportService.java` (new)
- `backend/src/main/java/com/bpdb/dms/controller/SearchController.java` (add export endpoint)
- `frontend/src/pages/Search.tsx` (add export button)

---

### 9. Performance Security (PS) Renewal Form (FR-074, FR-075)
**Status**: ⚠️ **PARTIAL**  
**Priority**: HIGH  
**Requirements**: FR-070 to FR-075

**Current State**:
- ✅ ExpiryTrackingService exists
- ✅ PS tracking implemented
- ✅ Renewal alerts working
- ❌ Renewal update form not fully implemented
- ❌ Document upload during renewal missing

**Tasks**:
- [ ] Create PS renewal form UI
- [ ] Implement date range selection for new coverage period
- [ ] Add renewal approval workflow
- [ ] Implement document upload during renewal
- [ ] Link new documents to existing PS records
- [ ] Add version tracking for PS documents
- [ ] Create renewal history view

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/PSRenewalService.java` (new or enhance)
- `backend/src/main/java/com/bpdb/dms/controller/ExpiryTrackingController.java` (add renewal endpoints)
- `frontend/src/pages/PSRenewal.tsx` (new)
- `frontend/src/components/PSRenewalForm.tsx` (new)

---

### 10. Reminder Dashboard (FR-078)
**Status**: ⚠️ **PARTIAL**  
**Priority**: HIGH  
**Requirements**: FR-078

**Current State**:
- ✅ ExpiryTrackingService provides data
- ✅ NotificationService exists
- ✅ Dashboard exists
- ❌ Unified reminder dashboard not implemented
- ❌ Quick actions missing

**Tasks**:
- [ ] Create unified reminder dashboard component
- [ ] Display pending renewals list
- [ ] Show upcoming expiries (30/15/7 days)
- [ ] Add compliance status indicators
- [ ] List missing documents
- [ ] Show delayed uploads
- [ ] Add alert summary by category and priority
- [ ] Implement quick actions (direct links to address items)

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/ReminderDashboardService.java` (new)
- `backend/src/main/java/com/bpdb/dms/controller/DashboardController.java` (add reminder endpoint)
- `frontend/src/pages/ReminderDashboard.tsx` (new)
- `frontend/src/components/ReminderSummary.tsx` (new)

---

### 11. Document Preview for Office Documents (FR-049)
**Status**: ⚠️ **PARTIAL**  
**Priority**: HIGH  
**Requirements**: FR-049

**Current State**:
- ✅ PDF preview working
- ✅ Image preview working
- ❌ Word document preview not implemented
- ❌ Excel document preview not implemented
- ❌ In-browser preview for Office docs missing

**Tasks**:
- [ ] Integrate Office document viewer (e.g., Office Online, OnlyOffice, or similar)
- [ ] Implement Word document preview
- [ ] Implement Excel document preview
- [ ] Add in-browser preview without download
- [ ] Handle large Office documents
- [ ] Add preview loading states

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/DocumentPreviewService.java` (enhance)
- `frontend/src/components/DocumentViewer.tsx` (enhance)
- `frontend/src/components/OfficeDocumentPreview.tsx` (new)

---

### 12. Dual Start Logic for Contracts (FR-097)
**Status**: ❌ **NOT STARTED**  
**Priority**: HIGH  
**Requirements**: FR-097

**Current State**:
- ✅ Document relationships exist
- ✅ Expiry tracking implemented
- ❌ Dual start logic (Sign Date vs LC Opening Date) not implemented
- ❌ Contract timeline activation method missing

**Tasks**:
- [ ] Add activation method field to Contract documents
- [ ] Implement Sign Date activation logic
- [ ] Implement LC Opening Date activation logic
- [ ] Create user-selectable activation method UI
- [ ] Calculate validity periods based on selected start date
- [ ] Support different validity periods per activation method
- [ ] Update expiry tracking to use correct start date

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/entity/Document.java` (add activationMethod field)
- `backend/src/main/java/com/bpdb/dms/service/ContractLifecycleService.java` (new)
- `frontend/src/components/ContractActivationSelector.tsx` (new)

---

## 🟠 MEDIUM PRIORITY (Enhancement Features)

### 13. Auto Indexing and Naming Convention (FR-013, FR-014)
**Status**: ⚠️ **PARTIAL**  
**Priority**: MEDIUM  
**Requirements**: FR-013, FR-014

**Current State**:
- ✅ DocumentIndexingService exists
- ✅ Basic indexing implemented
- ❌ Automatic naming convention not fully implemented
- ❌ Predefined naming rules missing

**Tasks**:
- [ ] Define naming convention rules per document type
- [ ] Implement automatic file naming based on metadata
- [ ] Add naming pattern configuration
- [ ] Create naming convention templates
- [ ] Implement automatic indexing on upload
- [ ] Add naming validation

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/DocumentNamingService.java` (new)
- `backend/src/main/java/com/bpdb/dms/service/FileUploadService.java` (enhance)
- `backend/src/main/java/com/bpdb/dms/config/NamingConventionConfig.java` (new)

---

### 14. Tender Item Tracking (FR-018)
**Status**: ❌ **NOT STARTED**  
**Priority**: MEDIUM  
**Requirements**: FR-018

**Current State**:
- ✅ OCR extraction working
- ✅ Metadata extraction exists
- ❌ Tender item tracking not implemented
- ❌ Manual entry for tender items missing

**Tasks**:
- [ ] Create TenderItem entity
- [ ] Implement OCR-based tender item extraction
- [ ] Add manual tender item entry UI
- [ ] Link tender items to Tender documents
- [ ] Create tender item tracking dashboard
- [ ] Add tender item search and filtering

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/entity/TenderItem.java` (new)
- `backend/src/main/java/com/bpdb/dms/service/TenderItemService.java` (new)
- `backend/src/main/java/com/bpdb/dms/controller/TenderItemController.java` (new)
- `frontend/src/pages/TenderItems.tsx` (new)

---

### 15. Document Attachment Linking (FR-016)
**Status**: ⚠️ **PARTIAL**  
**Priority**: MEDIUM  
**Requirements**: FR-016

**Current State**:
- ✅ Document relationships exist
- ✅ Document linking implemented
- ❌ Attachment linking (parent-child) not fully implemented
- ❌ Attachment management UI missing

**Tasks**:
- [ ] Implement parent-child document relationships
- [ ] Add attachment linking functionality
- [ ] Create attachment management UI
- [ ] Add attachment count display
- [ ] Implement attachment upload from document view
- [ ] Add attachment download/removal

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/entity/Document.java` (add parentDocument field)
- `backend/src/main/java/com/bpdb/dms/service/DocumentAttachmentService.java` (new)
- `frontend/src/components/DocumentAttachments.tsx` (new)

---

### 16. Monthly/Quarterly Summaries (FR-084, FR-085)
**Status**: ⚠️ **PARTIAL**  
**Priority**: MEDIUM  
**Requirements**: FR-084, FR-085

**Current State**:
- ✅ ReportingService exists
- ✅ Report generation working
- ❌ Automated monthly summaries not implemented
- ❌ Automated quarterly summaries not implemented
- ❌ Scheduled report generation missing

**Tasks**:
- [ ] Implement monthly summary report generation
- [ ] Implement quarterly summary report generation
- [ ] Add scheduled report generation (cron jobs)
- [ ] Create email delivery for scheduled reports
- [ ] Add report templates for monthly/quarterly summaries
- [ ] Implement report archival

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/MonthlySummaryService.java` (new)
- `backend/src/main/java/com/bpdb/dms/service/QuarterlySummaryService.java` (new)
- `backend/src/main/java/com/bpdb/dms/config/ScheduledReportConfig.java` (new)
- `backend/src/main/java/com/bpdb/dms/controller/ReportingController.java` (add scheduled endpoints)

---

### 17. Version Comparison and Diff (FR-089, FR-226)
**Status**: ⚠️ **PARTIAL**  
**Priority**: MEDIUM  
**Requirements**: FR-089, FR-226

**Current State**:
- ✅ DocumentVersioningService exists
- ✅ Version history tracking implemented
- ❌ Version comparison UI not implemented
- ❌ Diff visualization missing

**Tasks**:
- [ ] Implement version comparison algorithm
- [ ] Create diff visualization component
- [ ] Add side-by-side version comparison
- [ ] Implement change highlighting
- [ ] Add metadata change tracking
- [ ] Create version comparison API endpoint

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/VersionComparisonService.java` (new)
- `backend/src/main/java/com/bpdb/dms/controller/DocumentVersioningController.java` (add comparison endpoint)
- `frontend/src/components/VersionComparison.tsx` (new)

---

### 18. Department-Level Access Restrictions (FR-055)
**Status**: ⚠️ **PARTIAL**  
**Priority**: MEDIUM  
**Requirements**: FR-055, FR-056, FR-057

**Current State**:
- ✅ User department assignment exists
- ✅ Basic access control implemented
- ❌ Department-level document visibility not fully enforced
- ❌ Cross-department sharing approval workflow missing

**Tasks**:
- [ ] Enforce department-level document visibility
- [ ] Implement cross-department sharing with approval
- [ ] Add department hierarchy support
- [ ] Create department access configuration UI
- [ ] Implement permission inheritance from parent departments
- [ ] Add department-based Smart Folder filtering

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/service/DepartmentAccessService.java` (new)
- `backend/src/main/java/com/bpdb/dms/security/SecurityConfig.java` (enhance)
- `frontend/src/components/DepartmentAccessSettings.tsx` (new)

---

### 19. Secure File Sharing Among Users (FR-061)
**Status**: ⚠️ **PARTIAL**  
**Priority**: MEDIUM  
**Requirements**: FR-061

**Current State**:
- ✅ Document access control exists
- ❌ User-to-user sharing not implemented
- ❌ Time-limited sharing links missing
- ❌ Permission-based sharing missing

**Tasks**:
- [ ] Implement user-to-user document sharing
- [ ] Add time-limited sharing links
- [ ] Create permission-based sharing (view-only, download, edit)
- [ ] Implement sharing notification system
- [ ] Add sharing approval workflows
- [ ] Create sharing audit trail
- [ ] Build sharing management UI

**Files to Create/Modify**:
- `backend/src/main/java/com/bpdb/dms/entity/DocumentShare.java` (new)
- `backend/src/main/java/com/bpdb/dms/service/DocumentSharingService.java` (new)
- `backend/src/main/java/com/bpdb/dms/controller/DocumentSharingController.java` (new)
- `frontend/src/components/DocumentSharing.tsx` (new)

---

### 20. Explorer-Style Folder Interface (FR-040)
**Status**: ⚠️ **PARTIAL**  
**Priority**: MEDIUM  
**Requirements**: FR-040, FR-041

**Current State**:
- ✅ FolderExplorer component exists
- ✅ Basic folder navigation working
- ❌ Hierarchical tree structure not fully implemented
- ❌ Breadcrumb navigation missing
- ❌ Visual folder representation incomplete

**Tasks**:
- [ ] Enhance folder tree structure display
- [ ] Implement breadcrumb navigation
- [ ] Add quick folder access and switching
- [ ] Improve visual folder representation
- [ ] Add folder drag-and-drop
- [ ] Implement folder context menu

**Files to Create/Modify**:
- `frontend/src/components/FolderExplorer.tsx` (enhance)
- `frontend/src/components/FolderTree.tsx` (enhance)
- `frontend/src/components/BreadcrumbNavigation.tsx` (new)

---

## 🟢 LOW PRIORITY (Future Enhancements)

### 21. Advanced AI Features (FR-124 to FR-199)
**Status**: ❌ **NOT STARTED**  
**Priority**: LOW (Future Phase)  
**Requirements**: Section 4.12

**Tasks**:
- [ ] AI-powered document classification (FR-128)
- [ ] Natural language search (FR-133 to FR-137)
- [ ] AI assistant integration (FR-138 to FR-142)
- [ ] Advanced automation workflows (FR-143 to FR-146)
- [ ] E-signature integration (FR-151 to FR-154)
- [ ] Automated retention and disposition (FR-155 to FR-158)
- [ ] Legal hold management (FR-167 to FR-170)
- [ ] AI-assisted Smart Folder suggestions (FR-192, FR-193)
- [ ] Intelligent document grouping (FR-196 to FR-199)

**Note**: These are planned for Phase 6 (Months 16-18) per implementation plan.

---

### 22. Mobile and Offline Capabilities (FR-200 to FR-215)
**Status**: ❌ **NOT STARTED**  
**Priority**: LOW (Future Phase)  
**Requirements**: Section 4.14

**Tasks**:
- [ ] Native iOS application (FR-200)
- [ ] Native Android application (FR-200)
- [ ] Mobile document scanning and upload (FR-201)
- [ ] Offline document access (FR-205)
- [ ] Offline synchronization (FR-207)
- [ ] Conflict resolution (FR-208)
- [ ] Push notifications (FR-211)
- [ ] Mobile biometric authentication (FR-214)

**Note**: These are planned for Phase 9 (Months 39-44) per implementation plan.

---

### 23. Advanced Collaboration Features (FR-216 to FR-240)
**Status**: ❌ **NOT STARTED**  
**Priority**: LOW (Future Phase)  
**Requirements**: Section 4.15

**Tasks**:
- [ ] Real-time collaborative editing (FR-216 to FR-220)
- [ ] Document annotation system (FR-221 to FR-225)
- [ ] Advanced version control with branching (FR-226 to FR-230)
- [ ] Secure external sharing (FR-231 to FR-235)
- [ ] Collaboration workspaces (FR-236 to FR-240)

**Note**: These are planned for Phase 10 (Months 45-50) per implementation plan.

---

## 📋 Testing & Quality Assurance

### 24. Comprehensive Testing
**Status**: ⚠️ **PARTIAL**  
**Priority**: HIGH

**Current State**:
- ✅ Some integration tests exist
- ✅ Test plan document created
- ❌ Full test coverage missing
- ❌ E2E tests not implemented

**Tasks**:
- [ ] Increase unit test coverage to >80%
- [ ] Complete integration tests for all services
- [ ] Implement E2E tests for critical workflows
- [ ] Add performance testing
- [ ] Create automated test suite for CI/CD
- [ ] Add load testing for concurrent users
- [ ] Implement security testing

---

## 🔧 Technical Debt & Improvements

### 25. Elasticsearch Full Integration
**Status**: ⚠️ **PARTIAL**  
**Priority**: MEDIUM

**Current State**:
- ✅ Elasticsearch dependency added
- ✅ DocumentIndex entity created
- ✅ Basic indexing working
- ❌ Full Elasticsearch query capabilities not utilized
- ❌ Faceted search not implemented
- ❌ Search analytics missing

**Tasks**:
- [ ] Implement complex Elasticsearch queries
- [ ] Add faceted search functionality
- [ ] Implement search analytics
- [ ] Add search result ranking
- [ ] Optimize search performance
- [ ] Add search index management

---

### 26. Performance Optimization
**Status**: ⚠️ **ONGOING**  
**Priority**: MEDIUM

**Tasks**:
- [ ] Optimize database queries
- [ ] Implement query result caching
- [ ] Add pagination for large result sets
- [ ] Optimize file upload processing
- [ ] Implement async processing for heavy operations
- [ ] Add CDN for static assets
- [ ] Optimize frontend bundle size

---

### 27. Security Hardening
**Status**: ⚠️ **ONGOING**  
**Priority**: HIGH

**Tasks**:
- [ ] Implement rate limiting
- [ ] Add input validation and sanitization
- [ ] Implement CSRF protection
- [ ] Add security headers
- [ ] Conduct security audit
- [ ] Implement data encryption at rest
- [ ] Add security monitoring and alerting

---

## 📝 Documentation

### 28. API Documentation
**Status**: ⚠️ **PARTIAL**  
**Priority**: MEDIUM

**Tasks**:
- [ ] Complete OpenAPI/Swagger documentation
- [ ] Add API examples
- [ ] Document error responses
- [ ] Create API usage guides
- [ ] Add authentication documentation

---

### 29. User Documentation
**Status**: ⚠️ **PARTIAL**  
**Priority**: MEDIUM

**Tasks**:
- [ ] Create user manual
- [ ] Add feature guides
- [ ] Create video tutorials
- [ ] Add FAQ section
- [ ] Create admin documentation

---

## 🎯 Quick Wins (Can be done quickly)

### 30. Search Filter Enhancements
- [ ] Add more filter options (vendor, status, custom metadata)
- [ ] Implement filter presets
- [ ] Add filter combination logic

### 31. Dashboard Widgets
- [ ] Add more chart types
- [ ] Implement interactive filters
- [ ] Add drill-down capabilities

### 32. Notification Preferences
- [ ] Add user notification preferences UI
- [ ] Implement channel selection (Email/SMS/In-App)
- [ ] Add notification frequency settings

---

## 📊 Progress Tracking

**Last Review Date**: [Current Date]  
**Next Review Date**: [Next Review Date]  
**Review Frequency**: Weekly

**Key Metrics**:
- Total Requirements: 138 (updated with new requirements)
- Implemented: 47 (34%)
- Partial: 20 (14%)
- Not Started: 71 (51%)

**Focus Areas for Next Sprint**:
1. **Phase 1**: Document Type Foundation (Remove APP, Add BILL)
2. **Phase 2**: Tender Workflow Auto-Creation (with folder-based selection)
3. **Phase 3**: APP Manual Entry Form Implementation
4. **Phase 3**: Bill Document Type and OCR Extraction

**See [IMPLEMENTATION_PHASES.md](./IMPLEMENTATION_PHASES.md) for complete phased implementation plan with detailed tasks, dependencies, and timelines.**

---

*This TODO list is based on comprehensive review of requirements.md v1.1 and current codebase implementation status. It should be updated regularly as features are completed.*
