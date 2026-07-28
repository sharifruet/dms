import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import {
  BusinessCenter as AppManagementIcon,
  Inventory2 as PackageIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import {
  procurementPackageService,
  ProcurementPackage,
} from '../services/procurementPackageService';
import { workflowService } from '../services/workflowService';

interface ProcurementPackageWithWorkflow extends ProcurementPackage {
  workflowId?: number | null;
  workflowStatus?: string | null;
  workflowInstanceId?: number | null;
  workflowInstanceStatus?: string | null;
}

const AppManagement: React.FC = () => {
  const navigate = useNavigate();
  const [packages, setPackages] = useState<ProcurementPackageWithWorkflow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadPackages = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await procurementPackageService.getPackages();
        const packagesWithWorkflowStatus = await Promise.all(
          (response.content || []).map(async (pkg) => {
            if (!pkg.packageNo) {
              return pkg;
            }

            try {
              const workflowStatus = await workflowService.getWorkflowStatusByPackageNo(pkg.packageNo);
              return {
                ...pkg,
                workflowId: workflowStatus.workflowId ?? null,
                workflowStatus: workflowStatus.workflowStatus ?? null,
                workflowInstanceId: workflowStatus.workflowInstanceId ?? null,
                workflowInstanceStatus: workflowStatus.workflowInstanceStatus ?? null,
              };
            } catch (workflowError) {
              return {
                ...pkg,
                workflowId: null,
                workflowStatus: null,
                workflowInstanceId: null,
                workflowInstanceStatus: null,
              };
            }
          })
        );

        setPackages(packagesWithWorkflowStatus);
      } catch (err: any) {
        setError(err.response?.data?.error || 'Failed to load APP management data');
      } finally {
        setLoading(false);
      }
    };

    loadPackages();
  }, []);

  const getStatusChipProps = (status?: string | null) => {
    switch (status) {
      case 'COMPLETED':
        return { label: 'Completed', backgroundColor: '#dcfce7', color: '#166534' };
      case 'IN_PROGRESS':
        return { label: 'In Progress', backgroundColor: '#dbeafe', color: '#1d4ed8' };
      case 'PENDING':
        return { label: 'Pending', backgroundColor: '#fef3c7', color: '#92400e' };
      case 'REJECTED':
        return { label: 'Rejected', backgroundColor: '#fee2e2', color: '#b91c1c' };
      case 'CANCELLED':
        return { label: 'Cancelled', backgroundColor: '#e5e7eb', color: '#374151' };
      default:
        return { label: 'No Workflow', backgroundColor: '#f3f4f6', color: '#6b7280' };
    }
  };

  return (
    <Box sx={{ p: { xs: 2, md: 4 } }}>
      <Box sx={{ mb: 4 }}>
        <Typography
          variant="h4"
          sx={{
            fontWeight: 700,
            fontSize: { xs: '1.5rem', md: '1.875rem' },
            color: '#111827',
            mb: 1,
            letterSpacing: '-0.02em',
          }}
        >
          APP Management
        </Typography>
        <Typography
          variant="body2"
          sx={{
            color: '#6b7280',
            fontSize: { xs: '0.875rem', md: '0.9375rem' },
          }}
        >
          Procurement package listing powered by the backend APP management API.
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card
        sx={{
          borderRadius: 3,
          boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.12)',
          border: '1px solid #f3f4f6',
          mb: 3,
        }}
      >
        <CardContent sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Box
              sx={{
                width: 52,
                height: 52,
                borderRadius: 2,
                backgroundColor: '#eff6ff',
                color: '#2563eb',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <AppManagementIcon />
            </Box>
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 600, color: '#111827' }}>
                APP Packages
              </Typography>
              <Typography variant="body2" sx={{ color: '#6b7280' }}>
                Showing package number, material description, method/type, source of fund, and the
                linked workflow status when available.
              </Typography>
            </Box>
          </Box>
        </CardContent>
      </Card>

      <Paper
        elevation={0}
        sx={{
          borderRadius: 3,
          border: '1px solid #e5e7eb',
          overflow: 'hidden',
        }}
      >
        {loading ? (
          <Box
            sx={{
              minHeight: 320,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <CircularProgress />
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow sx={{ backgroundColor: '#f9fafb' }}>
                  <TableCell sx={{ fontWeight: 700 }}>Package No</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Description of Materials</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Procurement Method and Types</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Source of fund</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {packages.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 8 }}>
                      <Box
                        sx={{
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: 'center',
                          gap: 1.5,
                          color: '#6b7280',
                        }}
                      >
                        <PackageIcon sx={{ fontSize: 36, color: '#9ca3af' }} />
                        <Typography variant="body1">No procurement packages found.</Typography>
                      </Box>
                    </TableCell>
                  </TableRow>
                ) : (
                  packages.map((pkg) => (
                    <TableRow
                      key={pkg.id}
                      hover
                      sx={{
                        '&:last-child td, &:last-child th': { border: 0 },
                      }}
                    >
                      <TableCell sx={{ fontWeight: 600, color: '#1f2937' }}>
                        {pkg.packageNo || '-'}
                      </TableCell>
                      <TableCell sx={{ color: '#374151', maxWidth: 420 }}>
                        <Box
                          sx={{
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                          }}
                        >
                          {pkg.description || '-'}
                        </Box>
                      </TableCell>
                      <TableCell sx={{ color: '#374151' }}>
                        {pkg.procurementMethod || '-'}
                      </TableCell>
                      <TableCell sx={{ color: '#374151' }}>
                        {pkg.sourceOfFund || '-'}
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1 }}>
                          <Chip
                            label={getStatusChipProps(pkg.workflowInstanceStatus).label}
                            size="small"
                            sx={{
                              backgroundColor: getStatusChipProps(pkg.workflowInstanceStatus).backgroundColor,
                              color: getStatusChipProps(pkg.workflowInstanceStatus).color,
                              fontWeight: 600,
                            }}
                          />
                          {pkg.workflowId ? (
                            <Button
                              size="small"
                              onClick={() => navigate(`/workflows?selected=${pkg.workflowId}`)}
                              sx={{ minWidth: 0, px: 0, textTransform: 'none' }}
                            >
                              View Workflow
                            </Button>
                          ) : null}
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>
    </Box>
  );
};

export default AppManagement;
