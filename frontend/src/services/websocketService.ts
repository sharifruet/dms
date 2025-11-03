import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface WebSocketMessage {
  type: string;
  payload: any;
  timestamp: string;
}

export interface NotificationMessage {
  id: number;
  type: string;
  title: string;
  message: string;
  priority: string;
  timestamp: string;
}

export interface DocumentStatusMessage {
  documentId: number;
  fileName: string;
  status: string;
  updatedBy: string;
  timestamp: string;
}

export interface ActivityMessage {
  id: string;
  userId: number;
  username: string;
  action: string;
  resourceType: string;
  resourceId: number;
  resourceName: string;
  timestamp: string;
}

export interface UserPresenceMessage {
  userId: number;
  username: string;
  status: 'online' | 'offline' | 'away';
  timestamp: string;
}

type MessageCallback = (message: any) => void;

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;
  private isConnecting = false;
  private isConnected = false;

  constructor() {
    this.client = null;
  }

  /**
   * Connect to WebSocket server
   */
  connect(token?: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.isConnected) {
        resolve();
        return;
      }

      if (this.isConnecting) {
        return;
      }

      this.isConnecting = true;

      const socketUrl = process.env.REACT_APP_WS_URL || 'http://localhost:8080/ws';
      
      // Create SockJS instance
      const socket = new SockJS(socketUrl);

      // Create STOMP client
      this.client = new Client({
        webSocketFactory: () => socket as any,
        connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
        debug: (str: string) => {
          console.log('[WebSocket Debug]', str);
        },
        reconnectDelay: this.reconnectDelay,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      // Connection success handler
      this.client.onConnect = (frame: any) => {
        console.log('[WebSocket] Connected:', frame);
        this.isConnected = true;
        this.isConnecting = false;
        this.reconnectAttempts = 0;
        resolve();
      };

      // Connection error handler
      this.client.onStompError = (frame: any) => {
        console.error('[WebSocket] STOMP Error:', frame);
        this.isConnecting = false;
        reject(new Error(frame.headers['message'] || 'WebSocket connection error'));
      };

      // Web socket error handler
      this.client.onWebSocketError = (error: any) => {
        console.error('[WebSocket] Connection Error:', error);
        this.isConnecting = false;
        this.handleReconnect();
      };

      // Connection close handler
      this.client.onWebSocketClose = () => {
        console.log('[WebSocket] Connection Closed');
        this.isConnected = false;
        this.handleReconnect();
      };

      // Activate the client
      this.client.activate();
    });
  }

  /**
   * Handle reconnection logic
   */
  private handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`[WebSocket] Reconnecting... Attempt ${this.reconnectAttempts}`);
      
      setTimeout(() => {
        const token = localStorage.getItem('token');
        this.connect(token || undefined).catch(console.error);
      }, this.reconnectDelay * this.reconnectAttempts);
    } else {
      console.error('[WebSocket] Max reconnection attempts reached');
    }
  }

  /**
   * Subscribe to a topic
   */
  subscribe(topic: string, callback: MessageCallback): string {
    if (!this.client || !this.isConnected) {
      console.warn('[WebSocket] Not connected. Cannot subscribe to:', topic);
      return '';
    }

    try {
      const subscription = this.client.subscribe(topic, (message: IMessage) => {
        try {
          const data = JSON.parse(message.body);
          callback(data);
        } catch (error) {
          console.error('[WebSocket] Error parsing message:', error);
        }
      });

      const subscriptionId = subscription.id;
      this.subscriptions.set(subscriptionId, subscription);
      console.log('[WebSocket] Subscribed to:', topic, 'ID:', subscriptionId);
      return subscriptionId;
    } catch (error) {
      console.error('[WebSocket] Subscription error:', error);
      return '';
    }
  }

  /**
   * Unsubscribe from a topic
   */
  unsubscribe(subscriptionId: string) {
    const subscription = this.subscriptions.get(subscriptionId);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(subscriptionId);
      console.log('[WebSocket] Unsubscribed:', subscriptionId);
    }
  }

  /**
   * Subscribe to user notifications
   */
  subscribeToNotifications(userId: number, callback: (notification: NotificationMessage) => void): string {
    return this.subscribe(`/user/${userId}/notifications`, callback);
  }

  /**
   * Subscribe to document status updates
   */
  subscribeToDocumentStatus(callback: (status: DocumentStatusMessage) => void): string {
    return this.subscribe('/topic/document-status', callback);
  }

  /**
   * Subscribe to activity feed
   */
  subscribeToActivityFeed(callback: (activity: ActivityMessage) => void): string {
    return this.subscribe('/topic/activity', callback);
  }

  /**
   * Subscribe to user presence
   */
  subscribeToUserPresence(callback: (presence: UserPresenceMessage) => void): string {
    return this.subscribe('/topic/user-presence', callback);
  }

  /**
   * Subscribe to department updates
   */
  subscribeToDepartment(department: string, callback: MessageCallback): string {
    return this.subscribe(`/topic/department/${department}`, callback);
  }

  /**
   * Send a message to the server
   */
  send(destination: string, body: any) {
    if (!this.client || !this.isConnected) {
      console.warn('[WebSocket] Not connected. Cannot send message to:', destination);
      return;
    }

    try {
      this.client.publish({
        destination,
        body: JSON.stringify(body),
      });
      console.log('[WebSocket] Message sent to:', destination);
    } catch (error) {
      console.error('[WebSocket] Send error:', error);
    }
  }

  /**
   * Update user presence
   */
  updatePresence(status: 'online' | 'away' | 'offline') {
    this.send('/app/presence', { status });
  }

  /**
   * Send typing indicator
   */
  sendTyping(resourceType: string, resourceId: number) {
    this.send('/app/typing', { resourceType, resourceId });
  }

  /**
   * Disconnect from WebSocket
   */
  disconnect() {
    if (this.client) {
      // Unsubscribe from all subscriptions
      this.subscriptions.forEach((subscription) => {
        subscription.unsubscribe();
      });
      this.subscriptions.clear();

      // Deactivate client
      this.client.deactivate();
      this.isConnected = false;
      console.log('[WebSocket] Disconnected');
    }
  }

  /**
   * Check if connected
   */
  isWebSocketConnected(): boolean {
    return this.isConnected;
  }

  /**
   * Get connection status
   */
  getConnectionStatus(): 'connected' | 'connecting' | 'disconnected' {
    if (this.isConnected) return 'connected';
    if (this.isConnecting) return 'connecting';
    return 'disconnected';
  }
}

// Create singleton instance
export const websocketService = new WebSocketService();

// Auto-connect when token is available
const token = localStorage.getItem('token');
if (token) {
  websocketService.connect(token).catch((error) => {
    console.error('[WebSocket] Auto-connect failed:', error);
  });
}

export default websocketService;

