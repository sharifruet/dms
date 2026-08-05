import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, useMediaQuery, useTheme } from '@mui/material';
import Sidebar from './components/Sidebar';
import MobileSidebar from './components/MobileSidebar';
import NotificationPermissionPrompt from './components/NotificationPermissionPrompt';
import Login from './pages/Login';
import ProcurementDashboard from './pages/procurement/ProcurementDashboard';
import PackageList from './pages/procurement/PackageList';
import PackageWorkspace from './pages/procurement/PackageWorkspace';
import ExpiryDashboard from './pages/procurement/ExpiryDashboard';
import ExceptionsDashboard from './pages/procurement/ExceptionsDashboard';
import Users from './pages/Users';
import Search from './pages/Search';
import Notifications from './pages/Notifications';
import Reports from './pages/Reports';
import DashboardManagement from './pages/DashboardManagement';
import Workflows from './pages/Workflows';
import Integrations from './pages/Integrations';
import AdvancedAnalytics from './pages/AdvancedAnalytics';
import MachineLearning from './pages/MachineLearning';
import SystemHealth from './pages/SystemHealth';
import Assets from './pages/Assets';
import AssetAssignments from './pages/AssetAssignments';
import DocumentTypeFields from './pages/DocumentTypeFields';
import StationeryTracking from './pages/StationeryTracking';
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
            path="/stationery" 
            element={isAuthenticated ? <StationeryTracking /> : <Navigate to="/login" />} 
          />
          {/* Procurement lifecycle - the primary workspace */}
          <Route
            path="/procurement"
            element={isAuthenticated ? <ProcurementDashboard /> : <Navigate to="/login" />}
          />
          <Route
            path="/procurement/packages"
            element={isAuthenticated ? <PackageList /> : <Navigate to="/login" />}
          />
          <Route
            path="/procurement/packages/:id"
            element={isAuthenticated ? <PackageWorkspace /> : <Navigate to="/login" />}
          />
          <Route
            path="/procurement/expiries"
            element={isAuthenticated ? <ExpiryDashboard /> : <Navigate to="/login" />}
          />
          <Route
            path="/procurement/exceptions"
            element={isAuthenticated ? <ExceptionsDashboard /> : <Navigate to="/login" />}
          />

          {/* Retired document-centric pages. Kept as redirects for one release so
              existing bookmarks and links keep working. */}
          <Route path="/dashboard" element={<Navigate to="/procurement" replace />} />
          <Route path="/documents" element={<Navigate to="/procurement/packages" replace />} />
          <Route path="/expiry-tracking" element={<Navigate to="/procurement/expiries" replace />} />
          <Route path="/app-entries" element={<Navigate to="/procurement/packages?stage=1" replace />} />
          <Route path="/bill-entries" element={<Navigate to="/procurement/packages?stage=13" replace />} />
          <Route path="/archive" element={<Navigate to="/procurement/packages?status=CLOSED" replace />} />
          <Route path="/versioning" element={<Navigate to="/procurement/packages" replace />} />

          <Route 
            path="/" 
            element={<Navigate to={isAuthenticated ? "/procurement" : "/login"} />} 
          />
        </Routes>
      </Box>
    </Box>
  );
}

export default App;
