# BPDB Procurement Lifecycle — Workflow Requirements

**Source:** `BPDB_Procurement_Lifecycle_Metadata_Mapping_Matrix 4.docx` (Doc Ref: BPDB-PROC-2026-M01, Version 1.0, July 2026, Draft for Review)
**Scope:** Sections 1 (Lifecycle Metadata Mapping Matrix), 2 (Budget Module Integration), 3 (Expiry Tracking Matrix)
**Target system:** OCR-Integrated Document Management System (DMS)
**Client answers incorporated:** 2026-08-13, from [procurement-open-points-questionnaire.md](procurement-open-points-questionnaire.md) (Q-1 … Q-20 all answered). Section 8 records each answer and what changed.

---

## 1. Overview

The procurement lifecycle is modelled in the DMS as a **sequential, stage-gated workflow**. A procurement package moves through **16 stages**, one after another. A stage becomes available only when the preceding stage is complete; a stage is complete only when its **required documents are uploaded** and its **mandatory metadata is captured** (OCR-extracted and/or manually entered) and **verified**.

```
1 APP Approved → 2 Tender Advertisement → 3 Tender Opening → 4 Tender Evaluation
→ 5 Contract Approval → 6 Notification of Award → 7 Performance Security
→ 8 Contract Signing → 9 Letter of Credit → 10 Manufacturing / Supply
→ 11 Inspection → 12 Delivery → 13 Bill Submission → 14 Payment
→ 15 Warranty Period → 16 Contract Close
```

### 1.1 Workflow rules (apply to every stage)

| ID | Requirement |
|----|-------------|
| WF-01 | Each procurement package has exactly one active stage at a time. The stage sequence is 1 → 16 and cannot be skipped. |
| WF-02 | A stage may be **entered** only when the previous stage status is `COMPLETED`. Stage 1 is entered on package creation. |
| WF-03 | A stage is **completed** when: (a) all required documents are uploaded, (b) all mandatory metadata fields are populated, (c) OCR-extracted values have been reviewed and confirmed by the assigned user. |
| WF-04 | Uploaded documents are passed to the OCR engine. Extracted fields auto-populate the stage metadata form and are flagged `OCR_SUGGESTED` until a user confirms them (`VERIFIED`) or overrides them (`MANUAL_OVERRIDE`). |
| WF-05 | Manual-input fields are entered by the user; no OCR is attempted for them. |
| WF-06 | Every document and metadata field is linked to the package via **Package Number**, which is the primary correlation key across all 16 stages. Records are not merely tagged with the package — they form a parent/child graph described in **Section 2**. |
| WF-07 | Backward movement (rework) is allowed only by an authorised role and must record a reason; the audit trail retains the prior stage data. |
| WF-08 | All stage transitions, document uploads, OCR results, edits and overrides are written to the audit trail (user, timestamp, before/after value). |
| WF-09 | Optional documents do not block stage completion. Documents marked "(If Applicable)" are conditional — the user declares applicability, and if applicable the document becomes mandatory. |
| WF-10 | Stage status values: `NOT_STARTED`, `IN_PROGRESS`, `PENDING_VERIFICATION`, `COMPLETED`, `REWORK`. |

### 1.2 Legend

- **OCR** — value extracted automatically from the uploaded document by the OCR engine.
- **Manual** — value entered by a user in the stage form.
- **Client Note** — alignment note carried over from the source matrix.

---

## 2. Data Linkage Model

Nothing in the lifecycle stands alone. Every document and every metadata record is a **child of the APP package** created at Stage 1, and each stage's records link back to the records of the stage that produced them. The result is a single connected chain per package: from the APP line item through the tender, the bids, the contract, the LC, the deliveries, the invoices, the payments, and finally the closure certificates.

### 2.1 Entity hierarchy

```
APP (Annual Procurement Plan document)
└── APP LINE ITEM  ── may be split into lots (Q-1) ──┐
    │                                                │
└── PROCUREMENT PACKAGE (one per lot)     ← root entity, key = Package Number   [Stage 1]
    │   docs: APP Document, APP Approval Memo
    │   fields: lot_number, lot_description (null when the line is not split)
    │
    ├── BUDGET RECORD (allocation / release / revision / additional)              [Sec. 5]
    │       drawn down from the DEPARTMENT ANNUAL BUDGET (Q-13)
    │
    ├── TENDER (1..n — a re-tender adds a new attempt, Q-2)                       [Stage 2]
    │   │   docs: Tender Notice, Tender Document, Advertisement Copy
    │   │   fields: attempt_no, is_current, failure_reason
    │   │
    │   └── TENDER OPENING                                                        [Stage 3]
    │       │   docs: Bid Opening Minutes, Bid Opening Register, Bidder List
    │       │   fields: Number of Bidders, Participating Bidders  (summary only)
    │       │   Bid Security is OUT OF SCOPE (Q-4) — not captured, not tracked
    │       │
    │       └── EVALUATION (BER)                                                  [Stage 4]
    │           │   docs: BER
    │           │   fields: Responsive Bidder List, Deviation (%) with OCE
    │           │
    │           └── BER BIDDER (one row per bidder — the only bidder record)
    │                   fields: Bidder Name, Bidding Price,
    │                           Responsive (Y/N), Deviation (%), Awarded (one)
    │
    ├── CONTRACT APPROVAL → references the awarded BER BIDDER                     [Stage 5]
    │   │   docs: Approval Note, Approval Memo
    │   │
    │   └── NOA → references CONTRACT APPROVAL                                    [Stage 6]
    │       │   docs: Notification of Award
    │       │
    │       ├── PERFORMANCE SECURITY (BG / PO) → references NOA                   [Stage 7]
    │       │
    │       └── CONTRACT                        ← second-level key = Contract No.  [Stage 8]
    │           │   docs: Signed Contract Agreement
    │           │
    │           ├── LETTER OF CREDIT                                              [Stage 9]
    │           │   └── LC AMENDMENT (0..n, versioned under its LC)
    │           │
    │           ├── PRICE SCHEDULE (e-GP) + PRODUCTION SCHEDULE                   [Stage 10]
    │           │   └── PROGRESS REPORT (0..n, optional)
    │           │
    │           ├── INSPECTION EVENT (1..n)                                       [Stage 11]
    │           │       docs: Inspection / Pre-Shipment / FAT / SAT Report
    │           │
    │           ├── DELIVERY (1..n) → references INSPECTION EVENT,                [Stage 12]
    │           │   │                            PRICE SCHEDULE line items
    │           │   │   docs: Delivery Challan, Packing List, GRN
    │           │   │
    │           │   └── INVOICE (1..n) → references DELIVERY(s)                   [Stage 13]
    │           │       │   docs: Invoice / Bill, Supporting Documents
    │           │       │   └── feeds BUDGET CONSUMPTION line
    │           │       │
    │           │       └── PAYMENT (1..n) → references INVOICE(s)                [Stage 14]
    │           │               docs: Payment Voucher, Payment Approval, Bank Advice
    │           │
    │           ├── WARRANTY → references ACCEPTANCE CERTIFICATE                  [Stage 15]
    │           │
    │           └── CONTRACT CLOSURE                                              [Stage 16]
    │                   docs: Completion Cert., Final Acceptance Cert., Close Memo
    │
    └── EXPIRY TRACKER (0..n) → polymorphic link to any expiry-bearing record     [Sec. 6]
```

### 2.2 Relationship table

| # | Child record (stage) | Parent / referenced record | Link key | Cardinality |
|---|----------------------|----------------------------|----------|-------------|
| L-01 | Procurement Package (1) | APP line item | APP line ID + Lot Number | **1 APP line → 1..n packages (lots)** (Q-1) |
| L-02 | Tender (2) | Procurement Package | Package Number + Attempt No | **1 → 1..n** — one row per tender attempt; only one is `is_current` (Q-2) |
| L-03 | Tender Opening (3) | Tender | Tender / Package Number | 1 → 1 |
| L-04 | Evaluation / BER (4) | Tender Opening | Opening ID | 1 → 1 |
| L-05 | BER Bidder (4) — the only bidder record | Evaluation | Evaluation ID + Bidder Name | 1 → n |
| L-06 | *(withdrawn — Bid Security is out of scope, Q-4)* | — | — | — |
| L-07 | Awarded bidder (4) | BER Bidder | Bidder row ID | 1 → 1 |
| L-08 | Contract Approval (5) | Awarded BER Bidder | Bidder row ID | 1 → 1 |
| L-09 | NOA (6) | Contract Approval | Approval ID | 1 → 1 |
| L-10 | Performance Security (7) | NOA | NOA ID | 1 → 1..n |
| L-11 | Contract (8) | NOA + Procurement Package | NOA ID / Package Number | 1 → 1 — a contract covers exactly **one** package/lot (Q-1: the "one contract spanning several APP packages" option was *not* selected) |
| L-12 | Letter of Credit (9) | Contract | Contract Number | 1 → 0..n |
| L-13 | LC Amendment (9) | Letter of Credit | LC Number | 1 → 0..n |
| L-14 | Price / Production Schedule (10) | Contract | Contract Number | 1 → 1 |
| L-15 | Progress Report (10) | Production Schedule | Schedule ID | 1 → 0..n |
| L-16 | Inspection Event (11) | Contract | Contract Number | 1 → 1..n |
| L-17 | Delivery (12) | Contract; Inspection Event; Price Schedule lines | Contract Number / Inspection ID / Item code | 1 → 1..n |
| L-18 | Invoice (13) | Contract; Delivery(s) | Contract Number / Delivery Reference Number | n ↔ n |
| L-19 | Payment (14) | Invoice(s) | Invoice Number | n ↔ n |
| L-20 | Warranty (15) | Contract; Delivery / Acceptance Certificate | Contract Number | 1 → 1 |
| L-21 | Contract Closure (16) | Contract | Contract Number | 1 → 1 |
| L-22 | Budget records (Sec. 5) | Procurement Package | Package Number | 1 → n |
| L-22a | Package allocation (Sec. 5) | Department Annual Budget | Fiscal Year + Department | 1 → n (Q-13) |
| L-23 | Budget Consumption line (Sec. 5) | Invoice | Invoice Number | 1 → 1 |
| L-24 | Expiry Tracker (Sec. 6) | Any expiry-bearing record | Record type + Record ID | 1 → 1 |

