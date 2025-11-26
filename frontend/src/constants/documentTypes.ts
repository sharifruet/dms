/**
 * Document types aligned with backend DocumentType enum
 * @see backend/src/main/java/com/bpdb/dms/model/DocumentType.java
 */
export enum DocumentType {
  TENDER_NOTICE = 'TENDER_NOTICE',
  TENDER_DOCUMENT = 'TENDER_DOCUMENT',
  CONTRACT_AGREEMENT = 'CONTRACT_AGREEMENT',
  BANK_GUARANTEE_BG = 'BANK_GUARANTEE_BG',
  PERFORMANCE_SECURITY_PS = 'PERFORMANCE_SECURITY_PS',
  PERFORMANCE_GUARANTEE_PG = 'PERFORMANCE_GUARANTEE_PG',
  BILL = 'BILL',
  CORRESPONDENCE = 'CORRESPONDENCE',
  STATIONERY_RECORD = 'STATIONERY_RECORD',
  OTHER = 'OTHER',
}

/**
 * Document type labels for display
 */
export const DocumentTypeLabels: Record<DocumentType, string> = {
  [DocumentType.TENDER_NOTICE]: 'Tender Notice',
  [DocumentType.TENDER_DOCUMENT]: 'Tender Document',
  [DocumentType.CONTRACT_AGREEMENT]: 'Contract Agreement',
  [DocumentType.BANK_GUARANTEE_BG]: 'Bank Guarantee (BG)',
  [DocumentType.PERFORMANCE_SECURITY_PS]: 'Performance Security (PS)',
  [DocumentType.PERFORMANCE_GUARANTEE_PG]: 'Performance Guarantee (PG)',
  [DocumentType.BILL]: 'Bill',
  [DocumentType.CORRESPONDENCE]: 'Correspondence',
  [DocumentType.STATIONERY_RECORD]: 'Stationery Record',
  [DocumentType.OTHER]: 'Other',
};

/**
 * Get display label for a document type
 */
export const getDocumentTypeLabel = (type: string): string => {
  return DocumentTypeLabels[type as DocumentType] || type;
};

/**
 * Document types that require tender workflow
 */
export const TENDER_WORKFLOW_TYPES: DocumentType[] = [
  DocumentType.TENDER_DOCUMENT,
  DocumentType.CONTRACT_AGREEMENT,
  DocumentType.BANK_GUARANTEE_BG,
  DocumentType.PERFORMANCE_SECURITY_PS,
  DocumentType.PERFORMANCE_GUARANTEE_PG,
  DocumentType.BILL,
  DocumentType.CORRESPONDENCE,
];

/**
 * Check if a document type requires tender workflow
 */
export const requiresTenderWorkflow = (docType: string): boolean => {
  return TENDER_WORKFLOW_TYPES.includes(docType as DocumentType);
};

/**
 * Get color for document type chip
 */
export const getDocumentTypeColor = (type: string): 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' => {
  if (!type) return 'default';
  const normalized = type.toUpperCase();
  
  if (normalized.includes('TENDER')) return 'primary';
  if (normalized.includes('CONTRACT')) return 'success';
  if (normalized.includes('GUARANTEE') || normalized.includes('SECURITY')) return 'info';
  if (normalized.includes('BILL')) return 'warning';
  
  return 'default';
};

/**
 * All document types as array for dropdowns
 */
export const ALL_DOCUMENT_TYPES = Object.values(DocumentType);

/**
 * Document types as options for select components
 */
export const DOCUMENT_TYPE_OPTIONS = ALL_DOCUMENT_TYPES.map(type => ({
  value: type,
  label: DocumentTypeLabels[type],
}));

