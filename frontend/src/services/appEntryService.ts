import api from './api';

export interface AppEntry {
  id: number;
  fiscalYear: number;
  allocationType?: string;
  budgetReleaseDate?: string;
  allocationAmount?: number;
  releaseInstallmentNo?: number;
  referenceMemoNumber?: string;
  department?: string;
  attachmentFilePath?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: {
    id: number;
    username: string;
  };
}

export interface CreateAppEntryRequest {
  fiscalYear: number;
  allocationType: string;
  budgetReleaseDate: string;
  allocationAmount: number;
  releaseInstallmentNo: number;
  referenceMemoNumber?: string;
  department?: string;
  attachment?: File;
}

export const appEntryService = {
  /**
   * Create a new APP entry
   */
  createAppEntry: async (request: CreateAppEntryRequest): Promise<AppEntry> => {
    const formData = new FormData();
    formData.append('fiscalYear', request.fiscalYear.toString());
    formData.append('allocationType', request.allocationType);
    formData.append('budgetReleaseDate', request.budgetReleaseDate);
    formData.append('allocationAmount', request.allocationAmount.toString());
    formData.append('releaseInstallmentNo', request.releaseInstallmentNo.toString());
    if (request.referenceMemoNumber) {
      formData.append('referenceMemoNumber', request.referenceMemoNumber);
    }
    if (request.department) {
      formData.append('department', request.department);
    }
    if (request.attachment) {
      formData.append('attachment', request.attachment);
    }

    const response = await api.post('/finance/app-entries', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.entry || response.data;
  },

  /**
   * Get all APP entries
   */
  getAppEntries: async (fiscalYear?: number): Promise<AppEntry[]> => {
    const params = new URLSearchParams();
    if (fiscalYear) {
      params.append('fiscalYear', fiscalYear.toString());
    }
    const response = await api.get(`/finance/app-entries?${params.toString()}`);
    return response.data.entries || [];
  },

  /**
   * Get APP entry by ID
   */
  getAppEntry: async (id: number): Promise<AppEntry> => {
    const response = await api.get(`/finance/app-entries/${id}`);
    return response.data.entry || response.data;
  },

  /**
   * Get next installment number for a fiscal year
   */
  getNextInstallmentNo: async (fiscalYear: number): Promise<number> => {
    const response = await api.get(`/finance/app-entries/next-installment?fiscalYear=${fiscalYear}`);
    return response.data.nextInstallmentNo || 1;
  },

  /**
   * Get distinct fiscal years
   */
  getFiscalYears: async (): Promise<number[]> => {
    const response = await api.get('/finance/app-entries/fiscal-years');
    return response.data.fiscalYears || [];
  },

  /**
   * Check if a duplicate entry exists
   */
  checkDuplicate: async (fiscalYear: number, installmentNo: number): Promise<boolean> => {
    const response = await api.get(
      `/finance/app-entries/check-duplicate?fiscalYear=${fiscalYear}&installmentNo=${installmentNo}`
    );
    return response.data.isDuplicate || false;
  },

  /**
   * Update an APP entry
   */
  updateAppEntry: async (id: number, request: CreateAppEntryRequest): Promise<AppEntry> => {
    const formData = new FormData();
    formData.append('fiscalYear', request.fiscalYear.toString());
    formData.append('allocationType', request.allocationType);
    formData.append('budgetReleaseDate', request.budgetReleaseDate);
    formData.append('allocationAmount', request.allocationAmount.toString());
    formData.append('releaseInstallmentNo', request.releaseInstallmentNo.toString());
    if (request.referenceMemoNumber) {
      formData.append('referenceMemoNumber', request.referenceMemoNumber);
    }
    if (request.department) {
      formData.append('department', request.department);
    }
    if (request.attachment) {
      formData.append('attachment', request.attachment);
    }

    const response = await api.put(`/finance/app-entries/${id}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.entry || response.data;
  },

  /**
   * Delete an APP entry
   */
  deleteAppEntry: async (id: number): Promise<void> => {
    await api.delete(`/finance/app-entries/${id}`);
  },
};

