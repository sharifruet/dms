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
  getBills: async (fiscalYear?: number, vendor?: string): Promise<BillHeader[]> => {
    const params = new URLSearchParams();
    if (fiscalYear) params.append('fiscalYear', fiscalYear.toString());
    if (vendor) params.append('vendor', vendor);
    const response = await api.get(`/finance/bills?${params.toString()}`);
    return response.data;
  },

  // Get a single bill by ID
  getBill: async (id: number): Promise<BillHeader> => {
    const response = await api.get(`/finance/bills/${id}`);
    return response.data;
  },

  // Create a new bill
  createBill: async (bill: {
    fiscalYear: number;
    vendor?: string;
    invoiceNumber?: string;
    invoiceDate?: string;
    lines?: Array<{
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