### 2.3 Linkage requirements

| ID | Requirement |
|----|-------------|
| REQ-L1 | Every document uploaded anywhere in the lifecycle shall be stored with a link to its owning record and, transitively, to the **Procurement Package**. No document may exist unlinked. |
| REQ-L2 | **Package Number** shall be the root correlation key for Stages 1–7 and for all budget records; **Contract Number** shall be the second-level key for Stages 8–16. Both shall resolve to the same package. |
| REQ-L3 | When a stage's OCR extracts a key that already exists upstream (e.g. Package Number on the Tender Notice at Stage 2), the system shall auto-link the record to the existing parent and flag a mismatch instead of creating an orphan. |
| REQ-L4 | Where a document does not itself carry the parent key, the link shall be established from the **active stage context** (the package/contract the user is working in) and recorded explicitly. |
| REQ-L5 | The system shall prevent creating a child record when its parent record does not exist or is not yet `COMPLETED` (referential integrity aligned with the stage gates in §1.1). |
| REQ-L6 | Deleting or archiving a parent shall cascade to its children as archive-only; hard deletion of a linked record shall be blocked. |
| REQ-L7 | Many-to-many links (Invoice ↔ Delivery, Payment ↔ Invoice) shall be stored as explicit link records carrying the apportioned quantity/amount, so partial billing and part payment reconcile. |
| REQ-L8 | The **linked document view** shall render the full graph for a package: from any record the user can navigate up to the APP and down to closure, seeing every related document at every stage. |
| REQ-L9 | Universal search shall return a matching document together with its package context (Package Number, Contract Number, stage), not the file alone. |
| REQ-L10 | The system shall report **orphan and broken links** (a document with no parent, a child whose parent was reworked, an invoice with no delivery) on an exceptions dashboard. |
| REQ-L11 | Rework of an upstream stage (WF-07) shall retain existing downstream links and mark the affected downstream records as `REVALIDATION_REQUIRED` rather than breaking the chain. |
| REQ-L12 | Every link shall be audit-logged with the user, timestamp, and whether it was auto-established by OCR key match or set manually. |
| REQ-L13 | **Lot split (Q-1).** One APP line item may produce several packages. Each lot is a full package with its own Package Number, its own budget, tender, contract and closure; `lot_number` distinguishes lots of the same APP line. A package created without a split carries a null `lot_number`. The APP value of the line shall be apportioned across its lots and the apportionment shall reconcile to the line total. |
| REQ-L14 | **Re-tender (Q-2).** A failed tender shall not be deleted or overwritten. Re-tendering creates a **new Tender record under the same package**, with `attempt_no` incremented, the previous attempt retained as history with its documents, bidders and BER intact, and `is_current` moved to the new attempt. Package Number, APP linkage and budget are preserved unchanged. |
| REQ-L15 | Stages 3–7 shall resolve to the **current** tender attempt. Records belonging to a superseded attempt remain readable and are labelled with their attempt number; they are excluded from stage-gate evaluation and from the package's live figures. |

### 2.4 Worked example — one package end to end

```
Package "GD-24-115" (APP 2026, Price 4,500 lac BDT)
 └ Tender Notice GD-24-115 (Open Tender, closing 2026-08-20, validity 120 d)
    └ Opening 2026-08-20 — Number of Bidders: 5   (summary only, no bidder records)
       └ BER
          ├ Bidder ABC Ltd,  4,320 lac, responsive, −4.0% vs OCE  → AWARDED
          ├ Bidder XYZ Corp, 4,610 lac, responsive, +2.4% vs OCE
          └ Bidder … (3 more rows)
          └ Approval Note 2026-09-05 (Chief Engineer)
             └ NOA 2026-09-10 (PS 10% BDT, PG due 2026-09-24, signing due 2026-10-05)
                ├ PG: BG-8890, Sonali Bank, valid to 2027-06-30
                └ Contract C-2026-0342 (2026-10-01, 4,320 lac, completion 2027-04-30)
                   ├ LC-559001 (exp 2027-02-28) → LC Amendment #1 (exp 2027-04-15)
                   ├ e-GP price schedule (12 line items) + production schedule
                   ├ Inspection PDI 2027-01-12 (FAT passed)
                   ├ Delivery DC-441 (2027-02-02, 8 items) → Invoice INV-9001 (2,880 lac)
                   │                                            → Payment PV-3301 (2,880 lac)
                   ├ Delivery DC-458 (2027-03-18, 4 items) → Invoice INV-9044 (1,440 lac)
                   │                                            → Payment PV-3390 (1,440 lac)
                   ├ Warranty 2027-03-18 → 2028-03-17 (Acceptance Certificate)
                   └ Closure: Completion 2027-03-18, Contract Close 2028-03-25
```

Budget for GD-24-115: Allocation 5,000 lac → Consumption 4,320 lac (auto, from INV-9001 + INV-9044) → Remaining 680 lac.

---

## 3. Data Persistence Model

Everything captured in the lifecycle — every OCR-extracted value and every manually entered value — must be persisted durably, with its **provenance** (where it came from), its **verification state**, and its **history**. A value is never held only in a form or only in the extracted text blob.

### 3.1 Design principles

| ID | Principle |
|----|-----------|
| P-1 | **Typed graph tables.** The entities in §2.1 are persisted as real tables with real foreign keys, not as loose key/value rows. Stage gates and linkage integrity (REQ-L5) are enforced in the database. |
| P-2 | **Provenance store alongside.** Every field value is *additionally* written to a provenance store (`extracted_field`) recording source (OCR / manual / derived / import), confidence, source document, page and region, and verification status. The typed column is the working value; the provenance row explains it. |
| P-3 | **Raw is immutable.** The raw OCR output is never overwritten by a correction. Corrections create a new value with the raw retained, so accuracy can be measured and disputes resolved. |
| P-4 | **Store raw + normalized.** Each field keeps the literal text as read (`raw_value`) and the parsed value in a typed column (`text_value` / `numeric_value` / `date_value` / `bool_value`), so amounts and dates are queryable and reportable. |
| P-5 | **Full history.** Every change to a persisted value is versioned in history with user, timestamp, old value, new value and reason. |
| P-6 | **Re-OCR is additive.** Re-running OCR on a document creates a new OCR result version; it does not destroy previous extractions or user corrections. |
| P-7 | **Money is never a bare number.** Every amount is stored with its currency and, where applicable, its unit (e.g. lac BDT) and FX rate used. |
| P-8 | **Nothing is orphaned.** Every persisted row carries its link to the owning entity and, transitively, to the package (REQ-L1). |

### 3.2 Reuse of the existing schema

| Existing object | Use in this design | Change required |
|---|---|---|
| `documents` | Physical file + `extracted_text` (full OCR text) | Add `package_id`, `contract_id`, `stage_code` for direct package context (REQ-L9) |
| `document_versions` | Re-uploads / amended documents (REQ-X7) | None |
| `document_metadata` (key, value, confidence) | Superseded by `extracted_field` | Migrate existing rows into `extracted_field`; retain view for compatibility |
| `document_type_fields` (fieldKey, ocrPattern, validationRules) | Field catalogue + OCR patterns per document type | Extend with `stage_code`, `entity_type`, `entity_column`, `capture_source`, `is_mandatory` |
| `document_relationships` | Free-form document↔document links | Keep for ad-hoc links; structural links move to typed FKs + `document_link` |
| `expiry_tracking` | Expiry trackers (Section 6) | Add `entity_type`, `entity_id`, `package_id`, `superseded_by_id` |
| `app_headers` / `app_lines` | APP import (Stage 1) | Link `app_lines.id` → `procurement_package.app_line_id` |
| `bill_headers` / `bill_lines` | Existing billing/finance — **kept running independently** (Q-6) | None. No bridge FK; the finance module and the procurement `invoice` are separate records and duplicate entry is accepted by the client |
| `audit_logs` | Audit trail (REQ-X6, REQ-L12) | None |

