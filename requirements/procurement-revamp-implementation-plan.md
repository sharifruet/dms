# Procurement Lifecycle Revamp — Implementation Plan

**Companion to:** [procurement-lifecycle-workflow-requirements.md](procurement-lifecycle-workflow-requirements.md)
**Covers:** database, backend and frontend revamp to make the 16-stage procurement lifecycle the spine of the DMS
**Status:** Phases 1–6 built and verified. **Phase 6.5 (answer reconciliation) is now the active phase** — see §9. Phases 7–8 (migration, hardening/rollout) outstanding.
**Decisions taken:** (1) The procurement workspace **replaces** the document-centric pages rather than sitting alongside them — see §2.4. (2) The full questionnaire returned on **2026-08-13**; all 20 answers are recorded in §8 of the requirements. The as-built schema anticipated some answers correctly and some not — §9 is the reconciliation backlog.

---

## 0. What has been built

| Layer | Delivered | Verified by |
|---|---|---|
| **Database** | Changesets `031`–`037`: 36 new tables, alters to `documents` / `expiry_tracking` / `document_type_fields`, plus seeded stage definitions (36 required-document rows, 61 catalogue fields across all 16 stages) | `liquibase:update` applies all 174 changesets against a clean PostgreSQL 15; `liquibase:rollback` of the 12 procurement changesets returns clean |
| **Backend** | 35 entities, 36 repositories, 11 services, 5 controllers under `com.bpdb.dms.procurement` | `mvn clean compile` green (99 classes); 9 unit tests pass |
| **Frontend** | 5 pages, 7 components, typed API client, routing revamp with redirects | `react-scripts build` green, bundle emitted |

**Built ahead of the answers (Q-1).** The build assumed *both* halves of Q-1: that an APP line may yield several lot packages **and** that one contract may cover several packages. So `contract` carries no `package_id`; the relationship lives in a `contract_package` link table with `is_primary` and `allocation_pct`, and `budget_consumption` is unique on `(invoice_id, package_id)` so an invoice apportions across a contract's packages. `procurement_package` gained `lot_number` / `lot_description`.

The returned answer selects **only the lot-split half**. The lot-split work is confirmed correct; the multi-package-contract machinery is a superset of what the client needs. §9 records the decision on what to do about it.

**Not yet done:** Phase 6.5 (§9 — reconciling the build against the answers), Phase 7 (migrating `document_metadata` → `extracted_field`, APP/document backfill) and Phase 8 (integration tests over the full 16-stage path, performance, security review, UAT, and the actual route retirement). The old pages currently redirect but have not been deleted — deliberately, per R-8.

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
| ~~`BillEntries.tsx`~~ | **Not retired** — the finance module stays independent (Q-6). `/bill-entries` keeps working alongside the Stage 13 panel | none |
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
| `BillService`, `BillOCRService` | **Retained, not folded** (Q-6): the client keeps the finance module running independently and accepts duplicate entry. Stage 13 invoice capture is a separate path with no FK bridge to `bill_headers` |
| `DocumentRelationshipController` | Kept for ad-hoc links only; structural linkage moves to `document_link` |

**Resolved (Q-16).** The client confirms nothing depends on the existing generic workflow engine: *"No current / existing workflow engine will no longer needed. You can revamp."* The recommendation to have the two coexist is therefore dropped — `StageEngine` becomes the **sole** workflow authority. `WorkflowController` / `WorkflowService` / the `workflows` table and the folder→workflow mapping (`025-add-folder-workflow-mapping`) are retired on the same schedule as the other retirements in this section: routes redirected in Phase 8 after pilot sign-off, code removed one release later. Any approval routing the procurement module needs is expressed as the Maker/Checker permission check of Q-17, not as a routed workflow.

---

## 3. Phases

Nine phases. Each ends with something demonstrable. Phases 1–3 are the critical path; 4–7 can partly parallelise across two developers.

### Phase 0 — Decisions and scaffolding *(before any code)*

| Task | Output |
|---|---|
| ~~Client answers the open points~~ | **Done 2026-08-13** — all 20 answered; register in §8 of the requirements, build reconciliation in §9 below |
| ~~Alongside vs replace~~ | **Decided: replace.** Retirement map in §2.4 |
| ~~Resolve the `WorkflowController` overlap (Q-16)~~ | **Decided: retire the generic engine** — §2.4 |
| Validate the stage-gate model against 3–5 real BPDB packages | Confirms gates match practice (R-3) |
| Collect a labelled sample document set per document type | Baseline for the OCR accuracy harness (R-1) |
| Create feature branch, add `procurement` package skeleton, ADR for the capture model | Branch ready |

**Exit:** questionnaire returned and signed off, branch cut. **Risk if skipped:** schema rework mid-build — Q-1 and Q-2 change table cardinality, which is expensive to unwind after Phase 2.

**Current status: closed.** The questionnaire returned on 2026-08-13, after Phases 1–6 had been built against the stated assumptions. That risk partly materialised — Q-2 and Q-13 require table changes and Q-4 removes a table. The reconciliation is scoped in §9 and is small, because the assumptions were mostly right.

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

### Phase 6.5 — Answer reconciliation *(backend done; frontend outstanding)*

Fold the 2026-08-13 answers into the built schema, backend and UI. Scope, item by item, is in §9. This runs before Phase 7 so the migration lands on the final shape rather than migrating twice.

**Delivered:** changesets `038`–`040`; the backend changes D-1…D-15 except the UI halves; 20 procurement unit tests green (11 new, covering the money ceilings, the currency rule and the ICT derivation).
**Outstanding:** the frontend halves of D-1, D-2, D-5, D-7 (re-tender panel, departmental budget position, OCE field on the BER form, removal of the over-billing override control), and applying the changesets to a database.

**Exit:** every row in §9 closed or explicitly deferred with a reason; `liquibase update`/`rollback` clean; unit tests green.
**Estimate:** ~0.5 sprint.

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
| R-6 | ~~Client open points unanswered when Phase 1 starts~~ | *Materialised, contained.* Phases 1–6 were built before the answers returned. Three answers move tables (Q-2 re-tender, Q-4 bid security, Q-13 department budget) and one (Q-1) leaves a superset in place. Total reconciliation ≈ half a sprint — see §9. The Phase 0 gate was right; the cost of skipping it was real but bounded because the assumptions were documented and mostly correct |
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

---

## 9. Reconciliation backlog — build vs. the 2026-08-13 answers

Phases 1–6 were built against the assumptions stated in the questionnaire. Most held. This section is the delta, ordered by cost. New work lands in changesets `038`–`040`; nothing already applied is edited in place, because changesets `031`–`037` have run in environments outside this branch.

### 9.1 Schema changes required

