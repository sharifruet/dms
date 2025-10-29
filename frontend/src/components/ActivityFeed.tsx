import React, { useState, useEffect, useRef } from 'react';
import {
  Box,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  Chip,
  IconButton,
  Tooltip,
  Divider,
  Badge,
  CircularProgress,
} from '@mui/material';
import {
  Description as DocumentIcon,
  Upload as UploadIcon,
  Download as DownloadIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Share as ShareIcon,
  Person as PersonIcon,
  CheckCircle as ApproveIcon,
  Cancel as RejectIcon,
  FiberManualRecord as OnlineIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { formatDistanceToNow } from 'date-fns';
import websocketService, { ActivityMessage, UserPresenceMessage } from '../services/websocketService';

interface Activity extends ActivityMessage {
  id: string;
}

interface UserPresence {
  userId: number;
  username: string;
  status: 'online' | 'offline' | 'away';
  lastSeen?: Date;
}

const ActivityFeed: React.FC = () => {
  const [activities, setActivities] = useState<Activity[]>([]);
  const [userPresences, setUserPresences] = useState<Map<number, UserPresence>>(new Map());
  const [loading, setLoading] = useState(true);
  const [isConnected, setIsConnected] = useState(false);
  const activitySubscriptionRef = useRef<string>('');
  const presenceSubscriptionRef = useRef<string>('');

  useEffect(() => {
    // WebSocket not yet implemented on backend - show mock data instead
    // const token = localStorage.getItem('token');
    
    const initializeWebSocket = async () => {
      try {
        // Generate mock activity data
        generateMockActivities();
        setIsConnected(false); // Not connected to real WebSocket
        // if (!websocketService.isWebSocketConnected()) {
        //   await websocketService.connect(token || undefined);
        // }
        // setIsConnected(true);
        // subscribeToFeeds();
      } catch (error) {
        console.error('Failed to initialize activity feed:', error);
        setIsConnected(false);
      } finally {
        setLoading(false);
      }
    };

    initializeWebSocket();

    // Cleanup on unmount
    return () => {
      if (activitySubscriptionRef.current) {
        websocketService.unsubscribe(activitySubscriptionRef.current);
      }
      if (presenceSubscriptionRef.current) {
        websocketService.unsubscribe(presenceSubscriptionRef.current);
      }
    };
  }, []);

  const generateMockActivities = () => {
    // Generate some mock activities for demo purposes
    const mockActions = ['uploaded', 'updated', 'deleted', 'shared', 'viewed', 'approved', 'rejected'];
    const mockResources = ['document', 'workflow', 'report'];
    const mockUsers = ['admin', 'john.doe', 'jane.smith', 'bob.jones'];
    
    const mockActivities: Activity[] = Array.from({ length: 10 }, (_, i) => ({
      id: `mock-${i}`,
      userId: i + 1,
      username: mockUsers[Math.floor(Math.random() * mockUsers.length)],
      action: mockActions[Math.floor(Math.random() * mockActions.length)],
      resourceType: mockResources[Math.floor(Math.random() * mockResources.length)],
      resourceId: Math.floor(Math.random() * 100) + 1,
      resourceName: `Sample ${mockResources[Math.floor(Math.random() * mockResources.length)]} ${i + 1}`,
      timestamp: new Date(Date.now() - Math.random() * 3600000).toISOString(),
      metadata: {},
    }));
    
    setActivities(mockActivities);
    
    // Mock user presences
    const mockPresences = new Map<number, UserPresence>();
    mockUsers.forEach((username, index) => {
      mockPresences.set(index + 1, {
        userId: index + 1,
        username,
        status: Math.random() > 0.5 ? 'online' : 'offline',
        lastSeen: Math.random() > 0.5 ? new Date(Date.now() - Math.random() * 3600000) : undefined,
      });
    });
    setUserPresences(mockPresences);
  };

  const subscribeToFeeds = () => {
    // WebSocket feeds disabled - using mock data
    // Subscribe to activity feed
    // activitySubscriptionRef.current = websocketService.subscribeToActivityFeed((activity: ActivityMessage) => {
    //   console.log('Received activity:', activity);
    //   
    //   const newActivity: Activity = {
    //     ...activity,
    //     id: activity.id || `${Date.now()}-${Math.random()}`,
    //   };
    //
    //   setActivities((prev) => {
    //     // Add to beginning and limit to 50 items
    //     const updated = [newActivity, ...prev].slice(0, 50);
    //     return updated;
    //   });
    // });
    //
    // // Subscribe to user presence
    // presenceSubscriptionRef.current = websocketService.subscribeToUserPresence((presence: UserPresenceMessage) => {
    //   console.log('Received presence:', presence);
    //   
    //   setUserPresences((prev) => {
    //     const updated = new Map(prev);
    //     updated.set(presence.userId, {
    //       userId: presence.userId,
    //       username: presence.username,
    //       status: presence.status,
    //       lastSeen: presence.status === 'offline' ? new Date(presence.timestamp) : undefined,
    //     });
    //     return updated;
    //   });
    // });
  };

  const getActivityIcon = (action: string, resourceType: string) => {
    const iconStyle = { fontSize: '1.2rem' };

    if (action.includes('upload')) return <UploadIcon sx={iconStyle} />;
    if (action.includes('download')) return <DownloadIcon sx={iconStyle} />;
    if (action.includes('edit') || action.includes('update')) return <EditIcon sx={iconStyle} />;
    if (action.includes('delete')) return <DeleteIcon sx={iconStyle} />;
    if (action.includes('share')) return <ShareIcon sx={iconStyle} />;
    if (action.includes('approve')) return <ApproveIcon sx={iconStyle} />;
    if (action.includes('reject')) return <RejectIcon sx={iconStyle} />;
    
    if (resourceType === 'document') return <DocumentIcon sx={iconStyle} />;
    if (resourceType === 'user') return <PersonIcon sx={iconStyle} />;
    
    return <DocumentIcon sx={iconStyle} />;
  };

  const getActivityColor = (action: string) => {
    if (action.includes('upload') || action.includes('create')) return '#10b981';
    if (action.includes('delete')) return '#ef4444';
    if (action.includes('edit') || action.includes('update')) return '#3b82f6';
    if (action.includes('approve')) return '#10b981';
    if (action.includes('reject')) return '#ef4444';
    if (action.includes('share')) return '#8b5cf6';
    return '#6b7280';
  };

  const getPresenceColor = (status: string) => {
    if (status === 'online') return '#10b981';
    if (status === 'away') return '#f59e0b';
    return '#9ca3af';
  };

  const getPresenceText = (presence: UserPresence | undefined) => {
    if (!presence) return 'Offline';
    if (presence.status === 'online') return 'Online';
    if (presence.status === 'away') return 'Away';
    if (presence.lastSeen) {
      return `Last seen ${formatDistanceToNow(presence.lastSeen, { addSuffix: true })}`;
    }
    return 'Offline';
  };

  const handleRefresh = () => {
    setLoading(true);
    setActivities([]);
    
    // Resubscribe to feeds
    if (activitySubscriptionRef.current) {
      websocketService.unsubscribe(activitySubscriptionRef.current);
    }
    if (presenceSubscriptionRef.current) {
      websocketService.unsubscribe(presenceSubscriptionRef.current);
    }
    
    setTimeout(() => {
      subscribeToFeeds();
      setLoading(false);
    }, 500);
  };

  return (
    <Paper
      elevation={0}
      sx={{
        height: '100%',
        border: '1px solid #e5e7eb',
        borderRadius: 3,
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Header */}
      <Box
        sx={{
          px: 3,
          py: 2,
          borderBottom: '1px solid #e5e7eb',
          backgroundColor: '#fafbfc',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Typography
            variant="h6"
            sx={{
              fontWeight: 600,
              fontSize: '1.125rem',
              color: '#1f2937',
            }}
          >
            Activity Feed
          </Typography>
          <Chip
            label={isConnected ? 'Live' : 'Disconnected'}
            size="small"
            sx={{
              height: 20,
              fontSize: '0.75rem',
              backgroundColor: isConnected ? '#d1fae5' : '#fee2e2',
              color: isConnected ? '#065f46' : '#991b1b',
              fontWeight: 600,
            }}
          />
        </Box>
        <Tooltip title="Refresh">
          <IconButton size="small" onClick={handleRefresh} disabled={loading}>
            <RefreshIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </Box>

      {/* Activity List */}
      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {loading ? (
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              height: '200px',
            }}
          >
            <CircularProgress size={40} />
          </Box>
        ) : activities.length === 0 ? (
          <Box
            sx={{
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              alignItems: 'center',
              height: '200px',
              color: '#9ca3af',
            }}
          >
            <Typography variant="body2">No recent activity</Typography>
            <Typography variant="caption" sx={{ mt: 0.5 }}>
              Activity will appear here in real-time
            </Typography>
          </Box>
        ) : (
          <List sx={{ p: 0 }}>
            {activities.map((activity, index) => {
              const userPresence = userPresences.get(activity.userId);
              
              return (
                <React.Fragment key={activity.id}>
                  <ListItem
                    sx={{
                      px: 3,
                      py: 2,
                      '&:hover': {
                        backgroundColor: '#f9fafb',
                      },
                    }}
                  >
                    <ListItemAvatar>
                      <Badge
                        overlap="circular"
                        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                        badgeContent={
                          <Tooltip title={getPresenceText(userPresence)}>
                            <OnlineIcon
                              sx={{
                                fontSize: '0.75rem',
                                color: getPresenceColor(userPresence?.status || 'offline'),
                              }}
                            />
                          </Tooltip>
                        }
                      >
                        <Avatar
                          sx={{
                            width: 40,
                            height: 40,
                            backgroundColor: getActivityColor(activity.action),
                          }}
                        >
                          {getActivityIcon(activity.action, activity.resourceType)}
                        </Avatar>
                      </Badge>
                    </ListItemAvatar>
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                          <Typography
                            variant="body2"
                            sx={{ fontWeight: 600, color: '#1f2937' }}
                          >
                            {activity.username}
                          </Typography>
                          <Typography variant="body2" sx={{ color: '#6b7280' }}>
                            {activity.action}
                          </Typography>
                          <Typography
                            variant="body2"
                            sx={{ fontWeight: 600, color: '#1f2937' }}
                          >
                            {activity.resourceName}
                          </Typography>
                        </Box>
                      }
                      secondary={
                        <Typography
                          variant="caption"
                          sx={{ color: '#9ca3af', fontSize: '0.75rem' }}
                        >
                          {formatDistanceToNow(new Date(activity.timestamp), {
                            addSuffix: true,
                          })}
                        </Typography>
                      }
                    />
                  </ListItem>
                  {index < activities.length - 1 && <Divider component="li" />}
                </React.Fragment>
              );
            })}
          </List>
        )}
      </Box>
    </Paper>
  );
};

export default ActivityFeed;

