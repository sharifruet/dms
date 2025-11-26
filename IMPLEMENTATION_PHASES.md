# DMS Implementation Phases - Based on TODOs

**Last Updated**: [Current Date]  
**Based on**: TODO.md and Requirements Specification v1.2  
**Approach**: Incremental delivery with focus on critical path items

---

## Phase Overview

| Phase | Duration | Focus Area | Critical Path | Status |
|-------|----------|------------|---------------|--------|
| **Phase 1** | 2-3 weeks | Document Type Foundation | ✅ Critical | ✅ **COMPLETED** |
| **Phase 2** | 3-4 weeks | Workflow & Folder Integration | ✅ Critical | 🔴 Not Started |
| **Phase 3** | 3-4 weeks | Finance & Billing Overhaul | ✅ Critical | 🔴 Not Started |
| **Phase 4** | 2-3 weeks | Document Type Detection | ⚠️ High | 🔴 Not Started |
| **Phase 5** | 2-3 weeks | Search & Export Enhancement | ⚠️ High | 🔴 Not Started |
| **Phase 6** | 2-3 weeks | Security & Watermarking | ⚠️ High | 🔴 Not Started |
| **Phase 7** | 2-3 weeks | Smart Folders & Organization | 🟡 Medium | 🔴 Not Started |
| **Phase 8** | 2-3 weeks | Reminder & Notification Features | 🟡 Medium | 🔴 Not Started |
| **Phase 9** | 1-2 weeks | Document Management Polish | 🟡 Medium | 🔴 Not Started |
| **Phase 10** | Ongoing | Technical Debt & Optimization | 🟢 Low | 🔴 Not Started |

**Total Estimated Duration**: 21-30 weeks (5-7.5 months)

---

## Phase 1: Document Type Foundation (2-3 weeks)
**Priority**: 🔴 CRITICAL  
**Dependencies**: None  
**Blocks**: Phase 2, Phase 3, Phase 4

### Objectives
- Update document type definitions to align with new requirements
- Remove APP as document type
- Add BILL as document type
- Prepare foundation for workflow and finance features

### Tasks from TODO

#### 1.1 Remove APP from Document Types
- [ ] Remove APP from DocumentType enum in backend
- [ ] Remove APP from frontend DocumentType constants
- [ ] Update all references to APP document type
- [ ] Update document type validation logic
- [ ] Remove APP from document type auto-detection patterns

**Files**:
- `backend/src/main/java/com/bpdb/dms/model/DocumentType.java`
- `frontend/src/constants/documentTypes.ts`
- `backend/src/main/java/com/bpdb/dms/service/FileUploadService.java`

#### 1.2 Add BILL as Document Type
- [ ] Add BILL enum value to DocumentType
- [ ] Add BILL to frontend DocumentType constants
- [ ] Add BILL to document type labels and colors
- [ ] Update document type validation to include BILL
- [ ] Add BILL to OCR classification patterns

**Files**:
- `backend/src/main/java/com/bpdb/dms/model/DocumentType.java`
- `frontend/src/constants/documentTypes.ts`
- `backend/src/main/java/com/bpdb/dms/service/OCRService.java`

#### 1.3 Update Document Type Lists and References
- [ ] Update requirements documentation references
- [ ] Update UI components that display document types
- [ ] Update search filters and dropdowns
- [ ] Update workflow document type lists
- [ ] Update Smart Folder templates

**Files**:
- All components using document types
- `frontend/src/pages/Documents.tsx`
- `frontend/src/components/DocumentTypeSelector.tsx`

### Deliverables
- ✅ APP removed from all document type definitions
- ✅ BILL added as valid document type
- ✅ All references updated and tested
- ✅ No broken functionality from type changes

### Success Criteria
- System compiles without errors
- No APP references remain in document type code
- BILL is recognized as valid document type
- Existing documents with APP type handled gracefully
- All tests pass

