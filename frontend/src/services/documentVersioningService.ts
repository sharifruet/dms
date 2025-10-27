import api from './api';

export interface DocumentVersion {
  id: number;
  document: {
    id: number;
    originalName: string;
  };
  versionNumber: string;
  filePath: string;
  fileSize: number;
  fileHash: string;
  mimeType: string;
  createdBy: {
    id: number;
    username: string;
  };
  changeDescription: string;
  versionType: string;
  isCurrent: boolean;
  isArchived: boolean;
  archivedAt: string;
  metadata: string;
  createdAt: string;
}

export interface VersionComparison {
  version1: {
    versionNumber: string;
    createdAt: string;
    createdBy: string;
    changeDescription: string;
    fileSize: number;
  };
  version2: {
    versionNumber: string;
    createdAt: string;
    createdBy: string;
    changeDescription: string;
    fileSize: number;
  };
  differences: {
    fileSizeChanged: boolean;
    contentChanged: boolean;
    timeDifference: number;
  };
}

export const documentVersioningService = {
  // Create a new version of a document
  createDocumentVersion: async (
    documentId: number,
    file: File,
    changeDescription: string,
    versionType: string
  ): Promise<DocumentVersion> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('changeDescription', changeDescription);
    formData.append('versionType', versionType);

    const response = await api.post(`/documents/${documentId}/versions`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // Get all versions of a document
  getDocumentVersions: async (documentId: number): Promise<DocumentVersion[]> => {
    const response = await api.get(`/documents/${documentId}/versions`);
    return response.data;
  },

  // Get a specific version of a document
  getDocumentVersion: async (documentId: number, versionNumber: string): Promise<DocumentVersion> => {
    const response = await api.get(`/documents/${documentId}/versions/${versionNumber}`);
    return response.data;
  },

  // Restore a document to a specific version
  restoreDocumentToVersion: async (documentId: number, versionNumber: string): Promise<any> => {
    const response = await api.post(`/documents/${documentId}/versions/${versionNumber}/restore`);
    return response.data;
  },

  // Archive old versions
  archiveOldVersions: async (documentId: number, keepVersions: number = 5): Promise<void> => {
    await api.post(`/documents/${documentId}/versions/archive?keepVersions=${keepVersions}`);
  },

  // Get version count for a document
  getVersionCount: async (documentId: number): Promise<number> => {
    const response = await api.get(`/documents/${documentId}/versions/count`);
    return response.data;
  },

  // Compare two versions
  compareVersions: async (
    documentId: number,
    version1: string,
    version2: string
  ): Promise<VersionComparison> => {
    const response = await api.get(
      `/documents/${documentId}/versions/compare?version1=${version1}&version2=${version2}`
    );
    return response.data;
  },

  // Download a specific version
  downloadVersion: async (documentId: number, versionNumber: string): Promise<Blob> => {
    const response = await api.get(`/documents/${documentId}/versions/${versionNumber}/download`, {
      responseType: 'blob',
    });
    return response.data;
  }
};