| # | Answer | As built | Change | Status |
|---|---|---|---|---|
| D-1 | **Q-2** — re-tender adds a Tender under the same package | `tender.package_id` carried `UNIQUE` (`uk_tender_package`) — strictly one tender per package | `038`: dropped `uk_tender_package`; added `attempt_no`, `is_current`, `failure_reason`; added `UNIQUE (package_id, attempt_no)` and a partial unique index on `(package_id) WHERE is_current`; backfilled existing rows to attempt 1. New `TenderService.reTender()`; `TenderRepository.findByPackageId` replaced by `findByPackageIdAndIsCurrentTrue` at all four call sites | **Backend done.** Stage 2 re-tender panel outstanding |
| D-2 | **Q-13** — budget allocated annually at department level | `budget_entry` package-scoped only; no departmental layer | `039`: new `department_budget` with `UNIQUE (fiscal_year, department)`; `budget_entry` += `department_budget_id`. `BudgetService.departmentPosition()` reports allocated/committed/remaining; `GET|POST /api/procurement/department-budgets` | **Backend done.** `BudgetPanel` outstanding |
| D-3 | **Q-4** — Bid Security out of scope | `bid_security` table, entity, repository, 4 catalogue rows, expiry item E-2 | `040`: table dropped (rollback recreates it), entity and repository deleted, catalogue seeds removed, existing E-2 trackers cancelled rather than orphaned, `BID_SECURITY` removed from `ProcurementExpiryService` | **Done** |
| D-4 | **Q-6** — finance module stays independent | `invoice` has no `bill_headers` FK as built | **None needed** — the as-built shape already matched; only the requirements doc claimed a bridge. §2.4 corrected: `BillService` is *retained*, not folded | **Done** |
| D-5 | **Q-9** — OCE is manual | Catalogue seeded `oce_value` as `OCR` | `040`: seed row flipped to `MANUAL`, marked required and mandatory, relabelled "OCE Value (manual entry)" | **Backend done.** BER form field outstanding |
| D-6 | **Q-8** — master lists | Values not constrained | `040`: new `procurement_master_list` seeded with NCT/ICT · OTM/LTM/RFQ/DPM · Service/Works/Goods | **Seeded only.** No code reads the table yet — REQ-2.4 validation and the Stage 2 form binding are both outstanding |

### 9.2 The Q-1 overshoot — decision needed

The build assumed a contract may span several packages; the answer says it may not. What exists: `contract_package` (link table, `is_primary`, `allocation_pct`), `budget_consumption` unique on `(invoice_id, package_id)` with apportionment logic, and no `contract.package_id`.

**Decided: kept and constrained.** `StageDataService.linkContractToPackage()` is now the single place a contract is attached to a package, and it refuses a second package with a message naming the one already linked. The rule lives in the service rather than in a DB constraint, so relaxing it later is a one-line change. Rationale: ripping out the link table means changing `contract`, `budget_consumption`, the apportionment logic in `BudgetService` and every query that joins through `contract_package`, to arrive at a model that is strictly less capable. Lot-wise tendering is exactly the context where a client later says "actually, this contract covers Lot 1 and Lot 2". The cost of keeping it is one link row per contract and a guard; the cost of removing it and needing it back is the same rework twice.

**Still outstanding:** hide the apportionment controls in the UI, since they can no longer be exercised.

### 9.3 Behavioural changes (no schema impact)

| # | Answer | Change | Status |
|---|---|---|---|
| D-7 | **Q-12** — no over-billing | Enforced at write time, not merely flagged: `ProcurementRecordService.saveInvoice` rejects an invoice that would breach the contract value, measured against its siblings so editing an existing invoice is not compared with itself. `savePayment` does the same against the invoiced total. `ValidationService.overBillingMessage` names the excess and points at the Stage 8 revision route | **Backend done.** Remove the override control from the Stage 13 UI |
| D-8 | **Q-5** — LC applicability from Procurement Type | `StageEngine.lcExpected()` derives it from `tender.procurement_type = 'ICT'`, matched leniently (casing, stray whitespace) since OCR rarely returns a clean token. Surfaced as `StageReadiness.applicabilitySuggested` for the UI toggle. Marking an ICT package Not Applicable is permitted and logged as an override | **Backend done** |
| D-9 | **Q-17** — Maker / Checker | `040` seeds both roles and five procurement permissions. `SecurityConfig` now gates the procurement routes on them: approval actions (stage complete, Not Applicable, rework, re-tender, budget) are Checker-only; capture and read are open to both. Pinned by `MakerCheckerAuthorizationTest` (9 tests) | **Done.** The same-user submit-and-approve warning is outstanding |
| D-10 | **Q-16** — retire the generic workflow engine | `WorkflowController` / `WorkflowService` / `workflows` moved into the §2.4 retirement map | **Documented.** Do **not** delete before Phase 8 sign-off |
| D-11 | **Q-19** — English only | Already `eng` in all three property profiles — no change needed beyond recording that this is now a decision rather than a default. Drops the Bangla/mixed-script work from Phase 3's harness, which simplifies R-1 materially | **Done** |
| D-12 | **Q-14** — single currency per package | `ValidationService.mismatchedCurrency` compares against the contract currency fixed at Stage 8; enforced at write time for invoices and payments and reported at Stages 13/14. `fx_rate` column kept — it costs nothing and documents the constraint | **Backend done.** Remove the FX field from the capture UI |
| D-13 | **Q-15** / **Q-18** | `040` adds `expiry_policy`, seeded 90/60/30/15/7 per tracked document with Checker as default recipient. Retention: REQ-P17 now specifies 1 year with purge as an explicit admin action, never a scheduled job | **Seeded only.** No code reads `expiry_policy` yet; the notification scheduler still uses its own defaults |
| D-14 | **Q-7** — APP layout | `AppColumnProfile` + `AppWorkbookParser` + `AppPackageImportService`, driven by header labels rather than fixed offsets, with `POST /api/procurement/packages/import-app` (supports `dryRun`). Verified against the supplied workbook: 7 packages across 2 sheets | **Done.** See §12 |
| D-15 | **Q-20** — one department | No cross-department access rules in Phase 8's security review; `department` stays on packages and budgets so multi-department needs no schema change | **Noted** |

### 9.4 What did not change

Q-3 (bidder data from the BER only), Q-10 (inspection events), Q-11 (partial delivery for all categories) confirm the design as built. Q-11's unanswered half — who declares a delivery final — is assigned to the Checker role and should be confirmed at UAT.

### 9.5 Verification status

Run against a clean PostgreSQL 16 (`docker compose up -d postgres`).