### 3.3 Lifecycle graph tables

Sketch DDL (PostgreSQL; delivered as Liquibase changesets). `id BIGSERIAL PK` and the audit columns `created_by, created_at, updated_by, updated_at` are implied on every table and omitted below.

```sql
-- Stage 1 — root
procurement_package (
  app_line_id BIGINT REFERENCES app_lines,      -- APP Excel origin
  package_number VARCHAR(100) UNIQUE NOT NULL,  -- root correlation key (REQ-L2)
  lot_number VARCHAR(50), lot_description TEXT, -- lot split (REQ-L13, Q-1); null when unsplit
  package_description TEXT,
  approving_authority VARCHAR(255),
  price_lac_bdt NUMERIC(18,2),
  fiscal_year INT, department VARCHAR(255),
  current_stage SMALLINT, status VARCHAR(30))

package_stage (                                  -- one row per stage, drives §1.1 gates
  package_id BIGINT NOT NULL REFERENCES procurement_package,
  stage_code SMALLINT NOT NULL,                  -- 1..16
  status VARCHAR(30) NOT NULL,                   -- NOT_STARTED … COMPLETED / REWORK
  is_applicable BOOLEAN DEFAULT TRUE,            -- Stage 9 Not Applicable (REQ-9.5)
  not_applicable_reason TEXT,
  entered_at TIMESTAMP, completed_at TIMESTAMP, completed_by BIGINT,
  rework_reason TEXT,
  UNIQUE (package_id, stage_code))

-- Stage 2 — one row per tender attempt; re-tender adds a row (REQ-L14, Q-2)
tender (package_id BIGINT NOT NULL REFERENCES procurement_package,
  attempt_no INT NOT NULL DEFAULT 1,
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  failure_reason TEXT,                           -- why the previous attempt was re-tendered
  procurement_type VARCHAR(100),                 -- NCT | ICT (Q-8)
  procurement_method VARCHAR(100),               -- OTM | LTM | RFQ | DPM (Q-8)
  procurement_nature VARCHAR(100),               -- SERVICE | WORKS | GOODS (Q-8)
  opening_date DATE, closing_date DATE,
  tender_validity_days INT, tender_validity_date DATE,
  UNIQUE (package_id, attempt_no))
-- partial unique index: one current attempt per package
--   CREATE UNIQUE INDEX uk_tender_current ON tender(package_id) WHERE is_current;

-- Stage 3 — summary only; no per-bidder entity here (REQ-3.2)
tender_opening (tender_id BIGINT UNIQUE REFERENCES tender,
  opening_date DATE,
  number_of_bidders INT,
  participating_bidders TEXT)                    -- as read from the minutes

-- bid_security: WITHDRAWN (Q-4) — Bid Security is out of scope for this application

-- Stage 4 — the BER is the single source of bidder data
evaluation (opening_id BIGINT UNIQUE REFERENCES tender_opening,
  oce_value NUMERIC(18,2),                       -- MANUAL entry at BER upload (Q-9)
  currency VARCHAR(3), evaluation_date DATE)

ber_bidder (evaluation_id BIGINT REFERENCES evaluation,
  bidder_name VARCHAR(255),
  bidding_price NUMERIC(18,2), currency VARCHAR(3),
  is_responsive BOOLEAN,
  deviation_pct NUMERIC(9,4),
  rank INT,
  is_awarded BOOLEAN)

-- Stage 5
contract_approval (package_id BIGINT REFERENCES procurement_package,
  awarded_bidder_id BIGINT REFERENCES ber_bidder,
  approval_date DATE, approving_authority VARCHAR(255))

-- Stage 6
noa (approval_id BIGINT UNIQUE REFERENCES contract_approval,
  noa_date DATE,
  ps_amount NUMERIC(18,2), ps_currency VARCHAR(3), ps_percentage NUMERIC(5,2),
  pg_submission_last_date DATE, contract_signing_last_date DATE)

-- Stage 7
performance_security (noa_id BIGINT REFERENCES noa,
  instrument_type VARCHAR(20), reference_no VARCHAR(100),
  amount NUMERIC(18,2), currency VARCHAR(3),
  issuing_bank VARCHAR(255), validity_date DATE)

-- Stage 8
contract (package_id BIGINT REFERENCES procurement_package,
  noa_id BIGINT UNIQUE REFERENCES noa,
  contract_number VARCHAR(100) UNIQUE NOT NULL,  -- second-level key (REQ-L2)
  contract_date DATE, contract_value NUMERIC(18,2), currency VARCHAR(3),
  completion_date DATE,
  delivery_period_days INT, warranty_period_months INT,
  supplier_name VARCHAR(255))

-- Stage 9
letter_of_credit (contract_id BIGINT REFERENCES contract,
  lc_number VARCHAR(100), lc_amount NUMERIC(18,2), lc_currency VARCHAR(3),
  lc_opening_date DATE, lc_expiry_date DATE,
  issuing_bank VARCHAR(255), advising_bank VARCHAR(255),
  is_current BOOLEAN)

lc_amendment (lc_id BIGINT REFERENCES letter_of_credit,
  amendment_no INT, amendment_date DATE,
  revised_amount NUMERIC(18,2), revised_expiry_date DATE, remarks TEXT)

-- Stage 10
price_schedule (contract_id BIGINT UNIQUE REFERENCES contract,
  source VARCHAR(20),                            -- E_GP
  delivery_period_days INT)

price_schedule_line (schedule_id BIGINT REFERENCES price_schedule,
  line_no INT, item_code VARCHAR(100), item_description TEXT,
  quantity NUMERIC(18,3), uom VARCHAR(20),
  unit_price NUMERIC(18,2), line_amount NUMERIC(18,2), currency VARCHAR(3))

production_schedule (contract_id BIGINT REFERENCES contract,
  start_date DATE, end_date DATE, delivery_period_days INT)

progress_report (production_schedule_id BIGINT REFERENCES production_schedule,
  report_date DATE, progress_pct NUMERIC(5,2), remarks TEXT)

-- Stage 11
inspection_event (contract_id BIGINT REFERENCES contract,
  inspection_type VARCHAR(10),                   -- PDI | PLI
  inspection_date DATE, location VARCHAR(255),
  fat_done BOOLEAN, sat_applicable BOOLEAN, sat_done BOOLEAN,
  result VARCHAR(30))

-- Stage 12
delivery (contract_id BIGINT REFERENCES contract,
  inspection_event_id BIGINT REFERENCES inspection_event,
  delivery_reference_number VARCHAR(100),
  delivery_date DATE, delivered_quantity NUMERIC(18,3),
  is_final BOOLEAN,
  UNIQUE (contract_id, delivery_reference_number))

delivery_line (delivery_id BIGINT REFERENCES delivery,
  price_schedule_line_id BIGINT REFERENCES price_schedule_line,
  quantity NUMERIC(18,3))

-- Stage 13
invoice (contract_id BIGINT REFERENCES contract,
  invoice_number VARCHAR(100), invoice_date DATE,
  invoice_amount NUMERIC(18,2), currency VARCHAR(3),
  supplier_name VARCHAR(255),
  -- no bill_headers bridge: the finance module runs independently (Q-6)
  UNIQUE (contract_id, supplier_name, invoice_number))

invoice_delivery_link (                           -- many-to-many (REQ-L7)
  invoice_id BIGINT REFERENCES invoice,
  delivery_id BIGINT REFERENCES delivery,
  apportioned_amount NUMERIC(18,2), apportioned_quantity NUMERIC(18,3),
  PRIMARY KEY (invoice_id, delivery_id))

-- Stage 14
payment (contract_id BIGINT REFERENCES contract,
  voucher_number VARCHAR(100), payment_date DATE,
  payment_amount NUMERIC(18,2), currency VARCHAR(3),
  bank_advice_ref VARCHAR(100))

payment_invoice_link (                            -- many-to-many (REQ-L7)
  payment_id BIGINT REFERENCES payment,
  invoice_id BIGINT REFERENCES invoice,
  apportioned_amount NUMERIC(18,2),
  PRIMARY KEY (payment_id, invoice_id))

-- Stage 15
warranty (contract_id BIGINT UNIQUE REFERENCES contract,
  final_delivery_id BIGINT REFERENCES delivery,
  warranty_start_date DATE, warranty_end_date DATE,
  acceptance_certificate_doc_id BIGINT REFERENCES documents)

-- Stage 16
contract_closure (contract_id BIGINT UNIQUE REFERENCES contract,
  completion_date DATE, contract_close_date DATE,
  outstanding_notes TEXT, closed_by BIGINT)
```

