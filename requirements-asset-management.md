## Asset Management (Purchase Department) — Feature Requirements

### 1. Purpose and Objectives
- **Purpose**: Provide a unified module to track company-owned assets (e.g., laptops, desktops, printers, peripherals, licenses) across their lifecycle and maintain an auditable record of allocations to employees.
- **Objectives**:
  - **Visibility**: Single source of truth for asset inventory, ownership/assignment, and status.
  - **Control**: Standardize procurement, allocation, transfer, return, repair/maintenance, and disposal.
  - **Compliance**: Maintain audit history, warranty, and license compliance.
  - **Reporting**: Summarized view by product and product hierarchy; utilization, aging, and cost reports.

### 2. Scope
- **In Scope**:
  - Product catalog and product hierarchy (Category → Subcategory → Product → Model/Variant).
  - Asset records (unique instances with serial numbers/ids) and inventory tracking (in stock, in repair, allocated, retired).
  - Asset allocation to employees, transfers between employees/departments, temporary loans.
  - Procurement linkage (vendor, PO, invoice), warranty and AMC tracking, service history.
  - Notifications for allocation, due returns, warranty expiry, service due dates.
  - Role-based permissions for Purchase, IT/Asset Managers, Approvers, Auditors.
  - Reports and dashboards: allocations by department, asset aging, warranty expiries, under/over-utilization, losses.
- **Out of Scope (initial release)**:
  - Automated hardware discovery/agent-based scanning.
  - Financial depreciation schedules (can be integrated later with Finance).
  - License metering for software usage.

### 3. Stakeholders and Roles
- **Purchase Officer**: Creates/updates product catalog, vendors, purchase orders, receives assets.
- **Asset Manager (IT/OPS)**: Manages inventory, allocates/transfers/recalls, oversees repairs and disposal.
- **Department Manager/Approver**: Approves allocation/transfer/return when required by policy.
- **Employee**: Views assigned assets, requests changes/returns.
- **Auditor**: Read-only access to inventory, history, and reports.
- **Admin**: Configures hierarchies, policies, and permissions.

### 4. Key Concepts and Glossary
- **Product**: A definable item type (e.g., "Laptop").
- **Product Hierarchy**: Category tree for summarization (e.g., Hardware → Computers → Laptops).
- **Asset**: A unique physical or logical instance of a product (e.g., specific laptop with serial number).
- **Allocation (Assignment)**: Association of an asset to an employee for a time period.
- **Lifecycle State**: Draft, In_Stock, Allocated, In_Transit, In_Repair, Lost, Retired, Disposed.
- **Service Event**: Maintenance/repair/warranty claim record for an asset.

### 5. Functional Requirements
- **5.1 Product and Hierarchy Management**
  - Create/read/update/archive products with attributes: name, SKU, model, brand, specifications, unit cost, warranty terms, lifecycle policy.
  - Configure hierarchical categories; support many-to-one mapping Product → Category (primary), and optional tags.
  - Merge/split categories and move products while preserving history.

- **5.2 Vendor and Procurement**
  - Manage vendors (name, contacts, SLAs) and contracts.
  - Record purchase orders (PO), line items, expected delivery, invoices, received quantities.
  - On receiving, instantiate assets with serial numbers, asset tags, warranty start/end dates.

- **5.3 Asset Inventory**
  - Create and track assets with fields: product, serialNo, assetTag, purchaseRef, cost, status, location, warrantyStart/End, owner department, custom attributes.
  - Bulk import/update via CSV/XLSX; validation and error reporting.
  - Barcode/QR support: generate and print labels; scan to lookup/update.

- **5.4 Allocation and Transfers**
  - Allocate asset(s) to employee with effective date(s), expected return date (optional), purpose, approver (optional), and policy checks.
  - Transfer allocation between employees or departments with audit trail.
  - Temporary loan support with reminders before due date.
  - Return and check-in process; condition assessment and next-state decision (stock/repair/retire).

- **5.5 Service, Warranty, and Disposal**
  - Log service events: issue, vendor ticket, parts replaced, costs, downtime.
  - Warranty/AMC reminders and escalation notifications.
  - Mark asset as Retired/Disposed with reason, data wipe confirmation (if device), and disposal certificate upload.

- **5.6 Policies and Approvals**
  - Configurable rules: max assets per role/grade, approval requirement thresholds (cost, type), mandatory fields by category.
  - Approval workflows: single-step or multi-step; approver groups.

- **5.7 Search, Filters, and Views**
  - Search by assetTag, serialNo, employee, status, location, product, vendor, PO.
  - Saved filters and export (CSV/XLSX/PDF) respecting permissions.

- **5.8 Notifications**
  - Channels: in-app, email; optional push.
  - Triggers: allocation/transfer/return, overdue returns, warranty expiry (configurable lead time), service due, policy violations.

- **5.9 Reporting and Analytics**
  - Summaries by product and hierarchy (e.g., Laptops allocated by department).
  - Utilization: allocated vs in stock by category; underutilized assets.
  - Aging and lifecycle: assets nearing warranty end; average time in repair.
  - Financial: total acquisition cost by product/category/vendor over period.

### 6. Non-Functional Requirements
- **Security & RBAC**: Enforce least privilege across roles; field-level masking for sensitive data.
- **Auditability**: Immutable audit log for allocations, transfers, edits, and lifecycle transitions.
- **Scalability**: Support 100k+ assets and 10k+ employees with responsive search and reports.
- **Performance**: Common list views < 1s p95; complex reports < 5s p95 (with caching/pre-aggregation where applicable).
- **Reliability**: Eventual consistency acceptable for aggregated dashboards; transactional integrity for allocations.
- **Compliance**: Retain historical records per policy (e.g., 7 years) and data privacy rules.