| Check | Result |
|---|---|
| `mvnw clean compile` | Green |
| Procurement unit tests | 20 pass, 0 fail — 9 existing plus 11 new in `ClientAnswerRulesTest` covering the money ceilings, the currency rule, the ICT derivation and tender attempt defaults |
| `liquibase:update` | **Green.** All 187 changesets apply to an empty database, the 13 new ones (`038-001` … `040-008`) included |
| `liquibase:rollback` | **Green, and verified by inspection rather than exit code.** Rolling back all 13 restores the prior schema exactly: `tender` returns to its original 11 columns with `uk_tender_package` back in place, `bid_security` is recreated with its 3 catalogue rows, `oce_value` reverts to `OCR`, and `department_budget` / `procurement_master_list` / `expiry_policy` / the two roles are gone. Re-applying afterwards is clean |
| Re-tender semantics | Verified in SQL: two attempts now coexist under one package (impossible before), and a second `is_current` attempt is refused by `uk_tender_current` — `duplicate key value violates unique constraint` |
| Seeded data | 9 master-list values, 5 expiry policies at 90/60/30/15/7, MAKER with 4 permissions and CHECKER with 7 |
| Wider test suite | **Repaired — see §10.** Was 61 tests / 48 errors with no context ever loading; now 72 tests, 0 errors, 55 passing, 3 skipped, 14 genuine assertion failures |

One pre-existing oddity worth knowing: `014-ensure-admin-user` carries `runAlways="true"`, so it re-executes on every update. Harmless, but it makes the update log noisier than the changeset count suggests.

### 9.6 Frontend — delivered

All six items are built, plus two gaps the work surfaced. See §13.

---

## 10. Test suite repair (JDK 25)

The suite was producing no signal at all: 61 tests, 48 errors, and not a single Spring
context ever loaded. It now runs end to end — **72 tests, 0 errors, 55 passing, 3 skipped,
14 assertion failures**. The failures that remain are real disagreements between a test
and the code, which is the point: they are findings, not noise.

### 10.1 Toolchain

| Change | Detail |
|---|---|
| JDK | Builds and runs on **JDK 25**. Bytecode is emitted at **Java 21** (`maven.compiler.release=21`) because Spring Framework 6.1.1 — the version Boot 3.2.0 pins — bundles an ASM that cannot parse class file major 69. Compiling to 25 makes component scanning fail with *"ASM ClassReader failed to parse class file"* on every `@SpringBootTest`. Emitting true Java 25 bytecode requires Spring Boot 3.5.x (Spring 6.2+), which also pulls new Hibernate and Spring Security majors — a deliberate upgrade, not a compiler flag |
| Mockito | 5.7 → **5.20.0**, Byte Buddy 1.14 → **1.17.7**. The pinned versions refused to instrument classes on a JVM they predate, so every `@MockBean` test died with *"Mockito cannot mock this class"* |
| Mockito agent | Wired via `maven-dependency-plugin properties` + surefire `-javaagent`. Mockito was self-attaching, which the JDK warns about and will refuse outright in a future release |

### 10.2 Defects found and fixed

The first of these is a **production bug**, not a test bug — the test suite surfaced it the
moment contexts started loading.

| # | Defect | Consequence |
|---|---|---|
| T-1 | `@EnableJpaRepositories` listed only `com.bpdb.dms.repository`. The scan is a whitelist, not a prefix match, so **none of the 36 procurement repositories were ever registered** | The application could not start with the procurement module — `No qualifying bean of type 'ExtractedFieldRepository'`. Compilation never catches this, which is why Phases 1–6 looked green. Fixed by adding `com.bpdb.dms.procurement.repository` |
| T-2 | `application-test.yml` contained `.properties` syntax in a YAML file, committed in the initial commit | Every Spring context in the suite failed to parse config. Merged its unique settings into `application-test.properties` and deleted it, so the test profile has one source of truth |
| T-3 | `@EnableJpaAuditing` and `@EnableJpaRepositories` sat on `DmsApplication` | Applied to every test slice, so `@WebMvcTest` died with *"JPA metamodel must not be empty"*. Moved to `JpaAuditingConfig` and `PersistenceConfig`, which web slices filter out |
| T-4 | 8 tests used `@AutoConfigureWebMvc`, which does not provide `MockMvc` | `No qualifying bean of type 'MockMvc'`. Corrected to `@AutoConfigureMockMvc` |
| T-5 | `DocumentControllerTest` mocked 4 of the controller's 13 collaborators | Context failure. Added the missing ones plus `SecurityConfig`, without which the 403 assertion was silently meaningless |
| T-6 | Integration tests saved users with no role; `User.role` is not nullable | `NULL not allowed for column "ROLE_ID"`. Added `TestRoles`, a shared idempotent fixture — Liquibase seeds roles in production, but tests run on H2 with no changelog |
| T-7 | Tests granted roles, but security gates GETs on `PERM_*` authorities | Blanket 403s. `@WithMockUser` now carries both role and permissions, matching how production derives authorities from roles |
| T-8 | OCR tests hard-coded one developer's Mac (`/Users/til/Downloads/test.png`, `/opt/homebrew`) | Failed on every other machine. Now **skip** when Tesseract or the sample image is absent, with paths overridable via `-Docr.test.image` / `-Docr.test.binary` |
| T-9 | Two OCR tests asserted `extractText` throws on bad input | It never has — it catches `TesseractException` and `Throwable` and returns `success = false`, deliberate graceful degradation so one bad scan does not abort a batch. Tests now pin the real contract |

### 10.3 The 14 remaining failures — all resolved

Every one turned out to be either a stale test or a real defect. None was a false alarm.

| # | Area | Verdict |
|---|---|---|
| T-10 | `FileUploadServiceTest` (2) | **Stale test.** Phase 2 made `BILL` and `CONTRACT_AGREEMENT` follow-up documents that must land in a folder already carrying a tender workflow; the tests passed `folderId = null` and were correctly rejected. They now supply a folder and mock `TenderWorkflowService`. A second assertion matched the uploaded `MultipartFile` by identity, but the service re-reads the saved file (a stream can only be read once) — now compared by content |
| T-11 | `AuthControllerTest` — bad credentials returned 400 (1) | **Real defect, fixed in the API.** Failed authentication is 401, not 400; a caller could not tell "wrong password" from "malformed request". `AuthController` now returns 401 for `AuthenticationException` and 500 for anything else, instead of reporting a database outage as a credential problem. The frontend interceptor redirects to `/login` on *any* 401, which would have bounced the user off the login page and swallowed the error — it now exempts the sign-in calls |
| T-12 | `AuthControllerTest` — response shape (2) | **Stale test.** `authService.ts` types login as flat `{token, username, role, department}` and register as `Promise<string>`. The tests asserted `$.user.username` and `$.message`, neither of which the API has ever produced. Registration also needs a role, which the request never set |
| T-13 | `DocumentArchive`, `StationeryTracking` (4) | **Stale test.** These endpoints carry `@PreAuthorize` for `PERM_DOCUMENT_ARCHIVE` and `PERM_DOCUMENT_EDIT`; the mock users held neither |
| T-14 | `DocumentIntegrationTest` (2) | **Stale test.** Deletion moved to `POST /{id}/delete` (soft delete) — `DELETE /{id}` no longer exists, hence 405. The upload assertion also read `$.fileName`, which is the generated storage name; the uploaded name is `$.originalName` |
| T-15 | `DocumentRelationships`, `DuplicateDetection` (2) | **Stale test.** Relationship creation answers with an envelope `{success, message, relationship}`, so the type sits one level down. And `isDuplicate()` is exposed by Jackson as `duplicate` — there is no `isDuplicate` key |
| T-16 | `DuplicateDetection` — version upload (1) | **Two real defects.** `DocumentVersioningService.saveVersionFile` hard-coded `user.dir + "/uploads/versions"`, ignoring `app.upload.dir` entirely, so tests wrote version files into the working tree and collided with leftovers on the next run — the suite was order-dependent and only failed when classes ran together. It also called `Files.copy` without `REPLACE_EXISTING`, so a name collision (document id + version + filename) threw `FileAlreadyExistsException` and failed the upload over a stale file. Both fixed; the error log now carries the document, version and cause rather than a bare filename |

