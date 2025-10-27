import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

class NotificationService {
  private getAuthHeaders() {
    const token = localStorage.getItem('token');
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    };
  }

  async getNotifications(page = 0, size = 20) {
    try {
      const response = await axios.get(`${API_BASE_URL}/notifications`, {
        headers: this.getAuthHeaders(),
        params: { page, size }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching notifications:', error);
      throw error;
    }
  }

  async getUnreadCount() {
    try {
      const response = await axios.get(`${API_BASE_URL}/notifications/unread-count`, {
        headers: this.getAuthHeaders()
      });
      return response.data.unreadCount;
    } catch (error) {
      console.error('Error fetching unread count:', error);
      throw error;
    }
  }

  async getUnreadNotifications() {
    try {
      const response = await axios.get(`${API_BASE_URL}/notifications/unread`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching unread notifications:', error);
      throw error;
    }
  }

  async markAsRead(notificationId: number) {
    try {
      const response = await axios.put(`${API_BASE_URL}/notifications/${notificationId}/read`, {}, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error marking notification as read:', error);
      throw error;
    }
  }

  async createNotification(notification: CreateNotificationRequest) {
    try {
      const response = await axios.post(`${API_BASE_URL}/notifications`, notification, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error creating notification:', error);
      throw error;
    }
  }

  async getNotificationPreferences() {
    try {
      const response = await axios.get(`${API_BASE_URL}/notifications/preferences`, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching notification preferences:', error);
      throw error;
    }
  }

  async updateNotificationPreference(preferenceId: number, preference: NotificationPreference) {
    try {
      const response = await axios.put(`${API_BASE_URL}/notifications/preferences/${preferenceId}`, preference, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error updating notification preference:', error);
      throw error;
    }
  }

  async createDefaultPreferences() {
    try {
      const response = await axios.post(`${API_BASE_URL}/notifications/preferences/default`, {}, {
        headers: this.getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('Error creating default preferences:', error);
      throw error;
    }
  }
}

export interface Notification {
  id: number;
  title: string;
  message: string;
  type: NotificationType;
  priority: NotificationPriority;
  status: NotificationStatus;
  channel?: string;
  relatedDocumentId?: number;
  relatedEntityType?: string;
  scheduledAt?: string;
  sentAt?: string;
  readAt?: string;
  expiresAt?: string;
  metadata?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateNotificationRequest {
  title: string;
  message: string;
  type: NotificationType;
  priority: NotificationPriority;
}

export interface NotificationPreference {
  id: number;
  notificationType: NotificationType;
  emailEnabled: boolean;
  smsEnabled: boolean;
  inAppEnabled: boolean;
  pushEnabled: boolean;
  minimumPriority: NotificationPriority;
  quietHoursStart?: string;
  quietHoursEnd?: string;
  timezone: string;
  language: string;
}

export enum NotificationType {
  DOCUMENT_UPLOAD = 'DOCUMENT_UPLOAD',
  DOCUMENT_EXPIRY = 'DOCUMENT_EXPIRY',
  CONTRACT_EXPIRY = 'CONTRACT_EXPIRY',
  BG_EXPIRY = 'BG_EXPIRY',
  LC_EXPIRY = 'LC_EXPIRY',
  PS_EXPIRY = 'PS_EXPIRY',
  RENEWAL_REMINDER = 'RENEWAL_REMINDER',
  COMPLIANCE_ALERT = 'COMPLIANCE_ALERT',
  SYSTEM_ALERT = 'SYSTEM_ALERT',
  USER_INVITATION = 'USER_INVITATION',
  PASSWORD_RESET = 'PASSWORD_RESET',
  AUDIT_REPORT = 'AUDIT_REPORT',
  BACKUP_COMPLETE = 'BACKUP_COMPLETE',
  SECURITY_ALERT = 'SECURITY_ALERT',
  MAINTENANCE_NOTICE = 'MAINTENANCE_NOTICE'
}

export enum NotificationPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export enum NotificationStatus {
  PENDING = 'PENDING',
  SENT = 'SENT',
  DELIVERED = 'DELIVERED',
  READ = 'READ',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED'
}

export default new NotificationService();
