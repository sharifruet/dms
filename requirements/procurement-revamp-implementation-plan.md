# Procurement Lifecycle Revamp — Implementation Plan

**Companion to:** [procurement-lifecycle-workflow-requirements.md](procurement-lifecycle-workflow-requirements.md)
**Covers:** database, backend and frontend revamp to make the 16-stage procurement lifecycle the spine of the DMS
**Status:** Phases 1–6 built and verified. Phases 7–8 (migration, hardening/rollout) outstanding.
**Decisions taken:** (1) The procurement workspace **replaces** the document-centric pages rather than sitting alongside them — see §2.4. (2) Q-1 answered "both" — an APP line may split into lots *and* a contract may span packages; the schema reflects this (see §9).

---

## 0. What has been built

| Layer | Delivered | Verified by |
|---|---|---|
| **Database** | Changesets `031`–`037`: 36 new tables, alters to `documents` / `expiry_tracking` / `document_type_fields`, plus seeded stage definitions (36 required-document rows, 61 catalogue fields across all 16 stages) | `liquibase:update` applies all 174 changesets against a clean PostgreSQL 15; `liquibase:rollback` of the 12 procurement changesets returns clean |
| **Backend** | 35 entities, 36 repositories, 11 services, 5 controllers under `com.bpdb.dms.procurement` | `mvn clean compile` green (99 classes); 9 unit tests pass |
| **Frontend** | 5 pages, 7 components, typed API client, routing revamp with redirects | `react-scripts build` green, bundle emitted |

**Client answer incorporated (Q-1).** Because one APP line may yield several lot packages *and* one contract may cover several packages, `contract` carries no `package_id`; the relationship lives in a `contract_package` link table with an `is_primary` flag and `allocation_pct`. Budget consumption apportions an invoice across every package on its contract — by explicit percentages where set, otherwise evenly — so `budget_consumption` is unique on `(invoice_id, package_id)` rather than on the invoice alone. `procurement_package` gained `lot_number` / `lot_description`.

**Not yet done:** Phase 7 (migrating `document_metadata` → `extracted_field`, APP/document backfill) and Phase 8 (integration tests over the full 16-stage path, performance, security review, UAT, and the actual route retirement). The old pages currently redirect but have not been deleted — deliberately, per R-8.

---

## 1. Why a revamp and not an add-on

### 1.1 Current state

| Tier | What exists today | Gap against the requirements |
|---|---|---|
| **DB** | 23 tables. `documents` + `document_metadata` (key/value/confidence), `document_type_fields`, `document_relationships`, `expiry_tracking`, `app_headers`/`app_lines`, `bill_headers`/`bill_lines` | No procurement package, no stages, no lifecycle graph. Metadata is untyped EAV with no source, no verification state, no history. Expiry trackers point at a document, not at the instrument they guard. |
| **Backend** | Spring Boot 3.2 / Java 21. 82 entities, 44 repositories, 53 services, 31 controllers. `OCRService` (Tess4j) extracts text + a regex metadata map; `DocumentMetadataService` persists key/value | Document-centric. No stage gating, no lifecycle state machine, no field provenance, no cross-stage validation. OCR patterns are hard-coded in `OCRService` rather than catalogue-driven. |
| **Frontend** | React 18 + TypeScript + MUI + Redux Toolkit + React Query, CRA (`react-scripts`). 25 pages, all entity-CRUD (`Documents`, `ExpiryTracking`, `AppEntries`, `BillEntries`, …) | No package view, no stage workflow UI, no OCR verify-and-confirm screen, no linked-document graph. A user cannot see "where is this package and what is blocking it". |

### 1.2 The problem

The system today answers *"what documents do we have?"*. The requirements demand it answer *"where is package GD-24-115, what is blocking it, what does it cost, and what expires next?"*. That is a different center of gravity: the **procurement package** becomes the aggregate root and documents become evidence attached to it.

This cannot be bolted on, because three things must change at the core:

1. **The data model gains a spine** — 24 linked tables where there were none (§3.3 of the requirements).
2. **Capture becomes provenance-tracked** — every value carries source, confidence, verification state and history (`extracted_field`), replacing untyped `document_metadata`.
3. **The UI reorients around a workflow** — stage-by-stage progression with gates, not a document list.

### 1.3 What is *not* being thrown away