---

## Phase 2: Workflow & Folder Integration (3-4 weeks)
**Priority**: 🔴 CRITICAL  
**Dependencies**: Phase 1  
**Blocks**: Phase 3 (Bill workflow integration)

### Objectives
- Implement folder-based workflow system
- Auto-create workflows on Tender Notice upload
- Establish one-to-one folder-workflow mapping
- Support multiple documents per workflow
- Remove separate workflow selection UI

### Tasks from TODO

#### 2.1 Database Schema Updates
- [ ] Create database migration for folder-workflow mapping
- [ ] Add workflow_id foreign key to folders table
- [ ] Add folder_id foreign key to workflows table (one-to-one relationship)
- [ ] Add indexes for performance
- [ ] Create migration rollback scripts

**Files**:
- `backend/src/main/resources/db/changelog/XXX-add-folder-workflow-mapping.xml`

#### 2.2 Backend Entity Updates
- [ ] Add workflow relationship to Folder entity
- [ ] Add folder relationship to Workflow entity
- [ ] Update entity relationships (one-to-one)
- [ ] Add validation annotations
- [ ] Update repository methods

**Files**:
- `backend/src/main/java/com/bpdb/dms/entity/Folder.java`
- `backend/src/main/java/com/bpdb/dms/entity/Workflow.java`

#### 2.3 Tender Workflow Service
- [ ] Create TenderWorkflowService
- [ ] Implement automatic workflow creation on Tender Notice upload
- [ ] Implement folder-to-workflow mapping logic
- [ ] Add validation to prevent multiple Tender Notices per folder
- [ ] Add workflow status tracking
- [ ] Add workflow completion logic

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/TenderWorkflowService.java` (new)
- `backend/src/main/java/com/bpdb/dms/service/FolderService.java` (enhance)

#### 2.4 File Upload Service Updates
- [ ] Add folder validation for Tender Notice uploads
- [ ] Implement automatic workflow creation logic
- [ ] Add duplicate Tender Notice prevention
- [ ] Add folder-workflow association for related documents
- [ ] Update document upload to use folder-based workflow association

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/FileUploadService.java`

#### 2.5 Folder Service Enhancements
- [ ] Add workflow validation methods
- [ ] Add getWorkflowByFolder method
- [ ] Add checkIfFolderHasWorkflow method
- [ ] Add workflow association validation
- [ ] Update folder creation to support workflow linking

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/FolderService.java`

#### 2.6 Controller Updates
- [ ] Add workflow mapping endpoints to FolderController
- [ ] Add validation endpoints
- [ ] Update document upload endpoints
- [ ] Add error handling for workflow-related errors

**Files**:
- `backend/src/main/java/com/bpdb/dms/controller/FolderController.java`
- `backend/src/main/java/com/bpdb/dms/controller/DocumentController.java`

#### 2.7 Frontend Workflow UI Updates
- [ ] Remove workflow selector from document upload form
- [ ] Make folder selection mandatory for Tender Notice uploads
- [ ] Add validation messages for folder selection
- [ ] Add error handling for workflow-related errors
- [ ] Update folder selector component with validation
- [ ] Create Tender Workflows page/dashboard

**Files**:
- `frontend/src/components/DocumentUpload.tsx`
- `frontend/src/components/FolderSelector.tsx` (new or enhance)
- `frontend/src/pages/TenderWorkflows.tsx` (new)
- `frontend/src/pages/DocumentsEnhanced.tsx`

#### 2.8 Multiple Documents Per Workflow Support
- [ ] Update workflow document tracking to support multiple instances
- [ ] Add support for multiple PS, PG, Bills, Correspondence per workflow
- [ ] Update workflow status calculation
- [ ] Add document instance tracking
- [ ] Update workflow dashboard to show multiple documents

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/TenderWorkflowService.java`
- `backend/src/main/java/com/bpdb/dms/entity/WorkflowInstance.java` (enhance if needed)