### 3.4 Document linkage table

```sql
document_link (
  document_id BIGINT NOT NULL REFERENCES documents,
  entity_type VARCHAR(50) NOT NULL,     -- 'EVALUATION', 'CONTRACT', 'INVOICE', …
  entity_id   BIGINT NOT NULL,
  package_id  BIGINT NOT NULL REFERENCES procurement_package,  -- denormalised for search
  contract_id BIGINT REFERENCES contract,
  stage_code  SMALLINT NOT NULL,
  doc_role    VARCHAR(80) NOT NULL,     -- 'TENDER_NOTICE', 'GRN', 'PAYMENT_VOUCHER', …
  is_mandatory BOOLEAN, is_applicable BOOLEAN,
  link_origin VARCHAR(20),              -- OCR_KEY_MATCH | STAGE_CONTEXT | MANUAL (REQ-L12)
  UNIQUE (document_id, entity_type, entity_id, doc_role))
```

- REQ-P1 No document may be persisted without at least one `document_link` row resolving to a package (REQ-L1); uploads without context are held in a quarantine list surfaced on the exceptions dashboard (REQ-L10).
- REQ-P2 `doc_role` shall be constrained to the document catalogue in §3.7 so that "required documents" per stage can be checked by query.

### 3.5 Field provenance store — the OCR / manual capture record

This is the single table that satisfies "persist everything extracted and everything input".

```sql
extracted_field (
  entity_type VARCHAR(50) NOT NULL,      -- owning graph entity
  entity_id   BIGINT NOT NULL,
  package_id  BIGINT NOT NULL REFERENCES procurement_package,
  stage_code  SMALLINT NOT NULL,
  field_key   VARCHAR(100) NOT NULL,     -- e.g. 'lc_expiry_date' (catalogue §3.7)
  field_label VARCHAR(255),
  data_type   VARCHAR(20) NOT NULL,      -- TEXT | NUMBER | DATE | BOOL | CURRENCY

  raw_value        TEXT,                 -- literal text as read — immutable (P-3)
  text_value       TEXT,                 -- normalized
  numeric_value    NUMERIC(18,4),
  date_value       DATE,
  bool_value       BOOLEAN,
  currency         VARCHAR(3),
  unit             VARCHAR(20),          -- e.g. 'LAC_BDT' (P-7)
  fx_rate          NUMERIC(18,6),

  capture_source   VARCHAR(20) NOT NULL, -- OCR | MANUAL | DERIVED | IMPORT
  document_id      BIGINT REFERENCES documents,
  ocr_result_id    BIGINT REFERENCES ocr_result,
  page_no          INT,
  bbox             VARCHAR(60),          -- x,y,w,h for click-to-source highlight
  ocr_confidence   NUMERIC(5,4),

  status           VARCHAR(30) NOT NULL, -- OCR_SUGGESTED | VERIFIED | MANUAL_OVERRIDE | REJECTED
  verified_by BIGINT, verified_at TIMESTAMP,
  is_mandatory BOOLEAN, validation_state VARCHAR(30), validation_message TEXT,
  UNIQUE (entity_type, entity_id, field_key))

extracted_field_history (                -- append-only (P-5)
  extracted_field_id BIGINT REFERENCES extracted_field,
  version INT,
  old_value TEXT, new_value TEXT,
  old_status VARCHAR(30), new_status VARCHAR(30),
  changed_by BIGINT, changed_at TIMESTAMP, change_reason TEXT)
```

- REQ-P3 Every field listed in the catalogue (§3.7) shall produce an `extracted_field` row when captured — whether by OCR or manual entry. Manual fields carry `capture_source = MANUAL` and no confidence.
- REQ-P4 The typed graph column (§3.3) and the `extracted_field` row shall be written in the same transaction and shall not diverge; the graph column is the working value, the field row the evidence.
- REQ-P5 `raw_value` shall never be updated after insert. A correction updates the normalized/typed columns, sets `status = MANUAL_OVERRIDE`, and appends to `extracted_field_history`.
- REQ-P6 `bbox` and `page_no` shall be persisted where the OCR engine provides them, so the UI can highlight the source region on the document (supports REQ-X3).
- REQ-P7 Fields extracted with confidence below the configured threshold shall persist with `status = OCR_SUGGESTED` and `validation_state = NEEDS_REVIEW`, and shall block stage completion until reviewed (WF-03).
- REQ-P8 A field that OCR could not find shall persist as a row with null values and `validation_state = NOT_FOUND` rather than being absent — so "missing" is a recorded fact, not an unknown.

### 3.6 OCR job and result persistence

```sql
ocr_job (document_id BIGINT REFERENCES documents,
  status VARCHAR(20),                    -- QUEUED | RUNNING | SUCCESS | FAILED
  engine VARCHAR(50), engine_version VARCHAR(30),
  attempt_no INT, started_at TIMESTAMP, finished_at TIMESTAMP,
  duration_ms INT, error_message TEXT)

ocr_result (job_id BIGINT REFERENCES ocr_job,
  document_id BIGINT REFERENCES documents,
  version INT,                           -- re-OCR is additive (P-6)
  full_text TEXT, page_count INT,
  avg_confidence NUMERIC(5,4),
  language VARCHAR(20), is_current BOOLEAN)

ocr_page (result_id BIGINT REFERENCES ocr_result,
  page_no INT, page_text TEXT, confidence NUMERIC(5,4))
```

- REQ-P9 The full OCR text shall be persisted per document and per page, retained even after fields are extracted, to support full-text search and re-extraction without re-scanning.
- REQ-P10 Re-running OCR shall insert a new `ocr_result` version and flip `is_current`; prior versions and the fields derived from them remain retrievable.
- REQ-P11 Failed OCR jobs shall persist with the error and attempt count, and shall be visible on the exceptions dashboard rather than failing silently.
- REQ-P12 Documents whose OCR yields no usable text (e.g. poor scans) shall be flagged for manual entry, and the resulting values persist as `capture_source = MANUAL` against the same document.

### 3.7 Field catalogue — what is persisted at each stage

Every row below is a persisted field: a typed column in §3.3 **and** an `extracted_field` row carrying its provenance. `Src` = O (OCR), M (Manual), A (Auto/derived).

| Stage | Entity | Field key | Type | Src |
|---|---|---|---|---|
| 1 | procurement_package | package_number, package_description, approving_authority | text | O |
| 1 | procurement_package | price_lac_bdt | currency | O |
| 2 | tender | procurement_type, procurement_method, procurement_nature | text | O |
| 2 | tender | opening_date, closing_date | date | O |
| 2 | tender | tender_validity | number/date | O |
| 3 | tender_opening | number_of_bidders, participating_bidders | number/text | O |
| 4 | evaluation | oce_value | currency | **M** (Q-9 — entered at BER upload) |
| 4 | ber_bidder | bidder_name | text | O |
| 4 | ber_bidder | bidding_price, currency | currency | O |
| 4 | ber_bidder | deviation_pct | number | O |
| 4 | ber_bidder | is_responsive, is_awarded | bool | O/M |
| 5 | contract_approval | approval_date | date | **M** |
| 5 | contract_approval | approving_authority | text | **M** |
| 6 | noa | ps_amount, ps_currency | currency | O |
| 6 | noa | pg_submission_last_date, contract_signing_last_date | date | O |
| 7 | performance_security | validity_date | date | O |
| 7 | performance_security | issuing_bank | text | O |
| 8 | contract | contract_number | text | **M** |
| 8 | contract | contract_date, completion_date | date | **M** |
| 8 | contract | contract_value, currency | currency | **M** |
| 8 | contract | delivery_period_days, warranty_period_months | number | **M** |
| 9 | letter_of_credit | lc_number, issuing_bank, advising_bank | text | **M** |
| 9 | letter_of_credit | lc_amount, lc_currency | currency | **M** |
| 9 | letter_of_credit | lc_opening_date, lc_expiry_date | date | **M** |
| 9 | lc_amendment | revised_amount, revised_expiry_date | currency/date | **M** |
| 10 | price_schedule_line | item_code, description, quantity, unit_price, line_amount | mixed | O |
| 10 | production_schedule | delivery_period_days | number | O |
| 10 | progress_report | report_date, progress_pct | date/number | O |
| 11 | inspection_event | inspection_type (PDI/PLI) | enum | O |
| 11 | inspection_event | inspection_date, result, sat_applicable | mixed | O/M |
| 12 | delivery | delivery_date | date | O |
| 12 | delivery | delivered_quantity | number | O |
| 12 | delivery | delivery_reference_number | text | O |
| 13 | invoice | invoice_number, supplier_name | text | O |
| 13 | invoice | invoice_date | date | O |
| 13 | invoice | invoice_amount, currency | currency | O |
| 14 | payment | voucher_number | text | O |
| 14 | payment | payment_date | date | O |
| 14 | payment | payment_amount | currency | O |
| 15 | warranty | warranty_start_date, warranty_end_date | date | O |
| 16 | contract_closure | completion_date, contract_close_date | date | O |
| — | department_budget | annual departmental allocation | currency | **M** |
| — | budget_entry | allocation / release / revision / additional amounts | currency | **M** |
| — | budget_consumption | consumed_amount | currency | **A** |
| — | expiry_tracking | expiry_date per tracked instrument | date | **A** |

