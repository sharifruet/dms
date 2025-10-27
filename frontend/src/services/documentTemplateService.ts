import api from './api';

export interface DocumentTemplate {
  id: number;
  name: string;
  description: string;
  templateType: string;
  filePath: string;
  fileSize: number;
  mimeType: string;
  templateContent: string;
  variables: string;
  validationRules: string;
  status: string;
  isPublic: boolean;
  usageCount: number;
  createdBy: {
    id: number;
    username: string;
  };
  lastModifiedBy: {
    id: number;
    username: string;
  };
  lastUsedAt: string;
  metadata: string;
  createdAt: string;
  updatedAt: string;
}

export interface TemplateStatistics {
  totalTemplates: number;
  activeTemplates: number;
  publicTemplates: number;
  draftTemplates: number;
}

export interface GenerateDocumentRequest {
  variables: Record<string, any>;
}

export const documentTemplateService = {
  // Create a new document template
  createTemplate: async (
    name: string,
    description: string,
    templateType: string,
    file: File,
    templateContent?: string
  ): Promise<DocumentTemplate> => {
    const formData = new FormData();
    formData.append('name', name);
    formData.append('description', description);
    formData.append('templateType', templateType);
    formData.append('file', file);
    if (templateContent) {
      formData.append('templateContent', templateContent);
    }

    const response = await api.post('/templates', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // Update document template
  updateTemplate: async (
    templateId: number,
    name?: string,
    description?: string,
    templateType?: string,
    file?: File,
    templateContent?: string
  ): Promise<DocumentTemplate> => {
    const formData = new FormData();
    if (name) formData.append('name', name);
    if (description) formData.append('description', description);
    if (templateType) formData.append('templateType', templateType);
    if (file) formData.append('file', file);
    if (templateContent) formData.append('templateContent', templateContent);

    const response = await api.put(`/templates/${templateId}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // Delete document template
  deleteTemplate: async (templateId: number): Promise<void> => {
    await api.delete(`/templates/${templateId}`);
  },

  // Generate document from template
  generateDocumentFromTemplate: async (
    templateId: number,
    variables: Record<string, any>
  ): Promise<string> => {
    const response = await api.post(`/templates/${templateId}/generate`, { variables });
    return response.data;
  },

  // Get templates by type
  getTemplatesByType: async (templateType: string, page: number = 0, size: number = 10): Promise<{
    content: DocumentTemplate[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  }> => {
    const response = await api.get(`/templates/type/${templateType}?page=${page}&size=${size}`);
    return response.data;
  },

  // Get public templates
  getPublicTemplates: async (page: number = 0, size: number = 10): Promise<{
    content: DocumentTemplate[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  }> => {
    const response = await api.get(`/templates/public?page=${page}&size=${size}`);
    return response.data;
  },

  // Get templates for user
  getTemplatesForUser: async (page: number = 0, size: number = 10): Promise<{
    content: DocumentTemplate[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  }> => {
    const response = await api.get(`/templates/user?page=${page}&size=${size}`);
    return response.data;
  },

  // Get most used templates
  getMostUsedTemplates: async (limit: number = 10): Promise<DocumentTemplate[]> => {
    const response = await api.get(`/templates/most-used?limit=${limit}`);
    return response.data;
  },

  // Get recently used templates
  getRecentlyUsedTemplates: async (limit: number = 10): Promise<DocumentTemplate[]> => {
    const response = await api.get(`/templates/recently-used?limit=${limit}`);
    return response.data;
  },

  // Search templates
  searchTemplates: async (query: string, page: number = 0, size: number = 10): Promise<{
    content: DocumentTemplate[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  }> => {
    const response = await api.get(`/templates/search?query=${query}&page=${page}&size=${size}`);
    return response.data;
  },

  // Get template statistics
  getTemplateStatistics: async (): Promise<TemplateStatistics> => {
    const response = await api.get('/templates/statistics');
    return response.data;
  },

  // Validate template variables
  validateTemplateVariables: async (
    templateId: number,
    variables: Record<string, any>
  ): Promise<boolean> => {
    const response = await api.post(`/templates/${templateId}/validate`, { variables });
    return response.data;
  },

  // Get template variables
  getTemplateVariables: async (templateId: number): Promise<Record<string, any>> => {
    const response = await api.get(`/templates/${templateId}/variables`);
    return response.data;
  },

  // Download template file
  downloadTemplate: async (templateId: number): Promise<Blob> => {
    const response = await api.get(`/templates/${templateId}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },

  // Clone template
  cloneTemplate: async (templateId: number, newName: string): Promise<DocumentTemplate> => {
    const response = await api.post(`/templates/${templateId}/clone`, { newName });
    return response.data;
  }
};
