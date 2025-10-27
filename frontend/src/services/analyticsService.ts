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

export interface AnalyticsData {
  id?: number;
  analyticsType: string;
  metricName: string;
  metricValue?: number;
  dimensions?: string;
  tags?: string;
  timestamp?: string;
  userId?: string;
  sessionId?: string;
  ipAddress?: string;
  userAgent?: string;
  createdAt?: string;
}

export interface AnalyticsInsights {
  totalMetrics: number;
  averageValue: number;
  trend: string;
  insights: string[];
  recommendations: string[];
}

export interface AnalyticsTrend {
  timestamp: string;
  value: number;
  label: string;
}

export interface AnalyticsAggregation {
  sum: number;
  average: number;
  min: number;
  max: number;
  count: number;
}

export interface AnalyticsComparison {
  currentPeriod: {
    value: number;
    label: string;
  };
  previousPeriod: {
    value: number;
    label: string;
  };
  change: number;
  changePercentage: number;
}

export interface AnalyticsStatistics {
  totalDataPoints: number;
  uniqueMetrics: number;
  activeUsers: number;
  dataRetentionDays: number;
}

export const analyticsService = {
  // Create analytics data
  createAnalyticsData: async (data: AnalyticsData): Promise<AnalyticsData> => {
    const response = await api.post('/analytics', data);
    return response.data;
  },

  // Get analytics data
  getAnalyticsData: async (params?: {
    analyticsType?: string;
    metricName?: string;
    startDate?: string;
    endDate?: string;
    page?: number;
    size?: number;
  }): Promise<{ content: AnalyticsData[]; totalElements: number; totalPages: number }> => {
    const response = await api.get('/analytics', { params });
    return response.data;
  },

  // Get analytics insights
  getAnalyticsInsights: async (params?: {
    analyticsType?: string;
    metricName?: string;
    startDate?: string;
    endDate?: string;
  }): Promise<AnalyticsInsights> => {
    const response = await api.get('/analytics/insights', { params });
    return response.data;
  },

  // Get analytics trends
  getAnalyticsTrends: async (params: {
    metricName: string;
    startDate?: string;
    endDate?: string;
    granularity?: string;
  }): Promise<AnalyticsTrend[]> => {
    const response = await api.get('/analytics/trends', { params });
    return response.data;
  },

  // Get analytics aggregations
  getAnalyticsAggregations: async (params: {
    metricName: string;
    startDate?: string;
    endDate?: string;
    aggregationType?: string;
  }): Promise<AnalyticsAggregation> => {
    const response = await api.get('/analytics/aggregations', { params });
    return response.data;
  },

  // Get analytics comparisons
  getAnalyticsComparisons: async (params: {
    metricName: string;
    startDate: string;
    endDate: string;
    comparisonPeriod: string;
  }): Promise<AnalyticsComparison> => {
    const response = await api.get('/analytics/comparisons', { params });
    return response.data;
  },

  // Get analytics statistics
  getAnalyticsStatistics: async (): Promise<AnalyticsStatistics> => {
    const response = await api.get('/analytics/statistics');
    return response.data;
  },

  // Export analytics data
  exportAnalyticsData: async (params?: {
    analyticsType?: string;
    metricName?: string;
    startDate?: string;
    endDate?: string;
    format?: string;
  }): Promise<{ downloadUrl: string; filename: string; recordCount: number }> => {
    const response = await api.get('/analytics/export', { params });
    return response.data;
  },

  // Get real-time analytics
  getRealTimeAnalytics: async (): Promise<{ metrics: AnalyticsData[]; timestamp: string }> => {
    const response = await api.get('/analytics/realtime');
    return response.data;
  },

  // Get analytics dashboard data
  getDashboardData: async (): Promise<{
    overview: AnalyticsInsights;
    trends: AnalyticsTrend[];
    topMetrics: AnalyticsData[];
    alerts: string[];
  }> => {
    const response = await api.get('/analytics/dashboard');
    return response.data;
  }
};

export default analyticsService;
