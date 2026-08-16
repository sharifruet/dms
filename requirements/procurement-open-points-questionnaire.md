# Procurement Lifecycle — Client Alignment Questionnaire

**For:** Bangladesh Power Development Board (BPDB)
**From:** DMS implementation team
**Companion to:** [procurement-lifecycle-workflow-requirements.md](procurement-lifecycle-workflow-requirements.md) · [procurement-revamp-implementation-plan.md](procurement-revamp-implementation-plan.md)
**Source:** the "Client Input / Alignment Notes" column of `BPDB_Procurement_Lifecycle_Metadata_Mapping_Matrix 4.docx`, which is blank throughout

---

## How to use this document

We have drafted the 16-stage lifecycle, the data linkage model and the persistence design from the mapping matrix. Where the matrix was silent we made an assumption, stated it explicitly, and built the design on it. **This questionnaire asks you to confirm or correct those assumptions.**

Each question gives: what we assumed, why it matters, and what it costs to change later. Answer in the **Answer** line under each question; a one-line answer is enough.

**Questions are grouped by urgency:**

| Group | Questions | When we need it |
|---|---|---|
| **A — Blocking** | Q-1 … Q-6 | Before any database work starts. These change table structure; changing them after build is expensive. |
| **B — Needed during build** | Q-7 … Q-14 | Before the stage they affect is built. Weeks of slack. |
| **C — Configuration** | Q-15 … Q-20 | Before go-live. No structural impact. |

---

## Group A — Blocking (needed before database work)

### Q-1. Can one APP line item become more than one package?

**Our assumption:** one APP line → one procurement package → one tender → one contract.

**Why it matters:** if a single APP line is tendered in lots (Lot 1, Lot 2 …), or if one contract covers several APP packages, the relationship changes from one-to-one to many-to-many and the linkage keys change at the root of the model. This is the single most structural question here.

**Please confirm one:**
- [ ] One APP line always produces exactly one package (our assumption)
- [X] One APP line may be split into multiple packages/lots
- [ ] One contract may cover several APP packages
- [ ] Both of the above

**Answer:** One APP line may be split into multiple packages/lots

---

### Q-2. What happens when a tender fails and is re-tendered?

**Our assumption:** not modelled — the design currently allows one tender per package.

**Why it matters:** re-tendering is common. We need to know whether the second tender is a new Tender record under the *same* package (preserving budget and APP linkage, with tender history) or an entirely new package. This changes the tender-to-package cardinality and the stage rework rules.

**Please confirm one:**
- [X] New Tender record under the same package, previous tender retained as history
- [ ] Entirely new package
- [ ] Other (describe)

**Answer:** New Tender record under the same package, previous tender retained as history

---

### Q-3. Is bidder information really only needed from the BER?

**Our assumption (per your instruction):** no per-bidder record is created at Tender Opening (Stage 3). Stage 3 stores only *Number of Bidders* and *Participating Bidders* as summary text. All bidder data — name, price, responsive status, deviation % — is captured at Stage 4 from the BER.

**Why it matters:** the source matrix lists *Bidder Name* and *Bidding Price* under Stage 3. Under our design those values do not exist in the system between bid opening and BER upload — a gap that can be days or weeks. If anyone needs to query bidders during that window, we must add bidder records back at Stage 3.

**Please confirm one:**
- [X] Correct — bidder data is only needed once the BER exists (our current design)
- [ ] We need bidder names and prices visible from Tender Opening, before the BER

**Answer:**  Correct — bidder data is only needed once the BER exists (our current design)

---

### Q-4. Which bidders' Bid Security do you track, and who records its release?

