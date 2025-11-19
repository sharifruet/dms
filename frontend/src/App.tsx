import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, useMediaQuery, useTheme } from '@mui/material';
import Sidebar from './components/Sidebar';
import MobileSidebar from './components/MobileSidebar';
import NotificationPermissionPrompt from './components/NotificationPermissionPrompt';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import DocumentsEnhanced from './pages/DocumentsEnhanced';
import Users from './pages/Users';
import Search from './pages/Search';
import Notifications from './pages/Notifications';
import ExpiryTracking from './pages/ExpiryTracking';
import Reports from './pages/Reports';
import DashboardPage from './pages/DashboardPage';
import DashboardManagement from './pages/DashboardManagement';
import Workflows from './pages/Workflows';
import DocumentVersioning from './pages/DocumentVersioning';
import Integrations from './pages/Integrations';
import AdvancedAnalytics from './pages/AdvancedAnalytics';
import MachineLearning from './pages/MachineLearning';
import SystemHealth from './pages/SystemHealth';
import Assets from './pages/Assets';
import AssetAssignments from './pages/AssetAssignments';
import DocumentTypeFields from './pages/DocumentTypeFields';
import Archive from './pages/Archive';
import StationeryTracking from './pages/StationeryTracking';
import AppEntries from './pages/AppEntries';
import BillEntries from './pages/BillEntries';
import { useAppSelector } from './hooks/redux';

function App() {
  const { isAuthenticated } = useAppSelector((state) => state.auth);
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', backgroundColor: '#ffffff' }}>
      {isAuthenticated && !isMobile && <Sidebar />}
      {isAuthenticated && isMobile && <MobileSidebar />}
      {isAuthenticated && <NotificationPermissionPrompt />}
      <Box 
        component="main" 
        sx={{ 
          flexGrow: 1, 
          ml: isAuthenticated && !isMobile ? '260px' : 0,
          minHeight: '100vh',
          backgroundColor: '#ffffff',
        }}
      >
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
            element={isAuthenticated ? <DocumentsEnhanced /> : <Navigate to="/login" />} 
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
            path="/assets" 
            element={isAuthenticated ? <Assets /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/asset-assignments" 
            element={isAuthenticated ? <AssetAssignments /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/reports" 
            element={isAuthenticated ? <Reports /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/dashboard-management" 
            element={isAuthenticated ? <DashboardManagement /> : <Navigate to="/login" />} 
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
            path="/document-type-fields" 
            element={isAuthenticated ? <DocumentTypeFields /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/archive" 
            element={isAuthenticated ? <Archive /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/stationery" 
            element={isAuthenticated ? <StationeryTracking /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/app-entries" 
            element={isAuthenticated ? <AppEntries /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/bill-entries" 
            element={isAuthenticated ? <BillEntries /> : <Navigate to="/login" />} 
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
