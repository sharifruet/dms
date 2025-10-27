import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

export interface SearchFilters {
  documentTypes?: string[];
  departments?: string[];
  uploadedBy?: string[];
  startDate?: string;
  endDate?: string;
  minOcrConfidence?: number;
  isActive?: boolean;
}

export interface SearchResultItem {
  documentId: number;
  fileName: string;
  originalName: string;
  documentType: string;
  description: string;
  department: string;
  uploadedBy: string;
  createdAt: string;
  ocrConfidence: number;
  classificationConfidence: number;
  score: number;
  highlights?: { [key: string]: string[] };
}

export interface SearchResult {
  totalHits: number;
  maxScore: number;
  items: SearchResultItem[];
  pageNumber: number;
  pageSize: number;
  totalPages: number;
}

export interface SearchStatistics {
  totalDocuments: number;
  activeDocuments: number;
  documentTypeCounts: { [key: string]: number };
}

class SearchService {
  private getAuthHeaders() {
    const token = localStorage.getItem('token');
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    };
  }

  async searchDocuments(
    query?: string,
    filters?: SearchFilters,
    page: number = 0,
    size: number = 20
  ): Promise<SearchResult> {
    const params = new URLSearchParams();
    
    if (query) params.append('query', query);
    if (filters?.documentTypes) {
      filters.documentTypes.forEach(type => params.append('documentTypes', type));
    }
    if (filters?.departments) {
      filters.departments.forEach(dept => params.append('departments', dept));
    }
    if (filters?.uploadedBy) {
      filters.uploadedBy.forEach(user => params.append('uploadedBy', user));
    }
    if (filters?.startDate) params.append('startDate', filters.startDate);
    if (filters?.endDate) params.append('endDate', filters.endDate);
    if (filters?.minOcrConfidence !== undefined) {
      params.append('minOcrConfidence', filters.minOcrConfidence.toString());
    }
    if (filters?.isActive !== undefined) {
      params.append('isActive', filters.isActive.toString());
    }
    
    params.append('page', page.toString());
    params.append('size', size.toString());

    const response = await axios.get(
      `${API_BASE_URL}/search/documents?${params.toString()}`,
      { headers: this.getAuthHeaders() }
    );
    
    return response.data;
  }

  async getSuggestions(prefix: string, limit: number = 10): Promise<string[]> {
    const response = await axios.get(
      `${API_BASE_URL}/search/suggestions?prefix=${encodeURIComponent(prefix)}&limit=${limit}`,
      { headers: this.getAuthHeaders() }
    );
    
    return response.data;
  }

  async getSearchStatistics(): Promise<SearchStatistics> {
    const response = await axios.get(
      `${API_BASE_URL}/search/statistics`,
      { headers: this.getAuthHeaders() }
    );
    
    return response.data;
  }

  async advancedSearch(
    request: {
      query?: string;
      documentTypes?: string[];
      departments?: string[];
      uploadedBy?: string[];
      startDate?: string;
      endDate?: string;
      minOcrConfidence?: number;
      isActive?: boolean;
      sortBy?: string;
      sortDirection?: string;
    },
    page: number = 0,
    size: number = 20
  ): Promise<SearchResult> {
    const response = await axios.post(
      `${API_BASE_URL}/search/advanced?page=${page}&size=${size}`,
      request,
      { headers: this.getAuthHeaders() }
    );
    
    return response.data;
  }

  async findSimilarDocuments(documentId: number, limit: number = 10): Promise<SearchResultItem[]> {
    const response = await axios.get(
      `${API_BASE_URL}/search/similar/${documentId}?limit=${limit}`,
      { headers: this.getAuthHeaders() }
    );
    
    return response.data;
  }

  async reindexAllDocuments(): Promise<{ message: string }> {
    const response = await axios.post(
      `${API_BASE_URL}/search/reindex`,
      {},
      { headers: this.getAuthHeaders() }
    );
    
    return response.data;
  }
}

export const searchService = new SearchService();