### Deliverables
- ✅ Database schema with folder-workflow mapping
- ✅ Automatic workflow creation on Tender Notice upload
- ✅ One-to-one folder-workflow relationship enforced
- ✅ Folder-based workflow association for all documents
- ✅ Multiple document support per workflow
- ✅ Updated UI without separate workflow selector

### Success Criteria
- Tender Notice uploads require folder selection
- Workflow automatically created with folder name
- Related documents associated via folder selection
- Multiple PS/PG/Bills can be added to same workflow
- No separate workflow selection UI needed
- All edge cases handled with appropriate error messages

---

## Phase 3: Finance & Billing Overhaul (3-4 weeks)
**Priority**: 🔴 CRITICAL  
**Dependencies**: Phase 1, Phase 2 (for Bill workflow integration)

### Objectives
- Replace APP Excel upload with manual entry form
- Implement Bill document type with OCR extraction
- Create verification interfaces for OCR data
- Link bills to workflows and APP entries

### Tasks from TODO

#### 3.1 APP Manual Entry Form - Backend

**3.1.1 Database Schema Updates**
- [ ] Create migration to update app_headers/app_entries schema
- [ ] Add new fields: fiscal_year, allocation_type, budget_release_date, allocation_amount, release_installment_no, reference_memo_number, attachment_file_path
- [ ] Add indexes for fiscal_year and installment_no
- [ ] Add unique constraint on fiscal_year + release_installment_no

**Files**:
- `backend/src/main/resources/db/changelog/XXX-update-app-entries-schema.xml`

**3.1.2 Entity Updates**
- [ ] Create/Update AppEntry entity with new fields
- [ ] Add validation annotations
- [ ] Add relationships if needed
- [ ] Update repository methods

**Files**:
- `backend/src/main/java/com/bpdb/dms/entity/AppEntry.java` (new or modify)

**3.1.3 Service Implementation**
- [ ] Create AppEntryService for manual entry
- [ ] Implement fiscal year validation
- [ ] Implement installment number auto-increment logic
- [ ] Implement duplicate validation with warning
- [ ] Implement required field validation
- [ ] Handle PDF attachment upload and storage
- [ ] Remove or deprecate AppExcelImportService

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/AppEntryService.java` (new)
- `backend/src/main/java/com/bpdb/dms/service/AppExcelImportService.java` (remove/deprecate)

**3.1.4 Controller Implementation**
- [ ] Create/Update AppEntryController
- [ ] Add CRUD endpoints for APP entries
- [ ] Add fiscal year list endpoint
- [ ] Add validation endpoints
- [ ] Add file upload endpoint for attachments

**Files**:
- `backend/src/main/java/com/bpdb/dms/controller/AppEntryController.java` (new or modify)

#### 3.2 APP Manual Entry Form - Frontend

- [ ] Complete rewrite of AppEntries.tsx page
- [ ] Create AppEntryForm component
- [ ] Implement fiscal year dropdown with predefined values
- [ ] Implement allocation type dropdown
- [ ] Add date picker for budget release date
- [ ] Implement installment number field with auto-increment
- [ ] Add manual override for installment number
- [ ] Add PDF file upload for attachment
- [ ] Implement form validation
- [ ] Add duplicate warning dialog with confirmation
- [ ] Add error handling and user feedback
- [ ] Update list view to show new fields

**Files**:
- `frontend/src/pages/AppEntries.tsx` (complete rewrite)
- `frontend/src/components/AppEntryForm.tsx` (new)
- `frontend/src/services/appEntryService.ts` (new or update)

#### 3.3 Bill Document Type - Backend

**3.3.1 OCR Service for Bills**
- [ ] Create BillOCRService
- [ ] Implement vendor name extraction
- [ ] Implement invoice number extraction
- [ ] Implement invoice date extraction
- [ ] Implement fiscal year extraction
- [ ] Implement line items extraction
- [ ] Implement tax amount extraction
- [ ] Implement total amount extraction
- [ ] Add OCR confidence scoring for each field
- [ ] Handle OCR failures gracefully
- [ ] Store original OCR values separately

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/BillOCRService.java` (new)
- `backend/src/main/java/com/bpdb/dms/service/OCRService.java` (enhance)

