import api from './api';

export interface BillHeader {
  id: number;
  fiscalYear: number;
  vendor?: string;
  invoiceNumber?: string;
  invoiceDate?: string;
  createdAt?: string;
  updatedAt?: string;
  lines?: BillLine[];
  createdBy?: {
    id: number;
    username: string;
  };
}

export interface BillLine {
  id: number;
  projectIdentifier?: string;
  department?: string;
  costCenter?: string;
  category?: string;
  amount: number;
  taxAmount?: number;
  appLine?: {
    id: number;
  };
}

export const financeService = {
  // Get all bills
  getBills: async (fiscalYear?: number, vendor?: string, page: number = 0, size: number = 100): Promise<{ content: BillHeader[]; totalElements: number; totalPages: number }> => {
    const params = new URLSearchParams();
    if (fiscalYear) params.append('fiscalYear', fiscalYear.toString());
    if (vendor) params.append('vendor', vendor);
    params.append('page', page.toString());
    params.append('size', size.toString());
    const response = await api.get(`/finance/bills?${params.toString()}`);
    // Handle both paginated response and legacy list response
    if (response.data.content) {
      return response.data;
    } else {
      // Legacy format - wrap in paginated format
      return {
        content: response.data,
        totalElements: response.data.length,
        totalPages: 1
      };
    }
  },

  // Get a single bill by ID
  getBill: async (id: number): Promise<BillHeader> => {
    const response = await api.get(`/finance/bills/${id}`);
    return response.data;
  },

  // Get budget summary (budget and billed amounts)
  getBudgetSummary: async (): Promise<{
    totalBudget: number;
    totalBilled: number;
    remaining: number;
    utilizationPct: number;
  }> => {
    const response = await api.get('/finance/dashboard/budget-summary');
    return response.data;
  },

  // Create a new bill
  createBill: async (bill: {
    fiscalYear: number;
    vendor?: string;
    invoiceNumber?: string;
    invoiceDate?: string;
    lines?: Array<{
      appLineId?: number;
      projectIdentifier?: string;
      department?: string;
      costCenter?: string;
      category?: string;
      amount: number;
      taxAmount?: number;
    }>;
  }): Promise<number> => {
    const response = await api.post('/finance/bills', bill);
    return response.data;
  },
};

