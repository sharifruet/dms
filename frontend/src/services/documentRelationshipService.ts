import api from './api';

export enum DocumentRelationshipType {
  CONTRACT_TO_LC = 'CONTRACT_TO_LC',
  LC_TO_BG = 'LC_TO_BG',
  BG_TO_PO = 'BG_TO_PO',
  PO_TO_CORRESPONDENCE = 'PO_TO_CORRESPONDENCE',
  CONTRACT_TO_BG = 'CONTRACT_TO_BG',
  CONTRACT_TO_PO = 'CONTRACT_TO_PO',
  LC_TO_PO = 'LC_TO_PO',
  BG_TO_CORRESPONDENCE = 'BG_TO_CORRESPONDENCE',
  OTHER = 'OTHER'
}

export interface DocumentRelationship {
  id: number;
  sourceDocument: {
    id: number;
    fileName: string;
    originalName?: string;
    documentType: string;
  };
  targetDocument: {
    id: number;
    fileName: string;
    originalName?: string;
    documentType: string;
  };
  relationshipType: DocumentRelationshipType;
  description?: string;
  createdBy: {
    id: number;
    username: string;
    firstName?: string;
    lastName?: string;
  };
  createdAt: string;
  updatedAt: string;
}

export interface CreateRelationshipRequest {
  targetDocumentId: number;
  relationshipType: DocumentRelationshipType;
  description?: string;
}

export const documentRelationshipService = {
  /**
   * Get all relationships for a document
   */
  getRelationships: async (documentId: number): Promise<DocumentRelationship[]> => {
    const response = await api.get(`/documents/${documentId}/relationships`);
    return response.data.relationships || [];
  },

  /**
   * Create a relationship between two documents
   */
  createRelationship: async (
    documentId: number,
    request: CreateRelationshipRequest
  ): Promise<DocumentRelationship> => {
    const response = await api.post(`/documents/${documentId}/relationships`, request);
    return response.data.relationship;
  },

  /**
   * Delete a relationship
   */
  deleteRelationship: async (documentId: number, relationshipId: number): Promise<void> => {
    await api.delete(`/documents/${documentId}/relationships/${relationshipId}`);
  },

  /**
   * Get related document IDs
   */
  getRelatedDocumentIds: async (documentId: number): Promise<number[]> => {
    const response = await api.get(`/documents/${documentId}/relationships/related`);
    return response.data.relatedDocumentIds || [];
  },
};

