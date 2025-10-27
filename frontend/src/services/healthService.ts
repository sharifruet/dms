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

export interface SystemHealthCheck {
  id?: number;
  checkName: string;
  checkType: string;
  status: string;
  component?: string;
  service?: string;
  environment?: string;
  thresholdValue?: number;
  actualValue?: number;
  responseTimeMs?: number;
  errorMessage?: string;
  checkData?: string;
  severity: string;
  retryCount?: number;
  maxRetries?: number;
  checkIntervalSeconds?: number;
  nextCheckAt?: string;
  executedAt?: string;
  isEnabled: boolean;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface SystemHealthOverview {
  overallHealthScore: number;
  statusDistribution: { [key: string]: number };
  criticalIssues: number;
  componentHealth: { [key: string]: any };
  serviceHealth: { [key: string]: any };
}

export interface HealthCheckStatistics {
  totalChecks: number;
  healthyChecks: number;
  warningChecks: number;
  criticalChecks: number;
  failedChecks: number;
  enabledChecks: number;
}

export interface HealthCheckExecutionResult {
  status: string;
  responseTime: number;
  actualValue: number;
  errorMessage?: string;
  severity: string;
}

export interface ComponentHealth {
  healthScore: number;
  totalChecks: number;
  healthyChecks: number;
  warningChecks: number;
  criticalChecks: number;
  failedChecks: number;
}

export interface ServiceHealth {
  healthScore: number;
  totalChecks: number;
  healthyChecks: number;
  warningChecks: number;
  criticalChecks: number;
  failedChecks: number;
}

export const healthService = {
  // Create a new health check
  createHealthCheck: async (healthCheck: SystemHealthCheck): Promise<SystemHealthCheck> => {
    const response = await api.post('/health/checks', healthCheck);
    return response.data;
  },

  // Get all health checks
  getHealthChecks: async (params?: {
    checkType?: string;
    status?: string;
    component?: string;
    service?: string;
    severity?: string;
    page?: number;
    size?: number;
  }): Promise<{ content: SystemHealthCheck[]; totalElements: number; totalPages: number }> => {
    const response = await api.get('/health/checks', { params });
    return response.data;
  },

  // Get health check by ID
  getHealthCheckById: async (id: number): Promise<SystemHealthCheck> => {
    const response = await api.get(`/health/checks/${id}`);
    return response.data;
  },

  // Execute health check
  executeHealthCheck: async (id: number): Promise<HealthCheckExecutionResult> => {
    const response = await api.post(`/health/checks/${id}/execute`);
    return response.data;
  },

  // Get system health overview
  getSystemHealthOverview: async (): Promise<SystemHealthOverview> => {
    const response = await api.get('/health/overview');
    return response.data;
  },

  // Get health check statistics
  getHealthCheckStatistics: async (): Promise<HealthCheckStatistics> => {
    const response = await api.get('/health/statistics');
    return response.data;
  },

  // Get health check history
  getHealthCheckHistory: async (id: number, params?: {
    startDate?: string;
    endDate?: string;
  }): Promise<SystemHealthCheck[]> => {
    const response = await api.get(`/health/checks/${id}/history`, { params });
    return response.data;
  },

  // Get component health
  getComponentHealth: async (component: string): Promise<ComponentHealth> => {
    const response = await api.get(`/health/components/${component}`);
    return response.data;
  },

  // Get service health
  getServiceHealth: async (service: string): Promise<ServiceHealth> => {
    const response = await api.get(`/health/services/${service}`);
    return response.data;
  },

  // Get critical health alerts
  getCriticalHealthAlerts: async (): Promise<SystemHealthCheck[]> => {
    const response = await api.get('/health/alerts');
    return response.data;
  },

  // Acknowledge health alert
  acknowledgeHealthAlert: async (id: number): Promise<void> => {
    await api.post(`/health/alerts/${id}/acknowledge`);
  },

  // Get health check types
  getHealthCheckTypes: async (): Promise<string[]> => {
    const response = await api.get('/health/check-types');
    return response.data;
  },

  // Get health check components
  getHealthCheckComponents: async (): Promise<string[]> => {
    const response = await api.get('/health/components');
    return response.data;
  },

  // Get health check services
  getHealthCheckServices: async (): Promise<string[]> => {
    const response = await api.get('/health/services');
    return response.data;
  },

  // Get health check environments
  getHealthCheckEnvironments: async (): Promise<string[]> => {
    const response = await api.get('/health/environments');
    return response.data;
  },

  // Get real-time health status
  getRealTimeHealthStatus: async (): Promise<{
    checks: SystemHealthCheck[];
    lastUpdated: string;
    overallStatus: string;
  }> => {
    const response = await api.get('/health/realtime');
    return response.data;
  }
};

export default healthService;