Deliberately preserved and reused: authentication/JWT, roles & permissions, `documents` + `document_versions` + file storage, `audit_logs`, notifications, Elasticsearch indexing, the OCR engine integration, the MUI/Redux/React Query frontend stack, Docker/Jenkins pipeline. Pages in unrelated domains (Assets, Asset Assignments, Stationery Tracking, System Health, Integrations, Users) stay untouched.

The document-centric pages that the procurement workspace supersedes **are** retired — see the retirement map in §2.4.

---

## 2. Target architecture

### 2.1 Backend module layout

New code lands in a dedicated package so the procurement domain is separable from the generic DMS:

```
com.bpdb.dms.procurement
├── entity/          ProcurementPackage, PackageStage, Tender, TenderOpening,
│                    BidSecurity, Evaluation, BerBidder, ContractApproval, Noa,
│                    PerformanceSecurity, Contract, LetterOfCredit, LcAmendment,
│                    PriceSchedule(+Line), ProductionSchedule, ProgressReport,
│                    InspectionEvent, Delivery(+Line), Invoice, Payment,
│                    Warranty, ContractClosure, DocumentLink, ExtractedField(+History),
│                    BudgetEntry, BudgetConsumption, OcrJob, OcrResult, OcrPage
├── repository/      one Spring Data repo per aggregate
├── service/
│   ├── StageEngine            stage gates, transitions, rework (WF-01…WF-10)
│   ├── StageDefinitionService required docs + mandatory fields per stage (catalogue-driven)
│   ├── CaptureService         writes typed column + extracted_field in one tx (REQ-P4)
│   ├── FieldCatalogueService  document_type_fields-driven field defs (REQ-P13)
│   ├── ExtractionService      OCR result → candidate fields → auto-link (REQ-L3)
│   ├── LinkageService         document_link, orphan detection (REQ-L1, L10)
│   ├── ValidationService      cross-stage rules (dates, amounts, counts)
│   ├── BudgetService          allocation/release/revision + derived remaining (REQ-B6)
│   ├── ExpiryService          tracker registration + supersede (REQ-E1…E7)
│   └── PackageQueryService    lifecycle view, graph read, dashboards
├── controller/      PackageController, StageController, CaptureController,
│                    BudgetController, ExpiryController, LinkageController
└── dto/             stage payloads, capture requests, graph projections
```

**Key design point — the StageEngine is the only writer of stage status.** Controllers never set `package_stage.status` directly. Completion is computed: required documents present ∧ mandatory catalogue fields `VERIFIED` ∧ validations pass.

### 2.2 Frontend structure

```
frontend/src/
├── pages/procurement/
│   ├── PackageList.tsx          all packages, current stage, blockers, budget, expiries
│   ├── PackageWorkspace.tsx     the main screen — stage rail + active stage panel
│   ├── StagePanel.tsx           per-stage: required docs, field form, gate status
│   ├── VerifyPanel.tsx          OCR field review: value | confidence | source highlight
│   ├── LinkageGraph.tsx         the §2.1 tree, navigable
│   ├── BudgetPanel.tsx          allocation → consumption → remaining, drill to invoices
│   ├── ExpiryDashboard.tsx      consolidated, filterable, exportable
│   └── ExceptionsDashboard.tsx  orphans, broken links, failed OCR, overdue deadlines
├── components/procurement/
│   ├── StageRail.tsx            16-stage progress rail with gate state
│   ├── DocumentDropzone.tsx     upload → OCR job → live status
│   ├── FieldRow.tsx             one field: raw vs normalized, confidence, verify/override
│   ├── DocumentViewer.tsx       PDF/image with bbox highlight (REQ-P6)
│   └── BidderTable.tsx          BER bidder rows (Stage 4)
├── store/slices/procurementSlice.ts
├── services/procurementApi.ts
└── types/procurement.ts         generated from backend DTOs
```

**Key UX point — the verify screen is the heart of the app.** Document on the left, extracted fields on the right, click a field to highlight its source region. Confirm/override per field. This is where OCR accuracy becomes trustworthy data, and it is what gates every stage.

### 2.3 Database

24 new tables per §3.3–3.8 of the requirements, delivered as Liquibase changesets `011`–`016`, plus targeted alters to `documents`, `expiry_tracking` and `document_type_fields`.

### 2.4 Retirement map — pages and services being replaced

