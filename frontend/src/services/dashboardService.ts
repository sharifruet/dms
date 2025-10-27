import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

class DashboardService {
  private getAuthHeaders() {
    const token = localStorage.getItem('token');
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    };
  }

  async createDashboard(dashboard: CreateDashboardRequest) {
    try {
      const response = await axios.post(`${API_BASE_URL}/dashboards`, dashboard, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error creating dashboard:', error);
      throw error;
    }
  }

  async getDashboards(page = 0, size = 20) {
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboards`, {
        headers: this.getAuthHeaders(),
        params: { page, size }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching dashboards:', error);
      throw error;
    }
  }

  async getDashboard(dashboardId: number) {
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboards/${dashboardId}`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching dashboard:', error);
      throw error;
    }
  }

  async getExecutiveDashboardData() {
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboards/data/executive`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching executive dashboard data:', error);
      throw error;
    }
  }

  async getDepartmentDashboardData(department: string) {
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboards/data/department`, {
        headers: this.getAuthHeaders(),
        params: { department }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching department dashboard data:', error);
      throw error;
    }
  }

  async getUserDashboardData() {
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboards/data/user`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching user dashboard data:', error);
      throw error;
    }
  }

  async getSystemDashboardData() {
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboards/data/system`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching system dashboard data:', error);
      throw error;
    }
  }

  async getComplianceDashboardData() {
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboards/data/compliance`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching compliance dashboard data:', error);
      throw error;
    }
  }

  async getAnalyticsData(metricType: string, dimensionKey: string, days = 30) {
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboards/analytics`, {
        headers: this.getAuthHeaders(),
        params: { metricType, dimensionKey, days }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching analytics data:', error);
      throw error;
    }
  }

  async getDashboardWidgets(type: string) {
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboards/widgets/${type}`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching dashboard widgets:', error);
      throw error;
    }
  }

  async generateDashboardReport(dashboardId: number, format: string) {
    try {
      const response = await axios.post(`${API_BASE_URL}/dashboards/${dashboardId}/report`, null, {
        headers: this.getAuthHeaders(),
        params: { format }
      });
      return response.data;
    } catch (error) {
      console.error('Error generating dashboard report:', error);
      throw error;
    }
  }

  async getDefaultDashboard(type: string) {
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboards/default/${type}`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching default dashboard:', error);
      throw error;
    }
  }
}

export interface Dashboard {
  id: number;
  name: string;
  description?: string;
  type: DashboardType;
  createdBy: number;
  layoutConfig?: string;
  widgetsConfig?: string;
  refreshInterval?: number;
  isPublic: boolean;
  isDefault: boolean;
  accessCount: number;
  lastAccessedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDashboardRequest {
  name: string;
  description?: string;
  type: DashboardType;
  layoutConfig?: string;
  widgetsConfig?: string;
}

export enum DashboardType {
  EXECUTIVE = 'EXECUTIVE',
  DEPARTMENT = 'DEPARTMENT',
  USER = 'USER',
  SYSTEM = 'SYSTEM',
  COMPLIANCE = 'COMPLIANCE',
  CUSTOM = 'CUSTOM'
}

export default new DashboardService();