### 10.4 Result

`mvn clean test` on JDK 25: **72 tests, 0 failures, 0 errors, 3 skipped** — green on consecutive runs, and no longer writing into the working tree. The 3 skips are the Tesseract-dependent OCR tests, which report why they skipped.

Two of the fixes above changed production behaviour and are worth calling out at review: the login status code (400 → 401, with the matching frontend interceptor change) and the version-file storage path. The rest were tests catching up with code that had moved on.


---

## 11. Implementation status against this plan

Audited 2026-08-14 by checking the code rather than the notes. Three items were recorded
as further along than they were; the table below is what is actually true.

### 11.1 Done and verified

- **Changesets `038`–`040`** — applied, rolled back and re-applied against a clean
  PostgreSQL 16; schema and seed data verified by query (§9.5)
- **D-1 re-tender**, **D-2 department budget**, **D-3 bid security removal**,
  **D-5 OCE manual**, **D-7 over-billing**, **D-8 LC applicability**, **D-11 English OCR**,
  **D-12 single currency**, and the **Q-1 one-package constraint** — backend complete
- **D-9 Maker / Checker** — roles seeded *and* enforced at the route level, with tests
- **D-14 APP importer** — parses the supplied workbook and creates packages (§12), reachable from the UI (§13)
- **§9.6 frontend** — all six items, plus manual field entry and role-aware actions (§13)
- **Test suite** — 97 tests, 0 failures, 0 errors, 3 skipped (§10, §12)

### 11.2 Seeded but not yet wired

Data exists; no code reads it. These look complete from the database side, which is
exactly why they are called out separately.

| Item | Gap |
|---|---|
| **D-6** master lists | `procurement_master_list` holds the nine values, but nothing validates against it. REQ-2.4 ("unmatched OCR values are flagged for manual selection") is not implemented |
| **D-13** expiry intervals | `expiry_policy` holds 90/60/30/15/7 per document type, but `ProcurementExpiryService` and the notification scheduler still use their own hard-coded defaults |

### 11.3 Not started

| Item | Note |
|---|---|
| **D-10** workflow-engine retirement | Documented in §2.4 only — deliberately deferred to Phase 8, after pilot sign-off |
| **Phase 7** | Migration and backfill |
| **Phase 8** | Integration tests over the 16-stage path, performance, security review, UAT, rollout |

### 11.4 One migration task this created

`SecurityConfig` now gates `/api/procurement/**` on `MAKER` / `CHECKER` / `ADMIN`. Existing
accounts hold the legacy `OFFICER` and `DD1`–`DD4` roles and will be refused the
procurement module until they are mapped across. That mapping belongs in Phase 7 alongside
the other backfills, and must happen before the pilot regardless of what the UI does.

---

## 12. Stage 1 — APP import (D-14)

Built against `requirements/APP 22-23 First Revision_2980.xls`, the workbook supplied in
answer to Q-7. This is the front door of the lifecycle: nothing reaches Stages 2–16 that
did not start here.

### 12.1 What the workbook actually looks like

Worth recording, because the layout drove the design and none of it was guessable:

- **Two sheets**, `Local (1st Revision2)` and `Local (1st Revision)`, holding different
  packages (GRL-18/19 and GRL-24…28) — complementary, not duplicates. Seven in total.
- **Not a flat table.** Each package spans two or three physical rows: the data row plus
  a "Planned Days" row, and sometimes an "Actual Dates" row, with the package columns
  left blank on the continuation rows.
- **Rows that look like data but are not**: a `(1) (2) (3)` column-numbering row, a
  "Grand Total for APP FY-2022-…" line, and a block of signature captions. Every one of
  them carries text in the Package No. column.
- **Two header rows.** `Estd. Cost (Tk. in Lac)` is a group heading spanning two
  sub-columns, split into `Unit Cost` and `Total Cost` on the row beneath.
- **Total Cost is usually a formula** (`=SUM(F8*J8)`), occasionally a literal.
- **Lot No. is `-` throughout** — none of these lines is split into lots.

### 12.2 Design

| Piece | Responsibility |
|---|---|
| `AppColumnProfile` | Where each field lives, expressed as **header-label aliases** rather than column offsets. Inserting or reordering a column needs no change at all; different wording needs an alias. Keyed by fiscal year with a default, so a future year that renames its headings is described rather than coded |
| `AppWorkbookParser` | Spreadsheet knowledge only. Finds the header, resolves columns in two passes, and reads rows. Knows nothing about packages or the database |
| `AppPackageImportService` | Turns rows into packages: duplicate refusal (REQ-1.3), lot handling (REQ-1.5), the APP value (REQ-1.4), and a report |
| `POST /api/procurement/packages/import-app` | Multipart upload, `department` and `dryRun` parameters |

A row counts as real only when it carries **both** a Status and a Package No., which is
what excludes the numbering row, the Grand Total and the signature block in one rule.

**Lots.** An unsplit line keeps its own Package No. A line tendered in lots produces one
package per lot, stored as `PKG-77-1`, `PKG-77-2`, with the lot also kept in `lot_number`
so the original APP line stays legible. Package Number is the unique key the whole
lifecycle hangs off, so each lot must have its own.

**Dry run.** `dryRun=true` produces an identical report and writes nothing. Worth running
first on any file nobody has imported before.

**Failure handling.** One unusable row is reported and skipped, not fatal — losing 99 good
rows to one bad one would be the wrong trade. Each outcome names the sheet and row so it
can be found in the spreadsheet.

### 12.3 A bug the sample data was hiding

The first implementation matched `Estd. Cost (Tk. in Lac)` for Total Cost. That heading
sits in the **Unit Cost** column, so the importer read the per-unit price as the package's
APP value.

Every row in the supplied workbook has quantity 1, so unit cost and total cost are equal
and the tests against the real file passed. It surfaced only against a synthetic row with
quantity 4 — where the imported value would have been a quarter of the truth, and would
have flowed straight into budget consumption and the over-billing ceiling at Stage 13.