The procurement workspace becomes the primary UI. The following are retired, with routes redirected so no bookmark 404s.

**Frontend — retire and redirect**

| Retired page | Replaced by | Redirect |
|---|---|---|
| `Documents.tsx`, `DocumentsEnhanced.tsx` | `PackageWorkspace` (documents are reached through the stage that owns them) + a document detail route | `/documents` → `/procurement/packages` |
| `ExpiryTracking.tsx` | `ExpiryDashboard` (entity-linked, supersede-aware) | `/expiry` → `/procurement/expiries` |
| `AppEntries.tsx` | Stage 1 panel + package creation from APP import | `/app-entries` → `/procurement/packages?stage=1` |
| `BillEntries.tsx` | Stage 13 panel (invoice) + Stage 14 (payment) | `/bill-entries` → `/procurement/packages?stage=13` |
| `Archive.tsx` | Closed packages filter on `PackageList` | `/archive` → `/procurement/packages?status=closed` |
| `DocumentVersioning.tsx` | Version history inside `DocumentViewer` | `/versioning` → document detail |
| `Dashboard.tsx`, `DashboardPage.tsx` | Lifecycle dashboard (stage distribution, blockers, budget, expiries) | `/dashboard` → `/procurement/dashboard` |

**Frontend — reworked, not retired**

| Page | Change |
|---|---|
| `Search.tsx` | Results carry package context — Package Number, Contract Number, stage (REQ-L9) |
| `DocumentTypeFields.tsx` | Repurposed as the **field catalogue admin**: stage, entity, column, capture source, mandatory flag, OCR pattern (REQ-P13) |
| `Reports.tsx` | Procurement report set added; document-count reports dropped |
| `Notifications.tsx` | Deadline and expiry alert types added |

**Backend — retire or fold**

| Component | Action |
|---|---|
| `DocumentMetadataService` + `document_metadata` endpoints | Deprecated in Phase 7, removed one release later; superseded by `CaptureService` / `extracted_field` |
| `ExpiryTrackingController` / `ExpiryTrackingService` | Reworked to entity-linked trackers, not deleted |
| `AppEntryService`, `AppDocumentService` | Folded into Stage 1 package creation |
| `BillService`, `BillOCRService` | Folded into Stage 13 invoice capture; `bill_headers`/`bill_lines` retained per open point 14 |
| `DocumentRelationshipController` | Kept for ad-hoc links only; structural linkage moves to `document_link` |

**Open conflict to resolve in Phase 0:** the existing generic `WorkflowController` / `workflows` table overlaps conceptually with `StageEngine`. Decide whether the procurement lifecycle is modelled *in* the generic workflow engine (flexible, but the engine was not built for stage gates with document/field preconditions) or the two coexist with the generic engine reserved for approvals. **Recommendation: coexist** — `StageEngine` owns the 16 stages; the generic engine is left for ad-hoc approval routing. This is question Q-16 in the questionnaire.

---

## 3. Phases

Nine phases. Each ends with something demonstrable. Phases 1–3 are the critical path; 4–7 can partly parallelise across two developers.

### Phase 0 — Decisions and scaffolding *(before any code)*

| Task | Output |
|---|---|
| Client answers the open points | [procurement-open-points-questionnaire.md](procurement-open-points-questionnaire.md) returned — **blocking, Q-1…Q-6 especially** |
| ~~Alongside vs replace~~ | **Decided: replace.** Retirement map in §2.4 |
| Resolve the `WorkflowController` overlap (Q-16) | Architecture decision |
| Validate the stage-gate model against 3–5 real BPDB packages | Confirms gates match practice (R-3) |
| Collect a labelled sample document set per document type | Baseline for the OCR accuracy harness (R-1) |
| Create feature branch, add `procurement` package skeleton, ADR for the capture model | Branch ready |

**Exit:** questionnaire returned and signed off, branch cut. **Risk if skipped:** schema rework mid-build — Q-1 and Q-2 change table cardinality, which is expensive to unwind after Phase 2.

**Current status: this phase is active and blocking. No code is being written until the questionnaire returns.**

---

### Phase 1 — Database foundation