- REQ-P13 The catalogue shall be persisted as configuration in `document_type_fields` (extended per §3.2), not hard-coded, so fields can be added per document type without a code change.
- REQ-P14 Mandatory-field checks for stage completion (WF-03) shall be driven from this catalogue.

### 3.8 Budget and expiry persistence

```sql
-- Annual departmental budget, drawn down per package (REQ-B0, Q-13)
department_budget (
  fiscal_year INT NOT NULL,
  department VARCHAR(255) NOT NULL,
  allocated_amount NUMERIC(18,2), currency VARCHAR(3) DEFAULT 'BDT',
  approved_by BIGINT, approved_at TIMESTAMP,
  UNIQUE (fiscal_year, department))

budget_entry (package_id BIGINT REFERENCES procurement_package,
  department_budget_id BIGINT REFERENCES department_budget,  -- source of the drawdown
  entry_type VARCHAR(20),                -- ALLOCATION | RELEASE | REVISION | ADDITIONAL
  amount NUMERIC(18,2), currency VARCHAR(3),
  effective_date DATE, reason TEXT,
  approved_by BIGINT)                    -- any user holding the approver permission (Q-13)

budget_consumption (package_id BIGINT REFERENCES procurement_package,
  invoice_id BIGINT UNIQUE REFERENCES invoice,   -- L-23, auto from Stage 13
  consumed_amount NUMERIC(18,2), currency VARCHAR(3),
  posted_at TIMESTAMP)
```

- REQ-P15 Remaining Budget (REQ-B6) shall be **derived**, not stored as an editable field; it may be persisted as a materialized figure for reporting provided it is recomputed on every component change.
- REQ-P16 `expiry_tracking` shall be extended with `entity_type`, `entity_id`, `package_id` and `superseded_by_id`, so a tracker points at the record it guards and an extension/amendment supersedes rather than overwrites the previous expiry (REQ-E5).

### 3.9 Retention, integrity and access

- REQ-P17 Persisted values, OCR results and field-change history are retained for **1 year** (Q-18), configurable; closure (Stage 16) archives but does not delete. Purging beyond the retention period is a separate, explicitly invoked administrative action — never automatic — and is audit-logged.
- REQ-P18 All monetary and date columns are typed (`NUMERIC`, `DATE`) — never free text — so validations in Section 4 and reporting work off the database.
- REQ-P19 Indexes shall exist on `package_number`, `contract_number`, `invoice_number`, `delivery_reference_number`, `document_link(package_id, stage_code)` and `extracted_field(entity_type, entity_id)`.
- REQ-P20 Row-level access shall follow the package/department ownership so persistence does not bypass the role model (REQ-X4).
- REQ-P21 Every insert/update to the tables in this section shall emit an `audit_logs` entry (REQ-X6).

---

## 4. Stage-by-Stage Requirements

### Stage 1 — APP Approved

- **Entry condition:** Package created in the system.
- **Links to:** root of the graph — the APP document creates the **Procurement Package**; every later record descends from it (L-01).
- **Required documents:** APP Document; APP Approval Memo.
- **OCR-extracted metadata:** Package Number, Package Description, Approving Authority, Price in lac BDT.
- **Manual input:** — (none)
- **Client note:** The APP Excel file will be uploaded into the system. Reference sample supplied: `APP 22-23 First Revision_2980.xls` (Q-7).
- **System behaviour:**
  - REQ-1.1 The system shall accept the APP as an Excel file and parse package rows from it, in addition to accepting scanned/PDF APP documents for OCR. The parser shall be driven by a **column mapping profile per fiscal year**, so a changed layout is a configuration change rather than a code change; the supplied `APP 22-23 First Revision` layout is the first profile.
  - REQ-1.2 The system shall create/associate a procurement package using the extracted **Package Number** as the lifecycle correlation key.
  - REQ-1.3 The system shall reject a duplicate Package Number and offer to open the existing package instead.
  - REQ-1.4 **Price in lac BDT** shall be stored as the package's approved APP value and made available to the Budget module (Section 5).
  - REQ-1.5 **Lot split (Q-1, REQ-L13).** The user may split one APP line into several packages, each given its own Package Number and Lot Number, with the line's APP value apportioned across the lots. The system shall show the unapportioned residue and shall block completion until the apportionment reconciles to the line total. An unsplit line produces a single package with a null Lot Number.
  - REQ-1.6 A package's department and fiscal year shall determine which **Department Annual Budget** it draws down (Section 5).
- **Exit criteria:** APP Document uploaded, Package Number + Package Description + Approving Authority + Price in lac BDT verified.

---

### Stage 2 — Tender Advertisement

- **Entry condition:** Stage 1 `COMPLETED`.
- **Links to:** **Tender** is a child of the Procurement Package via the Package Number printed on the Tender Notice (L-02). Tender Notice, Tender Document and Advertisement Copy are all attached to the same Tender record.
- **Required documents:** Tender Notice; Tender Document; Advertisement Copy.
- **OCR-extracted metadata:** Procurement Type, Procurement Method, Procurement Nature, Package Number, Opening Date, Closing Date, Tender Validity.
- **Manual input:** — (none)
- **System behaviour:**
  - REQ-2.1 The extracted **Package Number** shall be matched against the Stage 1 package; a mismatch shall raise a validation warning and block completion until resolved.
  - REQ-2.2 The system shall validate that Opening Date ≤ Closing Date and that Tender Validity is a future-dated period relative to Closing Date.
  - REQ-2.3 **Tender Validity** shall be registered in the Expiry Tracking Matrix (Section 6).
  - REQ-2.4 Procurement Type / Method / Nature shall be captured against a configurable master list; unmatched OCR values are flagged for manual selection. The seeded lists (Q-8) are:
    - **Procurement Type:** NCT (National Competitive Tender), ICT (International Competitive Tender)
    - **Procurement Method:** OTM, LTM, RFQ, DPM
    - **Procurement Nature:** Service, Works, Goods
  - REQ-2.5 **Procurement Type = ICT** shall set the default applicability of Stage 9 (Letter of Credit) to *applicable*; any other value defaults it to *not applicable* (REQ-9.5, Q-5). The default is a suggestion the user may change.
  - REQ-2.6 **Re-tender (Q-2, REQ-L14).** An authorised user may declare the current tender attempt failed, recording a reason, and open a new attempt. The new attempt starts at Stage 2 with a fresh Tender record under the same package; the previous attempt and all its Stage 2–4 records and documents are retained, read-only, labelled with their attempt number.
- **Exit criteria:** Tender Notice uploaded and all seven metadata fields verified, for the **current** tender attempt.

---

### Stage 3 — Tender Opening

- **Entry condition:** Stage 2 `COMPLETED` (and Closing Date reached).
- **Links to:** **Tender Opening** is a child of the Tender (L-03). This stage holds **summary information only** — no per-bidder entity is created here; the bidder records are created at Stage 4 from the BER (L-05).
- **Required documents:** Bid Opening Minutes; Bid Opening Register; Bidder List.
- **OCR-extracted metadata:** Number of Bidders, Participating Bidders.
- **Manual input:** — (none)
- **System behaviour:**
  - REQ-3.1 The system shall store **Number of Bidders** and **Participating Bidders** on the Tender Opening record as summary data captured from the Bid Opening Minutes / Bidder List.
  - REQ-3.2 The bidder documents uploaded here (Bid Opening Register, Bidder List) shall be retained and searchable, but the system shall **not** build a per-bidder data structure at this stage. The BER at Stage 4 is the single source of bidder data.
  - REQ-3.3 At Stage 4 the count of BER bidder rows shall reconcile with the **Number of Bidders** recorded here; a mismatch is flagged for review.
  - REQ-3.4 *(Withdrawn — Q-4.)* **Bid Security is out of scope for this application.** No bid security instrument is captured, no bidder-level financial record is created, and no bid security expiry is tracked. Bid security documents may still be filed as ordinary attachments to the Tender Opening, but the system holds no structured data about them and raises no alerts.
- **Exit criteria:** Bid Opening Minutes uploaded, Number of Bidders and Participating Bidders verified.

---

### Stage 4 — Tender Evaluation

