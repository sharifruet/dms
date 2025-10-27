import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Box } from '@mui/material';
import Navbar from './components/Navbar';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Documents from './pages/Documents';
import Users from './pages/Users';
import Search from './pages/Search';
import Notifications from './pages/Notifications';
import ExpiryTracking from './pages/ExpiryTracking';
import Reports from './pages/Reports';
import DashboardPage from './pages/DashboardPage';
import Workflows from './pages/Workflows';
import DocumentVersioning from './pages/DocumentVersioning';
import Integrations from './pages/Integrations';
import AdvancedAnalytics from './pages/AdvancedAnalytics';
import MachineLearning from './pages/MachineLearning';
import SystemHealth from './pages/SystemHealth';
import { useAppSelector } from './hooks/redux';

function App() {
  const { isAuthenticated } = useAppSelector((state) => state.auth);

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {isAuthenticated && <Navbar />}
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Routes>
          <Route 
            path="/login" 
            element={!isAuthenticated ? <Login /> : <Navigate to="/dashboard" />} 
          />
          <Route 
            path="/dashboard" 
            element={isAuthenticated ? <Dashboard /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/documents" 
            element={isAuthenticated ? <Documents /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/users" 
            element={isAuthenticated ? <Users /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/search" 
            element={isAuthenticated ? <Search /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/notifications" 
            element={isAuthenticated ? <Notifications /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/expiry-tracking" 
            element={isAuthenticated ? <ExpiryTracking /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/reports" 
            element={isAuthenticated ? <Reports /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/workflows" 
            element={isAuthenticated ? <Workflows /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/versioning" 
            element={isAuthenticated ? <DocumentVersioning /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/integrations" 
            element={isAuthenticated ? <Integrations /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/analytics" 
            element={isAuthenticated ? <AdvancedAnalytics /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/ml" 
            element={isAuthenticated ? <MachineLearning /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/health" 
            element={isAuthenticated ? <SystemHealth /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/" 
            element={<Navigate to={isAuthenticated ? "/dashboard" : "/login"} />} 
          />
        </Routes>
      </Box>
    </Box>
  );
}

export default App;