Fixed by resolving columns in two passes: strict headings first, so a group heading cannot
claim one of its sub-columns; looser aliases only for fields still unresolved, which still
supports a sheet whose cost column is undivided. Both `AppWorkbookParserTest` and
`AppPackageImportServiceTest` now pin it, the latter with a row where the two figures
differ.

**The lesson worth keeping:** a sample file with quantity 1 on every row cannot validate a
quantity calculation. Where real data is uniform, synthetic cases have to cover the
variation — and the client's file should not be the only fixture.

### 12.4 Verified

- `AppWorkbookParserTest` — 7 tests against the real workbook: all 7 packages found and
  nothing else, fiscal year read from the heading (including the `(1st Revision)` suffix),
  formula-backed costs resolved, `-` treated as no lot
- `AppPackageImportServiceTest` — 9 tests: real-workbook import, all 16 stages opened per
  package, dry run writes nothing, re-import leaves existing packages untouched, lot split,
  in-file duplicates, unusable rows, derived totals, and total-vs-unit cost
- Full suite: **97 tests, 0 failures, 0 errors, 3 skipped**

### 12.5 Not done

- **Column profiles are code, not data.** They are structured as data and isolated in one
  class, but adding a fiscal year still means editing `AppColumnProfile`. Full REQ-1.1
  compliance means moving them to a table with an admin screen — small, and best done when
  a second layout actually appears rather than guessed at now.
- **`app_line_id` is left null.** The importer creates packages directly and does not write
  `app_headers`/`app_lines`; per Q-6 the finance module stays independent. If the APP line
  provenance link is wanted, it needs deciding which of the two APP representations owns it.
- **No frontend.** `PackageList.tsx:188` still advertises "import an APP to create them in
  bulk" with nothing behind it. The endpoint now exists; wiring the button is part of §9.6.

---

## 13. Frontend (§9.6)

The reconciliation work of §9 was all backend, which meant none of it was reachable. This
closes that: re-tender, the departmental budget, the OCE field, the ICT toggle and the APP
importer are now things a user can actually do.

### 13.1 The six items

| Item | Delivered |
|---|---|
| **Stage 2 — re-tender** | `TenderAttempts.tsx`: the attempt history with superseded rows dimmed and their failure reason shown, plus the re-tender action. The dialog spells out what is preserved (package number, APP linkage, budget) and what reopens (Stages 2–7), because "re-tender" on its own does not tell the user whether they are about to lose the bidders they captured |
| **Stage 4 — OCE** | Delivered as part of `ManualFieldForm.tsx` — see §13.2, which turned out to be a bigger gap than the item described |
| **Stage 9 — ICT toggle** | `StagePanel` reads `readiness.applicabilitySuggested` and explains the derivation in words: whether the Tender Notice says ICT, what that normally implies, and — when the user has gone the other way — that the choice is theirs and is recorded. Guidance, not a lock (REQ-9.6) |
| **Stage 13/14 — override removed** | "Complete with override" no longer appears on the money stages. Over-billing is refused when the invoice is saved (Q-12), so a stage-level override there would promise something the server refuses. A note explains where the real limit lives rather than leaving a silently missing button |
| **Budget panel** | The department's annual position — allocated, committed, remaining — above the package figures, with an explicit "no annual budget on file" state. Over-commitment is shown as a planning warning, matching REQ-B0 where it is reported rather than blocked |
| **APP import** | `AppImportDialog.tsx` on `PackageList`, replacing the empty-state text that advertised an importer which did not exist. Leads with **Preview without saving** (the `dryRun` flag), and reports created / skipped / failed / warnings as tables naming the sheet and row |

### 13.2 A bigger gap behind the OCE item

The OCE field could not simply be added, because **no manual field could be entered at
all**. `StagePanel` rendered only `detail.fields` — values OCR had already produced — and
never touched `detail.catalogue`. `saveStageFields` existed in the API client and was
called from nowhere.

Stages 5, 8 and 9 are manual end to end, and the OCE at Stage 4 is typed in at BER upload
because it exists nowhere else. **17 catalogue fields across four stages had no way in**,
which means those stages could never have been completed through the UI.

`ManualFieldForm.tsx` fills that: inputs for every manual catalogue field, pre-filled from
what is captured so it doubles as a correction form, mandatory-but-empty fields sorted to
the top and marked as blocking. Worth noting that the stage gate had been correct all
along — readiness listed "OCE Value (manual entry) (not captured)" as an outstanding item.
The gate was right; there was simply no way to satisfy it.

### 13.3 Role-aware actions

`useProcurementRole` centralises the Maker/Checker split (Q-17) so the UI stops offering
actions the server will refuse. A Maker sees every stage and can capture and correct
freely, but the approval buttons — complete, not applicable, rework, re-tender, budget
entry — are a Checker's, and are replaced by a line saying so.

This matters more than it looks. Before it, a Maker would click Complete and get a bare
403; a permission boundary and a broken feature are indistinguishable from the user's
side.

### 13.4 Verified

`npm run build` compiles and emits a bundle, with **no warnings from any of the new or
modified files**. (The build reports pre-existing unused-import warnings in unrelated
components, and `src/pages/__tests__/Login.test.tsx` has been broken since the initial
commit — wrong relative paths and a missing `@testing-library/react`. Neither is touched
here; the stale test file is worth deleting or fixing separately.)

### 13.5 Not done

- **No frontend tests.** There is no component-test setup in this project — the one test
  file present does not compile — so this is verified by build and inspection only. Adding
  React Testing Library is a small piece of groundwork worth doing before the UI grows further.
- **Department budget cannot be set from the UI.** `saveDepartmentBudget` is in the API
  client and the panel says a Checker can set one, but the form is not built. The endpoint
  works; this is a small follow-up.
- **The FX rate field** named in §9.6 does not exist in the UI — the invoice and payment
  forms only ever had a currency field, which is correct and stays. Nothing to remove.

---

## 14. End-to-end run against real data (2026-08-14)

The first time the lifecycle has been executed rather than unit-tested: backend started
against PostgreSQL, the supplied APP imported through the API, and a real package walked
through the stages.

Everything below was found by running. None of it was visible to the 97 backend tests or
the frontend build.

### 14.1 What worked

- **APP import.** Dry run and real import both clean: 7 packages across 2 sheets, correct
  fiscal year, correct values, each outcome naming its source row
- **Stage gates.** Correctly refused an incomplete stage with a 409 and the reasons listed
- **Manual capture** (once 14.2 was fixed), document upload, and stage completion
- **LC applicability (Q-5).** ICT → suggested applicable; NCT → suggested not applicable.
  Verified in both directions on two packages
- **Re-tender (Q-2).** Attempt 1 superseded with its failure reason and Procurement Type
  retained, attempt 2 opened, Stages 2–7 reopened, Stage 1 left completed, package number
  and APP value untouched — exactly REQ-L14

