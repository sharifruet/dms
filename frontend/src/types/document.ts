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
