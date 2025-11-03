/**
 * Push Notification Service
 * Handles browser and desktop notifications
 */

export interface NotificationOptions {
  title: string;
  body: string;
  icon?: string;
  badge?: string;
  tag?: string;
  data?: any;
  requireInteraction?: boolean;
  silent?: boolean;
  vibrate?: number[];
  actions?: NotificationAction[];
}

class PushNotificationService {
  private permission: NotificationPermission = 'default';
  private isSupported: boolean = false;

  constructor() {
    this.isSupported = 'Notification' in window;
    if (this.isSupported) {
      this.permission = Notification.permission;
    }
  }

  /**
   * Check if notifications are supported
   */
  isNotificationSupported(): boolean {
    return this.isSupported;
  }

  /**
   * Get current permission status
   */
  getPermission(): NotificationPermission {
    return this.permission;
  }

  /**
   * Request notification permission
   */
  async requestPermission(): Promise<NotificationPermission> {
    if (!this.isSupported) {
      console.warn('Notifications are not supported in this browser');
      return 'denied';
    }

    try {
      this.permission = await Notification.requestPermission();
      console.log('[Notifications] Permission:', this.permission);
      return this.permission;
    } catch (error) {
      console.error('[Notifications] Permission request error:', error);
      return 'denied';
    }
  }

  /**
   * Show a browser notification
   */
  async showNotification(options: NotificationOptions): Promise<Notification | null> {
    if (!this.isSupported) {
      console.warn('Notifications are not supported');
      return null;
    }

    // Request permission if not granted
    if (this.permission !== 'granted') {
      const permission = await this.requestPermission();
      if (permission !== 'granted') {
        console.warn('Notification permission denied');
        return null;
      }
    }

    try {
      const notification = new Notification(options.title, {
        body: options.body,
        icon: options.icon || '/logo192.png',
        badge: options.badge || '/logo192.png',
        tag: options.tag,
        data: options.data,
        requireInteraction: options.requireInteraction || false,
        silent: options.silent || false,
        vibrate: options.vibrate || [200, 100, 200],
      });

      // Auto-close after 10 seconds if not requiring interaction
      if (!options.requireInteraction) {
        setTimeout(() => notification.close(), 10000);
      }

      // Handle notification click
      notification.onclick = (event) => {
        event.preventDefault();
        window.focus();
        
        // Handle custom data
        if (options.data?.url) {
          window.location.href = options.data.url;
        }
        
        notification.close();
      };

      return notification;
    } catch (error) {
      console.error('[Notifications] Show notification error:', error);
      return null;
    }
  }

  /**
   * Show notification for new document
   */
  showDocumentNotification(documentName: string, action: string = 'uploaded') {
    this.showNotification({
      title: 'Document Update',
      body: `${documentName} has been ${action}`,
      icon: '/logo192.png',
      tag: 'document-update',
      data: { url: '/documents' },
    });
  }

  /**
   * Show notification for new notification
   */
  showNewNotification(title: string, message: string, priority: string = 'MEDIUM') {
    const requireInteraction = priority === 'HIGH' || priority === 'CRITICAL';
    
    this.showNotification({
      title: title,
      body: message,
      icon: '/logo192.png',
      tag: 'system-notification',
      requireInteraction,
      data: { url: '/notifications' },
    });
  }

  /**
   * Show notification for workflow action
   */
  showWorkflowNotification(workflowName: string, action: string) {
    this.showNotification({
      title: 'Workflow Update',
      body: `${workflowName} requires your ${action}`,
      icon: '/logo192.png',
      tag: 'workflow-update',
      requireInteraction: true,
      data: { url: '/workflows' },
    });
  }

  /**
   * Show notification for expiry alert
   */
  showExpiryNotification(itemName: string, daysUntilExpiry: number) {
    const urgency = daysUntilExpiry <= 7 ? 'urgent' : 'warning';
    const requireInteraction = daysUntilExpiry <= 7;

    this.showNotification({
      title: 'Expiry Alert',
      body: `${itemName} expires in ${daysUntilExpiry} days`,
      icon: '/logo192.png',
      tag: 'expiry-alert',
      requireInteraction,
      data: { url: '/expiry-tracking', urgency },
      vibrate: requireInteraction ? [200, 100, 200, 100, 200] : [200, 100, 200],
    });
  }

  /**
   * Show notification for system alert
   */
  showSystemAlert(message: string, priority: 'info' | 'warning' | 'error' = 'info') {
    const requireInteraction = priority === 'error';
    
    this.showNotification({
      title: 'System Alert',
      body: message,
      icon: '/logo192.png',
      tag: 'system-alert',
      requireInteraction,
      data: { priority },
    });
  }

  /**
   * Show notification for user mention
   */
  showMentionNotification(mentionedBy: string, context: string) {
    this.showNotification({
      title: 'You were mentioned',
      body: `${mentionedBy} mentioned you: ${context}`,
      icon: '/logo192.png',
      tag: 'mention',
      requireInteraction: true,
      data: { url: '/notifications' },
    });
  }

  /**
   * Clear all notifications with a specific tag
   */
  clearNotificationsByTag(tag: string) {
    // Note: This is a limitation of the Notifications API
    // We can't programmatically close notifications by tag
    console.log('[Notifications] Clear by tag:', tag);
  }

  /**
   * Check if permission is granted
   */
  isPermissionGranted(): boolean {
    return this.permission === 'granted';
  }

  /**
   * Test notification (for debugging)
   */
  testNotification() {
    this.showNotification({
      title: 'Test Notification',
      body: 'This is a test notification from DMS',
      icon: '/logo192.png',
      tag: 'test',
    });
  }
}

// Create singleton instance
export const pushNotificationService = new PushNotificationService();

// Auto-request permission on first load if supported
if (pushNotificationService.isNotificationSupported()) {
  const hasAskedBefore = localStorage.getItem('notification-permission-asked');
  
  if (!hasAskedBefore && pushNotificationService.getPermission() === 'default') {
    // Show a friendly prompt instead of requesting immediately
    console.log('[Notifications] Ready to request permission when user interacts');
  }
}

export default pushNotificationService;

