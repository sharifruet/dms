import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

class ReportingService {
  private getAuthHeaders() {
    const token = localStorage.getItem('token');
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    };
  }

  async createReport(report: CreateReportRequest) {
    try {
      const response = await axios.post(`${API_BASE_URL}/reports`, report, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error creating report:', error);
      throw error;
    }
  }

  async getReports(page = 0, size = 20) {
    try {
      const response = await axios.get(`${API_BASE_URL}/reports`, {
        headers: this.getAuthHeaders(),
        params: { page, size }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching reports:', error);
      throw error;
    }
  }

  async getPublicReports(page = 0, size = 20) {
    try {
      const response = await axios.get(`${API_BASE_URL}/reports/public`, {
        headers: this.getAuthHeaders(),
        params: { page, size }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching public reports:', error);
      throw error;
    }
  }

  async getReport(reportId: number) {
    try {
      const response = await axios.get(`${API_BASE_URL}/reports/${reportId}`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching report:', error);
      throw error;
    }
  }

  async getDocumentSummaryData(parameters: Record<string, any> = {}) {
    try {
      const response = await axios.get(`${API_BASE_URL}/reports/data/document-summary`, {
        headers: this.getAuthHeaders(),
        params: parameters
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching document summary data:', error);
      throw error;
    }
  }

  async getUserActivityData(parameters: Record<string, any> = {}) {
    try {
      const response = await axios.get(`${API_BASE_URL}/reports/data/user-activity`, {
        headers: this.getAuthHeaders(),
        params: parameters
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching user activity data:', error);
      throw error;
    }
  }

  async getExpiryReportData(parameters: Record<string, any> = {}) {
    try {
      const response = await axios.get(`${API_BASE_URL}/reports/data/expiry-report`, {
        headers: this.getAuthHeaders(),
        params: parameters
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching expiry report data:', error);
      throw error;
    }
  }

  async getSystemPerformanceData(parameters: Record<string, any> = {}) {
    try {
      const response = await axios.get(`${API_BASE_URL}/reports/data/system-performance`, {
        headers: this.getAuthHeaders(),
        params: parameters
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching system performance data:', error);
      throw error;
    }
  }

  async getAnalyticsData(metricType: string, dimensionKey: string) {
    try {
      const response = await axios.get(`${API_BASE_URL}/reports/analytics`, {
        headers: this.getAuthHeaders(),
        params: { metricType, dimensionKey }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching analytics data:', error);
      throw error;
    }
  }

  async recordAnalytics(analytics: RecordAnalyticsRequest) {
    try {
      const response = await axios.post(`${API_BASE_URL}/reports/analytics`, analytics, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error recording analytics:', error);
      throw error;
    }
  }
}

export interface Report {
  id: number;
  name: string;
  description?: string;
  type: ReportType;
  format: ReportFormat;
  status: ReportStatus;
  createdBy: number;
  parameters?: string;
  filePath?: string;
  fileSize?: number;
  generatedAt?: string;
  expiresAt?: string;
  isScheduled: boolean;
  scheduleCron?: string;
  lastGeneratedAt?: string;
  nextGenerationAt?: string;
  isPublic: boolean;
  accessCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReportRequest {
  name: string;
  description?: string;
  type: ReportType;
  format: ReportFormat;
  parameters?: Record<string, any>;
}

export interface RecordAnalyticsRequest {
  metricType: MetricType;
  metricName: string;
  metricValue: number;
  dimensionKey?: string;
  dimensionValue?: string;
}

export enum ReportType {
  DOCUMENT_SUMMARY = 'DOCUMENT_SUMMARY',
  USER_ACTIVITY = 'USER_ACTIVITY',
  EXPIRY_REPORT = 'EXPIRY_REPORT',
  DEPARTMENT_SUMMARY = 'DEPARTMENT_SUMMARY',
  VENDOR_SUMMARY = 'VENDOR_SUMMARY',
  AUDIT_REPORT = 'AUDIT_REPORT',
  SYSTEM_PERFORMANCE = 'SYSTEM_PERFORMANCE',
  STORAGE_UTILIZATION = 'STORAGE_UTILIZATION',
  COMPLIANCE_REPORT = 'COMPLIANCE_REPORT',
  CUSTOM_REPORT = 'CUSTOM_REPORT'
}

export enum ReportFormat {
  PDF = 'PDF',
  EXCEL = 'EXCEL',
  WORD = 'WORD',
  CSV = 'CSV',
  JSON = 'JSON',
  HTML = 'HTML'
}

export enum ReportStatus {
  PENDING = 'PENDING',
  GENERATING = 'GENERATING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED'
}

export enum MetricType {
  DOCUMENT_COUNT = 'DOCUMENT_COUNT',
  USER_ACTIVITY = 'USER_ACTIVITY',
  STORAGE_USAGE = 'STORAGE_USAGE',
  SYSTEM_PERFORMANCE = 'SYSTEM_PERFORMANCE',
  EXPIRY_METRICS = 'EXPIRY_METRICS',
  SEARCH_METRICS = 'SEARCH_METRICS',
  UPLOAD_METRICS = 'UPLOAD_METRICS',
  ACCESS_METRICS = 'ACCESS_METRICS',
  COMPLIANCE_METRICS = 'COMPLIANCE_METRICS',
  CUSTOM_METRIC = 'CUSTOM_METRIC'
}

export default new ReportingService();