| Task | Detail |
|---|---|
| `011-create-procurement-core.xml` | `procurement_package`, `package_stage`, `document_link` |
| `012-create-tender-award.xml` | `tender`, `tender_opening`, `bid_security`, `evaluation`, `ber_bidder`, `contract_approval`, `noa`, `performance_security` |
| `013-create-contract-execution.xml` | `contract`, `letter_of_credit`, `lc_amendment`, `price_schedule(+line)`, `production_schedule`, `progress_report`, `inspection_event`, `delivery(+line)` |
| `014-create-billing-closure.xml` | `invoice`, `invoice_delivery_link`, `payment`, `payment_invoice_link`, `warranty`, `contract_closure` |
| `015-create-capture-store.xml` | `extracted_field`, `extracted_field_history`, `ocr_job`, `ocr_result`, `ocr_page` |
| `016-alter-existing.xml` | `documents` += `package_id, contract_id, stage_code`; `expiry_tracking` += `entity_type, entity_id, package_id, superseded_by_id`; `document_type_fields` += `stage_code, entity_type, entity_column, capture_source, is_mandatory` |
| `017-seed-field-catalogue.xml` | Seed §3.7 catalogue (~45 fields) + stage document requirements |
| Indexes | Per REQ-P19 |
| Rollback blocks | Every changeset reversible |

**Exit:** `liquibase update` and `rollback` both clean on a fresh Postgres; seeded catalogue queryable.
**Estimate:** ~1 sprint.

---

### Phase 2 — Backend domain core

| Task | Detail |
|---|---|
| JPA entities + repositories | ~30 entities mirroring Phase 1, with FK relationships and optimistic locking |
| `StageEngine` | Gate evaluation, transition, rework, Not-Applicable (WF-01…WF-10, REQ-9.5) |
| `StageDefinitionService` | Reads required docs + mandatory fields from the catalogue — no hard-coded stage logic |
| `CaptureService` | Single transaction writing typed column + `extracted_field` + history (REQ-P4, P5) |
| `LinkageService` | `document_link` creation, OCR-key auto-link, orphan detection (REQ-L3, L4, L10) |
| `ValidationService` | Cross-stage rules: date ordering, bidder count reconciliation, amount ceilings, quantity rollup |
| Unit tests | Stage gate matrix, capture provenance, link integrity |

**Exit:** a package can be driven 1 → 16 through service-layer tests with no HTTP or UI.
**Estimate:** ~2 sprints. **This is the highest-value phase — the rest is exposure.**

---

### Phase 3 — OCR pipeline rework

| Task | Detail |
|---|---|
| Persist OCR jobs/results | Refactor `OCRService` to write `ocr_job` / `ocr_result` / `ocr_page`; async with retry (REQ-P9…P12) |
| Catalogue-driven extraction | Move regex patterns out of `OCRService` into `document_type_fields.ocr_pattern`; `ExtractionService` resolves patterns by document type + stage |
| Field candidates | Extraction emits `extracted_field` rows as `OCR_SUGGESTED` with confidence, page, bbox |
| Auto-link | Package/Contract number match → link to existing parent, else flag (REQ-L3) |
| Excel path | Extend `AppExcelImportService` to create packages from APP lines (Stage 1); add e-GP price schedule import (Stage 10) |
| Re-OCR | Additive versioning, `is_current` flip (REQ-P10) |
| Accuracy harness | Sample document set + per-field accuracy report |

**Exit:** uploading a real tender notice produces reviewable, source-linked candidate fields.
**Estimate:** ~2 sprints. **Highest technical risk** — see §5.

---

### Phase 4 — Backend API surface

| Endpoint group | Routes |
|---|---|
| Packages | `GET/POST /api/procurement/packages`, `GET /{id}`, `GET /{id}/graph`, `GET /{id}/timeline` |
| Stages | `GET /{id}/stages`, `GET /{id}/stages/{code}`, `POST /{id}/stages/{code}/complete`, `POST /rework`, `POST /not-applicable` |
| Capture | `GET /stages/{code}/fields`, `PUT /fields/{fieldId}` (verify/override), `POST /fields/bulk-verify`, `GET /fields/{id}/history` |
| Documents | `POST /{id}/stages/{code}/documents`, `GET /documents/{id}/ocr`, `POST /documents/{id}/reocr` |
| Budget | `GET/POST /{id}/budget`, `GET /{id}/budget/summary` |
| Expiry | `GET /expiries`, `POST /expiries/{id}/supersede`, `GET /expiries/export` |
| Exceptions | `GET /exceptions/orphans`, `/broken-links`, `/failed-ocr`, `/overdue` |
| Search | Extend `SearchController` to return package context with hits (REQ-L9) |

