# Remaining Implementation Tasks

## ✅ Recently Completed
1. ✅ Workflow-to-APP entry relationship (backend & frontend)
2. ✅ APP entry selector during tender notice upload
3. ✅ Workflow details view with APP entry display
4. ✅ Change APP Entry action in workflows
5. ✅ Dashboard Budget vs Billed - Per APP (pie charts & progress bars)
6. ✅ Bill fields as document metadata (instead of separate tables)
7. ✅ Bill OCR extraction and BillFieldsEditor component
8. ✅ Document viewer integration with bill fields

---

## 🚧 High Priority - Remaining Tasks

### 1. Refactor Bill Entries Page (Priority: HIGH)
**Current State**: `BillEntries.tsx` still uses old `financeService.getBills()` with `BillHeader`/`BillLine` entities  
**Required**: Refactor to use documents with `documentType = 'BILL'`

**Tasks**:
- [ ] Update `BillEntries.tsx` to query documents filtered by `documentType = 'BILL'`
- [ ] Remove dependency on `financeService.getBills()` (old bill table structure)
- [ ] Display bill fields from document metadata (vendor, invoice number, date, amounts)
- [ ] Integrate `BillFieldsEditor` component for editing
- [ ] Add search/filter by:
  - Vendor name
  - Invoice number
  - Fiscal year (from bill metadata)
  - Date range
  - Amount range
- [ ] Show workflow link if bill is part of a workflow
- [ ] Show linked APP entry info via workflow

**Files to Update**:
- `frontend/src/pages/BillEntries.tsx`
- `frontend/src/services/documentService.ts` (may need bill-specific query helpers)

---

### 2. Enhanced Bill List View in Documents Page (Priority: MEDIUM)
**Current State**: Documents list shows all documents, but doesn't highlight bill-specific fields

**Tasks**:
- [ ] Add bill-specific columns to document table when filtering by BILL type:
  - Vendor Name
  - Invoice Number
  - Invoice Date
  - Total Amount
  - OCR Confidence indicator
- [ ] Add bill-specific filters to DocumentsEnhanced:
  - Vendor filter
  - Fiscal year filter (for bills)
  - Amount range filter
- [ ] Quick view tooltip showing bill summary

**Files to Update**:
- `frontend/src/pages/DocumentsEnhanced.tsx`

---

### 3. Workflow Budget Tracking (Priority: MEDIUM)
**Current State**: Workflows show linked APP entry, but don't show budget utilization

**Tasks**:
- [ ] In workflow details view, add budget tracking section:
  - Total bills amount for this workflow (sum of all BILL documents in workflow folder)
  - Compare vs. linked APP entry allocation amount
  - Budget utilization percentage
  - Visual warning if bills exceed allocation
- [ ] Show in workflow list/table as a quick indicator

**Files to Update**:
- `frontend/src/pages/Workflows.tsx`
- Backend: May need endpoint to get bills summary for a workflow

---

## 📋 Medium Priority Tasks

### 4. Bill Dashboard/Summary Page (Priority: MEDIUM - Optional)
**New Feature**: Dedicated page for bill analytics

**Tasks**:
- [ ] Create `BillDashboard.tsx` page
- [ ] Show statistics:
  - Total bills by fiscal year
  - Bills by vendor summary
  - Bills by amount range
  - Bills pending verification (low OCR confidence)
  - Bills linked to workflows
  - Bills by linked APP entry
- [ ] Add charts/graphs for bill trends

**Files to Create**:
- `frontend/src/pages/BillDashboard.tsx`
- Backend: May need `BillDashboardService` or extend `FinanceDashboardService`

---

### 5. Enhanced Document Search for Bills (Priority: LOW)
**Current State**: Search works but doesn't have bill-specific features

**Tasks**:
- [ ] Enhanced bill search with bill-specific filters
- [ ] Search by vendor, invoice number, amounts
- [ ] Export bill search results to Excel/PDF

**Files to Update**:
- `frontend/src/pages/Search.tsx`
- Backend: Enhance search service for bill-specific queries

---

### 6. Workflow Budget Warnings (Priority: LOW)
**Enhancement**: Visual warnings when bills approach/exceed allocation

**Tasks**:
- [ ] Add visual indicators in workflow details:
  - Green: < 75% utilized
  - Yellow: 75-90% utilized
  - Red: > 90% utilized or exceeded
- [ ] Add notifications when approaching limits

**Files to Update**:
- `frontend/src/pages/Workflows.tsx`

---

## 🔍 Additional Enhancements (Lower Priority)

### 7. Document Viewer Enhancements
- [ ] Show workflow link if bill is part of workflow (already in metadata)
- [ ] Show linked APP entry info via workflow in bill document view
- [ ] Quick actions: "View Workflow", "View Budget Summary"

### 8. Dashboard Enhancements
- [ ] Add filters to Budget vs Billed section (by fiscal year, by department)
- [ ] Drill-down from APP pie charts to see workflows/bills
- [ ] Export budget reports

### 9. APP Entry Enhancements
- [ ] Show linked workflows count in APP entries list
- [ ] Show total bills amount linked via workflows
- [ ] Quick link to view all bills for an APP entry

---

## 📊 Summary by Priority

### High Priority (Do Next)
1. **Refactor Bill Entries Page** - Critical for consistency with new architecture

### Medium Priority (Important but can wait)
2. Enhanced Bill List View in Documents Page
3. Workflow Budget Tracking
4. Bill Dashboard/Summary (optional)

### Low Priority (Nice to have)
5. Enhanced Document Search for Bills
6. Workflow Budget Warnings
7-9. Various UI enhancements

---

## 🎯 Recommended Next Steps

**Phase 1: Bill Entries Refactoring (1-2 weeks)**
1. Update `BillEntries.tsx` to use documents API
2. Display bills from document list with bill fields
3. Add bill-specific filters and search
4. Remove old bill table dependencies

**Phase 2: Workflow Budget Tracking (1 week)**
1. Add budget utilization to workflow details
2. Calculate bills total per workflow
3. Show warnings/indicators

**Phase 3: Polish & Enhancements (Ongoing)**
1. Bill dashboard (optional)
2. Enhanced search
3. UI improvements

---

## 📝 Notes

### Architecture Decisions
- **Bills are now documents** with `documentType = 'BILL'` and metadata fields
- **No separate bill tables** - all bill data stored in document metadata
- **APP → Workflows → Bills** relationship:
  - APP entries can be linked to multiple workflows
  - Each workflow can have multiple bills (in the workflow's folder)
  - Bill amount = sum of bills in all workflows linked to APP

### Backend APIs Available
- ✅ `GET /api/documents?documentType=BILL` - Get all bill documents
- ✅ `GET /api/documents/{id}` - Get document with metadata (includes bill fields)
- ✅ `PUT /api/documents/{id}/metadata` - Update bill fields
- ✅ `GET /api/workflows/{id}/app-entry` - Get linked APP entry
- ✅ `GET /api/finance/dashboard/budget-by-app` - Get per-APP budget summary
- ✅ `GET /api/finance/dashboard/budget-summary` - Get total budget summary

### Frontend Components Available
- ✅ `BillFieldsEditor` - Edit bill fields with OCR confidence
- ✅ `AppEntrySelector` - Select APP entries
- ✅ `DocumentViewer` - View documents (includes BillFieldsEditor for BILL docs)
- ✅ Dashboard budget charts (per APP)

