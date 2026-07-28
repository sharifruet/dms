import api from './api';

export interface ProcurementPackage {
  id: number;
  documentId?: number;
  packageNo: string;
  description?: string;
  procurementMethod?: string;
  sourceOfFund?: string;
  status?: string;
}

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const procurementPackageService = {
  getPackages: async (
    page = 0,
    size = 100,
    sortBy = 'id',
    sortDir: 'asc' | 'desc' = 'asc'
  ): Promise<PaginatedResponse<ProcurementPackage>> => {
    const response = await api.get<PaginatedResponse<ProcurementPackage>>(
      `/procurement-packages?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`
    );
    return response.data;
  },
};

export default procurementPackageService;