Plus: role checks per route (REQ-X4), `audit_logs` on every mutation (REQ-P21), OpenAPI spec published for FE type generation.

**Exit:** full lifecycle drivable via Postman; OpenAPI spec green.
**Estimate:** ~1.5 sprints.

---

### Phase 5 — Frontend core workflow

| Task | Detail |
|---|---|
| API client + types | Generated from OpenAPI into `types/procurement.ts` |
| `PackageList` | Sortable/filterable, current stage, blocker chip, budget bar, expiry badge |
| `StageRail` | 16-stage rail: done / active / blocked / not-applicable |
| `PackageWorkspace` + `StagePanel` | Required-document checklist, upload dropzone, field form, gate status, complete button (disabled with reason when blocked) |
| `VerifyPanel` + `DocumentViewer` | Side-by-side verify with bbox highlight, per-field confirm/override, bulk verify, confidence colouring |
| Stage-specific panels | `BidderTable` (Stage 4), price schedule lines (10), delivery/invoice/payment link pickers (12–14) |

**Exit:** a user can take a package from APP to Contract Signing entirely in the UI.
**Estimate:** ~2.5 sprints.

---

### Phase 6 — Frontend supporting views

| Task | Detail |
|---|---|
| `LinkageGraph` | Navigable §2.1 tree from any record up to APP and down to closure (REQ-L8) |
| `BudgetPanel` | Allocation/release/revision entry, consumption drill-through to invoices |
| `ExpiryDashboard` | Filter by type/stage/date, colour by proximity, PDF/Excel export (REQ-E7) |
| `ExceptionsDashboard` | Orphans, broken links, failed OCR, overdue deadlines |
| Lifecycle dashboard | Stage distribution, elapsed time per stage, overdue packages (REQ-X5) |
| Notifications | Wire deadline/expiry alerts into the existing notification centre |
| Rework `Search`, `DocumentTypeFields`, `Reports` | Per the "reworked, not retired" table in §2.4 |
| Flat document list inside the workspace | Mitigates R-9 — users who think in documents, not stages |

**Exit:** every requirement in Sections 5–7 has a screen, and every retired page's function exists in the new UI.
**Estimate:** ~2 sprints.

---

### Phase 7 — Migration and backfill

| Task | Detail |
|---|---|
| `document_metadata` → `extracted_field` | Migrate key/value/confidence with `capture_source = OCR`, `status = OCR_SUGGESTED`; retain compatibility view |
| APP backfill | Create packages from existing `app_lines` |
| Document re-linking | Best-effort attach existing `documents` to packages via extracted keys; unmatched → quarantine list for manual linking |
| Expiry backfill | Repoint existing `expiry_tracking` rows to entities |
| Deprecation | Mark `DocumentMetadataService` and legacy metadata endpoints deprecated; remove after one release |
| Dry-run tooling | Migration runs in report-only mode first, with a reconciliation count |

**Exit:** production copy migrated in staging with a signed-off reconciliation report.
**Estimate:** ~1 sprint. **Do not skip the dry run.**

---

### Phase 8 — Hardening and rollout

| Task | Detail |
|---|---|
| Integration tests | Full 16-stage happy path + rework + Not-Applicable + partial delivery/billing |
| Performance | Package graph read, expiry dashboard, search with package context under expected volume |
| Security review | Row-level access on package/department; verify no route bypasses the role model |
| OCR accuracy sign-off | Per-field accuracy against the sample set; threshold tuning per field |
| UAT | Client walkthrough with real packages |
| Rollout | Feature-flag procurement module; pilot department first, then general release |
| **Page retirement** | *After pilot sign-off only* (R-8): cut the routes in §2.4 over to redirects, remove retired pages from navigation. Redirects — not deletions — held for one full release |
| Deprecation removal | Remove `DocumentMetadataService` and legacy metadata endpoints one release after Phase 7 deprecation |

**Exit:** UAT signed off, pilot live, retired routes redirecting.
**Estimate:** ~1.5 sprints.

---

## 4. Sequencing

