import React from 'react';
import {
  Container,
  Typography,
  Grid,
  Card,
  CardContent,
  CardActions,
  Button,
  Box,
} from '@mui/material';
import {
  Description,
  People,
  Upload,
  Search,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAppSelector } from '../hooks/redux';

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAppSelector((state) => state.auth);

  const dashboardCards = [
    {
      title: 'Documents',
      description: 'Manage and view all documents',
      icon: <Description sx={{ fontSize: 40 }} />,
      color: '#1976d2',
      action: () => navigate('/documents'),
    },
    {
      title: 'Users',
      description: 'Manage system users and roles',
      icon: <People sx={{ fontSize: 40 }} />,
      color: '#dc004e',
      action: () => navigate('/users'),
    },
    {
      title: 'Upload',
      description: 'Upload new documents',
      icon: <Upload sx={{ fontSize: 40 }} />,
      color: '#2e7d32',
      action: () => navigate('/documents'),
    },
    {
      title: 'Search',
      description: 'Search documents and content',
      icon: <Search sx={{ fontSize: 40 }} />,
      color: '#ed6c02',
      action: () => navigate('/documents'),
    },
  ];

  return (
    <Container maxWidth="lg">
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Dashboard
        </Typography>
        <Typography variant="h6" color="text.secondary">
          Welcome back, {user?.username}! ({user?.role})
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Department: {user?.department || 'N/A'}
        </Typography>
      </Box>

      <Grid container spacing={3}>
        {dashboardCards.map((card, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <Card
              sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                transition: 'transform 0.2s',
                '&:hover': {
                  transform: 'translateY(-4px)',
                },
              }}
            >
              <CardContent sx={{ flexGrow: 1, textAlign: 'center' }}>
                <Box sx={{ color: card.color, mb: 2 }}>
                  {card.icon}
                </Box>
                <Typography variant="h6" component="h2" gutterBottom>
                  {card.title}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {card.description}
                </Typography>
              </CardContent>
              <CardActions sx={{ justifyContent: 'center', pb: 2 }}>
                <Button
                  variant="contained"
                  onClick={card.action}
                  sx={{ backgroundColor: card.color }}
                >
                  Open
                </Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Box sx={{ mt: 4 }}>
        <Typography variant="h5" gutterBottom>
          System Status
        </Typography>
        <Grid container spacing={2}>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" color="primary">
                  Documents
                </Typography>
                <Typography variant="h4">0</Typography>
                <Typography variant="body2" color="text.secondary">
                  Total documents
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" color="primary">
                  Users
                </Typography>
                <Typography variant="h4">1</Typography>
                <Typography variant="body2" color="text.secondary">
                  Active users
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" color="primary">
                  Storage
                </Typography>
                <Typography variant="h4">0 MB</Typography>
                <Typography variant="body2" color="text.secondary">
                  Used storage
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box>
    </Container>
  );
};

export default Dashboard;
