import api from './api';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ProductCategory {
  id?: number;
  parent?: ProductCategory | null;
  name: string;
  code?: string;
  active?: boolean;
}

export interface Product {
  id?: number;
  category: ProductCategory;
  name: string;
  sku?: string;
  brand?: string;
  model?: string;
  specsJson?: string;
  defaultWarrantyMonths?: number;
  active?: boolean;
}

export interface Asset {
  id?: number;
  product: Product;
  serialNo?: string;
  assetTag: string;
  status?: string;
  location?: string;
  warrantyStart?: string;
  warrantyEnd?: string;
  acquisitionCost?: number;
  customJson?: string;
}

export interface AssetAssignment {
  id?: number;
  asset: Asset;
  user: { id: number };
  startDate: string;
  endDate?: string;
  expectedReturnDate?: string;
  status?: string;
}

export const AssetAPI = {
  listAssets: (params: { page?: number; size?: number; sortBy?: string; sortDir?: 'asc' | 'desc' } = {}) =>
    api.get<Page<Asset>>('/assets', { params }),
  getAsset: (id: number) => api.get<Asset>(`/assets/${id}`),
  createAsset: (data: Asset) => api.post<Asset>('/assets', data),
  updateAsset: (id: number, data: Asset) => api.put<Asset>(`/assets/${id}`, data),
  deleteAsset: (id: number) => api.delete<void>(`/assets/${id}`),
};

export const ProductAPI = {
  listProducts: (params: { page?: number; size?: number; sortBy?: string; sortDir?: 'asc' | 'desc' } = {}) =>
    api.get<Page<Product>>('/products', { params }),
  getProduct: (id: number) => api.get<Product>(`/products/${id}`),
  createProduct: (data: Product) => api.post<Product>('/products', data),
  updateProduct: (id: number, data: Product) => api.put<Product>(`/products/${id}`, data),
  deleteProduct: (id: number) => api.delete<void>(`/products/${id}`),
};

export const CategoryAPI = {
  listCategories: (params: { page?: number; size?: number; sortBy?: string; sortDir?: 'asc' | 'desc' } = {}) =>
    api.get<Page<ProductCategory>>('/product-categories', { params }),
  getCategory: (id: number) => api.get<ProductCategory>(`/product-categories/${id}`),
  createCategory: (data: ProductCategory) => api.post<ProductCategory>('/product-categories', data),
  updateCategory: (id: number, data: ProductCategory) => api.put<ProductCategory>(`/product-categories/${id}`, data),
  deleteCategory: (id: number) => api.delete<void>(`/product-categories/${id}`),
};

export const AssignmentAPI = {
  listAssignments: (params: { page?: number; size?: number; sortBy?: string; sortDir?: 'asc' | 'desc' } = {}) =>
    api.get<Page<AssetAssignment>>('/asset-assignments', { params }),
  getAssignment: (id: number) => api.get<AssetAssignment>(`/asset-assignments/${id}`),
  createAssignment: (data: AssetAssignment) => api.post<AssetAssignment>('/asset-assignments', data),
  updateAssignment: (id: number, data: AssetAssignment) => api.put<AssetAssignment>(`/asset-assignments/${id}`, data),
  deleteAssignment: (id: number) => api.delete<void>(`/asset-assignments/${id}`),
};
