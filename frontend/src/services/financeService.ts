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

export interface AppBudgetSummary {
  appId: number;
  fiscalYear: number;
  releaseInstallmentNo?: number;
  allocationType?: string;
  allocationAmount: number;
  totalBilled: number;
  remaining: number;
  utilizationPct: number;
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

  // Get per-APP budget vs billed summary
  getBudgetByApp: async (): Promise<AppBudgetSummary[]> => {
    const response = await api.get('/finance/dashboard/budget-by-app');
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

  // Extract bill information from uploaded file using OCR (Phase 3)
  extractBillOCR: async (file: File): Promise<BillOCRResult> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/finance/bills/extract-ocr', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.ocrResult;
  },
};

export interface BillOCRResult {
  success: boolean;
  errorMessage?: string;
  extractedText?: string;
  vendorName?: string;
  vendorNameConfidence?: number;
  invoiceNumber?: string;
  invoiceNumberConfidence?: number;
  invoiceDate?: string;
  invoiceDateConfidence?: number;
  fiscalYear?: number;
  fiscalYearConfidence?: number;
  totalAmount?: number;
  totalAmountConfidence?: number;
  taxAmount?: number;
  taxAmountConfidence?: number;
  subtotalAmount?: number;
  subtotalAmountConfidence?: number;
  lineItems?: BillLineItemOCR[];
  overallConfidence?: number;
  originalValues?: {
    vendorName?: string;
    invoiceNumber?: string;
    invoiceDate?: string;
    fiscalYear?: number;
    totalAmount?: number;
    taxAmount?: number;
  };
}

export interface BillLineItemOCR {
  projectIdentifier?: string;
  department?: string;
  costCenter?: string;
  category?: string;
  description?: string;
  amount?: number;
  taxAmount?: number;
  confidence?: number;
}

