import api from './api';
import {
  AppImportReport,
  BerBidder,
  BudgetEntry,
  Delivery,
  DepartmentBudget,
  DepartmentBudgetPosition,
  ExtractedField,
  ExtractedFieldHistory,
  InspectionEvent,
  Invoice,
  Payment,
  ProcurementPackage,
  StageDetail,
  StageReadiness,
  Tender,
  UploadResult,
} from '../types/procurement';

export interface PackagePage {
  content: ProcurementPackage[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PackageQuery {
  query?: string;
  stage?: number;
  status?: string;
  department?: string;
  page?: number;
  size?: number;
}

const procurementService = {
  // ----------------------------------------------------------------- packages

  listPackages: async (params: PackageQuery = {}): Promise<PackagePage> => {
    const response = await api.get('/procurement/packages', { params });
    return response.data;
  },

  getPackage: async (id: number): Promise<ProcurementPackage> => {
    const response = await api.get(`/procurement/packages/${id}`);
    return response.data;
  },

  createPackage: async (pkg: Partial<ProcurementPackage>): Promise<ProcurementPackage> => {
    const response = await api.post('/procurement/packages', pkg);
    return response.data;
  },

  createFromAppLine: async (appLineId: number, lotNumbers: string[] = []): Promise<ProcurementPackage[]> => {
    const response = await api.post(`/procurement/packages/from-app-line/${appLineId}`, lotNumbers);
    return response.data;
  },

  getProgress: async (packageId: number): Promise<StageReadiness[]> => {
    const response = await api.get(`/procurement/packages/${packageId}/progress`);
    return response.data;
  },

  getGraph: async (packageId: number): Promise<Record<string, any>> => {
    const response = await api.get(`/procurement/packages/${packageId}/graph`);
    return response.data;
  },

  getDashboard: async (): Promise<Record<string, any>> => {
    const response = await api.get('/procurement/packages/dashboard');
    return response.data;
  },

  // ------------------------------------------------------------------- stages

  getStage: async (packageId: number, stageCode: number): Promise<StageDetail> => {
    const response = await api.get(`/procurement/packages/${packageId}/stages/${stageCode}`);
    return response.data;
  },

  saveStageFields: async (
    packageId: number,
    stageCode: number,
    values: Record<string, string>,
  ): Promise<ExtractedField[]> => {
    const response = await api.put(
      `/procurement/packages/${packageId}/stages/${stageCode}/fields`,
      values,
    );
    return response.data;
  },

  /**
   * Complete a stage. A 409 carries the readiness object explaining what is blocking,
   * which is what the UI shows rather than a generic failure.
   */
  completeStage: async (packageId: number, stageCode: number, overrideReason?: string) => {
    const response = await api.post(
      `/procurement/packages/${packageId}/stages/${stageCode}/complete`,
      overrideReason ? { overrideReason } : {},
    );
    return response.data;
  },

  markNotApplicable: async (packageId: number, stageCode: number, reason: string) => {
    const response = await api.post(
      `/procurement/packages/${packageId}/stages/${stageCode}/not-applicable`,
      { reason },
    );
    return response.data;
  },

  reworkStage: async (packageId: number, stageCode: number, reason: string) => {
    const response = await api.post(
      `/procurement/packages/${packageId}/stages/${stageCode}/rework`,
      { reason },
    );
    return response.data;
  },

  // ------------------------------------------------------------ repeating rows

  saveBidders: async (packageId: number, bidders: BerBidder[]): Promise<BerBidder[]> => {
    const response = await api.put(`/procurement/packages/${packageId}/stages/4/bidders`, bidders);
    return response.data;
  },

  saveInspection: async (packageId: number, event: InspectionEvent): Promise<InspectionEvent> => {
    const response = await api.post(`/procurement/packages/${packageId}/stages/11/inspections`, event);
    return response.data;
  },

  saveDelivery: async (packageId: number, delivery: Delivery): Promise<Delivery> => {
    const response = await api.post(`/procurement/packages/${packageId}/stages/12/deliveries`, delivery);
    return response.data;
  },

  saveInvoice: async (packageId: number, invoice: Invoice, deliveryIds: number[]): Promise<Invoice> => {
    const response = await api.post(`/procurement/packages/${packageId}/stages/13/invoices`, {
      invoice,
      deliveryIds,
    });
    return response.data;
  },

  savePayment: async (packageId: number, payment: Payment, invoiceIds: number[]): Promise<Payment> => {
    const response = await api.post(`/procurement/packages/${packageId}/stages/14/payments`, {
      payment,
      invoiceIds,
    });
    return response.data;
  },

  // ------------------------------------------------------------------- capture

  verifyField: async (fieldId: number): Promise<ExtractedField> => {
    const response = await api.put(`/procurement/fields/${fieldId}/verify`);
    return response.data;
  },

  overrideField: async (fieldId: number, value: string, reason?: string): Promise<ExtractedField> => {
    const response = await api.put(`/procurement/fields/${fieldId}/override`, { value, reason });
    return response.data;
  },

  rejectField: async (fieldId: number, reason: string): Promise<ExtractedField> => {
    const response = await api.put(`/procurement/fields/${fieldId}/reject`, { reason });
    return response.data;
  },

  bulkVerify: async (packageId: number, stageCode: number): Promise<{ verified: number }> => {
    const response = await api.post('/procurement/fields/bulk-verify', { packageId, stageCode });
    return response.data;
  },

  fieldHistory: async (fieldId: number): Promise<ExtractedFieldHistory[]> => {
    const response = await api.get(`/procurement/fields/${fieldId}/history`);
    return response.data;
  },

  // ----------------------------------------------------------------- documents

  uploadDocument: async (
    file: File,
    packageId: number,
    stageCode: number,
    docRole: string,
  ): Promise<UploadResult> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/procurement/documents/upload', formData, {
      params: { packageId, stageCode, docRole },
    });
    return response.data;
  },

  getOcr: async (documentId: number): Promise<Record<string, any>> => {
    const response = await api.get(`/procurement/documents/${documentId}/ocr`);
    return response.data;
  },

  // ------------------------------------------------------------- re-tendering

  /**
   * Declare the current tender failed and open a fresh attempt under the same package
   * (Q-2). The failed attempt keeps its documents, bidders and BER.
   */
  reTender: async (packageId: number, reason: string): Promise<Tender> => {
    const response = await api.post(
      `/procurement/packages/${packageId}/stages/2/re-tender`,
      { reason },
    );
    return response.data;
  },

  /** Every tender attempt for the package, newest first. */
  getTenderAttempts: async (packageId: number): Promise<Tender[]> => {
    const response = await api.get(`/procurement/packages/${packageId}/stages/2/attempts`);
    return response.data;
  },

  // -------------------------------------------------------- budget and expiry

  getBudget: async (packageId: number): Promise<Record<string, any>> => {
    const response = await api.get(`/procurement/packages/${packageId}/budget`);
    return response.data;
  },

  addBudgetEntry: async (packageId: number, entry: BudgetEntry): Promise<BudgetEntry> => {
    const response = await api.post(`/procurement/packages/${packageId}/budget`, entry);
    return response.data;
  },

  /** The department's annual drawdown position - allocated, committed, remaining. */
  getDepartmentBudget: async (
    fiscalYear: number,
    department: string,
  ): Promise<DepartmentBudgetPosition> => {
    const response = await api.get('/procurement/department-budgets', {
      params: { fiscalYear, department },
    });
    return response.data;
  },

  /** Set a department's annual budget. Checker-only (Q-13, Q-17). */
  saveDepartmentBudget: async (budget: DepartmentBudget): Promise<DepartmentBudget> => {
    const response = await api.post('/procurement/department-budgets', budget);
    return response.data;
  },

  // ------------------------------------------------------------- APP import

  /**
   * Create packages in bulk from an APP workbook (Stage 1, REQ-1.1).
   * With dryRun the report is identical but nothing is written.
   */
  importApp: async (
    file: File,
    options: { department?: string; dryRun?: boolean } = {},
  ): Promise<AppImportReport> => {
    const form = new FormData();
    form.append('file', file);
    if (options.department) {
      form.append('department', options.department);
    }
    form.append('dryRun', String(options.dryRun ?? false));
    const response = await api.post('/procurement/packages/import-app', form);
    return response.data;
  },

  getExpiries: async (withinDays = 90) => {
    const response = await api.get('/procurement/expiries', { params: { withinDays } });
    return response.data;
  },

  getPackageExpiries: async (packageId: number) => {
    const response = await api.get(`/procurement/packages/${packageId}/expiries`);
    return response.data;
  },

  supersedeExpiry: async (id: number, expiryDate: string, reason: string) => {
    const response = await api.post(`/procurement/expiries/${id}/supersede`, { expiryDate, reason });
    return response.data;
  },

  getExceptions: async (): Promise<Record<string, any>> => {
    const response = await api.get('/procurement/exceptions');
    return response.data;
  },
};

export default procurementService;
