import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  IconButton,
  Badge,
  Popover,
  Box,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemButton,
  Divider,
  Button,
  Chip,
  CircularProgress,
} from '@mui/material';
import {
  Notifications as NotificationsIcon,
  NotificationsActive as NotificationsActiveIcon,
  Circle as CircleIcon,
} from '@mui/icons-material';
import { formatDistanceToNow } from 'date-fns';
import { useAppSelector } from '../hooks/redux';
import websocketService, { NotificationMessage } from '../services/websocketService';
import pushNotificationService from '../services/pushNotificationService';
import api from '../services/api';

interface Notification {
  id: number;
  type: string;
  title: string;
  message: string;
  priority: string;
  isRead: boolean;
  createdAt: string;
}

const NotificationBell: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAppSelector((state) => state.auth);
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [hasNewNotification, setHasNewNotification] = useState(false);
  const subscriptionRef = useRef<string>('');

  useEffect(() => {
    if (user?.username) {
      fetchNotifications();
      // WebSocket not yet implemented on backend - polling instead
      // subscribeToNotifications();
      
      // Poll for new notifications every 30 seconds
      const pollInterval = setInterval(fetchNotifications, 30000);
      
      return () => {
        clearInterval(pollInterval);
        if (subscriptionRef.current) {
          websocketService.unsubscribe(subscriptionRef.current);
        }
      };
    }
  }, [user?.username]);

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const response = await api.get('/notifications', {
        params: { page: 0, size: 10 },
      });
      
      const fetchedNotifications = response.data.content || [];
      setNotifications(fetchedNotifications);
      
      const unread = fetchedNotifications.filter((n: Notification) => !n.isRead).length;
      setUnreadCount(unread);
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  const subscribeToNotifications = () => {
    if (!user?.username) return;

    // Connect WebSocket if not connected
    const token = localStorage.getItem('token');
    if (!websocketService.isWebSocketConnected()) {
      websocketService.connect(token || undefined).catch(console.error);
    }

    // Use a simple hash of username as ID for WebSocket subscription
    const simpleHash = (str: string) => {
      let hash = 0;
      for (let i = 0; i < str.length; i++) {
        hash = ((hash << 5) - hash) + str.charCodeAt(i);
        hash = hash & hash;
      }
      return Math.abs(hash);
    };

    const userId = simpleHash(user.username);

    // Subscribe to user notifications
    subscriptionRef.current = websocketService.subscribeToNotifications(
      userId,
      (notification: NotificationMessage) => {
        console.log('Received notification:', notification);

        // Add to notifications list
        const newNotification: Notification = {
          id: notification.id,
          type: notification.type,
          title: notification.title,
          message: notification.message,
          priority: notification.priority,
          isRead: false,
          createdAt: notification.timestamp,
        };

        setNotifications((prev) => [newNotification, ...prev].slice(0, 20));
        setUnreadCount((prev) => prev + 1);
        setHasNewNotification(true);

        // Show desktop notification
        pushNotificationService.showNewNotification(
          notification.title,
          notification.message,
          notification.priority
        );

        // Reset animation after 2 seconds
        setTimeout(() => setHasNewNotification(false), 2000);
      }
    );
  };

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
    setHasNewNotification(false);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleMarkAsRead = async (notificationId: number) => {
    try {
      await api.put(`/notifications/${notificationId}/read`);
      
      setNotifications((prev) =>
        prev.map((n) => (n.id === notificationId ? { ...n, isRead: true } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
    }
  };

  const handleNotificationClick = (notification: Notification) => {
    handleMarkAsRead(notification.id);
    handleClose();
    
    // Navigate based on notification type
    if (notification.type === 'DOCUMENT_UPLOADED' || notification.type === 'DOCUMENT_SHARED') {
      navigate('/documents');
    } else if (notification.type === 'WORKFLOW_ASSIGNED') {
      navigate('/workflows');
    } else if (notification.type === 'EXPIRY_ALERT') {
      navigate('/expiry-tracking');
    } else {
      navigate('/notifications');
    }
  };

  const handleViewAll = () => {
    handleClose();
    navigate('/notifications');
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'CRITICAL':
        return '#ef4444';
      case 'HIGH':
        return '#f59e0b';
      case 'MEDIUM':
        return '#3b82f6';
      default:
        return '#6b7280';
    }
  };

  const open = Boolean(anchorEl);

  return (
    <>
      <IconButton
        onClick={handleClick}
        sx={{
          color: '#6b7280',
          '&:hover': {
            backgroundColor: '#f3f4f6',
          },
        }}
      >
        <Badge badgeContent={unreadCount} color="error">
          {hasNewNotification ? (
            <NotificationsActiveIcon
              sx={{
                animation: 'ring 0.5s ease-in-out',
                '@keyframes ring': {
                  '0%, 100%': { transform: 'rotate(0deg)' },
                  '10%, 30%': { transform: 'rotate(-10deg)' },
                  '20%, 40%': { transform: 'rotate(10deg)' },
                },
              }}
            />
          ) : (
            <NotificationsIcon />
          )}
        </Badge>
      </IconButton>

      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        PaperProps={{
          sx: {
            width: 380,
            maxHeight: 500,
            mt: 1.5,
            borderRadius: 2,
            boxShadow: '0 10px 40px rgba(0,0,0,0.12)',
          },
        }}
      >
        {/* Header */}
        <Box
          sx={{
            px: 3,
            py: 2,
            borderBottom: '1px solid #e5e7eb',
            backgroundColor: '#fafbfc',
          }}
        >
          <Typography
            variant="h6"
            sx={{
              fontWeight: 600,
              fontSize: '1rem',
              color: '#1f2937',
            }}
          >
            Notifications
          </Typography>
          {unreadCount > 0 && (
            <Typography
              variant="caption"
              sx={{
                color: '#6b7280',
                fontSize: '0.75rem',
              }}
            >
              {unreadCount} unread
            </Typography>
          )}
        </Box>

        {/* Notifications List */}
        <Box sx={{ maxHeight: 380, overflow: 'auto' }}>
          {loading ? (
            <Box
              sx={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                py: 4,
              }}
            >
              <CircularProgress size={32} />
            </Box>
          ) : notifications.length === 0 ? (
            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                alignItems: 'center',
                py: 4,
                color: '#9ca3af',
              }}
            >
              <NotificationsIcon sx={{ fontSize: '3rem', mb: 1 }} />
              <Typography variant="body2">No notifications</Typography>
            </Box>
          ) : (
            <List sx={{ p: 0 }}>
              {notifications.map((notification, index) => (
                <React.Fragment key={notification.id}>
                  <ListItem disablePadding>
                    <ListItemButton
                      onClick={() => handleNotificationClick(notification)}
                      sx={{
                        px: 3,
                        py: 2,
                        backgroundColor: notification.isRead ? 'transparent' : '#f0f9ff',
                        '&:hover': {
                          backgroundColor: notification.isRead ? '#f9fafb' : '#e0f2fe',
                        },
                      }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'flex-start', width: '100%', gap: 1.5 }}>
                        {!notification.isRead && (
                          <CircleIcon
                            sx={{
                              fontSize: '0.5rem',
                              color: '#3b82f6',
                              mt: 0.5,
                            }}
                          />
                        )}
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                            <Typography
                              variant="body2"
                              sx={{
                                fontWeight: 600,
                                color: '#1f2937',
                                fontSize: '0.875rem',
                              }}
                            >
                              {notification.title}
                            </Typography>
                            <Chip
                              label={notification.priority}
                              size="small"
                              sx={{
                                height: 18,
                                fontSize: '0.65rem',
                                fontWeight: 600,
                                backgroundColor: `${getPriorityColor(notification.priority)}20`,
                                color: getPriorityColor(notification.priority),
                              }}
                            />
                          </Box>
                          <Typography
                            variant="body2"
                            sx={{
                              color: '#6b7280',
                              fontSize: '0.8125rem',
                              mb: 0.5,
                            }}
                          >
                            {notification.message}
                          </Typography>
                          <Typography
                            variant="caption"
                            sx={{
                              color: '#9ca3af',
                              fontSize: '0.75rem',
                            }}
                          >
                            {formatDistanceToNow(new Date(notification.createdAt), {
                              addSuffix: true,
                            })}
                          </Typography>
                        </Box>
                      </Box>
                    </ListItemButton>
                  </ListItem>
                  {index < notifications.length - 1 && <Divider />}
                </React.Fragment>
              ))}
            </List>
          )}
        </Box>

        {/* Footer */}
        {notifications.length > 0 && (
          <>
            <Divider />
            <Box sx={{ p: 2, textAlign: 'center' }}>
              <Button
                fullWidth
                onClick={handleViewAll}
                sx={{
                  color: '#3b82f6',
                  fontWeight: 600,
                  fontSize: '0.875rem',
                  textTransform: 'none',
                  '&:hover': {
                    backgroundColor: '#f0f9ff',
                  },
                }}
              >
                View All Notifications
              </Button>
            </Box>
          </>
        )}
      </Popover>
    </>
  );
};

export default NotificationBell;