- **Entry condition:** Stage 3 `COMPLETED`.
- **Links to:** **Evaluation (BER)** is a child of the Tender Opening (L-04). **BER Bidder** rows are children of the Evaluation (L-05) and are the *only* bidder records in the system — bidder identity, price and evaluation outcome are captured together, in one place, from the BER. Exactly one row is flagged as awarded (L-07).
- **Required documents:** BER (Bid Evaluation Report).
- **OCR-extracted metadata:** Deviation (%) with OCE, Responsive Bidder List (per bidder: Bidder Name, Bidding Price, Responsive Y/N, Deviation %).
- **Manual input:** OCE value (Q-9).
- **System behaviour:**
  - REQ-4.1 The system shall extract the bidder table from the BER and store one **BER Bidder** row per bidder, carrying Bidder Name, Bidding Price, responsive flag and Deviation (%). This is the single bidder record for the package; Stage 3 does not duplicate it.
  - REQ-4.2 **Deviation (%) with OCE** shall be stored per bidder row and shown against the OCE (Officially Certified Estimate) value.
  - REQ-4.2a The **OCE value shall be entered manually by the user at BER upload** (Q-9); it is not extracted and is not held elsewhere in the system. It is mandatory before the stage can complete, since every deviation figure is meaningless without it. Where the BER also states a deviation per bidder, the system shall recompute deviation from the entered OCE and flag any bidder row where the two disagree beyond a configurable tolerance.
  - REQ-4.3 The count of BER Bidder rows shall reconcile with **Number of Bidders** from Stage 3 (REQ-3.3); a mismatch is flagged, not blocked, since the BER may list only evaluated bidders.
  - REQ-4.4 The system shall allow one responsive bidder row to be marked as the recommended/awarded bidder, carried forward to Stages 5–8.
- **Exit criteria:** BER uploaded, deviation and responsive bidder list verified, recommended bidder selected.

---

### Stage 5 — Contract Approval

- **Entry condition:** Stage 4 `COMPLETED`.
- **Links to:** **Contract Approval** references the awarded **BER Bidder** row from Stage 4 (L-08), inheriting bidder name and price without re-entry.
- **Required documents:** Approval Note; Approval Memo.
- **OCR-extracted metadata:** — (none)
- **Manual input:** Approval Date, Approving Authority.
- **System behaviour:**
  - REQ-5.1 Approval Date and Approving Authority shall be entered manually; OCR is not applied to these fields.
  - REQ-5.2 Approving Authority shall be selected from a configurable authority master list.
  - REQ-5.3 Approval Date shall not precede the Stage 4 completion date.
- **Exit criteria:** Approval Note uploaded, Approval Date and Approving Authority recorded.

---

### Stage 6 — Notification of Award (NOA)

- **Entry condition:** Stage 5 `COMPLETED`.
- **Links to:** **NOA** is a child of the Contract Approval (L-09) and therefore resolves through it to the awarded BER Bidder, the Tender and the Package.
- **Required documents:** Notification of Award (NOA).
- **OCR-extracted metadata:** Performance Security Amount, PS Currency, PG Submission Last Date, Contract Signing Last Date.
- **Manual input:** — (none)
- **System behaviour:**
  - REQ-6.1 **PG Submission Last Date** and **Contract Signing Last Date** shall generate deadline alerts against Stage 7 and Stage 8 respectively.
  - REQ-6.2 If Stage 7 is not completed by PG Submission Last Date, or Stage 8 by Contract Signing Last Date, the package shall be flagged as overdue on the dashboard.
  - REQ-6.3 Performance Security Amount + PS Currency shall be carried forward as the expected value for Stage 7 verification.
- **Exit criteria:** NOA uploaded and all four metadata fields verified.

---

### Stage 7 — Performance Security

- **Entry condition:** Stage 6 `COMPLETED`.
- **Links to:** **Performance Security** is a child of the NOA (L-10); its amount is validated against the NOA's Performance Security Amount.
- **Required documents:** BG / PO (Bank Guarantee or Pay Order).
- **OCR-extracted metadata:** Validity, Issuing Bank.
- **Manual input:** — (none)
- **System behaviour:**
  - REQ-7.1 The system shall compare the submitted security amount against the Stage 6 **Performance Security Amount**; a shortfall is flagged.
  - REQ-7.2 **Validity** shall be registered as **PG Expiry Date** in the Expiry Tracking Matrix (Section 6).
  - REQ-7.3 Issuing Bank shall be captured against a configurable bank master list.
- **Exit criteria:** BG/PO uploaded, Validity and Issuing Bank verified.

---

### Stage 8 — Contract Signing

- **Entry condition:** Stage 7 `COMPLETED`.
- **Links to:** **Contract** is a child of the NOA and of the Procurement Package (L-11). From here on, Contract Number is the working key for Stages 9–16, and it always resolves back to the Package Number.
- **Required documents:** Signed Contract Agreement.
- **OCR-extracted metadata:** — (none)
- **Manual input:** Contract Number, Contract Date, Contract Value, Currency, Completion Date, Delivery Period, Warranty Period.
- **System behaviour:**
  - REQ-8.1 All seven contract fields shall be entered manually; OCR is not applied.
  - REQ-8.2 **Contract Number** becomes a secondary correlation key alongside Package Number for Stages 9–16.
  - REQ-8.3 **Completion Date** shall be registered in the Expiry Tracking Matrix (Section 6).
  - REQ-8.4 **Warranty Period** shall drive the derivation of Warranty Start/End Dates in Stage 15.
  - REQ-8.5 **Delivery Period** shall drive the expected delivery window used in Stages 10 and 12.
  - REQ-8.6 Contract Value + Currency shall be reported against the package's APP value (Stage 1) and against Budget (Section 5).
  - REQ-8.7 Contract Date shall not precede Approval Date (Stage 5).
- **Exit criteria:** Signed Contract Agreement uploaded and all seven fields recorded.

---

### Stage 9 — Letter of Credit (LC)

- **Entry condition:** Stage 8 `COMPLETED`.
- **Links to:** **LC** is a child of the Contract (L-12); each **LC Amendment** is a versioned child of its LC (L-13) and supersedes the amended values.
- **Required documents:** Letter of Credit (LC); LC Amendment *(if applicable)*.
- **OCR-extracted metadata:** — (none)
- **Manual input:** LC Number, LC Amount, LC Currency, LC Opening Date, LC Expiry Date, Issuing Bank, Advising Bank.
- **System behaviour:**
  - REQ-9.1 All LC fields shall be entered manually; OCR is not applied.
  - REQ-9.2 **LC Expiry Date** shall be registered in the Expiry Tracking Matrix (Section 6).
  - REQ-9.3 LC Amendments shall be uploadable any number of times, each as a versioned attachment to the parent LC, and an amendment may revise LC Amount and/or LC Expiry Date — the revised values supersede the originals for expiry tracking.
  - REQ-9.4 LC Amount shall be validated against Contract Value (Stage 8); a variance is flagged for review, not blocked.
  - REQ-9.5 **Applicability (Q-5).** The stage applies to international tenders. Its default applicability is derived from **Procurement Type** captured on the Tender Notice at Stage 2 (REQ-2.5): `ICT` → applicable, anything else → not applicable. The default is a suggestion, not a lock.
  - REQ-9.6 **Any authorised user may mark the stage Not Applicable** (Q-5) — no separate approval step. A reason is mandatory, the action is audit-logged, and the workflow proceeds to Stage 10. Marking an `ICT` package Not Applicable, or marking a non-`ICT` package applicable, is permitted but recorded as an override of the derived default.
- **Exit criteria:** LC uploaded and all seven fields recorded, or stage marked Not Applicable.

---

### Stage 10 — Manufacturing / Supply

- **Entry condition:** Stage 9 `COMPLETED` or Not Applicable.
- **Links to:** **Production Schedule** and **e-GP Price Schedule** are children of the Contract (L-14); Progress Reports hang off the Production Schedule (L-15). Price schedule line items become the item baseline referenced by Delivery (Stage 12) and Invoice (Stage 13).
- **Required documents:** Production Schedule; Progress Report *(optional)*; e-GP Price Schedule.
- **OCR-extracted metadata:** Delivery Period.
- **Manual input:** — (none)
- **System behaviour:**
  - REQ-10.1 The extracted **Delivery Period** shall be reconciled against the contractual Delivery Period from Stage 8; a difference is flagged.
  - REQ-10.2 Progress Reports are optional and may be uploaded repeatedly during the stage without blocking completion.
  - REQ-10.3 The **e-GP price schedule** shall be stored against the contract and used as the item/price baseline for Delivery (Stage 12) and Bill Submission (Stage 13).
- **Exit criteria:** Production Schedule and e-GP price schedule uploaded, Delivery Period verified.

---

### Stage 11 — Inspection

- **Entry condition:** Stage 10 `COMPLETED`.
- **Links to:** each **Inspection Event** is a child of the Contract (L-16) and is referenced by the Delivery it clears (L-17).
- **Required documents:** Inspection Report; Pre-Shipment Inspection Report; FAT Report; SAT Report *(if applicable)*.
- **OCR-extracted metadata:** Inspection Type (PDI / PLI).
- **Manual input:** — (none)
- **System behaviour:**
  - REQ-11.1 **Inspection Type** shall be constrained to the values PDI (Pre-Despatch Inspection) and PLI (Pre-Loading Inspection).
  - REQ-11.2 Multiple inspection events shall be supported within the stage, each with its own report set and inspection type.
  - REQ-11.3 SAT Report applicability shall be declared by the user; if applicable it becomes mandatory before completion.
