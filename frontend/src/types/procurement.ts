// Procurement lifecycle types.
// Mirrors com.bpdb.dms.procurement on the backend - see
// requirements/procurement-lifecycle-workflow-requirements.md

export type StageStatus =
  | 'NOT_STARTED'
  | 'IN_PROGRESS'
  | 'PENDING_VERIFICATION'
  | 'COMPLETED'
  | 'REWORK';

export type CaptureSource = 'OCR' | 'MANUAL' | 'DERIVED' | 'IMPORT';

export type FieldStatus = 'OCR_SUGGESTED' | 'VERIFIED' | 'MANUAL_OVERRIDE' | 'REJECTED';

/**
 * CONFLICT: a later OCR pass read something different from the value a person confirmed.
 * The confirmed value is kept; the disagreement is flagged so someone can check the
 * document rather than either reading silently winning.
 */
export type ValidationState = 'VALID' | 'NEEDS_REVIEW' | 'NOT_FOUND' | 'INVALID' | 'CONFLICT';

export interface ProcurementPackage {
  id: number;
  appLineId?: number;
  packageNumber: string;
  lotNumber?: string;
  lotDescription?: string;
  packageDescription?: string;
  approvingAuthority?: string;
  priceLacBdt?: number;
  fiscalYear?: number;
  department?: string;
  currentStage: number;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PackageStage {
  id: number;
  packageId: number;
  stageCode: number;
  status: StageStatus;
  isApplicable: boolean;
  notApplicableReason?: string;
  enteredAt?: string;
  completedAt?: string;
  completedBy?: number;
  reworkReason?: string;
}

/** Why a stage cannot be completed yet - drives the disabled Complete button. */
export interface StageReadiness {
  packageId: number;
  stageCode: number;
  stageName: string;
  status: StageStatus;
  applicable: boolean;
  /**
   * What the system derives this stage's applicability should be (Q-5, REQ-2.5).
   * Stage 9 uses it to pre-set the LC toggle from the tender's Procurement Type: ICT
   * suggests an LC is needed. It is a suggestion, not a lock.
   */
  applicabilitySuggested?: boolean;
  ready: boolean;
  blockers: string[];
  missingDocuments: string[];
  unconfirmedFields: string[];
  validationErrors: string[];
}

/**
 * One tender attempt. A failed tender is re-tendered under the same package rather than
 * being edited or replaced, so a package may hold several (Q-2, REQ-L14).
 */
export interface Tender {
  id: number;
  packageId: number;
  attemptNo: number;
  isCurrent: boolean;
  failureReason?: string;
  procurementType?: string;
  procurementMethod?: string;
  procurementNature?: string;
  openingDate?: string;
  closingDate?: string;
  tenderValidityDays?: number;
  tenderValidityDate?: string;
}

/** A department's annual budget, which package allocations draw down (Q-13, REQ-B0). */
export interface DepartmentBudget {
  id?: number;
  fiscalYear: number;
  department: string;
  allocatedAmount: number;
  currency?: string;
  notes?: string;
  approvedBy?: number;
  approvedAt?: string;
}

/** How much of a department's annual budget its packages have committed. */
export interface DepartmentBudgetPosition {
  fiscalYear?: number;
  department?: string;
  departmentBudgetId?: number;
  currency: string;
  allocated: number;
  committed: number;
  remaining: number;
  overCommitted: boolean;
}

/** One row's fate during an APP import, with enough context to find it in the sheet. */
export interface AppImportOutcome {
  origin: string;
  packageNumber?: string;
  reason: string;
}

/** What an APP import did (REQ-1.1). A dry run reports identically but writes nothing. */
export interface AppImportReport {
  dryRun: boolean;
  fiscalYear?: number;
  profileName?: string;
  rowsRead: number;
  skippedSheets: string[];
  created: AppImportOutcome[];
  skipped: AppImportOutcome[];
  failed: AppImportOutcome[];
  warnings: AppImportOutcome[];
  createdCount: number;
  skippedCount: number;
  failedCount: number;
  clean: boolean;
}

export interface ExtractedField {
  id: number;
  entityType: string;
  entityId?: number;
  packageId: number;
  stageCode: number;
  fieldKey: string;
  fieldLabel?: string;
  dataType: string;
  /** Exactly what the document said. Never changes (REQ-P5). */
  rawValue?: string;
  textValue?: string;
  numericValue?: number;
  dateValue?: string;
  boolValue?: boolean;
  currency?: string;
  unit?: string;
  captureSource: CaptureSource;
  documentId?: number;
  ocrResultId?: number;
  pageNo?: number;
  bbox?: string;
  ocrConfidence?: number;
  status: FieldStatus;
  verifiedBy?: number;
  verifiedAt?: string;
  isMandatory?: boolean;
  validationState?: ValidationState;
  validationMessage?: string;
}

export interface ExtractedFieldHistory {
  id: number;
  extractedFieldId: number;
  version: number;
  oldValue?: string;
  newValue?: string;
  oldStatus?: string;
  newStatus?: string;
  changedBy?: number;
  changedAt?: string;
  changeReason?: string;
}

export interface CatalogueField {
  id: number;
  documentType: string;
  fieldKey: string;
  fieldLabel: string;
  fieldType: string;
  isOcrMappable?: boolean;
  displayOrder?: number;
  stageCode?: number;
  entityType?: string;
  entityColumn?: string;
  captureSource?: CaptureSource;
  isMandatory?: boolean;
}

export interface StageDocumentRequirement {
  id: number;
  stageCode: number;
  docRole: string;
  docLabel: string;
  isMandatory: boolean;
  isConditional: boolean;
  displayOrder: number;
}

export interface DocumentLink {
  id: number;
  documentId: number;
  entityType: string;
  entityId?: number;
  packageId: number;
  contractId?: number;
  stageCode: number;
  docRole: string;
  linkOrigin: string;
  createdAt?: string;
}

export interface BerBidder {
  id?: number;
  evaluationId?: number;
  bidderName: string;
  biddingPrice?: number;
  currency?: string;
  isResponsive?: boolean;
  deviationPct?: number;
  bidRank?: number;
  isAwarded?: boolean;
  remarks?: string;
}

export interface Delivery {
  id?: number;
  contractId?: number;
  inspectionEventId?: number;
  deliveryReferenceNumber?: string;
  deliveryDate?: string;
  deliveredQuantity?: number;
  isFinal?: boolean;
}

export interface Invoice {
  id?: number;
  contractId?: number;
  invoiceNumber: string;
  invoiceDate?: string;
  invoiceAmount?: number;
  currency?: string;
  supplierName?: string;
}

export interface Payment {
  id?: number;
  contractId?: number;
  voucherNumber?: string;
  paymentDate?: string;
  paymentAmount?: number;
  currency?: string;
  bankAdviceRef?: string;
}

export interface InspectionEvent {
  id?: number;
  contractId?: number;
  inspectionType?: string;
  inspectionDate?: string;
  location?: string;
  fatDone?: boolean;
  satApplicable?: boolean;
  satDone?: boolean;
  result?: string;
}

export interface BudgetSummary {
  packageId: number;
  totalAllocation: number;
  totalRelease: number;
  totalRevision: number;
  totalAdditional: number;
  totalConsumption: number;
  totalAvailable: number;
  remaining: number;
  lowBudget: boolean;
  overspent: boolean;
}

export interface BudgetEntry {
  id?: number;
  packageId?: number;
  entryType: 'ALLOCATION' | 'RELEASE' | 'REVISION' | 'ADDITIONAL';
  amount: number;
  currency?: string;
  effectiveDate?: string;
  reason?: string;
}

export interface StageDetail {
  stageCode: number;
  stageName: string;
  stage: PackageStage;
  readiness: StageReadiness;
  requiredDocuments: StageDocumentRequirement[];
  documents: DocumentLink[];
  catalogue: CatalogueField[];
  fields: ExtractedField[];
  warnings: string[];
  bidders?: BerBidder[];
  inspections?: InspectionEvent[];
  deliveries?: Delivery[];
  invoices?: Invoice[];
  payments?: Payment[];
  cumulativeDelivered?: number;
  /** The e-GP item baseline Stages 12 and 13 are measured against (REQ-10.3). */
  priceSchedule?: PriceSchedule | null;
  priceScheduleLines?: PriceScheduleLine[];
}

/** The e-GP price schedule stored against the contract (REQ-10.3). */
export interface PriceSchedule {
  id?: number;
  contractId?: number;
  source?: string;
  deliveryPeriodDays?: number;
}

export interface PriceScheduleLine {
  id?: number;
  scheduleId?: number;
  lineNo?: number;
  itemCode?: string;
  itemDescription?: string;
  quantity?: number;
  uom?: string;
  unitPrice?: number;
  lineAmount?: number;
  currency?: string;
}

/** A permitted value for Procurement Type / Method / Nature (Q-8, REQ-2.4). */
export interface MasterListValue {
  id?: number;
  listKey: string;
  code: string;
  label?: string;
  displayOrder?: number;
}

/** One stage on the package timeline, with how long it took (REQ-X5). */
export interface StageTimelineEntry {
  stageCode: number;
  stageName: string;
  status: string;
  applicable: boolean;
  enteredAt?: string;
  completedAt?: string;
  completedBy?: number;
  reworkReason?: string;
  elapsedDays?: number;
  open?: boolean;
}

/** A deadline somebody must act before (REQ-X8). */
export interface PackageDeadline {
  packageId: number;
  packageNumber?: string;
  deadlineKey: string;
  label: string;
  dueDate: string;
  daysRemaining: number;
  overdue: boolean;
}

export interface UploadResult {
  document: { id: number; originalName: string };
  packageNumber: string;
  fields: ExtractedField[];
  ocrConfidence?: number;
  ocrError?: string;
  /** OCR produced nothing usable - the values have to be typed in (REQ-P12). */
  manualEntryRequired?: boolean;
  /** Whether captured values carry a source region to highlight (REQ-P6). */
  sourceRegionsAvailable?: boolean;
}

/**
 * The executive rollup (REQ-X5). Every figure is derived from the lifecycle records by
 * ExecutiveDashboardService — see that class for what each count means.
 *
 * Money arrives twice: the raw BDT amount and the same figure in crore, because the
 * client reports in crore and rounding in the browser would not survive review.
 */
export interface ExecutiveSnapshot {
  generatedAt: string;
  fiscalYear: number | null;
  /** Years that actually have packages, newest first — the FY selector's options. */
  fiscalYears: number[];
  currency: string;
  kpis: {
    totalContracts: number;
    liveTenders: number;
    runningContracts: number;
    completedContracts: number;
    approvedBudget: number;
    allocatedBudget: number;
    expenditure: number;
    remainingBudget: number;
  };
  appProgress: {
    totalPackages: number;
    plannedBudget: number;
    plannedBudgetCrore: number;
    publishedTenders: number;
    awarded: number;
    underExecution: number;
    completed: number;
    delayed: number;
    completionPct: number;
    /** Null when the previous year holds no packages — no comparison, not "no change". */
    previousYearCompletionPct: number | null;
    completionTrendPct: number | null;
  };
  tenderStatistics: ChartBucket[];
  procurementNature: ChartBucket[];
  procurementMethod: ChartBucket[];
  budget: {
    approved: number;
    released: number;
    expenditure: number;
    remaining: number;
    approvedCrore: number;
    releasedCrore: number;
    expenditureCrore: number;
    remainingCrore: number;
    utilizationPct: number;
  };
  performanceSecurity: {
    active: number;
    expiringIn30Days: number;
    expiringIn15Days: number;
    expiringIn7Days: number;
    expired: number;
    renewed: number;
  };
  documents: {
    total: number;
    todayUploads: number;
    ocrProcessed: number;
    ocrPending: number;
    ocrFailed: number;
    archived: number;
    /** Null when Elasticsearch is unreachable. */
    indexed: number | null;
  };
  alerts: ExecutiveAlert[];
}

export interface ChartBucket {
  label: string;
  value: number;
}

export interface ExecutiveAlert {
  key: string;
  tone: 'danger' | 'warn' | 'info' | 'neutral';
  title: string;
  description: string;
  count: number;
}

export const STAGE_NAMES: string[] = [
  '',
  'APP Approved',
  'Tender Advertisement',
  'Tender Opening',
  'Tender Evaluation',
  'Contract Approval',
  'Notification of Award',
  'Performance Security',
  'Contract Signing',
  'Letter of Credit',
  'Manufacturing / Supply',
  'Inspection',
  'Delivery',
  'Bill Submission',
  'Payment',
  'Warranty Period',
  'Contract Close',
];

export const STAGE_COUNT = 16;