**3.3.2 Bill Service Updates**
- [ ] Remove manual entry logic
- [ ] Implement bill upload via file upload only
- [ ] Integrate OCR extraction
- [ ] Implement bill verification flow
- [ ] Store corrected values separately from OCR values
- [ ] Link bills to fiscal year and APP lines
- [ ] Validate bill year matches APP year
- [ ] Auto-calculate totals
- [ ] Validate arithmetic consistency
- [ ] Track OCR accuracy metrics

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/BillService.java`
- `backend/src/main/java/com/bpdb/dms/dto/BillOCRResult.java` (new)

**3.3.3 Controller Updates**
- [ ] Update FinanceController bill endpoints
- [ ] Add bill upload endpoint
- [ ] Add OCR extraction endpoint
- [ ] Add bill verification/save endpoint
- [ ] Add bill correction endpoint

**Files**:
- `backend/src/main/java/com/bpdb/dms/controller/FinanceController.java`

**3.3.4 Document Integration**
- [ ] Ensure bill files are stored as documents
- [ ] Index bill documents for search
- [ ] Link bills to workflows (via folder)
- [ ] Support multiple bills per workflow

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/DocumentIndexingService.java`

#### 3.4 Bill Document Type - Frontend

- [ ] Complete rewrite of BillEntries.tsx page
- [ ] Create BillUploadForm component
- [ ] Create BillVerificationForm component
- [ ] Implement file upload (image/PDF only)
- [ ] Show OCR extraction progress
- [ ] Display OCR-extracted data in verification form
- [ ] Highlight low-confidence fields
- [ ] Allow manual correction of all fields
- [ ] Show original OCR values vs corrected values
- [ ] Handle OCR failure gracefully (allow full manual entry)
- [ ] Add validation for arithmetic consistency
- [ ] Update list view
- [ ] Remove manual entry form completely

**Files**:
- `frontend/src/pages/BillEntries.tsx` (complete rewrite)
- `frontend/src/components/BillUploadForm.tsx` (new)
- `frontend/src/components/BillVerificationForm.tsx` (new)
- `frontend/src/services/financeService.ts` (update)

### Deliverables
- ✅ APP manual entry form with all required fields
- ✅ Bill upload and OCR extraction system
- ✅ Bill verification interface
- ✅ APP and Bill linked to workflows
- ✅ No Excel upload for APP
- ✅ No manual entry form for Bills

### Success Criteria
- Users can create APP entries via form only
- Bills can only be uploaded (no manual entry)
- OCR extracts bill data with confidence scores
- Users can verify and correct OCR data
- Bills are properly linked to workflows and APP entries
- All validations work correctly
- Error handling is comprehensive

---

## Phase 4: Document Type Detection Enhancement (2-3 weeks)
**Priority**: ⚠️ HIGH  
**Dependencies**: Phase 1

### Objectives
- Enhance auto-detection of document types using OCR
- Improve classification accuracy
- Add confidence scoring
- Better manual override experience

### Tasks from TODO

#### 4.1 Classification Service
- [ ] Create DocumentClassificationService
- [ ] Implement keyword-based classification rules
- [ ] Implement pattern matching for document types
- [ ] Add confidence scoring algorithm
- [ ] Integrate with OCR results
- [ ] Add classification accuracy tracking

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/DocumentClassificationService.java` (new)

#### 4.2 OCR Integration
- [ ] Enhance OCRService to extract document type clues
- [ ] Add document type detection patterns
- [ ] Integrate with classification service
- [ ] Pass confidence scores to frontend

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/OCRService.java`