### 14.2 Fixed to get the run moving

| # | Defect | Impact |
|---|---|---|
| E-1 | `mvn spring-boot:run` failed: *"Unable to find a single main class"*. `com.bpdb.dms.util.TestProcurementExtraction` carries a second `main` in production source | **The application could not be started at all** by the documented command. Fixed by naming `mainClass` in the pom; the scratch class arguably does not belong in `src/main` |
| E-2 | `extracted_field_history`, `budget_consumption` and `ocr_page` had no `created_at`, though every procurement entity maps it. Changeset 037 added the missing `updated_at` everywhere but `created_at` to only three of six tables | **Nothing could be captured, corrected, or billed.** Every field save failed with `ERROR: column "created_at" does not exist`; budget consumption and OCR page writes would have failed the same way. Fixed by changeset `041`. **The tests could not catch this**: they run on H2 with `ddl-auto=create-drop`, which builds the schema from the entities, so the column always exists there. Only the Liquibase-built schema diverges |

### 14.3 Found and left for a decision *(E-3 and E-4 since fixed — see §15)*

**E-3 — OCR overwrites human-verified values. Most serious.**

After four Stage 1 fields were entered and verified, uploading the APP document ran
extraction, which found nothing (no Tesseract, placeholder PDF) and **overwrote all four**:
`text_value` and `numeric_value` nulled, `capture_source` flipped MANUAL → OCR, `status`
reset VERIFIED → OCR_SUGGESTED, `validation_state` set NOT_FOUND.

`CaptureService.captureFromOcr` fetches the existing row and applies the new reading
unconditionally. `raw_value` is protected — REQ-P5 holds — but the working value and the
verification state are not. Nothing guards "a person already confirmed this".

The consequence is that any document upload or re-OCR after verification destroys
confirmed data, which undermines the model the design rests on: the verify screen is where
OCR becomes trustworthy, and an empty OCR pass currently beats a human.

Worth deciding rather than patching blind — the sensible rule is probably that OCR may not
downgrade a `VERIFIED` or `MANUAL_OVERRIDE` field, and should instead record a competing
candidate for review. That is a design choice about provenance, not a one-line fix.

**E-4 — the APP importer writes no provenance rows.**

Imported packages carry `package_number`, description, authority and value on the
`procurement_package` row, but no `extracted_field` rows. Stage 1 therefore reports all
four mandatory fields as "not captured" and cannot be completed, even though the data is
plainly there — and because the catalogue marks them `capture_source = OCR`, the manual
entry form does not offer them either. **Every imported package is stuck at Stage 1** until
someone re-types values that were imported correctly.

This is REQ-P4: the typed column and the provenance row are meant to be written in the same
transaction and never diverge. The importer writes one and not the other. The fix is for
`AppPackageImportService` to capture each value through `CaptureService` with
`capture_source = IMPORT` — which the enum already anticipates.

**E-5 — no Maker or Checker users exist.** Confirmed in practice: the database holds only
`admin` and `dd1_user`–`dd4_user`. The roles are seeded, the routes enforce them, and
nobody can log in as either. Already recorded as §11.4; the run makes it concrete.

**E-6 — `/actuator/health` reports DOWN** because Redis is not running. Cosmetic here, but
worth knowing before it is wired to a deployment probe.

### 14.4 The pattern

E-2 and E-4 are the same shape: a divergence between two representations of the same fact
that no test could see, because the tests build the schema from the entities and assert on
the typed columns. Both were invisible until real data met the real schema.

Worth taking seriously before Phase 8: **a smoke test that runs Liquibase against
PostgreSQL and drives one package end to end** would have caught E-1, E-2 and E-4 on the
first run. That is a more valuable next investment than broadening unit coverage.

### 14.5 State left behind

The dev database now holds the 7 imported BPDB packages. GRL-18 carries test artefacts
(Stage 1 completed, one superseded tender attempt); GRL-19 has a Stage 2 capture. Real data
worth keeping for the stage-model validation Phase 0 wanted, but it is not pristine.

---

## 15. Provenance fixes: E-3 and E-4 (2026-08-15)

Done together, because separately neither holds. Making the importer write provenance rows
is pointless while the first document upload wipes them; protecting values is of limited
use while imported packages have none.

### 15.1 E-3 — a re-read may not destroy what is there

Two rules in `CaptureService.captureFromOcr`:

1. **An empty read never erases an existing value.** OCR that found nothing records that
   it found nothing and leaves the value alone. A field with no value yet is still marked
   `NOT_FOUND`, so REQ-P8 is unaffected — "missing" remains a recorded fact.
2. **A machine reading never overrules a person.** Once a field is `VERIFIED` or
   `MANUAL_OVERRIDE`, OCR may disagree but may not replace: the confirmed value stands and
   the field is marked `CONFLICT`, with a message naming *both* readings. Neither is
   discarded and neither silently wins. Whitespace and casing differences are not treated
   as disagreement.

The conflict is advisory, not blocking — an OCR misread must not be able to stop a stage
whose value a person already got right. It shows in the field row in red, distinct from
the amber of a low-confidence caution.

**Also fixed in passing:** `captureManual` set `captureSource = MANUAL` *before* testing
whether the previous source was OCR, so the test could never be true and correcting an OCR
reading was recorded as a plain verification rather than an override. The audit trail was
quietly losing the distinction between "I checked this" and "I disagreed with this".

### 15.2 E-4 — the importer writes provenance

New `CaptureService.captureImported`, used by `AppPackageImportService` for the four
Stage 1 fields, in the same transaction as the package row (REQ-P4).

Imported values arrive **confirmed**, on the same reasoning as manual entry: a spreadsheet
cell is read exactly rather than guessed at, and the value is the client's own approved
plan. Asking someone to re-verify 28 deterministically parsed fields is busywork, and
busywork is how verification screens stop being read. The origin travels with the value —
each history row records `Imported from APP 'Local (1st Revision2)' row 8`.

### 15.3 Proven end to end, not just unit-tested

The same sequence that destroyed data yesterday, re-run against PostgreSQL on a cleared
database:

| Step | Before | Now |
|---|---|---|
| Import 7 packages | package rows only, no provenance | 7 packages, 4 captured fields each, `IMPORT` / `VERIFIED` |
| Stage 1 field gate after import | all four "not captured" | satisfied, nothing outstanding |
| Upload APP document (OCR finds nothing) | **all four values nulled**, status reset, package stuck | all four intact, still confirmed |
| Complete Stage 1 | impossible | HTTP 200, package advances to Stage 2 |
| Trace a value to its source | not possible | history names the sheet and row |

An imported package now goes from workbook to Stage 2 without anyone re-typing a value
that was already correct.

### 15.4 Verified

