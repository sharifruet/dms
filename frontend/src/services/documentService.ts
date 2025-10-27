import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor to include JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor to handle token expiration
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export interface Document {
  id?: number;
  fileName: string;
  filePath?: string;
  documentType: string;
  uploadedBy?: string;
  uploadedAt?: string;
  department?: string;
  size?: string;
  status?: string;
  description?: string;
  tags?: string;
  fileHash?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface DocumentUploadRequest {
  file: File;
  title: string;
  description?: string;
  documentType: string;
  department: string;
  tags?: string;
  userId: number;
}

export interface DocumentSearchParams {
  searchTerm?: string;
  documentType?: string;
  department?: string;
  uploadedBy?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export interface DocumentStatistics {
  totalDocuments: number;
  documentsByType: { [key: string]: number };
  documentsByDepartment: { [key: string]: number };
  totalSize: number;
  recentUploads: number;
}

export const documentService = {
  // Upload a new document
  uploadDocument: async (uploadRequest: DocumentUploadRequest): Promise<Document> => {
    const formData = new FormData();
    formData.append('file', uploadRequest.file);
    formData.append('userId', uploadRequest.userId.toString());
    formData.append('title', uploadRequest.title);
    formData.append('description', uploadRequest.description || '');
    formData.append('documentType', uploadRequest.documentType);
    formData.append('department', uploadRequest.department);
    formData.append('tags', uploadRequest.tags || '');

    const response = await api.post('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // Get all documents with pagination and filtering
  getDocuments: async (params?: DocumentSearchParams): Promise<{ content: Document[]; totalElements: number; totalPages: number }> => {
    const response = await api.get('/documents', { params });
    return response.data;
  },

  // Get document by ID
  getDocumentById: async (id: number): Promise<Document> => {
    const response = await api.get(`/documents/${id}`);
    return response.data;
  },

  // Update document metadata
  updateDocument: async (id: number, document: Partial<Document>): Promise<Document> => {
    const response = await api.put(`/documents/${id}`, document);
    return response.data;
  },

  // Delete document (soft delete)
  deleteDocument: async (id: number): Promise<void> => {
    await api.delete(`/documents/${id}`);
  },

  // Download document
  downloadDocument: async (id: number): Promise<Blob> => {
    const response = await api.get(`/documents/${id}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },

  // Get document preview
  getDocumentPreview: async (id: number): Promise<string> => {
    const response = await api.get(`/documents/${id}/preview`);
    return response.data.previewUrl;
  },

  // Get document statistics
  getDocumentStatistics: async (): Promise<DocumentStatistics> => {
    const response = await api.get('/documents/statistics');
    return response.data;
  },

  // Search documents
  searchDocuments: async (searchTerm: string, filters?: {
    documentType?: string;
    department?: string;
    uploadedBy?: string;
    startDate?: string;
    endDate?: string;
  }): Promise<Document[]> => {
    const params = { searchTerm, ...filters };
    const response = await api.get('/documents/search', { params });
    return response.data;
  },

  // Get documents by user
  getDocumentsByUser: async (userId: number): Promise<Document[]> => {
    const response = await api.get(`/documents/user/${userId}`);
    return response.data;
  },

  // Get documents by department
  getDocumentsByDepartment: async (department: string): Promise<Document[]> => {
    const response = await api.get(`/documents/department/${department}`);
    return response.data;
  },

  // Get recent documents
  getRecentDocuments: async (limit: number = 10): Promise<Document[]> => {
    const response = await api.get(`/documents/recent?limit=${limit}`);
    return response.data;
  },

  // Get popular documents
  getPopularDocuments: async (limit: number = 10): Promise<Document[]> => {
    const response = await api.get(`/documents/popular?limit=${limit}`);
    return response.data;
  },

  // Get document versions
  getDocumentVersions: async (documentId: number): Promise<Document[]> => {
    const response = await api.get(`/documents/${documentId}/versions`);
    return response.data;
  },

  // Restore document version
  restoreDocumentVersion: async (documentId: number, versionId: number): Promise<Document> => {
    const response = await api.post(`/documents/${documentId}/versions/${versionId}/restore`);
    return response.data;
  },

  // Get document comments
  getDocumentComments: async (documentId: number): Promise<any[]> => {
    const response = await api.get(`/documents/${documentId}/comments`);
    return response.data;
  },

  // Add document comment
  addDocumentComment: async (documentId: number, comment: { content: string; userId: number }): Promise<any> => {
    const response = await api.post(`/documents/${documentId}/comments`, comment);
    return response.data;
  },

  // Get document annotations
  getDocumentAnnotations: async (documentId: number): Promise<any[]> => {
    const response = await api.get(`/documents/${documentId}/annotations`);
    return response.data;
  },

  // Add document annotation
  addDocumentAnnotation: async (documentId: number, annotation: {
    content: string;
    userId: number;
    x: number;
    y: number;
    page: number;
  }): Promise<any> => {
    const response = await api.post(`/documents/${documentId}/annotations`, annotation);
    return response.data;
  },

  // Share document
  shareDocument: async (documentId: number, shareRequest: {
    userIds: number[];
    permissions: string[];
    expiresAt?: string;
  }): Promise<any> => {
    const response = await api.post(`/documents/${documentId}/share`, shareRequest);
    return response.data;
  },

  // Get document sharing info
  getDocumentSharing: async (documentId: number): Promise<any[]> => {
    const response = await api.get(`/documents/${documentId}/sharing`);
    return response.data;
  },

  // Revoke document sharing
  revokeDocumentSharing: async (documentId: number, userId: number): Promise<void> => {
    await api.delete(`/documents/${documentId}/sharing/${userId}`);
  },

  // Get document audit log
  getDocumentAuditLog: async (documentId: number): Promise<any[]> => {
    const response = await api.get(`/documents/${documentId}/audit`);
    return response.data;
  },

  // Bulk operations
  bulkDeleteDocuments: async (documentIds: number[]): Promise<void> => {
    await api.post('/documents/bulk/delete', { documentIds });
  },

  bulkUpdateDocuments: async (documentIds: number[], updates: Partial<Document>): Promise<void> => {
    await api.post('/documents/bulk/update', { documentIds, updates });
  },

  bulkDownloadDocuments: async (documentIds: number[]): Promise<Blob> => {
    const response = await api.post('/documents/bulk/download', { documentIds }, {
      responseType: 'blob',
    });
    return response.data;
  },

  // Export documents
  exportDocuments: async (filters?: DocumentSearchParams, format: string = 'csv'): Promise<Blob> => {
    const response = await api.post('/documents/export', { filters, format }, {
      responseType: 'blob',
    });
    return response.data;
  },

  // Import documents
  importDocuments: async (file: File): Promise<{ success: number; failed: number; errors: string[] }> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post('/documents/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  }
};

export default documentService;
