import React, { useState, useEffect } from 'react';
import {
  Snackbar,
  Alert,
  Button,
  Box,
  Typography,
  IconButton,
} from '@mui/material';
import {
  Close as CloseIcon,
  Notifications as NotificationsIcon,
} from '@mui/icons-material';
import pushNotificationService from '../services/pushNotificationService';

const NotificationPermissionPrompt: React.FC = () => {
  const [open, setOpen] = useState(false);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    // Check if we should show the prompt
    const hasAskedBefore = localStorage.getItem('notification-permission-asked');
    const hasDismissed = localStorage.getItem('notification-permission-dismissed');
    
    if (
      !hasAskedBefore &&
      !hasDismissed &&
      pushNotificationService.isNotificationSupported() &&
      pushNotificationService.getPermission() === 'default'
    ) {
      // Show prompt after 3 seconds
      const timer = setTimeout(() => {
        setOpen(true);
      }, 3000);

      return () => clearTimeout(timer);
    }
  }, []);

  const handleAllow = async () => {
    const permission = await pushNotificationService.requestPermission();
    localStorage.setItem('notification-permission-asked', 'true');
    
    if (permission === 'granted') {
      // Show success notification
      pushNotificationService.showNotification({
        title: 'Notifications Enabled',
        body: 'You will now receive real-time notifications',
        icon: '/logo192.png',
      });
    }
    
    setOpen(false);
  };

  const handleDismiss = () => {
    localStorage.setItem('notification-permission-dismissed', 'true');
    setDismissed(true);
    setOpen(false);
  };

  const handleClose = () => {
    setOpen(false);
  };

  if (!pushNotificationService.isNotificationSupported()) {
    return null;
  }

  return (
    <Snackbar
      open={open}
      anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      onClose={handleClose}
      sx={{ mt: 8 }}
    >
      <Alert
        severity="info"
        icon={<NotificationsIcon />}
        sx={{
          width: '100%',
          maxWidth: 500,
          boxShadow: '0 10px 40px rgba(0,0,0,0.2)',
          borderRadius: 2,
          '.MuiAlert-message': {
            width: '100%',
          },
        }}
        action={
          <IconButton
            size="small"
            aria-label="close"
            color="inherit"
            onClick={handleClose}
          >
            <CloseIcon fontSize="small" />
          </IconButton>
        }
      >
        <Box>
          <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 0.5 }}>
            Enable Desktop Notifications
          </Typography>
          <Typography variant="body2" sx={{ mb: 2, fontSize: '0.875rem' }}>
            Stay updated with real-time notifications about document updates, workflows, and more.
          </Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              size="small"
              variant="contained"
              onClick={handleAllow}
              sx={{
                textTransform: 'none',
                fontWeight: 600,
                backgroundColor: '#3b82f6',
                '&:hover': {
                  backgroundColor: '#2563eb',
                },
              }}
            >
              Allow
            </Button>
            <Button
              size="small"
              variant="outlined"
              onClick={handleDismiss}
              sx={{
                textTransform: 'none',
                fontWeight: 600,
                borderColor: '#d1d5db',
                color: '#6b7280',
                '&:hover': {
                  borderColor: '#9ca3af',
                  backgroundColor: 'transparent',
                },
              }}
            >
              Not Now
            </Button>
          </Box>
        </Box>
      </Alert>
    </Snackbar>
  );
};

export default NotificationPermissionPrompt;