- **108 backend tests, 0 failures, 0 errors, 3 skipped** — 11 new: 8 in
  `OcrOverwriteProtectionTest` covering both rules and the override-provenance fix, 3 in
  `AppPackageImportServiceTest` covering capture, typed-column agreement, and survival of
  a later empty OCR pass
- Frontend builds clean, with `CONFLICT` surfaced in `FieldRow`
- End-to-end re-run as above

### 15.5 Still open from §14

- **E-5** — no Maker or Checker users exist; legacy accounts cannot reach the module
- **E-6** — `/actuator/health` reports DOWN because Redis is absent
- **The smoke test** — still the highest-value next investment. It would have caught E-1,
  E-2 and E-4 on their first run, and it is what would stop the next one of these reaching
  a user rather than a developer

---

## 16. The schema check, the smoke test, and E-5 (2026-08-16)

### 16.1 Why these two tests exist

Every serious defect this project has produced has been the same shape: two
representations of one fact that disagreed, where nothing in the build could see the
disagreement. The suite runs on H2 with the schema generated from the entities, so the
entities and the schema agree *by construction* — the one place drift can occur is the one
place the tests could not look.

Both new tests run against a throwaway PostgreSQL container with the **real changelog**
applied (`PostgresLiquibaseTest`, using Testcontainers).

### 16.2 `LiquibaseSchemaAgreementTest` — the class of defect, not the instance

Walks the Hibernate metamodel and compares every mapped column against
`information_schema`. Three assertions:

| Assertion | Job |
|---|---|
| `theProcurementSchemaAgreesWithItsEntities` | Hard gate on the module under development. Clean today — this is the check that would have caught the missing `created_at` columns that made every field capture in the application fail |
| `noTableOutsideTheKnownLegacyListHasDrifted` | A ratchet. Pre-existing drift elsewhere is quarantined in a named list; anything *new* fails |
| `theKnownLegacyDriftListDoesNotOutliveTheProblem` | Stops the quarantine list rotting — once a table is fixed it must leave the list, so it cannot silently re-absorb a regression |

**It immediately found more than it was written for.** Beyond the procurement columns
already fixed, the wider application has substantial drift:

- **Seven entities have no table at all** — `webhooks`, `document_templates`,
  `document_comments`, `smart_folders`, `system_metrics`, `optimization_tasks`,
  `ml_predictions`. These features cannot function against a real database; the first
  query throws.
- **Eight tables are missing mapped columns** — `tenants`, `backup_records`, `reports`,
  `dashboards`, `integration_configs`, `system_health_checks`, `ml_models`, and
  **`document_versions`**.
- Hibernate's own `validate` additionally reports a type mismatch on
  `analytics.metric_value` (NUMERIC where the entity expects a float).

`document_versions` is the one to look at first: unlike the enterprise features around it,
it is on a path users reach — it backs the upload-duplicate-as-version flow. The rest is
either dead code or an unbuilt feature, and the entities should probably be deleted rather
than the tables written.

None of this is in scope here; it is quarantined so the ratchet works, and listed so the
size of it is known.

### 16.3 `LifecycleSmokeTest` — the run, automated

The hand-driven run from §14, now a build step: import the real workbook, check the Stage 1
gate is satisfied by the import alone, prove an empty OCR pass does not undo it, confirm
the gate still refuses without documents, complete Stage 1, derive LC applicability from an
ICT tender, re-tender and check the failed attempt survives, and confirm the APP value is
unchanged at the end.

Deliberately **not** transactional and deliberately ordered, so each step lands for real
and the next has to cope with what it left — rolling back between methods would hide the
problems it exists to find. It caught one immediately: `document_link` has a real foreign
key to `documents`, so fabricated document ids are rejected. The test now inserts real rows.

### 16.4 E-5 — legacy accounts can reach the module again

The obstacle was structural: `users.role_id` holds **one** role, so making an existing DD1
account a MAKER would have cost it the role granting its document access.

So the gating moved from role names to permissions. `SecurityConfig` now checks
`PERM_PROCUREMENT_*` and `PERM_BUDGET_APPROVE` rather than `hasAnyRole("MAKER", …)`, and
changeset `042` grants the two capture permissions to `OFFICER` and `DD1`–`DD4`. Both
things can now be true of one account.

**Nobody is promoted by migration.** The legacy roles get view and capture only; approval —
completing a stage, marking it Not Applicable, rework, re-tender, budget — is granted to
`CHECKER` and `ADMIN` alone. Which people should approve is BPDB's decision, and a
changeset that quietly handed four levels of Deputy Director the right to sign off a stage
would be making it for them. Two tests pin exactly that: a migrated DD1 can open the
module and cannot complete a stage.

Applied and verified against the live database.

### 16.5 Verified

- **121 backend tests, 0 failures, 0 errors, 3 skipped** (from 108): 3 schema agreement,
  8 lifecycle smoke, 2 legacy-role authorization
- Changesets `041` and `042` applied to the live database; role permissions confirmed by query

### 16.6 Worth doing next

- **`document_versions` drift** — the only quarantined table on a live user path
- **Turn `ddl-auto=validate` on** for the PostgreSQL tests once the legacy drift is cleared;
  it would then catch type mismatches too, automatically, for every test that touches
  Postgres
- The seven table-less entities: decide feature-by-feature whether to write the changeset
  or delete the entity

---

## 17. Clearing the drift the schema check found (2026-08-16)

### 17.1 `document_versions` — a broken feature, fixed

The entity maps fifteen columns; changeset 006 created seven. The other eight were never
added, so Hibernate builds an INSERT naming columns PostgreSQL does not have and document
versioning fails at the first attempt.

Not a dormant corner — it is reachable two ways: `POST /api/documents/{id}/versions`, and
uploading a duplicate with `action=version`. Changeset **043** adds the eight columns
(`file_size`, `file_hash`, `mime_type`, `version_type`, `is_current`, `is_archived`,
`archived_at`, `metadata`). Applied to the live database; the table now has all fifteen.

**One thing deliberately not done.** A partial unique index enforcing one current version
per document would be the natural companion, and `DocumentVersioningService` does maintain
that invariant — it clears the previous current version before marking the new one. But it
does so with separate saves inside one transaction, and Hibernate's action queue runs
inserts before updates: the new row would be written while the old one still says
`is_current`, and a legitimate save would fail on the constraint. Adding it now would trade
a drift bug for a concurrency bug. Worth having once the service flushes before inserting.

### 17.2 The seven table-less entities — split by what they actually are

Reachability decided each one, not preference:

| Entity | Controller | Service | Call |
|---|---|---|---|
| `DocumentComment` | — | — | **Deleted.** Referenced nowhere but its own repository |
| `SystemMetric` | — | — | **Deleted.** Same |
| `MLPrediction` | — | — | **Deleted.** Same |
| `Webhook` | yes | yes | Left for a decision |
| `DocumentTemplate` | yes | yes | Left for a decision |
| `SmartFolderDefinition` | yes | yes | Left for a decision |
| `MLModel` | yes | yes | Left for a decision |
| `OptimizationTask` | — | yes | Left for a decision |