**Our assumption:** Bid Security is uploaded against the Tender Opening purely so its expiry can be tracked (it appears in your Expiry Tracking Matrix but in no stage's document list). No bidder-level financial record is created.

**Why it matters:** if you need to track Bid Security *per bidder* — who submitted what, whose is still held, whose has been returned — we need a bidder-linked record, which conflicts with the Q-3 decision to hold no bidder data at Stage 3.

**Please answer:**
- Bid Security tracked for: [ ] all bidders  [ ] responsive bidders only  [ ] awarded bidder only
- Who records its release, and at which stage?

**Answer:** Bid Security stuffs are out of scope of this application

---

### Q-5. Which contracts require a Letter of Credit?

**Our assumption:** Stage 9 (LC) is applicable to import/LC-based contracts only. For other contracts a user marks the stage *Not Applicable* with a reason and the workflow proceeds to Stage 10.

**Why it matters:** determines whether Stage 9 is a hard gate or an optional branch, and whether "Not Applicable" needs an approval step or is a routine user action.

**Please answer:**
- Which contract categories require an LC?
- Does marking a stage Not Applicable need approval, or can any authorised user do it?

**Answer:** ICT - International Compatible Tender may require LC. We can get this from Tender Notice document Procurement Type field contains the value such as ICT.
 - Any authorised user do it

---

### Q-6. Is billing staying in the existing finance module?

**Our assumption:** the existing `bill_headers` / `bill_lines` tables remain the system of record for billing, and the new `invoice` record bridges to them rather than replacing them.

**Why it matters:** determines whether Stage 13 writes to new tables, existing tables, or both — and whether existing bill data must be migrated. Getting this wrong means either duplicated invoice data or a broken finance module.

**Please confirm one:**
- [ ] Keep the existing finance module; procurement invoices link to it (our assumption)
- [ ] Replace it — procurement becomes the system of record for billing
- [X] Keep both independently (accepting duplicate entry)

**Answer:** Keep both independently (accepting duplicate entry)

---

## Group B — Needed during build

### Q-7. APP Excel format (Stage 1)

We need a sample APP Excel file and confirmation of its column layout, so package rows can be parsed reliably. Does the layout change between fiscal years?

**Answer:** APP 22-23 First Revision_2980.xls

---

### Q-8. Master lists (Stage 2)

Please supply the permitted values for **Procurement Type**, **Procurement Method** and **Procurement Nature**. Values not on the list will be flagged for manual selection rather than accepted silently.

**Answer:** Procurement Type: NCT|ICT, Procurement Method: OTM|LTM|RFQ|DPM, Procurement Nature: Service|Works|Goods

---

### Q-9. OCE value source (Stage 4)

*Deviation (%) with OCE* is extracted from the BER. Where does the OCE (Officially Certified Estimate) figure itself come from — is it in the BER, held elsewhere, or entered manually?

**Answer:** Manual Input during BER upload

---

### Q-10. Inspection events (Stage 11)

Which inspections apply to which contract categories, and when is a SAT Report required? We currently support multiple inspection events per contract, each typed PDI or PLI.

**Answer:** Keep as it is now

---

### Q-11. Partial delivery (Stage 12)

**Our assumption:** partial deliveries are permitted, with cumulative delivered quantity tracked against the e-GP price schedule.

Is partial delivery allowed for all contract categories, or only some? Who declares a delivery final?

**Answer:** For all contract categories. 

---

### Q-12. Over-billing and over-payment tolerance (Stages 13–14)

**Our assumption:** cumulative invoice amount is validated against contract value, and cumulative payment against the linked invoice; exceeding either is flagged and requires an authorised override.

**Please answer:**
- Is any tolerance permitted (e.g. price variation clauses, taxes pushing the total above contract value)?
- Which role may override?

**Answer:** Over-billing is not permitted for now

---

### Q-13. Budget hierarchy (Section 5)

**Our assumption:** budget is held per package, with Remaining Budget derived from allocation ± revisions + additional − consumption.

**Please answer:**
- Is budget allocated annually at department level and drawn down per package, or allocated per package directly?
- Who approves a Revised Budget, and does it need a workflow?

**Answer:**  budget allocated annually at department level. Who approves a Revised Budget is not significant here. Anyone having aprover role or approver permission can approve. 

---

### Q-14. Multi-currency (Stages 8, 9, 13, 14)

Do contracts, LCs and invoices ever mix currencies within one package? If so, at what exchange rate do we compute budget consumption — rate on invoice date, rate on payment date, or a fixed contract rate? What is the rate source?

**Answer:** NO, one package will not have multiple currency

---

## Group C — Configuration (needed before go-live)

### Q-15. Expiry warning intervals and recipients (Section 6)

For each tracked document, confirm the advance-warning intervals and who receives the alert. Our default is 90 / 60 / 30 / 15 / 7 days to the responsible officer.

| Document | Expiry field | Warning intervals | Notify whom |
|---|---|---|---|
| Tender Notice | Tender Validity | | |
| Bid Security | Bid Security Expiry | | |
| Performance Guarantee | PG Expiry Date | | |
| Letter of Credit | LC Expiry Date | | |
| Contract Agreement | Completion Date | | |
| Warranty Certificate | Warranty End Date | | |

Ans: Use some defaults and keep configurable.
---

### Q-16. Approval workflow engine

The DMS already has a general-purpose workflow engine. The procurement lifecycle needs its own stage engine (gates based on documents and verified fields), which that engine was not built for.

**Our recommendation:** the two coexist — the stage engine owns the 16 stages; the existing workflow engine is reserved for ad-hoc approval routing.

Do you currently use the existing workflow engine for anything that must keep working?

**Answer:** No current / existing workflow engine will no longer needed. You can revamp.

---

### Q-17. Roles and stage permissions

Who may: enter a stage, complete a stage, override a validation, send a stage back for rework, and reopen a closed contract? Please map these to your existing DD1–DD4 roles or supply the intended roles.

**Answer:** Create 2 roles maker and checker. Maker will enter and checker will approve everything. 

---

### Q-18. Retention period

How long must documents, OCR results and field-change history be retained? Closure archives but never deletes under our design.

**Answer:** 1 year

---

### Q-19. Document language

Are procurement documents in English, Bangla, or mixed? This materially affects OCR accuracy and which engine configuration we use — it is the single biggest driver of how much manual correction your staff will face.

**Answer:** English only

---

### Q-20. Pilot department

Which department should pilot the system, and roughly how many live packages would be in scope for the pilot?

**Answer:** Keep only one department for now BPDB

---

## What we need back, and when

| | |
|---|---|
| **Blocking now** | Q-1 … Q-6. Database work cannot start without these. |
| **Also valuable now** | A sample APP Excel (Q-7) and 5–10 representative scanned documents per type — tender notice, BER, NOA, contract, LC, invoice — so we can measure OCR accuracy before committing to how much extraction is automated. |
| **Also valuable now** | Access to 3–5 completed real packages end to end, to validate that our stage gates match how BPDB actually works. |

Returned to: **aidev2@i2gether.com**

---

## Assumptions we are proceeding on if we hear nothing

If a Group B or C question is unanswered by the time its phase starts, we will build to the assumption stated in the question and flag it in the release notes. **Group A questions will not be assumed** — we will hold rather than guess, because unwinding those choices after the schema is built is expensive.
