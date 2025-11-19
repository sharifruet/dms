import api from './api';

export interface DocumentTypeField {
  id?: number;
  documentType: string;
  fieldKey: string;
  fieldLabel: string;
  fieldType: 'text' | 'number' | 'date' | 'select' | 'multiselect';
  isRequired: boolean;
  isOcrMappable: boolean;
  ocrPattern?: string;
  defaultValue?: string;
  validationRules?: string;
  fieldOptions?: string; // JSON string for select options
  displayOrder: number;
  isActive: boolean;
  description?: string;
}

export interface FieldOption {
  value: string;
  label: string;
}

export const documentTypeFieldService = {
  // Get all fields for a document type
  getFieldsForDocumentType: async (documentType: string): Promise<DocumentTypeField[]> => {
    const response = await api.get(`/document-type-fields/${documentType}`);
    return response.data;
  },

  // Get all fields including inactive (admin only)
  getAllFieldsForDocumentType: async (documentType: string): Promise<DocumentTypeField[]> => {
    const response = await api.get(`/document-type-fields/${documentType}/all`);
    return response.data;
  },

  // Create a new field configuration
  createField: async (field: DocumentTypeField): Promise<DocumentTypeField> => {
    const response = await api.post('/document-type-fields', field);
    return response.data;
  },

  // Update an existing field configuration
  updateField: async (id: number, field: DocumentTypeField): Promise<DocumentTypeField> => {
    const response = await api.put(`/document-type-fields/${id}`, field);
    return response.data;
  },

  // Delete a field configuration
  deleteField: async (id: number): Promise<void> => {
    await api.delete(`/document-type-fields/${id}`);
  },

  // Deactivate a field configuration
  deactivateField: async (id: number): Promise<DocumentTypeField> => {
    const response = await api.patch(`/document-type-fields/${id}/deactivate`);
    return response.data;
  },

  // Map OCR data to fields
  mapOcrData: async (documentType: string, ocrText: string): Promise<Record<string, string>> => {
    const response = await api.post(`/document-type-fields/${documentType}/map-ocr`, { ocrText });
    return response.data;
  },

  // Validate field values
  validateFields: async (
    documentType: string,
    fieldValues: Record<string, string>
  ): Promise<{ valid: boolean; message: string }> => {
    const response = await api.post(`/document-type-fields/${documentType}/validate`, fieldValues);
    return response.data;
  },

  // Parse field options JSON
  parseFieldOptions: (fieldOptions?: string): FieldOption[] => {
    if (!fieldOptions) return [];
    try {
      return JSON.parse(fieldOptions);
    } catch {
      return [];
    }
  },
};

export default documentTypeFieldService;

