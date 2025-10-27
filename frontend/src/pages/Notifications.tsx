import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Chip,
  Button,
  Pagination,
  Tabs,
  Tab,
  IconButton,
  Menu,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  Switch,
  FormControlLabel,
  Grid,
  Alert,
  CircularProgress,
  Badge,
  Tooltip
} from '@mui/material';
import {
  Notifications as NotificationsIcon,
  Email as EmailIcon,
  Sms as SmsIcon,
  Smartphone as SmartphoneIcon,
  MoreVert as MoreVertIcon,
  MarkEmailRead as MarkEmailReadIcon,
  Delete as DeleteIcon,
  Settings as SettingsIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
  Error as ErrorIcon,
  CheckCircle as CheckCircleIcon
} from '@mui/icons-material';
import notificationService, { Notification, NotificationType, NotificationPriority, NotificationStatus } from '../services/notificationService';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`notification-tabpanel-${index}`}
      aria-labelledby={`notification-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

const Notifications: React.FC = () => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [tabValue, setTabValue] = useState(0);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedNotification, setSelectedNotification] = useState<Notification | null>(null);
  const [preferencesOpen, setPreferencesOpen] = useState(false);
  const [createNotificationOpen, setCreateNotificationOpen] = useState(false);
  const [newNotification, setNewNotification] = useState({
    title: '',
    message: '',
    type: NotificationType.DOCUMENT_UPLOAD,
    priority: NotificationPriority.MEDIUM
  });

  useEffect(() => {
    loadNotifications();
    loadUnreadCount();
  }, [currentPage, tabValue]);

  const loadNotifications = async () => {
    try {
      setLoading(true);
      const response = await notificationService.getNotifications(currentPage, 20);
      setNotifications(response.content || []);
      setTotalPages(response.totalPages || 0);
    } catch (error) {
      setError('Failed to load notifications');
    } finally {
      setLoading(false);
    }
  };

  const loadUnreadCount = async () => {
    try {
      const count = await notificationService.getUnreadCount();
      setUnreadCount(count);
    } catch (error) {
      console.error('Failed to load unread count:', error);
    }
  };

  const handleMarkAsRead = async (notificationId: number) => {
    try {
      await notificationService.markAsRead(notificationId);
      await loadNotifications();
      await loadUnreadCount();
    } catch (error) {
      setError('Failed to mark notification as read');
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      const unreadNotifications = notifications.filter(n => !n.readAt);
      await Promise.all(unreadNotifications.map(n => notificationService.markAsRead(n.id)));
      await loadNotifications();
      await loadUnreadCount();
    } catch (error) {
      setError('Failed to mark all notifications as read');
    }
  };

  const handleCreateNotification = async () => {
    try {
      await notificationService.createNotification(newNotification);
      setCreateNotificationOpen(false);
      setNewNotification({
        title: '',
        message: '',
        type: NotificationType.DOCUMENT_UPLOAD,
        priority: NotificationPriority.MEDIUM
      });
      await loadNotifications();
    } catch (error) {
      setError('Failed to create notification');
    }
  };

  const getPriorityColor = (priority: NotificationPriority) => {
    switch (priority) {
      case NotificationPriority.CRITICAL:
        return 'error';
      case NotificationPriority.HIGH:
        return 'warning';
      case NotificationPriority.MEDIUM:
        return 'info';
      case NotificationPriority.LOW:
        return 'success';
      default:
        return 'default';
    }
  };

  const getPriorityIcon = (priority: NotificationPriority) => {
    switch (priority) {
      case NotificationPriority.CRITICAL:
        return <ErrorIcon />;
      case NotificationPriority.HIGH:
        return <WarningIcon />;
      case NotificationPriority.MEDIUM:
        return <InfoIcon />;
      case NotificationPriority.LOW:
        return <CheckCircleIcon />;
      default:
        return <InfoIcon />;
    }
  };

  const getChannelIcon = (channel?: string) => {
    switch (channel) {
      case 'EMAIL':
        return <EmailIcon />;
      case 'SMS':
        return <SmsIcon />;
      case 'IN_APP':
        return <NotificationsIcon />;
      case 'PUSH':
        return <SmartphoneIcon />;
      default:
        return <NotificationsIcon />;
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  const filteredNotifications = notifications.filter(notification => {
    switch (tabValue) {
      case 0: // All
        return true;
      case 1: // Unread
        return !notification.readAt;
      case 2: // High Priority
        return notification.priority === NotificationPriority.HIGH || 
               notification.priority === NotificationPriority.CRITICAL;
      default:
        return true;
    }
  });

  if (loading && notifications.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" component="h1">
          Notifications
          {unreadCount > 0 && (
            <Badge badgeContent={unreadCount} color="error" sx={{ ml: 2 }}>
              <NotificationsIcon />
            </Badge>
          )}
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<SettingsIcon />}
            onClick={() => setPreferencesOpen(true)}
            sx={{ mr: 1 }}
          >
            Preferences
          </Button>
          <Button
            variant="contained"
            onClick={() => setCreateNotificationOpen(true)}
          >
            Create Notification
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
            <Tab label="All" />
            <Tab 
              label={
                <Badge badgeContent={unreadCount} color="error">
                  Unread
                </Badge>
              } 
            />
            <Tab label="High Priority" />
          </Tabs>
        </Box>

        <TabPanel value={tabValue} index={0}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">
              All Notifications ({filteredNotifications.length})
            </Typography>
            {unreadCount > 0 && (
              <Button onClick={handleMarkAllAsRead}>
                Mark All as Read
              </Button>
            )}
          </Box>

          <List>
            {filteredNotifications.map((notification) => (
              <ListItem
                key={notification.id}
                sx={{
                  backgroundColor: notification.readAt ? 'transparent' : '#f5f5f5',
                  borderLeft: `4px solid ${
                    notification.readAt ? 'transparent' : 
                    notification.priority === NotificationPriority.CRITICAL ? '#f44336' :
                    notification.priority === NotificationPriority.HIGH ? '#ff9800' :
                    notification.priority === NotificationPriority.MEDIUM ? '#2196f3' : '#4caf50'
                  }`
                }}
              >
                <ListItemIcon>
                  <Tooltip title={notification.priority}>
                    {getPriorityIcon(notification.priority)}
                  </Tooltip>
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Box display="flex" alignItems="center" gap={1}>
                      <Typography variant="subtitle1" fontWeight={notification.readAt ? 'normal' : 'bold'}>
                        {notification.title}
                      </Typography>
                      <Chip
                        label={notification.priority}
                        size="small"
                        color={getPriorityColor(notification.priority)}
                      />
                      {notification.channel && (
                        <Tooltip title={`Sent via ${notification.channel}`}>
                          {getChannelIcon(notification.channel)}
                        </Tooltip>
                      )}
                    </Box>
                  }
                  secondary={
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        {notification.message}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {formatDate(notification.createdAt)}
                        {notification.readAt && (
                          <span> • Read at {formatDate(notification.readAt)}</span>
                        )}
                      </Typography>
                    </Box>
                  }
                />
                <IconButton
                  onClick={(e) => {
                    setAnchorEl(e.currentTarget);
                    setSelectedNotification(notification);
                  }}
                >
                  <MoreVertIcon />
                </IconButton>
              </ListItem>
            ))}
          </List>

          {totalPages > 1 && (
            <Box display="flex" justifyContent="center" mt={3}>
              <Pagination
                count={totalPages}
                page={currentPage + 1}
                onChange={(e, page) => setCurrentPage(page - 1)}
              />
            </Box>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <Typography variant="h6" mb={2}>
            Unread Notifications ({filteredNotifications.length})
          </Typography>
          {/* Same list as above but filtered for unread */}
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          <Typography variant="h6" mb={2}>
            High Priority Notifications ({filteredNotifications.length})
          </Typography>
          {/* Same list as above but filtered for high priority */}
        </TabPanel>
      </Card>

      {/* Notification Actions Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
      >
        {selectedNotification && !selectedNotification.readAt && (
          <MenuItem onClick={() => {
            handleMarkAsRead(selectedNotification.id);
            setAnchorEl(null);
          }}>
            <MarkEmailReadIcon sx={{ mr: 1 }} />
            Mark as Read
          </MenuItem>
        )}
        <MenuItem onClick={() => setAnchorEl(null)}>
          <DeleteIcon sx={{ mr: 1 }} />
          Delete
        </MenuItem>
      </Menu>

      {/* Create Notification Dialog */}
      <Dialog open={createNotificationOpen} onClose={() => setCreateNotificationOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Create Notification</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Title"
                value={newNotification.title}
                onChange={(e) => setNewNotification({ ...newNotification, title: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={4}
                label="Message"
                value={newNotification.message}
                onChange={(e) => setNewNotification({ ...newNotification, message: e.target.value })}
              />
            </Grid>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Type</InputLabel>
                <Select
                  value={newNotification.type}
                  onChange={(e) => setNewNotification({ ...newNotification, type: e.target.value as NotificationType })}
                >
                  {Object.values(NotificationType).map((type) => (
                    <MenuItem key={type} value={type}>
                      {type.replace(/_/g, ' ')}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Priority</InputLabel>
                <Select
                  value={newNotification.priority}
                  onChange={(e) => setNewNotification({ ...newNotification, priority: e.target.value as NotificationPriority })}
                >
                  {Object.values(NotificationPriority).map((priority) => (
                    <MenuItem key={priority} value={priority}>
                      {priority}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateNotificationOpen(false)}>Cancel</Button>
          <Button onClick={handleCreateNotification} variant="contained">
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* Preferences Dialog */}
      <Dialog open={preferencesOpen} onClose={() => setPreferencesOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Notification Preferences</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Configure your notification preferences for different types of alerts.
          </Typography>
          {/* TODO: Implement preferences form */}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPreferencesOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Notifications;
