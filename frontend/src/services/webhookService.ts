import api from './api';

export interface Webhook {
  id: number;
  name: string;
  description: string;
  url: string;
  eventType: string;
  secretKey: string;
  status: string;
  isEnabled: boolean;
  retryCount: number;
  timeoutSeconds: number;
  headers: string;
  payloadTemplate: string;
  createdBy: {
    id: number;
    username: string;
  };
  lastTriggeredAt: string;
  successCount: number;
  failureCount: number;
  lastError: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWebhookRequest {
  name: string;
  description: string;
  url: string;
  eventType: string;
}

export interface UpdateWebhookRequest {
  name: string;
  description: string;
  url: string;
  eventType: string;
  isEnabled: boolean;
}

export interface WebhookStatistics {
  totalWebhooks: number;
  activeWebhooks: number;
  enabledWebhooks: number;
  inactiveWebhooks: number;
}

export const webhookService = {
  // Create a new webhook
  createWebhook: async (request: CreateWebhookRequest): Promise<Webhook> => {
    const response = await api.post('/webhooks', request);
    return response.data;
  },

  // Update webhook
  updateWebhook: async (webhookId: number, request: UpdateWebhookRequest): Promise<Webhook> => {
    const response = await api.put(`/webhooks/${webhookId}`, request);
    return response.data;
  },

  // Delete webhook
  deleteWebhook: async (webhookId: number): Promise<void> => {
    await api.delete(`/webhooks/${webhookId}`);
  },

  // Test webhook
  testWebhook: async (webhookId: number): Promise<boolean> => {
    const response = await api.post(`/webhooks/${webhookId}/test`);
    return response.data;
  },

  // Get webhook statistics
  getWebhookStatistics: async (): Promise<WebhookStatistics> => {
    const response = await api.get('/webhooks/statistics');
    return response.data;
  },

  // Get webhooks by event type
  getWebhooksByEventType: async (eventType: string): Promise<Webhook[]> => {
    const response = await api.get(`/webhooks/event-type/${eventType}`);
    return response.data;
  },

  // Get webhooks for user
  getWebhooksForUser: async (page: number = 0, size: number = 10): Promise<{
    content: Webhook[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  }> => {
    const response = await api.get(`/webhooks?page=${page}&size=${size}`);
    return response.data;
  },

  // Get all webhooks
  getAllWebhooks: async (): Promise<Webhook[]> => {
    const response = await api.get('/webhooks/all');
    return response.data;
  },

  // Enable/disable webhook
  toggleWebhookStatus: async (webhookId: number, isEnabled: boolean): Promise<Webhook> => {
    const response = await api.patch(`/webhooks/${webhookId}/toggle`, { isEnabled });
    return response.data;
  },

  // Get webhook logs
  getWebhookLogs: async (webhookId: number, page: number = 0, size: number = 10): Promise<{
    content: any[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  }> => {
    const response = await api.get(`/webhooks/${webhookId}/logs?page=${page}&size=${size}`);
    return response.data;
  }
};