- **Exit criteria:** Inspection Report uploaded and Inspection Type verified for every declared inspection event.

---

### Stage 12 — Delivery

- **Entry condition:** Stage 11 `COMPLETED`.
- **Links to:** each **Delivery** is a child of the Contract and references its Inspection Event and the price schedule line items delivered (L-17). Challan, Packing List and GRN attach to that one Delivery record.
- **Required documents:** Delivery Challan; Packing List; Goods Received Note (GRN).
- **OCR-extracted metadata:** Delivery Date, Delivered Quantity, Delivery Reference Number.
- **Manual input:** — (none)
- **System behaviour:**
  - REQ-12.1 Partial deliveries shall be supported **for all contract categories** (Q-11): the stage holds multiple delivery records, each with its own challan/packing list/GRN and metadata set.
  - REQ-12.2 Cumulative **Delivered Quantity** shall be tracked against the contracted quantity from the e-GP price schedule (Stage 10).
  - REQ-12.3 **Delivery Date** shall be compared against the contractual delivery window derived from Delivery Period (Stage 8); late delivery is flagged.
  - REQ-12.4 **Delivery Reference Number** shall be unique per delivery record within the contract.
  - REQ-12.5 A delivery is declared **final** by a user holding the **Checker** role (Q-11 was silent on who declares finality; this follows the maker/checker model of Q-17). Declaring final closes the delivery set; reopening it requires the same role and is audit-logged.
- **Exit criteria:** At least one delivery record complete and the delivery declared final by a Checker (cumulative quantity reconciled).

---

### Stage 13 — Bill Submission

- **Entry condition:** Stage 12 has at least one completed delivery record.
- **Links to:** each **Invoice** is a child of the Contract and is linked many-to-many to the **Delivery** records it bills, via a link record carrying the apportioned quantity/amount (L-18, REQ-L7). The verified invoice also creates the Budget Consumption line (L-23).
- **Required documents:** Invoice / Bill; Supporting Documents.
- **OCR-extracted metadata:** Invoice Number, Invoice Date, Invoice Amount, Currency, Supplier Name.
- **Manual input:** — (none)
- **System behaviour:**
  - REQ-13.1 Multiple invoices shall be supported per contract (progressive billing).
  - REQ-13.2 **Invoice Number** shall be unique per supplier; duplicates are blocked.
  - REQ-13.3 **Over-billing is not permitted (Q-12).** Cumulative Invoice Amount across a contract shall not exceed Contract Value (Stage 8). An invoice that would breach the ceiling is **rejected** — this is a hard block with no override path and no tolerance band. The user is shown the contract value, the amount already billed and the headroom remaining. Where a genuine increase is required (price variation, taxes, scope change), the contract value must first be revised at Stage 8 with its own document trail; the invoice is then accepted against the revised value.
  - REQ-13.4 The OCR-extracted **Invoice Amount** shall feed **Budget Consumption** in the Budget module (Section 5) — this is the single automated input to Budget.
  - REQ-13.5 **Supplier Name** shall be reconciled with the awarded bidder from Stage 4.
- **Exit criteria:** Invoice uploaded and all five metadata fields verified for each submitted bill.

---

### Stage 14 — Payment

- **Entry condition:** Corresponding Stage 13 invoice `COMPLETED`.
- **Links to:** each **Payment** is linked many-to-many to the **Invoice** records it settles, via a link record carrying the apportioned amount so part payments reconcile (L-19, REQ-L7).
- **Required documents:** Payment Voucher; Payment Approval; Bank Advice *(optional)*.
- **OCR-extracted metadata:** Payment Voucher Number, Payment Date, Payment Amount.
- **Manual input:** — (none)
- **System behaviour:**
  - REQ-14.1 Each payment record shall be linked to one or more Stage 13 invoices.
  - REQ-14.2 Cumulative **Payment Amount** shall not exceed the linked invoice amount. Consistent with REQ-13.3 this is a hard block, not an override-able warning; a payment cannot settle more than the invoice it is attached to.
  - REQ-14.3 Part payments shall be supported; the outstanding balance per invoice is shown.
  - REQ-14.4 **Payment Date** shall not precede the linked Invoice Date.
- **Exit criteria:** Payment Voucher and Payment Approval uploaded, all three fields verified, invoice fully settled or explicitly closed.

---

### Stage 15 — Warranty Period

- **Entry condition:** Stage 14 `COMPLETED` for the final invoice.
- **Links to:** **Warranty** is a child of the Contract and references the Acceptance Certificate and the final Delivery that starts the warranty clock (L-20).
- **Required documents:** Acceptance Certificate.
- **OCR-extracted metadata:** Warranty Start Date, Warranty End Date.
- **Manual input:** — (none)
- **System behaviour:**
  - REQ-15.1 Extracted warranty dates shall be reconciled against the **Warranty Period** recorded in Stage 8; a mismatch is flagged for confirmation.
  - REQ-15.2 **Warranty End Date** shall be registered in the Expiry Tracking Matrix (Section 6).
  - REQ-15.3 The package remains in this stage for the duration of the warranty; Stage 16 becomes available only after Warranty End Date, unless closed early by an authorised role with a recorded reason.
- **Exit criteria:** Acceptance Certificate uploaded, warranty dates verified, Warranty End Date reached (or early-close approved).

---

### Stage 16 — Contract Close

- **Entry condition:** Stage 15 `COMPLETED`.
- **Links to:** **Contract Closure** is a child of the Contract (L-21) and closes the graph: the closure check in REQ-16.1 walks every linked record from the APP down to the last payment.
- **Required documents:** Completion Certificate; Final Acceptance Certificate; Contract Close Memo.
- **OCR-extracted metadata:** Completion Date, Contract Close Date.
- **Manual input:** — (none)
- **System behaviour:**
  - REQ-16.1 The system shall verify that all prior stages are `COMPLETED` (or explicitly marked Not Applicable) before allowing closure, and list any outstanding items.
  - REQ-16.2 On closure the system shall release/close all open expiry trackers for the package (Section 6) and stop further expiry alerts.
  - REQ-16.3 On closure the system shall reconcile Budget Consumption against Budget Release for the package and report the residual (Section 5).
  - REQ-16.4 A closed package becomes read-only and archived; reopening requires an authorised role and is audit-logged.
- **Exit criteria:** All three closure documents uploaded, Completion Date and Contract Close Date verified, package archived.

---

## 5. Budget Module Integration

Runs alongside the lifecycle rather than as a stage. Budget is **allocated annually at department level and drawn down per package** (Q-13).

| # | Module | Data Source | Requirement |
|---|--------|-------------|-------------|
| B-0 | Department Annual Budget | Manual entry | REQ-B0 The system shall hold one budget allocation per (fiscal year, department), entered manually. Package allocations draw down against it; the sum of package allocations for a fiscal year shall not exceed the departmental figure without an authorised override, and the departmental drawdown position (allocated / committed / remaining) shall be visible. |
| B-1 | Budget Allocation | Manual entry (Budget Management Module) | REQ-B1 The system shall allow allocation of budget to a package from its department's annual budget, entered manually. |
| B-2 | Budget Release | Manual entry | REQ-B2 The system shall record budget releases against an allocation, entered manually; cumulative release shall not exceed allocation without an authorised override. |
| B-3 | Revised Budget | Manual entry | REQ-B3 The system shall record budget revisions, each with effective date and reason; the revised figure supersedes the original for all calculations. **Approval is a single permission check, not a routed workflow** (Q-13): any user holding the approver role/permission may approve a revision. The approver, timestamp and reason are recorded. |
| B-4 | Additional Budget | Manual entry | REQ-B4 The system shall record additional budget as a separate line that increases the total available budget. |
| B-5 | Budget Consumption | **Automatic** — OCR-extracted Invoice/Bill amount | REQ-B5 The system shall compute Budget Consumption automatically from the Invoice Amount extracted at Stage 13, updating on each verified invoice. |
| B-6 | Remaining Budget | **Automatic** — system-calculated | REQ-B6 Remaining Budget = (Allocation ± Revisions + Additional) − Consumption, recalculated whenever any component changes. |

Additional requirements:

- REQ-B7 Budget figures shall be currency-aware and consistent with the currencies recorded at Stages 8, 9, 13 and 14. **A package is single-currency (Q-14):** contract, LC, invoices and payments within one package shall all share one currency, which is fixed at Stage 8 and validated at every later stage. A differing currency is rejected at entry. No FX conversion is performed and no rate source is required; should mixed currency ever become real, it is a change request, not a configuration change.
- REQ-B8 The system shall alert when Remaining Budget falls below a configurable threshold and when consumption would exceed available budget.
- REQ-B9 Budget entries and recalculations shall be audit-logged with user and timestamp.
- REQ-B10 A budget view per package shall show Allocation, Release, Revisions, Additional, Consumption and Remaining, with drill-through to the source invoices.

