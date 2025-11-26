export interface Document {
  id: number;
  fileName: string;
  originalName: string;
  filePath: string;
  fileSize: number;
  mimeType: string;
  documentType: string;
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
  uploadedAt?: string;
  folder?: {
    id: number;
    name: string;
    folderPath?: string;
  } | null;
}

export interface FileUploadResponse {
  success: boolean;
  message?: string;
  documentId?: number;
  fileName?: string;
  originalName?: string;
  fileSize?: number;
  mimeType?: string;
  documentType?: string;
  isDuplicate?: boolean;
  duplicateDocumentId?: number;
  duplicateFileName?: string;
  duplicateOriginalName?: string;
  duplicateFileSize?: number;
  duplicateMimeType?: string;
  duplicateDocumentType?: string;
  duplicateCreatedAt?: string;
  duplicateUploadedBy?: string;
  detectedDocumentType?: string;
  detectionConfidence?: number;
}

export interface DocumentCategory {
  id: number;
  name: string;
  displayName: string;
  description?: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface DocumentTypeField {
  id: number;
  fieldKey: string;
  fieldLabel: string;
  fieldType: string; // text, number, date, select, multiselect
  isRequired: boolean;
  defaultValue?: string;
  fieldOptions?: string; // JSON string for select/multiselect options
  displayOrder: number;
  description?: string;
  value?: string; // Current value for this document
}
