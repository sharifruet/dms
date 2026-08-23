import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, useMediaQuery, useTheme } from '@mui/material';
import Sidebar from './components/Sidebar';
import MobileSidebar from './components/MobileSidebar';
import NotificationPermissionPrompt from './components/NotificationPermissionPrompt';
import Login from './pages/Login';
// The finance module stays independent of the Stage 13 invoice path (Q-6), so BillEntries
// is the one document-era page that was never scheduled for retirement.
import BillEntries from './pages/BillEntries';
import ExecutiveDashboard from './pages/ExecutiveDashboard';
import ProcurementDashboard from './pages/procurement/ProcurementDashboard';
import PackageList from './pages/procurement/PackageList';
import PackageWorkspace from './pages/procurement/PackageWorkspace';
import ExpiryDashboard from './pages/procurement/ExpiryDashboard';
import ExceptionsDashboard from './pages/procurement/ExceptionsDashboard';
import Users from './pages/Users';
import Search from './pages/Search';
import Notifications from './pages/Notifications';
import Reports from './pages/Reports';
import Integrations from './pages/Integrations';
import SystemHealth from './pages/SystemHealth';
import DocumentTypeFields from './pages/DocumentTypeFields';
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
          {/* Executive Dashboard — the governance-wide rollup. It carries its own
              topbar/status strip/footer because it is a port of the signed-off
              dboard/ mock-up and keeps that theme intact. */}
          <Route
            path="/executive"
            element={isAuthenticated ? <ExecutiveDashboard /> : <Navigate to="/login" />}
          />
          <Route
            path="/login"
            element={!isAuthenticated ? <Login /> : <Navigate to="/procurement" />}
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
            path="/reports" 
            element={isAuthenticated ? <Reports /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/integrations" 
            element={isAuthenticated ? <Integrations /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/health" 
            element={isAuthenticated ? <SystemHealth /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/document-type-fields" 
            element={isAuthenticated ? <DocumentTypeFields /> : <Navigate to="/login" />} 
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

          {/* The document-centric pages the procurement workspace supersedes are gone. The
              routes stay as redirects so existing bookmarks land somewhere sensible rather
              than 404 — the redirect targets are the ones named in the retirement map. */}
          <Route path="/dashboard" element={<Navigate to="/procurement" replace />} />
          <Route path="/documents" element={<Navigate to="/procurement/packages" replace />} />
          <Route path="/expiry-tracking" element={<Navigate to="/procurement/expiries" replace />} />
          <Route path="/app-entries" element={<Navigate to="/procurement/packages?stage=1" replace />} />
          <Route path="/archive" element={<Navigate to="/procurement/packages?status=closed" replace />} />
          <Route path="/versioning" element={<Navigate to="/procurement/packages" replace />} />

          {/* Retired with their modules — nothing in the procurement requirements asked for
              a generic workflow engine (Q-16), custom dashboards, the metric store or the ML
              features, so these do not redirect anywhere in particular. */}
          <Route path="/workflows" element={<Navigate to="/procurement" replace />} />
          <Route path="/dashboard-management" element={<Navigate to="/procurement" replace />} />
          <Route path="/analytics" element={<Navigate to="/procurement" replace />} />
          <Route path="/ml" element={<Navigate to="/procurement" replace />} />

          {/* Never in procurement scope (Assets, Assignments, Stationery). Hidden from
              the nav; bookmarks still land on procurement rather than 404. */}
          <Route path="/assets" element={<Navigate to="/procurement" replace />} />
          <Route path="/asset-assignments" element={<Navigate to="/procurement" replace />} />
          <Route path="/stationery" element={<Navigate to="/procurement" replace />} />

          {/* Not retired at any point (Q-6): the client keeps the finance module running
              independently of the Stage 13 invoice path. */}
          <Route
            path="/bill-entries"
            element={isAuthenticated ? <BillEntries /> : <Navigate to="/login" />}
          />

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