The three with no reference anywhere were dead code inflating the drift list; deleting the
entity and repository is unambiguously right and costs nothing.

The other five are **reachable endpoints that return a 500** — real features that cannot
work. Writing their schema is about seventy columns across five tables, for features
outside the procurement revamp, none in the pilot scope (Q-20), and none verifiable from
here. That is exactly the "pressure to integrate everything" the plan names as **R-5**, so
the call is to surface them rather than guess:

> Either these features are wanted — in which case they need changesets *and* someone who
> can confirm they work — or they are abandoned, and the entities, repositories and
> controllers should go the way of the three above. Writing tables to silence a test would
> be the worst of the three options: it converts a loud failure into a quiet one.

### 17.3 `ddl-auto=validate` — not yet, and why

Still blocked: five entities have no table and six tables are missing columns, so `validate`
would fail every PostgreSQL test on legacy debt and hide anything new. The precondition is
§17.2 being decided.

Rather than wait, the type checking `validate` would have given us is now done directly for
the module we own — `procurementColumnsHoldTheKindOfValueTheirFieldExpects` asserts that
money columns are exact rather than floating point, that `*_date` columns are date types,
and that `is_*` columns are boolean. Money is the one that matters: a contract value stored
as a float is a rounding bug waiting for a large enough number.

### 17.4 Verified

- **122 backend tests, 0 failures, 0 errors, 3 skipped**
- Four schema checks now: procurement columns, procurement types, the drift ratchet, and
  the quarantine list not outliving its problem
- Quarantine list down from 15 tables to 11
- Changeset 043 applied to the live database

### 17.5 What is left

| | |
|---|---|
| **Decide** | The five reachable table-less features (§17.2) — fix or remove |
| **Then** | `ddl-auto=validate` on for PostgreSQL tests, making type drift automatic everywhere |
| **Then** | The remaining six legacy tables with missing columns, if their features are wanted |
| **Also open** | The one-current-version index once versioning flushes in the right order (§17.1) |

---

## 18. The cross-cutting requirements (2026-08-16)

Six REQ numbers that had zero implementing code. These are the layer that makes the system
a record rather than a workflow, and their absence was the honest answer to "are the
requirements implemented".

### 18.1 REQ-X6 — the action-level audit trail

The consequential one. Field *values* had history in `extracted_field_history`; the
*decisions* had nothing but log lines. Stage completions, Not-Applicable, rework,
re-tender, budget movements — none reached `audit_logs`.

`ProcurementAuditService` is a thin layer over the DMS's existing `AuditService` — one
audit table, one writer — adding a vocabulary for procurement actions and, where the
requirement asks for it, the before and after:

> `Stage 1 completed: IN_PROGRESS -> COMPLETED. Reason: Completed with override: documents held by the ministry`

Wired into `StageEngine` (complete, not-applicable, rework), `TenderService` (re-tender),
`BudgetService` (entries, departmental budget) and `ProcurementRecordService` (invoices,
payments, delivery finality). **Refusals are recorded too** — an attempt to bill beyond the
contract value is exactly what an auditor wants to see, whether or not it succeeded.

An audit failure never takes down the operation it describes, but it logs at ERROR with
the word AUDIT GAP, because a silent hole in an audit trail is worse than a noisy one.

### 18.2 REQ-E2/E3 — expiry warnings, and a much bigger find

The intervals now come from `expiry_policy` rather than code (Q-15), each threshold fires
once, and crossing several at once raises one warning rather than five. Recipients are the
responsible officer plus whichever roles the policy names.

`expiry_tracking` carried four fixed booleans (`alert_30_days`, `alert_15_days`,
`alert_7_days`, `alert_expired`) which cannot express a configurable set — there is nowhere
to record that a 90 or 60 day warning went out. Changeset **044** adds `sent_warnings`.

**The find:** the application has **fourteen `@Scheduled` methods and `@EnableScheduling`
appears nowhere.** Not one has ever run — including the pre-existing expiry alerts, which
is why REQ-E3 was unmet even though alert code existed.

Enabling it is deliberately opt-in (`app.scheduling.enabled`, default off), because the
switch is not selective and several dormant jobs are not obviously safe to wake in an
environment that has never run them: `ReportingService.cleanupOldData` and
`NotificationService.cleanupOldNotifications` **delete records**,
`DisasterRecoveryService.scheduledBackup` starts writing backups, and `MultiTenancyService`
reads `tenants`, whose schema is known to disagree with its entity — it would throw on
every run. **Review those before switching it on anywhere that matters.** Meanwhile
`POST /api/procurement/expiries/run-warnings` runs the pass on demand.

### 18.3 REQ-2.4 — master list validation

`procurement_master_list` is now read. Matching is lenient about what OCR gets wrong —
case, spacing, the label as well as the code, and the method inside a larger phrase, so the
supplied workbook's `e-GP/ OTM` resolves to `OTM` — but a code is not matched inside an
unrelated word.

A mismatch is a **warning, not an error**, exactly as the requirement words it: "flagged for
manual selection rather than accepted silently". Blocking a stage because a tender notice
was worded differently would push people out of the system, which costs more than the
inconsistency. The message names what was read *and* what is permitted.

### 18.4 REQ-12.5 — declaring a delivery final

Was a flag anyone could set in the delivery payload. It is now its own route,
`POST /stages/12/deliveries/{id}/final`, gated on the Checker permission, with `saveDelivery`
refusing to change finality at all. Q-11 never said who decides; this follows Q-17's
maker/checker split, and there is a matching reopen for a set closed too early.

### 18.5 REQ-E7 and REQ-P17

**Export** is CSV, RFC-4180 quoted, with days-remaining computed. Chosen over PDF because
the people who ask for this export are the ones who then want to sort and filter it.

**Retention** is an explicit administrator action and never a scheduled job. It defaults to
a dry run; only *closed* packages older than the period are ever in scope; and it removes
only accumulated history — superseded OCR results and field-change history — never
documents, values, or the audit trail. The client's one-year answer (Q-18) is shorter than
the warranty period on many of these contracts, so nothing here happens on a clock.

### 18.6 Verified

**146 backend tests, 0 failures, 0 errors, 3 skipped** (from 122). New: 7 audit trail,
7 master list, 8 expiry warning.

### 18.7 Still not done

- **Stages 3–16 have never been executed.** The smoke test covers 1, 2 and 9. Every stage
  driven so far has yielded defects unit tests missed; the BER bidder table, LC amendments,
  deliveries, invoices, payments, warranty and closure remain unexercised
- The five reachable table-less features (§17.2) still need a fix-or-remove decision
- Phases 7 and 8: migration, performance, security review, UAT
