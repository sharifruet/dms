import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

class ExpiryTrackingService {
  private getAuthHeaders() {
    const token = localStorage.getItem('token');
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    };
  }

  async createExpiryTracking(request: CreateExpiryTrackingRequest) {
    try {
      const response = await axios.post(`${API_BASE_URL}/expiry-tracking`, request, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error creating expiry tracking:', error);
      throw error;
    }
  }

  async updateExpiryTracking(trackingId: number, tracking: ExpiryTracking) {
    try {
      const response = await axios.put(`${API_BASE_URL}/expiry-tracking/${trackingId}`, tracking, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error updating expiry tracking:', error);
      throw error;
    }
  }

  async renewExpiryTracking(trackingId: number, request: RenewExpiryTrackingRequest) {
    try {
      const response = await axios.post(`${API_BASE_URL}/expiry-tracking/${trackingId}/renew`, request, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error renewing expiry tracking:', error);
      throw error;
    }
  }

  async getExpiryTrackingByDocument(documentId: number) {
    try {
      const response = await axios.get(`${API_BASE_URL}/expiry-tracking/document/${documentId}`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching expiry tracking by document:', error);
      throw error;
    }
  }

  async getActiveExpiryTracking(page = 0, size = 20) {
    try {
      const response = await axios.get(`${API_BASE_URL}/expiry-tracking/active`, {
        headers: this.getAuthHeaders(),
        params: { page, size }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching active expiry tracking:', error);
      throw error;
    }
  }

  async getExpiringDocuments(days = 30) {
    try {
      const response = await axios.get(`${API_BASE_URL}/expiry-tracking/expiring`, {
        headers: this.getAuthHeaders(),
        params: { days }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching expiring documents:', error);
      throw error;
    }
  }

  async getExpiredDocuments() {
    try {
      const response = await axios.get(`${API_BASE_URL}/expiry-tracking/expired`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching expired documents:', error);
      throw error;
    }
  }

  async getExpiryStatistics() {
    try {
      const response = await axios.get(`${API_BASE_URL}/expiry-tracking/statistics`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching expiry statistics:', error);
      throw error;
    }
  }
}

export interface ExpiryTracking {
  id: number;
  documentId: number;
  expiryType: ExpiryType;
  expiryDate: string;
  alert30Days: boolean;
  alert15Days: boolean;
  alert7Days: boolean;
  alertExpired: boolean;
  status: ExpiryStatus;
  renewalDate?: string;
  renewalDocumentId?: number;
  notes?: string;
  assignedTo?: number;
  department?: string;
  vendorName?: string;
  contractValue?: number;
  currency?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExpiryTrackingRequest {
  documentId: number;
  expiryType: ExpiryType;
  expiryDate: string;
  notes?: string;
}

export interface RenewExpiryTrackingRequest {
  newExpiryDate: string;
  renewalDocumentId?: number;
  notes?: string;
}

export enum ExpiryType {
  CONTRACT = 'CONTRACT',
  BANK_GUARANTEE = 'BANK_GUARANTEE',
  LETTER_OF_CREDIT = 'LETTER_OF_CREDIT',
  PERFORMANCE_SECURITY = 'PERFORMANCE_SECURITY',
  WARRANTY = 'WARRANTY',
  INSURANCE = 'INSURANCE',
  LICENSE = 'LICENSE',
  PERMIT = 'PERMIT',
  CERTIFICATE = 'CERTIFICATE',
  OTHER = 'OTHER'
}

export enum ExpiryStatus {
  ACTIVE = 'ACTIVE',
  EXPIRED = 'EXPIRED',
  RENEWED = 'RENEWED',
  CANCELLED = 'CANCELLED',
  SUSPENDED = 'SUSPENDED'
}

export default new ExpiryTrackingService();