### 7. Data Model (Proposed)
- Entities (simplified):
  - ProductCategory(id, parentId, name, code, active)
  - Product(id, categoryId, name, sku, brand, model, specsJson, defaultWarrantyMonths, tags[], active)
  - Vendor(id, name, contactsJson, slaJson, active)
  - PurchaseOrder(id, vendorId, poNumber, status, orderedAt, expectedAt, totalAmount)
  - PurchaseOrderLine(id, purchaseOrderId, productId, quantity, unitCost)
  - Asset(id, productId, serialNo, assetTag, status, location, warrantyStart, warrantyEnd, purchaseOrderLineId, acquisitionCost, ownerDepartmentId, customJson)
  - AssetAssignment(id, assetId, employeeId, startDate, endDateNullable, expectedReturnDateNullable, status, approvedByNullable)
  - AssetServiceEvent(id, assetId, eventType, description, openedAt, closedAtNullable, costNullable, vendorIdNullable, attachments[])
  - Department(id, name, code)
  - Employee(id, employeeCode, name, departmentId, email)
  - AuditLog(id, entityType, entityId, action, actorId, at, payloadJson)

Example category hierarchy representation:
```json
{
  "id": "cat_hw",
  "name": "Hardware",
  "children": [
    {
      "id": "cat_hw_computers",
      "name": "Computers",
      "children": [
        { "id": "cat_hw_computers_laptops", "name": "Laptops" },
        { "id": "cat_hw_computers_desktops", "name": "Desktops" }
      ]
    },
    { "id": "cat_hw_printers", "name": "Printers" }
  ]
}
```

### 8. Workflows
- **Procurement → Receiving**: PO created → approved → goods received → assets instantiated → labels printed.
- **Allocation**: Request → optional approval → allocate asset(s) → notify employee and manager.
- **Transfer**: Initiate transfer → approval (if needed) → reassign allocation → update records.
- **Return/Check-In**: Employee returns → condition check → update status (stock/repair/retire) → close allocation.
- **Service/Maintenance**: Create service event → track progress → close and update costs/warranty.
- **Retire/Dispose**: Approval → data wipe certification → mark disposed → archive.

### 9. Permissions and Access Control
- Purchase Officer: manage vendors, POs, receiving; view inventory; cannot allocate.
- Asset Manager: full asset lifecycle, allocations, transfers, service, disposal.
- Department Manager: approve allocations/transfers for their department; view department assets.
- Employee: view own assets; request allocation/return; acknowledge receipt/return.
- Auditor: read-only access to all records and history.
- Admin: configure categories, policies, roles.

### 10. UI/UX Requirements (High-Level)
- Product Catalog: category tree panel + product grid; bulk import; merge/move actions.
- Inventory: filterable table (status, category, product, location, vendor, warranty window) with bulk actions and label printing.
- Asset Detail: tabs for overview, allocation history, service history, documents.
- Allocation Wizard: select employee → select asset(s) → policy check → approval (if required) → review & confirm.
- Reports Dashboard: widgets for allocation by category, warranty expiries, utilization, aging, top vendors.

### 11. Integrations (Optional/Phase 2)
- HRIS for employee and department sync.
- SSO for user identity.
- Helpdesk/ITSM for ticket synchronization (service events).
- Finance/ERP for PO and invoice synchronization.

### 12. Reporting (Initial Set)
- Allocations by department and category/product.
- Assets nearing warranty expiry (next 30/60/90 days).
- Inventory status distribution by category.
- Asset aging (by acquisition date buckets).
- Service cost by vendor and category.

### 13. Notifications (Initial Set)
- Allocation confirmation to employee and manager.
- Return due reminders (configurable lead time and recurrence).
- Warranty/AMC expiry alerts.
- Service ticket updates.

### 14. API Surface (Proposed)
- Products: list/create/update/archive; categories: list/tree/create/update/move.
- Vendors and POs: CRUD, receive line items, instantiate assets.
- Assets: list/detail/create/bulkImport/updateStatus/printLabel.
- Assignments: allocate/transfer/return; history endpoints.
- Service Events: create/update/close; attachments upload.
- Reports: summarized endpoints by category and product; export endpoints.

### 15. Acceptance Criteria (Sample)
- Can create a 3-level category tree and move a product between categories while retaining allocation history.
- Receiving a PO line creates assets with generated tags and warranty dates.
- Allocation prevents assignment of assets not In_Stock and enforces policy caps.
- Transfer preserves full audit trail and updates visibility to new department.
- Warranty expiry notifications trigger based on configured lead times.
- Reports aggregate correctly by category hierarchy and by product.

### 16. Data Migration/Seeding (If applicable)
- Import existing products, categories, vendors.
- Bulk import current assets and allocations with validation.
- Backfill warranty end dates based on provided purchase dates and terms.

### 17. Risks and Constraints
- Data quality of historical assets; require validation and staged imports.
- Complex policy/approval variations between departments; make rules configurable.
- Performance of hierarchical aggregations; consider materialized paths or pre-aggregations.

### 18. Rollout Plan
- Phase 1: Catalog, inventory, basic allocation/return, basic reporting.
- Phase 2: Procurement integration, service workflows, notifications, expanded analytics.
- Phase 3: External integrations (HRIS/ERP/ITSM), advanced policies, mobile scanning.

### 19. Open Questions
- Require integration with existing PO/ERP in Phase 1?
- Department-specific approval matrices?
- Asset label format standard (Code128 vs QR) and required fields?
- Need for geo-location or site-level inventory?
