import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor to include JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor to handle token expiration
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export interface IntegrationConfig {
  id?: number;
  integrationName: string;
  integrationType: string;
  endpointUrl?: string;
  authenticationType?: string;
  credentials?: string;
  configuration?: string;
  status: string;
  environment?: string;
  lastSyncAt?: string;
  syncFrequencyMinutes?: number;
  retryCount?: number;
  maxRetries?: number;
  timeoutSeconds?: number;
  isEnabled: boolean;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface IntegrationStatistics {
  totalIntegrations: number;
  activeIntegrations: number;
  inactiveIntegrations: number;
  errorIntegrations: number;
  enabledIntegrations: number;
}

export interface IntegrationLog {
  id: number;
  integrationId: number;
  logLevel: string;
  message: string;
  details?: string;
  timestamp: string;
}

export const integrationService = {
  // Create a new integration
  createIntegration: async (integration: IntegrationConfig): Promise<IntegrationConfig> => {
    const response = await api.post('/integrations', integration);
    return response.data;
  },

  // Get all integrations
  getIntegrations: async (params?: {
    integrationType?: string;
    status?: string;
    environment?: string;
    page?: number;
    size?: number;
  }): Promise<{ content: IntegrationConfig[]; totalElements: number; totalPages: number }> => {
    const response = await api.get('/integrations', { params });
    return response.data;
  },

  // Get integration by ID
  getIntegrationById: async (id: number): Promise<IntegrationConfig> => {
    const response = await api.get(`/integrations/${id}`);
    return response.data;
  },

  // Update integration
  updateIntegration: async (id: number, integration: IntegrationConfig): Promise<IntegrationConfig> => {
    const response = await api.put(`/integrations/${id}`, integration);
    return response.data;
  },

  // Delete integration
  deleteIntegration: async (id: number): Promise<void> => {
    await api.delete(`/integrations/${id}`);
  },

  // Test integration connection
  testIntegration: async (id: number): Promise<{ success: boolean; message: string; responseTime?: number }> => {
    const response = await api.post(`/integrations/${id}/test`);
    return response.data;
  },

  // Get integration statistics
  getIntegrationStatistics: async (): Promise<IntegrationStatistics> => {
    const response = await api.get('/integrations/statistics');
    return response.data;
  },

  // Get integration logs
  getIntegrationLogs: async (id: number): Promise<IntegrationLog[]> => {
    const response = await api.get(`/integrations/${id}/logs`);
    return response.data;
  },

  // Sync integration data
  syncIntegration: async (id: number): Promise<{ success: boolean; message: string; recordsProcessed?: number }> => {
    const response = await api.post(`/integrations/${id}/sync`);
    return response.data;
  },

  // Get integration health
  getIntegrationHealth: async (id: number): Promise<{ status: string; lastSync: string; errorCount: number }> => {
    const response = await api.get(`/integrations/${id}/health`);
    return response.data;
  }
};

export default integrationService;