```
Phase 0  ▓▓
Phase 1    ▓▓▓▓
Phase 2        ▓▓▓▓▓▓▓▓
Phase 3            ▓▓▓▓▓▓▓▓          (starts once entities land)
Phase 4                ▓▓▓▓▓▓
Phase 5                    ▓▓▓▓▓▓▓▓▓▓ (starts once API contract frozen)
Phase 6                            ▓▓▓▓▓▓▓▓
Phase 7                                ▓▓▓▓
Phase 8                                    ▓▓▓▓▓▓
```

Roughly **13–14 sprints (~6–7 months)** with 2 backend + 2 frontend developers. Compressible to ~5 months if Phase 3's extraction work is timeboxed and manual entry is accepted as the fallback for low-accuracy document types.

**Hard dependencies:** 1 → 2 → 4 → 5. Phase 3 can trail Phase 4 (stages work with manual entry before OCR is tuned). Phase 6 needs Phase 5's shell.

---

## 5. Risks

| # | Risk | Impact | Mitigation |
|---|---|---|---|
| R-1 | **OCR accuracy on scanned Bangla/English procurement documents is too low to trust** | Users abandon verify screens and key everything manually | Timebox extraction tuning per document type; design every field to be manually enterable from day one; measure accuracy per field and publish it; treat OCR as *assistive*, never blocking |
| R-2 | **BER table extraction is hard** — bidder tables vary wildly in layout | Stage 4, now the sole source of bidder data, becomes a manual-entry bottleneck | Build a spreadsheet-style paste/edit grid for `ber_bidder` as the primary path, with OCR pre-fill as a bonus |
| R-3 | Stage gates too rigid for real BPDB practice (documents arrive out of order) | Users blocked, workarounds outside the system | Authorised override with recorded reason on every gate; validate the gate model against real packages in Phase 0 |
| R-4 | Migration of `document_metadata` loses or mis-attributes data | Historical metadata corrupted | Report-only dry run, reconciliation counts, compatibility view, no destructive deletes |
| R-5 | Scope creep from the 82 existing entities — pressure to "integrate everything" | Timeline slip | Procurement module is separable; only the reuse list in §1.3 is in scope |
| R-6 | Client open points (§8 of requirements) unanswered when Phase 1 starts | Schema rework | Phase 0 gate — do not start Phase 1 with open cardinality questions (points 11, 12) |
| R-7 | CRA (`react-scripts`) is unmaintained; build tooling ages out mid-project | Build/security friction | Optional Vite migration during Phase 5; not a blocker, keep separable |
| R-8 | **Retiring the document-centric pages (§2.4) removes the fallback** — if the stage workflow proves too rigid or OCR too weak, users have no old screen to fall back to | Work stops rather than degrades | Retire only in Phase 8, *after* pilot sign-off, never before; keep redirects (not deletions) for one full release; ensure every retired page's core function exists in the new UI before its route is cut; feature-flag the cutover per department so it is reversible |
| R-9 | Users trained on document-centric navigation cannot find documents in a stage-oriented UI | Adoption failure | Keep universal search prominent and package-context-aware (REQ-L9); provide a flat document list view within the package workspace; training in Phase 8 UAT |

---

## 6. Testing strategy

| Level | Coverage |
|---|---|
| Unit | StageEngine gate matrix (all 16 × all statuses), CaptureService provenance invariants (raw immutable, history appended), ValidationService rules |
| Integration | Full lifecycle 1 → 16; rework mid-lifecycle; Stage 9 Not-Applicable; partial delivery → progressive billing → part payment; budget consumption rollup; expiry supersede on LC amendment |
| Contract | OpenAPI spec vs FE generated types in CI |
| Data | Migration reconciliation counts; orphan detection returns zero on a clean fixture |
| E2E | Upload → OCR → verify → complete stage, for the three highest-volume document types |
| Accuracy | Per-field OCR accuracy report against a labelled sample set, run per release |

---

## 7. Definition of done

A phase is done when: code merged with tests, Liquibase changesets reversible, OpenAPI updated, audit logging verified on every mutation, role checks in place, and the demonstrable outcome in that phase's **Exit** row shown to the client.

The revamp is done when a BPDB user can open package GD-24-115, see it sitting at Stage 12 with two deliveries recorded, see that the LC expires in 40 days, see 680 lac remaining budget, click any figure through to the document it came from, and know exactly what is blocking the next stage.