#### 4.3 Frontend Enhancements
- [ ] Enhance DocumentTypeSelector component
- [ ] Show auto-detected type with confidence score
- [ ] Improve manual override UI
- [ ] Add classification accuracy feedback

**Files**:
- `frontend/src/components/DocumentTypeSelector.tsx`

### Deliverables
- ✅ Improved auto-detection accuracy
- ✅ Confidence scoring for detected types
- ✅ Better manual override UI
- ✅ Classification metrics tracking

---

## Phase 5: Search & Export Enhancement (2-3 weeks)
**Priority**: ⚠️ HIGH  
**Dependencies**: None

### Objectives
- Enhance search functionality
- Add Boolean operators
- Implement search export
- Add search analytics

### Tasks from TODO

#### 5.1 Search Service Enhancement
- [ ] Implement Boolean operators (AND, OR, NOT)
- [ ] Enhance metadata search with field targeting
- [ ] Add search suggestions
- [ ] Implement auto-complete
- [ ] Add search result highlighting
- [ ] Create search analytics

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/SearchService.java`
- `backend/src/main/java/com/bpdb/dms/controller/SearchController.java`

#### 5.2 Search Export
- [ ] Create SearchExportService
- [ ] Implement PDF export
- [ ] Implement Excel export
- [ ] Include search criteria in export
- [ ] Add metadata and document references

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/SearchExportService.java` (new)

#### 5.3 Frontend Updates
- [ ] Enhance Search page UI
- [ ] Add Boolean operator UI
- [ ] Add search suggestions component
- [ ] Add export button and options
- [ ] Show highlighted search results

**Files**:
- `frontend/src/pages/Search.tsx`
- `frontend/src/components/SearchSuggestions.tsx` (new)

### Deliverables
- ✅ Advanced search with Boolean operators
- ✅ Search export functionality
- ✅ Search suggestions and auto-complete
- ✅ Search analytics

---

## Phase 6: Security & Watermarking (2-3 weeks)
**Priority**: ⚠️ HIGH  
**Dependencies**: None

### Objectives
- Implement 2FA authentication
- Add document watermarking
- Enhance security features

### Tasks from TODO

#### 6.1 Two-Factor Authentication
- [ ] Implement TOTP support
- [ ] Add 2FA configuration in user settings
- [ ] Create QR code generation
- [ ] Implement SMS-based 2FA
- [ ] Implement Email-based 2FA
- [ ] Add 2FA backup codes
- [ ] Create 2FA setup UI

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/TwoFactorAuthService.java` (new)
- `backend/src/main/java/com/bpdb/dms/entity/User.java`
- `frontend/src/components/TwoFactorAuthSetup.tsx` (new)

#### 6.2 Document Watermarking
- [ ] Implement PDF watermarking
- [ ] Add watermark configuration
- [ ] Create watermark templates
- [ ] Implement image watermarking
- [ ] Add watermark preview

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/WatermarkService.java` (new)
- `backend/src/main/java/com/bpdb/dms/controller/DocumentController.java`

### Deliverables
- ✅ 2FA authentication system
- ✅ Document watermarking functionality
- ✅ Enhanced security features

---

## Phase 7: Smart Folders & Organization (2-3 weeks)
**Priority**: 🟡 MEDIUM  
**Dependencies**: Phase 2 (for workflow-related Smart Folders)

### Objectives
- Create predefined Smart Folder templates
- Implement user personalization
- Add folder summary views
- Enhance folder navigation

### Tasks from TODO

#### 7.1 Smart Folder Templates
- [ ] Create predefined templates
- [ ] Implement template service
- [ ] Add template selection UI
- [ ] Add template customization

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/SmartFolderTemplateService.java` (new)
- `frontend/src/components/SmartFolderTemplates.tsx` (new)

#### 7.2 Folder Summary
- [ ] Implement folder summary statistics
- [ ] Create summary API endpoint
- [ ] Add summary UI component
- [ ] Add real-time updates

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/FolderService.java`
- `frontend/src/components/FolderSummary.tsx`

