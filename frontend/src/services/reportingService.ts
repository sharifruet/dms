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

  // getAnalyticsData / recordAnalytics are gone with the /reports/analytics endpoints and
  // the metric store behind them - no requirement asked for a generic analytics sink.
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

export default new ReportingService();
