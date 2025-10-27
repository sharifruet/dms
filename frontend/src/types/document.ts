export enum DocumentType {
  TENDER = 'TENDER',
  PURCHASE_ORDER = 'PURCHASE_ORDER',
  LETTER_OF_CREDIT = 'LETTER_OF_CREDIT',
  BANK_GUARANTEE = 'BANK_GUARANTEE',
  CONTRACT = 'CONTRACT',
  CORRESPONDENCE = 'CORRESPONDENCE',
  STATIONERY_RECORD = 'STATIONERY_RECORD',
  OTHER = 'OTHER'
}

export interface Document {
  id: number;
  fileName: string;
  originalName: string;
  filePath: string;
  fileSize: number;
  mimeType: string;
  documentType: DocumentType;
  description?: string;
  tags?: string;
  uploadedBy: {
    id: number;
    username: string;
  };
  department: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface FileUploadResponse {
  success: boolean;
  message?: string;
  documentId?: number;
  fileName?: string;
  originalName?: string;
  fileSize?: number;
  mimeType?: string;
  documentType?: DocumentType;
}