### Deliverables
- ✅ Predefined Smart Folder templates
- ✅ Folder summary views
- ✅ Enhanced folder navigation

---

## Phase 8: Reminder & Notification Features (2-3 weeks)
**Priority**: 🟡 MEDIUM  
**Dependencies**: Phase 2

### Objectives
- Create unified reminder dashboard
- Enhance PS renewal forms
- Improve notification system

### Tasks from TODO

#### 8.1 Reminder Dashboard
- [ ] Create ReminderDashboardService
- [ ] Implement unified dashboard component
- [ ] Add pending renewals list
- [ ] Show upcoming expiries
- [ ] Add compliance status indicators
- [ ] Implement quick actions

**Files**:
- `backend/src/main/java/com/bpdb/dms/service/ReminderDashboardService.java` (new)
- `frontend/src/pages/ReminderDashboard.tsx` (new)

#### 8.2 PS Renewal
- [ ] Create PS renewal form
- [ ] Implement renewal workflow
- [ ] Add document upload during renewal
- [ ] Create renewal history view

**Files**:
- `frontend/src/pages/PSRenewal.tsx` (new)
- `frontend/src/components/PSRenewalForm.tsx` (new)

### Deliverables
- ✅ Unified reminder dashboard
- ✅ PS renewal forms
- ✅ Enhanced notification features

---

## Phase 9: Document Management Polish (1-2 weeks)
**Priority**: 🟡 MEDIUM  
**Dependencies**: Various phases

### Objectives
- Polish existing features
- Add missing enhancements
- Improve user experience

### Tasks from TODO

- [ ] Document preview for Office documents
- [ ] Version comparison and diff
- [ ] Attachment linking improvements
- [ ] Explorer-style folder interface enhancements

### Deliverables
- ✅ Polished document management features
- ✅ Enhanced user experience

---

## Phase 10: Technical Debt & Optimization (Ongoing)
**Priority**: 🟢 LOW  
**Dependencies**: All phases

### Objectives
- Address technical debt
- Optimize performance
- Improve code quality

### Tasks from TODO

- [ ] Increase test coverage
- [ ] Performance optimization
- [ ] Security hardening
- [ ] Code refactoring
- [ ] Documentation improvements

### Deliverables
- ✅ Improved code quality
- ✅ Better performance
- ✅ Enhanced security

---

## Implementation Strategy

### Sprint Planning
- Each phase can be broken into 2-week sprints
- Critical phases (1-3) should have priority
- Parallel work where possible (e.g., backend and frontend)

### Dependencies Graph
```
Phase 1 (Doc Types)
    ├── Phase 2 (Workflows)
    │   └── Phase 3 (Finance - Bills workflow integration)
    ├── Phase 3 (Finance)
    └── Phase 4 (Detection)

Phase 5 (Search) - Independent
Phase 6 (Security) - Independent
Phase 7 (Smart Folders) - Depends on Phase 2
Phase 8 (Reminders) - Depends on Phase 2
Phase 9 (Polish) - Depends on all
Phase 10 (Tech Debt) - Ongoing
```

### Risk Mitigation
- **Phase 1 Risk**: Breaking existing APP functionality
  - Mitigation: Maintain backward compatibility during transition
- **Phase 2 Risk**: Complex folder-workflow relationship
  - Mitigation: Thorough testing, incremental rollout
- **Phase 3 Risk**: OCR accuracy for bills
  - Mitigation: Extensive testing, manual correction fallback

### Success Metrics
- Phase completion on schedule
- All tests passing
- No regressions introduced
- User acceptance testing passed
- Performance benchmarks met

---

*This phased plan should be reviewed and updated regularly as implementation progresses.*

