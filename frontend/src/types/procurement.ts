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

export type ValidationState = 'VALID' | 'NEEDS_REVIEW' | 'NOT_FOUND' | 'INVALID';

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
  ready: boolean;
  blockers: string[];
  missingDocuments: string[];
  unconfirmedFields: string[];
  validationErrors: string[];
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
}

export interface UploadResult {
  document: { id: number; originalName: string };
  packageNumber: string;
  fields: ExtractedField[];
  ocrConfidence?: number;
  ocrError?: string;
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