---

## 6. Expiry Tracking Matrix

Cross-cutting. Any document reaching the system with an expiry-bearing field is registered as a tracked item.

| # | Document | Expiry Field | Captured at |
|---|----------|--------------|-------------|
| E-1 | Tender Notice | Tender Validity | Stage 2 |
| ~~E-2~~ | ~~Bid Security (BG / Pay Order)~~ | *withdrawn — out of scope (Q-4)* | — |
| E-3 | Performance Guarantee (PG) | PG Expiry Date | Stage 7 (Validity) |
| E-4 | Letter of Credit (LC) | LC Expiry Date | Stage 9 |
| E-5 | Contract Agreement & Price Schedule | Completion Date | Stage 8 |
| E-6 | Warranty Certificate | Warranty End Date | Stage 15 |

Requirements:

- REQ-E1 The system shall register an expiry tracker automatically when the corresponding field is verified at its stage.
- REQ-E2 Each tracker shall support configurable advance-warning intervals per document type. The seeded default for every tracked document is **90 / 60 / 30 / 15 / 7 days**, editable per document type by an administrator without a code change (Q-15).
- REQ-E3 The system shall raise notifications (in-app, email) at each warning interval and on the expiry date. The default recipient is the package's responsible officer plus all users holding the **Checker** role for that department (Q-15); the recipient set is configurable per document type.
- REQ-E4 Expired items shall be highlighted on the package view and on a consolidated expiry dashboard filterable by document type, package, stage and date range.
- REQ-E5 Extensions and amendments (e.g. LC amendment, PG extension, tender validity extension) shall update the tracked expiry date, retaining the previous value in history.
- REQ-E6 Trackers shall be closed automatically at Contract Close (REQ-16.2) or when the underlying instrument is released.
- REQ-E7 The expiry dashboard shall be exportable (PDF / Excel).

---

## 7. Cross-Cutting Requirements

- REQ-X1 **Correlation:** Package Number (Stage 1) and Contract Number (Stage 8) shall correlate every document, metadata record, budget entry and expiry tracker in the lifecycle, per the linkage model in Section 2.
- REQ-X2 **Linked document view:** From any stage the user shall be able to view the full document chain for the package across all 16 stages (see REQ-L8).
- REQ-X3 **OCR verification UI:** Every OCR-extracted field shall be presented with its confidence indicator, the source document, and inline edit; a field below a configurable confidence threshold is forced through manual confirmation.
- REQ-X4 **Role-based access (Q-17):** the procurement module defines **two roles — Maker and Checker**. The Maker creates packages, uploads documents, enters and corrects field values, and submits a stage. The Checker verifies OCR-suggested values, completes stages, marks a stage Not Applicable, sends a stage back for rework, declares a delivery final, approves budget entries and revisions, and reopens a closed package. A user may hold both roles, but the system shall warn when the same user both submits and approves the same stage. All stage entry, completion, override and rework actions are permission-controlled against these two roles.
- REQ-X4a **Existing workflow engine (Q-16):** the client confirms nothing depends on the current generic workflow engine. The `StageEngine` becomes the sole workflow authority for procurement; the generic `workflows` machinery is retired rather than kept in parallel.
- REQ-X4b **Document language (Q-19):** all procurement documents are in **English**. The OCR engine is configured for English only (`eng`); no Bangla language pack, no mixed-script handling and no bilingual field variants are provisioned. A document that turns out to be in Bangla falls through to manual entry.
- REQ-X4c **Deployment scope (Q-20):** a single department, **BPDB**, is in scope. Department remains a first-class column on packages and budgets so multi-department operation needs no schema change, but no cross-department access rules, routing or reporting are built now.
- REQ-X5 **Dashboard:** A lifecycle view shall show each package's current stage, elapsed time per stage, overdue deadlines, budget position and open expiries.
- REQ-X6 **Audit trail:** All actions described in this document shall be recorded immutably (who, what, when, before/after).
- REQ-X7 **Versioning:** Re-uploaded documents shall create a new version; prior versions remain retrievable.
- REQ-X8 **Notifications:** Deadline fields (PG Submission Last Date, Contract Signing Last Date, delivery window, expiry dates) shall drive scheduled alerts to the responsible role.

---

## 8. Client Alignment Register — answered 2026-08-13

All 20 questions in [procurement-open-points-questionnaire.md](procurement-open-points-questionnaire.md) are answered. Nothing in this document now rests on an unconfirmed assumption. The table records the answer and where it lands.

| Q | Question | Client answer | Effect on this document |
|---|---|---|---|
| **Q-1** | APP line → package cardinality | One APP line **may be split into lots**. (One contract spanning several APP packages was *not* selected.) | REQ-L13, REQ-1.5; `lot_number` on `procurement_package`; L-01 becomes 1 → 1..n. Contract → package stays **1 → 1** (L-11). |
| **Q-2** | Re-tender | New Tender record under the **same package**, previous tender retained as history | REQ-L14, REQ-L15, REQ-2.6; `tender` gains `attempt_no` / `is_current` / `failure_reason`; L-02 becomes 1 → 1..n |
| **Q-3** | Bidder data only from the BER | Confirmed — current design correct | No change (REQ-3.2, REQ-4.1 stand) |
| **Q-4** | Bid Security | **Out of scope** for this application | `bid_security` table withdrawn; REQ-3.4 withdrawn; L-06 withdrawn; expiry item E-2 withdrawn; catalogue rows removed |
| **Q-5** | LC applicability | ICT contracts may require an LC; derive from the Tender Notice **Procurement Type = ICT**. Any authorised user may mark Not Applicable | REQ-2.5, REQ-9.5, REQ-9.6 |
| **Q-6** | Billing system of record | **Keep both independently**, accepting duplicate entry | §3.2 finance row; `invoice.bill_header_id` bridge dropped |
| **Q-7** | APP Excel format | Sample supplied: `APP 22-23 First Revision_2980.xls` | REQ-1.1 — parser driven by a per-fiscal-year column mapping profile |
| **Q-8** | Master lists | Type NCT/ICT · Method OTM/LTM/RFQ/DPM · Nature Service/Works/Goods | REQ-2.4 (seeded lists) |
| **Q-9** | OCE source | **Manual input during BER upload** | REQ-4.2a; catalogue Src for `oce_value` changes O → M |
| **Q-10** | Inspection events | Keep as designed | No change |
| **Q-11** | Partial delivery | Permitted for **all** contract categories | REQ-12.1. *Finality* was not answered — assigned to the Checker role per Q-17 (REQ-12.5) |
| **Q-12** | Over-billing tolerance | **Not permitted** | REQ-13.3 becomes a hard block with no override; REQ-14.2 aligned |
| **Q-13** | Budget hierarchy | Allocated **annually at department level**, drawn down per package. Revision approval is a plain permission check | REQ-B0, REQ-B3; new `department_budget` table; L-22a |
| **Q-14** | Multi-currency | **No** — one package, one currency | REQ-B7; currency fixed at Stage 8 and validated downstream; no FX |
| **Q-15** | Expiry intervals | Use defaults, keep configurable | REQ-E2 (90/60/30/15/7 default), REQ-E3 (default recipients) |
| **Q-16** | Existing workflow engine | Nothing depends on it — **revamp freely** | REQ-X4a; `StageEngine` is the sole workflow authority |
| **Q-17** | Roles | Two roles: **Maker** and **Checker** | REQ-X4 |
| **Q-18** | Retention | **1 year** | REQ-P17 |
| **Q-19** | Document language | **English only** | REQ-X4b — OCR configured `eng` only |
| **Q-20** | Pilot scope | One department: **BPDB** | REQ-X4c |

### 8.1 Points the answers did not fully close

These are not blocking; each has a stated fallback in the requirement it affects.

1. **Q-11 — who declares a delivery final.** Not answered. Assigned to the Checker role (REQ-12.5); confirm at UAT.
2. **Q-15 — per-document intervals and recipients.** The client asked for "defaults, kept configurable". The defaults in REQ-E2/E3 apply until the client tunes them in the admin screen before go-live.
3. **Q-18 — 1 year retention** is short for procurement records that often have statutory retention far longer, and shorter than the warranty period of many contracts (Stage 15 alone can run 12–24 months). Purging is therefore implemented as an explicit administrative action rather than an automatic job (REQ-P17), so nothing is destroyed by a clock. Recommend the client re-confirm against BPDB's records-retention policy before any purge is run.
4. **Q-12 — over-billing.** "Not permitted **for now**" implies this may loosen. REQ-13.3 is implemented as a single ceiling check in `ValidationService`, so introducing a tolerance band or an override role later is a contained change.
